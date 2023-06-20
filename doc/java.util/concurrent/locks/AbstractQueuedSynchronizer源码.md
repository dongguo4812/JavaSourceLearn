# **介绍**

基于队列的抽象同步器，它是jdk中所有显示的线程同步工具的基础，像ReentrantLock/DelayQueue/CountdownLatch等等，都是借助AQS实现的。

```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {
```

![image-20230620194452634](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306201945685.png)

# **内部类**

## **Node**

AQS中的Node内部类的作用就是以队列的方式来存放线程节点

```java
    /**
     * Wait queue node class.
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     *
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     */
    static final class Node {
        /** Marker to indicate a node is waiting in shared mode */
        //用来表示在共享模式下等待的标记
        static final Node SHARED = new Node();
        /** Marker to indicate a node is waiting in exclusive mode */
        //用来表示在独占模式下等待的标记
        static final Node EXCLUSIVE = null;

        /** waitStatus value to indicate thread has cancelled */
        //waitStatus 的值，用来表示线程已经被取消
        static final int CANCELLED =  1;
        /** waitStatus value to indicate successor's thread needs unparking */
        //waitStatus的值，用来表示后继线程都需要被挂起
        static final int SIGNAL    = -1;
        /** waitStatus value to indicate thread is waiting on condition */
        //waitStatus 的值，表示线程在一个条件上等待
        static final int CONDITION = -2;
        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         * waitStatus 的值，表示下一个acquireShared应该无条件传播
         */
        static final int PROPAGATE = -3;

        /**
         * Status field, taking on only the values:
         *   SIGNAL:     The successor of this node is (or will soon be)
         *               blocked (via park), so the current node must
         *               unpark its successor when it releases or
         *               cancels. To avoid races, acquire methods must
         *               first indicate they need a signal,
         *               then retry the atomic acquire, and then,
         *               on failure, block.
         *   CANCELLED:  This node is cancelled due to timeout or interrupt.
         *               Nodes never leave this state. In particular,
         *               a thread with cancelled node never again blocks.
         *   CONDITION:  This node is currently on a condition queue.
         *               It will not be used as a sync queue node
         *               until transferred, at which time the status
         *               will be set to 0. (Use of this value here has
         *               nothing to do with the other uses of the
         *               field, but simplifies mechanics.)
         *   PROPAGATE:  A releaseShared should be propagated to other
         *               nodes. This is set (for head node only) in
         *               doReleaseShared to ensure propagation
         *               continues, even if other operations have
         *               since intervened.
         *   0:          None of the above
         *
         * The values are arranged numerically to simplify use.
         * Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular
         * values, just for sign.
         *
         * The field is initialized to 0 for normal sync nodes, and
         * CONDITION for condition nodes.  It is modified using CAS
         * (or when possible, unconditional volatile writes).
         * 等待状态值 仅接收以上4个值和默认的0
         */
        volatile int waitStatus;

        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         * 前驱结点
         */
        volatile Node prev;

        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         * 后继节点
         */
        volatile Node next;

        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         * 当前线程
         */
        volatile Thread thread;

        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         * 下一个条件等待节点
         */
        Node nextWaiter;

        /**
         * Returns true if node is waiting in shared mode.
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         *
         * @return the predecessor of this node
         * 获取当前节点的前驱节点
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker
        }

        //用于addWaiter方法
        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }
        //用于Condition中使用
        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

```

同步队列中Node应该长下面这个样子：

![image-20230620194533854](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306201945915.png)

## **ConditionObject**

ConditionObject实现了在基于AQS锁的情况下对获取到锁的线程进行有条件的等待和唤醒，其主要的方法是await和signal以及它们的变种。

ConditionObject是专门为AQS服务的，它的节点的构造，状态的标志等都与AQS有关，在wait操作和signal操作时都需要去操作AQS的同步队列。

```java
    /**
     * Condition implementation for a {@link
     * AbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     *
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        //序列化版本号
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        //等待队列中第一个节点
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        //等待队列中最后一个节点
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * Adds a new waiter to wait queue.
         * @return its new wait node
         * 向Condition队列添加等待者
         */
        //将当前线程封装为节点并设置为CONDITION加入到Condition队列中，这里如果lastWaiter不为CONDITION状态，那么会把它踢出Condition队列;
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            //检查节点的有效性，遍历队列,将状态不为CONDITION的节点剔除出队列
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            //将当前线程封装成节点并且设置为CONDITION加入到Condition队列中去
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            //尾节点为空则表明队列为空，将新节点设置为头节点
            if (t == null)
                firstWaiter = node;
            //尾节点不为空则表明队列不为空，将新节点设置为尾节点的后续节点
            else
                t.nextWaiter = node;
            //将新节点设置为尾节点
            lastWaiter = node;
            return node;
        }

        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * @param first (non-null) the first node on condition queue
         *              唤醒等待队列中的第一个线程节点，
         *              成功则返回
         *              失败则尝试唤醒下一个线程节点
         */
        private void doSignal(Node first) {
            do {
                //新的头节点为空则，队列为空
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                //断开头节点与队列的连接
                first.nextWaiter = null;
             //将头节点从等待队列中移除
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

        /**
         * Removes and transfers all nodes.
         * @param first (non-null) the first node on condition queue
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         * 检查逻辑
         * 从等待队列的头节点开始遍历，如果头节点的waitStatus != Node.CONDITION则将firstWaiter的引用指向头结点的下一个节点，
         * 则该下一个节点就变成了新的头节点，如此往复进行
         */
        private void unlinkCancelledWaiters() {
            //等待队列的头节点
            Node t = firstWaiter;
            //trail用于记录上一个有效的节点
            Node trail = null;
            //从头节点开始遍历
            while (t != null) {
                Node next = t.nextWaiter;
                //当前线程的等待状态不是 Node.CONDITION等待状态
                if (t.waitStatus != Node.CONDITION) {
                    //断开与下一个节点的连接 t独立出来便于GC回收
                    t.nextWaiter = null;
                    if (trail == null)
                        //将头节点的下一个节点设置为头节点
                        firstWaiter = next;
                    else
                        //通过nextWaiter连接到队列中
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    //有效节点
                    trail = t;
                //遍历下一个节点
                t = next;
            }
        }

        // public methods

        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         * signal的作用就是将await中Condition队列的第一个节点唤醒；
         */
        public final void signal() {
            //isHeldExclusively是需要子类继承的，在lock中判断当前线程是否是获得锁的线程,是则返回true，如果当前线程不是获取锁的线程则抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            //获取Condition队列中第一个Node
            Node first = firstWaiter;
            //队首的节点不为null
            if (first != null)
                //唤醒队首的节点
                doSignal(first);
        }

        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         *  检查Condition队列中节点在等待过程中的中断状态
         *  THROW_IE：表示在signal之前被中断唤醒
         *  REINTERRUPT：表示在signal之后有中断，在singnal之后被通断，需要保证singnal的行为最终完成，所以中断只用延续状态状态REINTERRUPT，不用抛出异常。
         */
        private int checkInterruptWhileWaiting(Node node) {
                     //中断状态
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        private void reportInterruptAfterWait(int interruptMode)
            throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         *等候机制： 持有锁的线程调用await方法将会主动放弃锁，将其加入等待队列中
         *  等候机制的操作分为如下几步：
         *  1.调用addConditionWaiter方法，将节点添加到等候队列
         *  2.调用fullyRelease方法，释放锁机制
         *  3.while循环，判断当前节点是否在同步队列中，如果不在则挂起当前线程
         *  4.获取锁，并判断线程是否中断
         */
        public final void await() throws InterruptedException {
            //await()方法对中断敏感，线程中断为true时调用await()就会抛出异常
            if (Thread.interrupted())
                throw new InterruptedException();
            //将当前线程封装成节点并且设置为CONDITION加入到Condition队列中去，这里如果lastWaiter不为CONDITION状态，那么会把它踢出Condition队列。
            Node node = addConditionWaiter();
            //释放node节点线程的锁， 锁可能有重入，所以这里不是直接调用AQS的release方法，详情见fullyRelease说明
            int savedState = fullyRelease(node);
            //标记线程在await过程中的中断转态，0表示未中断
            int interruptMode = 0;
            //判断节点是否在同步队列中，只有node节点进入了同步队列循环才会结束（即，被signal了）
            while (!isOnSyncQueue(node)) {
                //不再同步队列中，则使用LockSupport.park将其挂起
                LockSupport.park(this);
                //线程被唤醒或者中断后，判断线程的中断状态
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    //中断则推出循环
                    break;
            }
            //线程被唤醒后重新获取锁，锁状态恢复到savedState
            //不管线程是：未中断，还是signal中断，singnal后中断，前面的代码 都会保证node节点进入同步队列。
            //acquireQueued 方法获取到锁，并且在获取锁park的过程中有被中断，并且之前在await过程中，不是被signal之前就中断的情况，则标记后续处理中断的情况为interruptMode。
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            //重新获取到锁，把节点从condition队列中去除，同时也会清除被取消的节点
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            //线程被中断，根据中断条件选择抛出异常或者重新中断传递状态
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }
```

节点与节点之间通过前一个节点的nextWaiter指向下一个节点。所以不同于AbstractQueuedSynchronizer中的等待队列（同步队列）是双向的，ConditionObject中的等待队列是单向链表。

Condition等待队列中应该长如下这个样子：

![image-20230620194603391](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306201946387.png)

## **常用方法**

## **acquire**

获取锁

```java
    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    public final void acquire(int arg) {
        //尝试获取锁
        if (!tryAcquire(arg) &&
            //addWaiter:将线程封装到 Node 节点并添加到队列尾部
            //acquireQueued查看当前排队的 Node 是否在队列的前面，如果在前面，尝试获取锁资源。如果没在前面，线程进入到阻塞状态。
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            //中断当前线程，等待唤醒
            selfInterrupt();
    }
```

①尝试获取锁 

②如果获取锁没有成功，则构造节点加入等待队列中

 ③如果节点入队列成功，则线程自我中断让出资源。

### **tryAcquire**

尝试获取锁 

```java
    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }
```

我们找到tryAcquire方法发现，直接抛出了一个异常。将tryAcquire的具体实现预留给各种锁来实现

### **addWaiter**

当获取锁失败时，就开始构造节点入队列了。

```java
    /**
     * Creates and enqueues node for current thread and given mode.
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    private Node addWaiter(Node mode) {
        //创建节点 将当前线程封装为Node对象，当mode为null，代表互斥锁
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        //快速入队，入队不成功则交由enq来实现
        //当前尾节点
        Node pred = tail;
        //如果队列没有节点 创建一个哨兵节点
        if (pred != null) {
            // 当前线程Node节点的prev指向pred节点
            node.prev = pred;
            // 以CAS方式，尝试将tail指向node节点
            if (compareAndSetTail(pred, node)) {
                // 将pred的next指向node
                pred.next = node;
                return node;
            }
        }
        // 如果上述方式，CAS操作失败，导致加入到AQS末尾失败，就基于enq的方式添加到AQS队列
        enq(node);
        return node;
    }
```

#### **enq**

```java
    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     * @param node the node to insert
     * @return node's predecessor
     */
    private Node enq(final Node node) {
        // 死循环，直到插入成功
        for (;;) {
            Node t = tail;
            //尾节点为null 即等待队列为空
            if (t == null) { // Must initialize
                //创建哨兵节点  如果尾节点为null，说明同步队列还未初始化，则CAS操作新建头节点
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
              // 将node的prev指向当前的tail节点
                node.prev = t;
                // CAS尝试将node变成tail节点
                if (compareAndSetTail(t, node)) {
                    // 将之前尾节点的next指向要插入的节点
                    t.next = node;
                    return t;
                }
            }
        }
    }
```

### **acquireQueued**

```java
    /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting
     *线程被构造为节点进入队列后，接下来就是对队列中的节点进行获取锁的处理
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            //中断标识
            boolean interrupted = false;
            for (;;) {
                // 获取node的前驱节点
                final Node p = node.predecessor();
                //如果前一个节点为head头节点，说明当前节点应该获取锁，尝试获取锁资源
                if (p == head && tryAcquire(arg)) {
                   // 尝试获取锁资源成功，将头节点指向获取锁成功的节点，清空节点的thread和prev了，该节点成为新的哨兵节点
                    setHead(node);
                    // 将之前的头节点的next指向null，帮助快速GC
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                //如果前一个节点不是head头节点或者获取锁资源失败
                if (shouldParkAfterFailedAcquire(p, node) &&
                        //尝试将线程park
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                //获取锁失败，取消竞争锁
                cancelAcquire(node);
        }
    }
```

#### **cancelAcquire**

取消获取锁

```java
    /**
     * Cancels an ongoing attempt to acquire.
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        //如果待取消节点（node）为null，则直接返回。
        if (node == null)
            return;
        // 将node的thread置为null；取消节点对线程的引用
        node.thread = null;

        // Skip cancelled predecessors
        //将node节点往前驱节点方向所有连续的取消状态的节点出队
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        //1只有这里才设置为已取消状态
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves.
        //2如果当前节点为尾节点，当前节点出队，
        if (node == tail && compareAndSetTail(node, pred)) {
            //将前节点（这个时候已经是队尾节点）的next指向null，这里不保证它一定会成功，因为可能有其它新节点加入，用CAS方式避免将覆盖其它线程的操作。
            compareAndSetNext(pred, predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;
            //3如果当前节点不是尾节点，也不是头节点
            if (pred != head &&
                 //前节点状态已经为Node.SIGNAL  或者 将前节点状态更改为Node.SIGNAL成功
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                //当前节点的next节点存在并且没有取消
                if (next != null && next.waitStatus <= 0)
                    //则将前节点的next指向node节点的next
                    compareAndSetNext(pred, predNext, next);
            } else {
                //唤醒后继节点
                unparkSuccessor(node);
            }

            node.next = node; // help GC
        }
    }
```

##### **unparkSuccessor**

唤醒后继节点

```java
    /**
     * Wakes up node's successor, if one exists.
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
         //获取node节点的状态，在唤醒后继之前，先将node节点状态改为0
        int ws = node.waitStatus;
        if (ws < 0)
            //如果node节点状态小于0，cas修改为0
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        //当前节点的next节点
        Node s = node.next;
        //如果node的后继节点被取消，则从队尾向node节点遍历，找到距离node节点最近的waitStatus<=0的节点，然后唤醒s节点
        if (s == null || s.waitStatus > 0) {
        // next节点不需要唤醒，需要唤醒next的next
            s = null;
            // 从尾部往前找，找到状态正常的节点。（小于等于0代表正常状态）
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        //经过循环的获取，如果拿到状态正常的节点，并且不为null
        if (s != null)
            //线程唤醒
            LockSupport.unpark(s.thread);
    }
```

## **release**

释放锁

```java
    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     * release利用tryRelease先进行释放锁，tryRealse是由子类实现的方法，可以确保线程是获取到锁的，并进行释放锁，
     * unparkSuccessor主要是利用LockSupport.unpark唤醒线程;
     */
    public final boolean release(int arg) {
        //尝试释放锁，这个方法是由子类实现的方法
        if (tryRelease(arg)) {
            Node h = head;
            //头节点不为如果节点状态不是CANCELLED，也就是线程没有被取消，也就是不为0的，就进行唤醒
            if (h != null && h.waitStatus != 0)
                //唤醒线程
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
```

### **tryRelease**

尝试释放锁

```java
    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }
```

交由子类实现