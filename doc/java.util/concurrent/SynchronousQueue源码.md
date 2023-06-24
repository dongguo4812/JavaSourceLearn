# **介绍**

SynchronousQueue作为**阻塞队列**的时候，对于每一个take的线程会**阻塞直到有一个put的线程放入元素**为止，反之亦然。在SynchronousQueue**内部没有任何存放元素的能力**。所以类似peek操作或者迭代器操作也是无效的，元素只能通过put类操作或者take类操作才有效。通常队列的**第一个元素**是当前**第一个等待的线程**。如果没有线程阻塞在该队列则poll会返回null。从Collection的视角来看SynchronousQueue表现为一个**空的集合**。

SynchronousQueue支持生产者和消费者等待的公平性策略。默认非公平锁情况下，不能保证生产消费的顺序。如果是公平锁的话可以保证当前第一个队首的线程是等待时间最长的线程，这时可以视SynchronousQueue为一个**FIFO队列**。

SynchronousQueue 在 MQ 中被大量使用

```java
public class SynchronousQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E>, java.io.Serializable
```

![image-20230624204447291](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306242044371.png)

# **常量&变量**

```java
    //序列化版本号
    private static final long serialVersionUID = -3223113410248163686L;
    
    /** The number of CPUs, for spin control */
    // CPU的数量
    static final int NCPUS = Runtime.getRuntime().availableProcessors();

    /**
     * The number of times to spin before blocking in timed waits.
     * The value is empirically derived -- it works well across a
     * variety of processors and OSes. Empirically, the best value
     * seems not to vary with number of CPUs (beyond 2) so is just
     * a constant.
     * 有超时的情况自旋多少次，当CPU数量小于2的时候不自旋
     */
    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

    /**
     * The number of times to spin before blocking in untimed waits.
     * This is greater than timed value because untimed waits spin
     * faster since they don't need to check times on each spin.
     * 没有超时的情况自旋多少次
     */
    static final int maxUntimedSpins = maxTimedSpins * 16;

    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices.
     * 针对有超时的情况，自旋了多少次后，如果剩余时间大于1000纳秒就使用带时间的LockSupport.parkNanos()这个方法
     */
    static final long spinForTimeoutThreshold = 1000L;
    
    /**
     * The transferer. Set only in constructor, but cannot be declared
     * as final without further complicating serialization.  Since
     * this is accessed only at most once per public method, there
     * isn't a noticeable performance penalty for using volatile
     * instead of final here.
     * 传输器，即两个线程交换元素使用的东西
     */
    private transient volatile Transferer<E> transferer;
```

# **构造方法**

```java
    /**
     * Creates a {@code SynchronousQueue} with nonfair access policy.
     * 无参构造器默认为非公平的
     */
    public SynchronousQueue() {
        this(false);
    }

    /**
     * Creates a {@code SynchronousQueue} with the specified fairness policy.
     *
     * @param fair if true, waiting threads contend in FIFO order for
     *        access; otherwise the order is unspecified.
     */
    public SynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }

```

# **内部类**

SynchronousQueue 底层结构和其它队列完全不同，有着独特的两种数据结构：队列和堆栈

## **Transferer**



**Transferer是**SynchronousQueue的内部抽象类，双栈和双队列算法共享该类，只提供一个transfer方法，用于转移元素，从生产者转移到消费者或从消费者调用该方法从生产者获取数据。

transfer方法的第一个参数e若不为空，相当于生产者put数据，当前线程需要等待消费者取走数据才可以返回

若为空，相当于消费者获取数据，如果没有数据可以获取就一直阻塞，

transfer方法返回值为空，代表超时或中断

```java
    /**
     * Shared internal API for dual stacks and queues.
     * 堆栈和队列共同的接口
     * 负责执行 put or take
     */
    abstract static class Transferer<E> {
        /**
         * Performs a put or take.
         *
         * @param e if non-null, the item to be handed to a consumer;
         *          if null, requests that transfer return an item
         *          offered by producer.
         * @param timed if this operation should timeout
         * @param nanos the timeout, in nanoseconds
         * @return if non-null, the item provided or received; if null,
         *         the operation failed due to timeout or interrupt --
         *         the caller can distinguish which of these occurred
         *         by checking Thread.interrupted.
         */
        // e 为空的，会直接返回特殊值，不为空会传递给消费者
        // timed 为 true，说明会有超时时间
        abstract E transfer(E e, boolean timed, long nanos);
    }
```

**Transfer有两个实现类TransferQUEUE和TransferStack**

## **TransferStack**

一个用于保存元素的栈，它支持同步和异步模式下的元素传输。

```java
    /** Dual stack */
    // 堆栈 后入先出 非公平
    // Scherer-Scott 算法
    static final class TransferStack<E> extends Transferer<E> {
        /*
         * This extends Scherer-Scott dual stack algorithm, differing,
         * among other ways, by using "covering" nodes rather than
         * bit-marked pointers: Fulfilling operations push on marker
         * nodes (with FULFILLING bit set in mode) to reserve a spot
         * to match a waiting node.
         */

        /* Modes for SNodes, ORed together in node fields */
        // 栈中节点的几种类型：
        /** Node represents an unfulfilled consumer */
        // 1. 消费者（请求数据的）
        static final int REQUEST    = 0;
        /** Node represents an unfulfilled producer */
        // 2. 生产者（提供数据的）
        static final int DATA       = 1;
        /** Node is fulfilling another unfulfilled DATA or REQUEST */
        // 3. 二者正在撮合中
        static final int FULFILLING = 2;

        /** Returns true if m has fulfilling bit set. */
        static boolean isFulfilling(int m) { return (m & FULFILLING) != 0; }

        /** Node class for TransferStacks. */
        // 栈中的节点
        static final class SNode {
            // 下一个节点
            volatile SNode next;        // next node in stack
            // 匹配者
            volatile SNode match;       // the node matched to this
            // 等待着的线程
            volatile Thread waiter;     // to control park/unpark
            // 元素
            Object item;                // data; or null for REQUESTs
            // 模式，也就是节点的类型，是消费者，是生产者，还是正在撮合中
            int mode;
            // Note: item and mode fields don't need to be volatile
            // since they are always written before, and read after,
            // other volatile/atomic operations.

            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(SNode cmp, SNode val) {
                return cmp == next &&
                    UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            /**
             * Tries to match node s to this node, if so, waking up thread.
             * Fulfillers call tryMatch to identify their waiters.
             * Waiters block until they have been matched.
             *
             * @param s the node to match
             * @return true if successfully matched to s
             * SNode里面的方向，调用者m是s的下一个节点
             * 这时候m节点的线程应该是阻塞状态的
             */
            boolean tryMatch(SNode s) {
                // 如果m还没有匹配者，就把s作为它的匹配者
                if (match == null &&
                    UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) {
                    Thread w = waiter;
                    if (w != null) {    // waiters need at most one unpark
                        waiter = null;
                        // 唤醒m中的线程，两者匹配完毕
                        LockSupport.unpark(w);
                    }
                    // 匹配到了返回true
                    return true;
                }
                // 可能其它线程先一步匹配了m，返回其是否是s
                return match == s;
            }

            /**
             * Tries to cancel a wait by matching node to itself.
             */
            void tryCancel() {
                UNSAFE.compareAndSwapObject(this, matchOffset, null, this);
            }

            boolean isCancelled() {
                return match == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long matchOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = SNode.class;
                    matchOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("match"));
                    nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** The head (top) of the stack */
        // 栈的头节点
        volatile SNode head;

        boolean casHead(SNode h, SNode nh) {
            return h == head &&
                UNSAFE.compareAndSwapObject(this, headOffset, h, nh);
        }

        /**
         * Creates or resets fields of a node. Called only from transfer
         * where the node to push on stack is lazily created and
         * reused when possible to help reduce intervals between reads
         * and CASes of head and to avoid surges of garbage when CASes
         * to push nodes fail due to contention.
         */
        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) s = new SNode(e);
            s.mode = mode;
            s.next = next;
            return s;
        }

        /**
         * Puts or takes an item.
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /*
             * Basic algorithm is to loop trying one of three actions:
             *
             * 1. If apparently empty or already containing nodes of same
             *    mode, try to push node on stack and wait for a match,
             *    returning it, or null if cancelled.
             *
             * 2. If apparently containing node of complementary mode,
             *    try to push a fulfilling node on to stack, match
             *    with corresponding waiting node, pop both from
             *    stack, and return matched item. The matching or
             *    unlinking might not actually be necessary because of
             *    other threads performing action 3:
             *
             * 3. If top of stack already holds another fulfilling node,
             *    help it out by doing its match and/or pop
             *    operations, and then continue. The code for helping
             *    is essentially the same as for fulfilling, except
             *    that it doesn't return the item.
             */

            SNode s = null; // constructed/reused as needed
            // 根据e是否为null决定是生产者还是消费者
            int mode = (e == null) ? REQUEST : DATA;
            // 自旋+CAS
            for (;;) {
                // 栈顶元素
                SNode h = head;
                // 栈顶没有元素，或者栈顶元素跟当前元素是一个模式的
                // 也就是都是生产者节点或者都是消费者节点
                if (h == null || h.mode == mode) {  // empty or same-mode
                    // 如果有超时而且已到期
                    if (timed && nanos <= 0) {      // can't wait
                        // 如果头节点不为空且是取消状态
                        if (h != null && h.isCancelled())
                            // 就把头节点弹出，并进入下一次循环
                            casHead(h, h.next);     // pop cancelled node
                        else
                            // 否则，直接返回null（超时返回null）
                            return null;
                    } else if (casHead(h, s = snode(s, e, h, mode))) {
                        // 入栈成功（因为是模式相同的，所以只能入栈）
                        // 调用awaitFulfill()方法自旋+阻塞当前入栈的线程并等待被匹配到
                        SNode m = awaitFulfill(s, timed, nanos);
                        // 如果m等于s，说明取消了，那么就把它清除掉，并返回null
                        if (m == s) {               // wait was cancelled
                            clean(s);
                            // 被取消了返回null
                            return null;
                        }
                        // 到这里说明匹配到元素了
                        // 因为从awaitFulfill()里面出来要不被取消了要不就匹配到了
                        // 如果头节点不为空，并且头节点的下一个节点是s
                        // 就把头节点换成s的下一个节点
                        // 也就是把h和s都弹出了
                        // 也就是把栈顶两个元素都弹出了
                        if ((h = head) != null && h.next == s)
                            casHead(h, s.next);     // help s's fulfiller
                        // 根据当前节点的模式判断返回m还是s中的值
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) { // try to fulfill
                    // 到这里说明头节点和当前节点模式不一样
                    // 如果头节点不是正在撮合中

                    // 如果头节点已经取消了，就把它弹出栈
                    if (h.isCancelled())            // already cancelled
                        casHead(h, h.next);         // pop and retry
                    else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                        // 头节点没有在撮合中，就让当前节点先入队，再让他们尝试匹配
                        // 且s成为了新的头节点，它的状态是正在撮合中
                        for (;;) { // loop until matched or waiters disappear
                            SNode m = s.next;       // m is s's match
                            // 如果m为null，说明除了s节点外的节点都被其它线程先一步撮合掉了
                            // 就清空栈并跳出内部循环，到外部循环再重新入栈判断
                            if (m == null) {        // all waiters are gone
                                casHead(s, null);   // pop fulfill node
                                s = null;           // use new node next time
                                break;              // restart main loop
                            }
                            SNode mn = m.next;
                            // 如果m和s尝试撮合成功，就弹出栈顶的两个元素m和s
                            if (m.tryMatch(s)) {
                                casHead(s, mn);     // pop both s and m
                                // 返回撮合结果
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else                  // lost match
                                // 尝试撮合失败，说明m已经先一步被其它线程撮合了
                                // 就协助清除它
                                s.casNext(m, mn);   // help unlink
                        }
                    }
                } else {                            // help a fulfiller
                    // 到这里说明当前节点和头节点模式不一样
                    // 且头节点是正在撮合中
                    SNode m = h.next;               // m is h's match
                    if (m == null)                  // waiter is gone
                        // 如果m为null，说明m已经被其它线程先一步撮合了
                        casHead(h, null);           // pop fulfilling node
                    else {
                        SNode mn = m.next;
                        // 协助匹配，如果m和s尝试撮合成功，就弹出栈顶的两个元素m和s
                        if (m.tryMatch(h))          // help match
                            // 将栈顶的两个元素弹出后，再让s重新入栈
                            casHead(h, mn);         // pop both h and m
                        else                        // lost match
                            // 尝试撮合失败，说明m已经先一步被其它线程撮合了
                            // 就协助清除它
                            h.casNext(m, mn);       // help unlink
                    }
                }
            }
        }

        /**
         * Spins/blocks until node s is matched by a fulfill operation.
         *
         * @param s the waiting node
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched node, or s if cancelled
         *  三个参数：需要等待的节点，是否需要超时，超时时间
         */
        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            /*
             * When a node/thread is about to block, it sets its waiter
             * field and then rechecks state at least one more time
             * before actually parking, thus covering race vs
             * fulfiller noticing that waiter is non-null so should be
             * woken.
             *
             * When invoked by nodes that appear at the point of call
             * to be at the head of the stack, calls to park are
             * preceded by spins to avoid blocking when producers and
             * consumers are arriving very close in time.  This can
             * happen enough to bother only on multiprocessors.
             *
             * The order of checks for returning out of main loop
             * reflects fact that interrupts have precedence over
             * normal returns, which have precedence over
             * timeouts. (So, on timeout, one last check for match is
             * done before giving up.) Except that calls from untimed
             * SynchronousQueue.{poll/offer} don't check interrupts
             * and don't wait at all, so are trapped in transfer
             * method rather than calling awaitFulfill.
             */
            // 到期时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            // 当前线程
            Thread w = Thread.currentThread();
            // 自旋次数
            int spins = (shouldSpin(s) ?
                         (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                // 当前线程中断了，尝试清除s
                if (w.isInterrupted())
                    s.tryCancel();
                // 检查s是否匹配到了元素m（有可能是其它线程的m匹配到当前线程的s）
                SNode m = s.match;
                // 如果匹配到了，直接返回m
                if (m != null)
                    return m;
                // 如果需要超时
                if (timed) {
                    // 检查超时时间如果小于0了，尝试清除s
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                if (spins > 0)
                    // 如果还有自旋次数，自旋次数减一，并进入下一次自旋
                    spins = shouldSpin(s) ? (spins-1) : 0;
                    // 后面的elseif都是自旋次数没有了
                else if (s.waiter == null)
                    // 如果s的waiter为null，把当前线程注入进去，并进入下一次自旋
                    s.waiter = w; // establish waiter so can park next iter
                else if (!timed)
                    // 如果不允许超时，直接阻塞，并等待被其它线程唤醒，唤醒后继续自旋并查看是否匹配到了元素
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    // 如果允许超时且还有剩余时间，就阻塞相应时间
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * Returns true if node s is at head or there is an active
         * fulfiller.
         */
        boolean shouldSpin(SNode s) {
            SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }

        /**
         * Unlinks s from the stack.
         */
        void clean(SNode s) {
            s.item = null;   // forget item
            s.waiter = null; // forget thread

            /*
             * At worst we may need to traverse entire stack to unlink
             * s. If there are multiple concurrent calls to clean, we
             * might not see s if another thread has already removed
             * it. But we can stop when we see any node known to
             * follow s. We use s.next unless it too is cancelled, in
             * which case we try the node one past. We don't check any
             * further because we don't want to doubly traverse just to
             * find sentinel.
             */

            SNode past = s.next;
            if (past != null && past.isCancelled())
                past = past.next;

            // Absorb cancelled nodes at head
            SNode p;
            while ((p = head) != null && p != past && p.isCancelled())
                casHead(p, p.next);

            // Unsplice embedded nodes
            while (p != null && p != past) {
                SNode n = p.next;
                if (n != null && n.isCancelled())
                    p.casNext(n, n.next);
                else
                    p = n;
            }
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferStack.class;
                headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
```

## **TransferQueue**

**TransferQueue是公平的， **一个扩展的TransferQueue接口，用于支持异步和同步模式下的元素传输。

**TransferQueue是Scherer-Scott双列算法的扩展，使用内部节点来代替标记指针**

```java
    /** Dual Queue */
    //队列 先入先出 公平
    static final class TransferQueue<E> extends Transferer<E> {
        /*
         * This extends Scherer-Scott dual queue algorithm, differing,
         * among other ways, by using modes within nodes rather than
         * marked pointers. The algorithm is a little simpler than
         * that for stacks because fulfillers do not need explicit
         * nodes, and matching is done by CAS'ing QNode.item field
         * from non-null to null (for put) or vice versa (for take).
         */

        /** Node class for TransferQueue. */
        // 队列中的节点
        static final class QNode {
            // 下一个节点
            volatile QNode next;          // next node in queue
            // 存储的元素
            volatile Object item;         // CAS'ed to or from null
            // 等待着的线程
            volatile Thread waiter;       // to control park/unpark
            // 是否是数据节点
            final boolean isData;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                return next == cmp &&
                    UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean casItem(Object cmp, Object val) {
                return item == cmp &&
                    UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }

            /**
             * Tries to cancel by CAS'ing ref to this as item.
             */
            void tryCancel(Object cmp) {
                UNSAFE.compareAndSwapObject(this, itemOffset, cmp, this);
            }

            boolean isCancelled() {
                return item == this;
            }

            /**
             * Returns true if this node is known to be off the queue
             * because its next pointer has been forgotten due to
             * an advanceHead operation.
             */
            boolean isOffList() {
                return next == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** Head of queue */
        // 队列的头节点
        transient volatile QNode head;
        /** Tail of queue */
        // 队列的尾节点
        transient volatile QNode tail;
        /**
         * Reference to a cancelled node that might not yet have been
         * unlinked from queue because it was the last inserted node
         * when it was cancelled.
         * 指向已经取消的节点，这个节点可能还没有从队列中取消连接，因为他是取消时最后一个插入的节点
         */
        transient volatile QNode cleanMe;

        TransferQueue() {
            QNode h = new QNode(null, false); // initialize to dummy node.
            head = h;
            tail = h;
        }

        /**
         * Tries to cas nh as new head; if successful, unlink
         * old head's next node to avoid garbage retention.
         */
        void advanceHead(QNode h, QNode nh) {
            if (h == head &&
                UNSAFE.compareAndSwapObject(this, headOffset, h, nh))
                h.next = h; // forget old next
        }

        /**
         * Tries to cas nt as new tail.
         */
        void advanceTail(QNode t, QNode nt) {
            if (tail == t)
                UNSAFE.compareAndSwapObject(this, tailOffset, t, nt);
        }

        /**
         * Tries to CAS cleanMe slot.
         */
        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp &&
                UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }

        /**
         * Puts or takes an item.
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /* Basic algorithm is to loop trying to take either of
             * two actions:
             *
             * 1. If queue apparently empty or holding same-mode nodes,
             *    try to add node to queue of waiters, wait to be
             *    fulfilled (or cancelled) and return matching item.
             *
             * 2. If queue apparently contains waiting items, and this
             *    call is of complementary mode, try to fulfill by CAS'ing
             *    item field of waiting node and dequeuing it, and then
             *    returning matching item.
             *
             * In each case, along the way, check for and try to help
             * advance head and tail on behalf of other stalled/slow
             * threads.
             *
             * The loop starts off with a null check guarding against
             * seeing uninitialized head or tail values. This never
             * happens in current SynchronousQueue, but could if
             * callers held non-volatile/final ref to the
             * transferer. The check is here anyway because it places
             * null checks at top of loop, which is usually faster
             * than having them implicitly interspersed.
             */

            QNode s = null; // constructed/reused as needed
            boolean isData = (e != null);

            for (;;) {
                QNode t = tail;
                QNode h = head;
                if (t == null || h == null)         // saw uninitialized value
                    continue;                       // spin

                if (h == t || t.isData == isData) { // empty or same-mode
                    QNode tn = t.next;
                    if (t != tail)                  // inconsistent read
                        continue;
                    if (tn != null) {               // lagging tail
                        advanceTail(t, tn);
                        continue;
                    }
                    if (timed && nanos <= 0)        // can't wait
                        return null;
                    if (s == null)
                        s = new QNode(e, isData);
                    if (!t.casNext(null, s))        // failed to link in
                        continue;

                    advanceTail(t, s);              // swing tail and wait
                    Object x = awaitFulfill(s, e, timed, nanos);
                    if (x == s) {                   // wait was cancelled
                        clean(t, s);
                        return null;
                    }

                    if (!s.isOffList()) {           // not already unlinked
                        advanceHead(t, s);          // unlink if head
                        if (x != null)              // and forget fields
                            s.item = s;
                        s.waiter = null;
                    }
                    return (x != null) ? (E)x : e;

                } else {                            // complementary-mode
                    QNode m = h.next;               // node to fulfill
                    if (t != tail || m == null || h != head)
                        continue;                   // inconsistent read

                    Object x = m.item;
                    if (isData == (x != null) ||    // m already fulfilled
                        x == m ||                   // m cancelled
                        !m.casItem(x, e)) {         // lost CAS
                        advanceHead(h, m);          // dequeue and retry
                        continue;
                    }

                    advanceHead(h, m);              // successfully fulfilled
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E)x : e;
                }
            }
        }

        /**
         * Spins/blocks until node s is fulfilled.
         *
         * @param s the waiting node
         * @param e the comparison value for checking match
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched item, or s if cancelled
         */
        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            /* Same idea as TransferStack.awaitFulfill */
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = ((head.next == s) ?
                         (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                if (w.isInterrupted())
                    s.tryCancel(e);
                Object x = s.item;
                if (x != e)
                    return x;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                if (spins > 0)
                    --spins;
                else if (s.waiter == null)
                    s.waiter = w;
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * Gets rid of cancelled node s with original predecessor pred.
         */
        void clean(QNode pred, QNode s) {
            s.waiter = null; // forget thread
            /*
             * At any given time, exactly one node on list cannot be
             * deleted -- the last inserted node. To accommodate this,
             * if we cannot delete s, we save its predecessor as
             * "cleanMe", deleting the previously saved version
             * first. At least one of node s or the node previously
             * saved can always be deleted, so this always terminates.
             */
            while (pred.next == s) { // Return early if already unlinked
                QNode h = head;
                QNode hn = h.next;   // Absorb cancelled first node as head
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                QNode t = tail;      // Ensure consistent read for tail
                if (t == h)
                    return;
                QNode tn = t.next;
                if (t != tail)
                    continue;
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                if (s != t) {        // If not tail, try to unsplice
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn))
                        return;
                }
                QNode dp = cleanMe;
                if (dp != null) {    // Try unlinking previous cancelled node
                    QNode d = dp.next;
                    QNode dn;
                    if (d == null ||               // d is gone or
                        d == dp ||                 // d is off list or
                        !d.isCancelled() ||        // d not cancelled or
                        (d != t &&                 // d not tail and
                         (dn = d.next) != null &&  //   has successor
                         dn != d &&                //   that is on list
                         dp.casNext(d, dn)))       // d unspliced
                        casCleanMe(dp, null);
                    if (dp == pred)
                        return;      // s is already saved node
                } else if (casCleanMe(null, pred))
                    return;          // Postpone cleaning s
            }
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferQueue.class;
                headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
                tailOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("tail"));
                cleanMeOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("cleanMe"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
```

## **WaitQueue/LifoWaitQueue/FifoWaitQueue**

WaitQueue：一个用于等待元素传输的队列，它包含了等待传输的线程。

LifoWaitQueue： 一个后进先出（LIFO）的等待队列 ，用于在多线程中实现等待和通知机制

FifoWaitQueue： 它是一个先进先出的等待队列，通常用于操作系统中实现线程同步

```java
    static class WaitQueue implements java.io.Serializable { }
    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;
    }
    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;
    }
```

# **常用方法**

## **入队**put(E e)

这里主要介绍以栈方式实现的传输模式，以put(E e)方法为例



SynchronousQueue的put方法是一种阻塞方法，主要实现将一个元素插入到队列中。该方法将阻塞调用线程，直到另外一个线程取走该元素，才会返回。

```java
    /**
     * Adds the specified element to this queue, waiting if necessary for
     * another thread to receive it.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        // 元素不可为空
        if (e == null) throw new NullPointerException();
        // 直接调用传输器的transfer()方法
        // 三个参数分别是：传输的元素，是否需要超时，超时的时间
        if (transferer.transfer(e, false, 0) == null) {
            // 如果传输失败，直接让线程中断并抛出中断异常
            Thread.interrupted();
            throw new InterruptedException();
        }
    }
```

1. 首先会利用CAS操作尝试将元素插入到队列中。
2. 如果队列中已经存在一个元素了，那么就会将当前线程加入到等待队列中，然后阻塞当前线程，直到有其他线程取走了该元素。
3. 如果队列中还没有元素，那么就会返回。



调用transferer的transfer()方法，传入元素e，说明是生产者

## **出队**take()

这里主要介绍以栈方式实现的传输模式，以take()方法为例



SynchronousQueue的take方法是一个阻塞方法，它会一直等待直到有另一个线程调用SynchronousQueue的put方法才能返回。如果没有其他线程调用put方法，那么调用take方法的线程会一直处于等待状态。

```java
    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * for another thread to insert it.
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        // 直接调用传输器的transfer()方法
        // 三个参数分别是：null，是否需要超时，超时的时间
        // 第一个参数为null表示是消费者，要取元素
        E e = transferer.transfer(null, false, 0);
        // 如果取到了元素就返回
        if (e != null)
            return e;
        // 否则让线程中断并抛出中断异常
        Thread.interrupted();
        throw new InterruptedException();
    }
```

整个逻辑比较复杂，这里为了简单起见，屏蔽掉多线程处理的细节，只描述正常业务场景下的逻辑：

（1）如果栈中没有元素，或者栈顶元素跟将要入栈的元素模式一样，就入栈；

（2）入栈后自旋等待一会看有没有其它线程匹配到它，自旋完了还没匹配到元素就阻塞等待；

（3）阻塞等待被唤醒了说明其它线程匹配到了当前的元素，就返回匹配到的元素；

（4）如果两者模式不一样，且头节点没有在匹配中，就拿当前节点跟它匹配，匹配成功了就返回匹配到的元素；

（5）如果两者模式不一样，且头节点正在匹配中，当前线程就协助去匹配，匹配完成了再让当前节点重新入栈重新匹配；



### **transfer()**

transfer()方法用于将一个元素交换给另一个线程，

以栈方式TransferStack.transfer()

```java
        /**
         * Puts or takes an item.
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /*
             * Basic algorithm is to loop trying one of three actions:
             *
             * 1. If apparently empty or already containing nodes of same
             *    mode, try to push node on stack and wait for a match,
             *    returning it, or null if cancelled.
             *
             * 2. If apparently containing node of complementary mode,
             *    try to push a fulfilling node on to stack, match
             *    with corresponding waiting node, pop both from
             *    stack, and return matched item. The matching or
             *    unlinking might not actually be necessary because of
             *    other threads performing action 3:
             *
             * 3. If top of stack already holds another fulfilling node,
             *    help it out by doing its match and/or pop
             *    operations, and then continue. The code for helping
             *    is essentially the same as for fulfilling, except
             *    that it doesn't return the item.
             */

            SNode s = null; // constructed/reused as needed
            // 根据e是否为null决定是生产者还是消费者
            int mode = (e == null) ? REQUEST : DATA;
            // 自旋+CAS
            for (;;) {
                // 栈顶元素
                SNode h = head;
                // 栈顶没有元素，或者栈顶元素跟当前元素是一个模式的
                // 也就是都是生产者节点或者都是消费者节点
                if (h == null || h.mode == mode) {  // empty or same-mode
                    // 如果有超时而且已到期
                    if (timed && nanos <= 0) {      // can't wait
                        // 如果头节点不为空且是取消状态
                        if (h != null && h.isCancelled())
                            // 就把头节点弹出，并进入下一次循环
                            casHead(h, h.next);     // pop cancelled node
                        else
                            // 否则，直接返回null（超时返回null）
                            return null;
                    } else if (casHead(h, s = snode(s, e, h, mode))) {
                        // 入栈成功（因为是模式相同的，所以只能入栈）
                        // 调用awaitFulfill()方法自旋+阻塞当前入栈的线程并等待被匹配到
                        SNode m = awaitFulfill(s, timed, nanos);
                        // 如果m等于s，说明取消了，那么就把它清除掉，并返回null
                        if (m == s) {               // wait was cancelled
                            clean(s);
                            // 被取消了返回null
                            return null;
                        }
                        // 到这里说明匹配到元素了
                        // 因为从awaitFulfill()里面出来要不被取消了要不就匹配到了
                        // 如果头节点不为空，并且头节点的下一个节点是s
                        // 就把头节点换成s的下一个节点
                        // 也就是把h和s都弹出了
                        // 也就是把栈顶两个元素都弹出了
                        if ((h = head) != null && h.next == s)
                            casHead(h, s.next);     // help s's fulfiller
                        // 根据当前节点的模式判断返回m还是s中的值
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) { // try to fulfill
                    // 到这里说明头节点和当前节点模式不一样
                    // 如果头节点不是正在撮合中

                    // 如果头节点已经取消了，就把它弹出栈
                    if (h.isCancelled())            // already cancelled
                        casHead(h, h.next);         // pop and retry
                    else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                        // 头节点没有在撮合中，就让当前节点先入队，再让他们尝试匹配
                        // 且s成为了新的头节点，它的状态是正在撮合中
                        for (;;) { // loop until matched or waiters disappear
                            SNode m = s.next;       // m is s's match
                            // 如果m为null，说明除了s节点外的节点都被其它线程先一步撮合掉了
                            // 就清空栈并跳出内部循环，到外部循环再重新入栈判断
                            if (m == null) {        // all waiters are gone
                                casHead(s, null);   // pop fulfill node
                                s = null;           // use new node next time
                                break;              // restart main loop
                            }
                            SNode mn = m.next;
                            // 如果m和s尝试撮合成功，就弹出栈顶的两个元素m和s
                            if (m.tryMatch(s)) {
                                casHead(s, mn);     // pop both s and m
                                // 返回撮合结果
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else                  // lost match
                                // 尝试撮合失败，说明m已经先一步被其它线程撮合了
                                // 就协助清除它
                                s.casNext(m, mn);   // help unlink
                        }
                    }
                } else {                            // help a fulfiller
                    // 到这里说明当前节点和头节点模式不一样
                    // 且头节点是正在撮合中
                    SNode m = h.next;               // m is h's match
                    if (m == null)                  // waiter is gone
                        // 如果m为null，说明m已经被其它线程先一步撮合了
                        casHead(h, null);           // pop fulfilling node
                    else {
                        SNode mn = m.next;
                        // 协助匹配，如果m和s尝试撮合成功，就弹出栈顶的两个元素m和s
                        if (m.tryMatch(h))          // help match
                            // 将栈顶的两个元素弹出后，再让s重新入栈
                            casHead(h, mn);         // pop both h and m
                        else                        // lost match
                            // 尝试撮合失败，说明m已经先一步被其它线程撮合了
                            // 就协助清除它
                            h.casNext(m, mn);       // help unlink
                    }
                }
            }
        }
```

1. 如果有一个线程正在等待接收元素，那么将元素直接交换给该线程，方法立即返回。
2. 如果没有线程正在等待接收元素，则当前线程将被阻塞，直到有另一个线程来获取该元素。
3. 如果当前线程被中断，则方法将抛出InterruptedException异常。



#### **awaitFulfill(SNode s,** **boolean** **timed, long nanos)**

保证每个插入操作都会等待一个对应的删除操作，每个删除操作也会等待一个对应的插入操作。

```java
        /**
         * Spins/blocks until node s is matched by a fulfill operation.
         *
         * @param s the waiting node
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched node, or s if cancelled
         *  三个参数：需要等待的节点，是否需要超时，超时时间
         */
        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            /*
             * When a node/thread is about to block, it sets its waiter
             * field and then rechecks state at least one more time
             * before actually parking, thus covering race vs
             * fulfiller noticing that waiter is non-null so should be
             * woken.
             *
             * When invoked by nodes that appear at the point of call
             * to be at the head of the stack, calls to park are
             * preceded by spins to avoid blocking when producers and
             * consumers are arriving very close in time.  This can
             * happen enough to bother only on multiprocessors.
             *
             * The order of checks for returning out of main loop
             * reflects fact that interrupts have precedence over
             * normal returns, which have precedence over
             * timeouts. (So, on timeout, one last check for match is
             * done before giving up.) Except that calls from untimed
             * SynchronousQueue.{poll/offer} don't check interrupts
             * and don't wait at all, so are trapped in transfer
             * method rather than calling awaitFulfill.
             */
            // 到期时间
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            // 当前线程
            Thread w = Thread.currentThread();
            // 自旋次数
            int spins = (shouldSpin(s) ?
                         (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                // 当前线程中断了，尝试清除s
                if (w.isInterrupted())
                    s.tryCancel();
                // 检查s是否匹配到了元素m（有可能是其它线程的m匹配到当前线程的s）
                SNode m = s.match;
                // 如果匹配到了，直接返回m
                if (m != null)
                    return m;
                // 如果需要超时
                if (timed) {
                    // 检查超时时间如果小于0了，尝试清除s
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                if (spins > 0)
                    // 如果还有自旋次数，自旋次数减一，并进入下一次自旋
                    spins = shouldSpin(s) ? (spins-1) : 0;
                    // 后面的elseif都是自旋次数没有了
                else if (s.waiter == null)
                    // 如果s的waiter为null，把当前线程注入进去，并进入下一次自旋
                    s.waiter = w; // establish waiter so can park next iter
                else if (!timed)
                    // 如果不允许超时，直接阻塞，并等待被其它线程唤醒，唤醒后继续自旋并查看是否匹配到了元素
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    // 如果允许超时且还有剩余时间，就阻塞相应时间
                    LockSupport.parkNanos(this, nanos);
            }
        }
```

awaitFulfill方法是SynchronousQueue内部用于等待删除操作的方法，它会阻塞当前线程，直到有对应的插入操作到达。具体实现中，awaitFulfill方法会调用LockSupport.park方法阻塞当前线程，并将当前线程加入到一个等待队列中，等待对应的插入操作。

当有插入操作到达时，SynchronousQueue内部会调用fulfill方法，该方法会将等待队列中的线程唤醒，使它们可以继续执行。同时，fulfill方法也会返回插入操作的元素，以便删除操作使用。

需要注意的是，SynchronousQueue不允许存放null元素，否则会抛出NullPointerException。awaitFulfill方法是SynchronousQueue内部用于等待删除操作的方法，它会阻塞当前线程，直到有对应的插入操作到达。具体实现中，awaitFulfill方法会调用LockSupport.park方法阻塞当前线程，并将当前线程加入到一个等待队列中，等待对应的插入操作。

当有插入操作到达时，SynchronousQueue内部会调用fulfill方法，该方法会将等待队列中的线程唤醒，使它们可以继续执行。同时，fulfill方法也会返回插入操作的元素，以便删除操作使用。

需要注意的是，SynchronousQueue不允许存放null元素，否则会抛出NullPointerException。



#### **tryMatch(SNode s)**

SynchronousQueue是一个特殊的阻塞队列，其put和take操作必须配对才能成功。在SynchronousQueue中，tryMatch方法用于尝试将一个生产者和一个消费者匹配起来，如果匹配成功，则返回true，否则返回false。

```java
            /**
             * Tries to match node s to this node, if so, waking up thread.
             * Fulfillers call tryMatch to identify their waiters.
             * Waiters block until they have been matched.
             *
             * @param s the node to match
             * @return true if successfully matched to s
             * SNode里面的方向，调用者m是s的下一个节点
             * 这时候m节点的线程应该是阻塞状态的
             */
            boolean tryMatch(SNode s) {
                // 如果m还没有匹配者，就把s作为它的匹配者
                if (match == null &&
                    UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) {
                    Thread w = waiter;
                    if (w != null) {    // waiters need at most one unpark
                        waiter = null;
                        // 唤醒m中的线程，两者匹配完毕
                        LockSupport.unpark(w);
                    }
                    // 匹配到了返回true
                    return true;
                }
                // 可能其它线程先一步匹配了m，返回其是否是s
                return match == s;
            }
```

该方法中主要有两个部分：

1. 尝试匹配消费者和生产者：

从队首开始搜索一个等待消费者的节点h，如果找到了一个等待消费者的节点，则将当前节点node的前驱节点设置为h，并将node作为新的队首节点的后继节点。然后唤醒h线程以完成匹配。如果匹配成功，则返回true，否则返回false。

从队尾开始搜索一个等待生产者的节点t，如果找到了一个等待生产者的节点，则将当前节点node的后继节点设置为t，并将node作为新的队尾节点的后继节点。然后唤醒t线程以完成匹配。如果匹配成功，则返回true，否则返回false。

 2.清除已取消的节点：

在匹配的过程中，如果发现某个节点已经被取消了，就需要将其从队列中删除。对于已取消的节点，其前驱节点的后继节点需要跳过该节点，直到找到一个未取消的节点。在这个过程中，如果发现队首或队尾已经发生了变化，则需要重新从队首或队尾开始搜索。如果所有已取消的节点都清除完了，但匹配还没有成功，则返回false。









