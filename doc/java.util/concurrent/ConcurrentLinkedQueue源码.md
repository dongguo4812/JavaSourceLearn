# **介绍**

ConcurrentLinkedQueue是一个基于链接节点的无边界的线程安全队列，它采用FIFO原则对元素进行排序。采用“wait-free”算法（即CAS算法）来实现的。

```java
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E>
        implements Queue<E>, java.io.Serializable {
```

![image-20230627195834160](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306271958186.png)

# **常量&变量**

```java
     //序列化版本号
     private static final long serialVersionUID = 196745693267521676L;

    /**
     * A node from which the first live (non-deleted) node (if any)
     * can be reached in O(1) time.
     * Invariants:
     * - all live nodes are reachable from head via succ()
     * - head != null
     * - (tmp = head).next != tmp || tmp != head
     * Non-invariants:
     * - head.item may or may not be null.
     * - it is permitted for tail to lag behind head, that is, for tail
     *   to not be reachable from head!
     *   头节点
     */
    private transient volatile Node<E> head;

    /**
     * A node from which the last node on list (that is, the unique
     * node with node.next == null) can be reached in O(1) time.
     * Invariants:
     * - the last node is always reachable from tail via succ()
     * - tail != null
     * Non-invariants:
     * - tail.item may or may not be null.
     * - it is permitted for tail to lag behind head, that is, for tail
     *   to not be reachable from head!
     * - tail.next may or may not be self-pointing to tail.
     * 尾结点
     */
    private transient volatile Node<E> tail;
    

    private static final sun.misc.Unsafe UNSAFE;
    //头节点相对于对象的偏移量

    private static final long headOffset;
    //尾节点相对于对象的偏移量
    private static final long tailOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentLinkedQueue.class;
            headOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

```

head和tail都被transient修饰，不会被序列化，说明是通过writeObject和readObject来完成序列化。

# **构造方法**

## **ConcurrentLinkedQueue()**

创建一个初始为空的**ConcurrentLinkedQueue**

```java
    /**
     * Creates a {@code ConcurrentLinkedQueue} that is initially empty.
     */
    public ConcurrentLinkedQueue() {
        head = tail = new Node<E>(null);
    }

```

使用无参构造创建对象时，头尾节点都是指向一个空节点。

![image-20230627195935486](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306271959555.png)

## **ConcurrentLinkedQueue(Collection c)**

使用指定 collection 中的元素创建一个新的 ConcurrentLinkedQueue

```java
    /**
     * Creates a {@code ConcurrentLinkedQueue}
     * initially containing the elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        //h:头节点，t：尾节点
        Node<E> h = null, t = null;
        //遍历集合元素
        for (E e : c) {
            //判断元素是否为null
            checkNotNull(e);
            //构造成一个新节点，e为新节点的item值
            Node<E> newNode = new Node<E>(e);
            if (h == null)
                //如果头节点为null，则新节点为第一个元素，将头节点和尾节点指向该节点
                h = t = newNode;
            else {
                //头节点不为null，则将当前节点设置为尾节点的下一个节点
                t.lazySetNext(newNode);
                //将尾节点指向当前节点
                t = newNode;
            }
        }
        //遍历完之后头节点为null，说明集合c是空集合，这里的处理是创建一个空队列
        if (h == null)
            h = t = new Node<E>(null);
        //最终的头节点    
        head = h;
        //
最终的尾节点
        tail = t;
    }
```

# **内部类**

## **Node**

内部类Node是一个链表节点类，用于实现ConcurrentLinkedQueue的链表结构

```java
    /*
     * This is a modification of the Michael & Scott algorithm,
     * adapted for a garbage-collected environment, with support for
     * interior node deletion (to support remove(Object)).  For
     * explanation, read the paper.
     *
     * Note that like most non-blocking algorithms in this package,
     * this implementation relies on the fact that in garbage
     * collected systems, there is no possibility of ABA problems due
     * to recycled nodes, so there is no need to use "counted
     * pointers" or related techniques seen in versions used in
     * non-GC'ed settings.
     *
     * The fundamental invariants are:
     * - There is exactly one (last) Node with a null next reference,
     *   which is CASed when enqueueing.  This last Node can be
     *   reached in O(1) time from tail, but tail is merely an
     *   optimization - it can always be reached in O(N) time from
     *   head as well.
     * - The elements contained in the queue are the non-null items in
     *   Nodes that are reachable from head.  CASing the item
     *   reference of a Node to null atomically removes it from the
     *   queue.  Reachability of all elements from head must remain
     *   true even in the case of concurrent modifications that cause
     *   head to advance.  A dequeued Node may remain in use
     *   indefinitely due to creation of an Iterator or simply a
     *   poll() that has lost its time slice.
     *
     * The above might appear to imply that all Nodes are GC-reachable
     * from a predecessor dequeued Node.  That would cause two problems:
     * - allow a rogue Iterator to cause unbounded memory retention
     * - cause cross-generational linking of old Nodes to new Nodes if
     *   a Node was tenured while live, which generational GCs have a
     *   hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from
     * dequeued Nodes, and reachability does not necessarily have to
     * be of the kind understood by the GC.  We use the trick of
     * linking a Node that has just been dequeued to itself.  Such a
     * self-link implicitly means to advance to head.
     *
     * Both head and tail are permitted to lag.  In fact, failing to
     * update them every time one could is a significant optimization
     * (fewer CASes). As with LinkedTransferQueue (see the internal
     * documentation for that class), we use a slack threshold of two;
     * that is, we update head/tail when the current pointer appears
     * to be two or more steps away from the first/last node.
     *
     * Since head and tail are updated concurrently and independently,
     * it is possible for tail to lag behind head (why not)?
     *
     * CASing a Node's item reference to null atomically removes the
     * element from the queue.  Iterators skip over Nodes with null
     * items.  Prior implementations of this class had a race between
     * poll() and remove(Object) where the same element would appear
     * to be successfully removed by two concurrent operations.  The
     * method remove(Object) also lazily unlinks deleted Nodes, but
     * this is merely an optimization.
     *
     * When constructing a Node (before enqueuing it) we avoid paying
     * for a volatile write to item by using Unsafe.putObject instead
     * of a normal write.  This allows the cost of enqueue to be
     * "one-and-a-half" CASes.
     *
     * Both head and tail may or may not point to a Node with a
     * non-null item.  If the queue is empty, all items must of course
     * be null.  Upon creation, both head and tail refer to a dummy
     * Node with null item.  Both head and tail are only updated using
     * CAS, so they never regress, although again this is merely an
     * optimization.
     */

    private static class Node<E> {
        //item节点元素域 即Node中存放的元素
        volatile E item;
        //链表中下一个节点
        volatile Node<E> next;

        /**
         * Constructs a new node.  Uses relaxed write because item can
         * only be seen after publication via casNext.
         * 初始化时，获取item和next的偏移量，为后期的cas做准备
         */
        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }
        //在cas操作时更新item值，将item从cmp更新为val。
        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }
        //该方法是一个延迟设置操作，将节点的next指针设置为val。
        // 由于该方法使用putOrderedObject方法，因此在设置前会对变量进行内存屏障，保证可见性。
        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }
        //cas操作，用于将next指针从cmp更新为val。
        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        //item偏移量
        private static final long itemOffset;
        //下一个元素的偏移量
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
```

## **Itr**

itr是一个迭代器，用于遍历ConcurrentLinkedQueue中的元素

迭代器是弱一致性的，只是某一时刻的快照，因为同时有可能其他线程还在对队列进行修改

```java
private class Itr implements Iterator<E> {
        /**
         * Next node to return item for.
         * 下一个节点
         */
        private Node<E> nextNode;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         * 下一个节点中的元素
         */
        private E nextItem;

        /**
         * Node of the last returned item, to support remove.
         * 上一个节点
         */
        private Node<E> lastRet;

        Itr() {
            advance();
        }

        /**
         * Moves to next valid node and returns item to return for
         * next(), or null if no such.
         * 寻找下一个非空元素节点，并返回上一个元素节点中的元素。
         * 该方法会同时更新lastRet、next和nextItem。
         */
        private E advance() {
            lastRet = nextNode;
            E x = nextItem;

            Node<E> pred, p;
            if (nextNode == null) {
                p = first();
                pred = null;
            } else {
                pred = nextNode;
                p = succ(nextNode);
            }

            for (;;) {
                if (p == null) {
                    nextNode = null;
                    nextItem = null;
                    return x;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    return x;
                } else {
                    // skip over nulls
                    Node<E> next = succ(p);
                    if (pred != null && next != null)
                        pred.casNext(p, next);
                    p = next;
                }
            }
        }
        //判断是否还有下一个元素
        public boolean hasNext() {
            return nextNode != null;
        }
        //返回下一个元素，并将next、nextItem和lastRet更新。如果已经没有下一个元素，则抛出NoSuchElementException异常。
        public E next() {
            if (nextNode == null) throw new NoSuchElementException();
            return advance();
        }
        //移除上一个元素节点，并将lastRet置为null。如果上一个元素节点为null，则抛出IllegalStateException异常。
        public void remove() {
            Node<E> l = lastRet;
            if (l == null) throw new IllegalStateException();
            // rely on a future traversal to relink.
            l.item = null;
            lastRet = null;
        }
    }
```

## **CLQSpliterator**

内部类CLQSpliterator用于并发迭代队列，实现了Spliterator接口，可以用于Stream流的并发处理。

```java
    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class CLQSpliterator<E> implements Spliterator<E> {
        //最大批处理大小
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final ConcurrentLinkedQueue<E> queue;
        //当前节点，初始化之前都为null
        Node<E> current;    // current node; null until initialized
        //分割批次的大小
        int batch;          // batch size for splits
        //如果没有元素了，为true
        boolean exhausted;  // true when no more nodes
        CLQSpliterator(ConcurrentLinkedQueue<E> queue) {
            this.queue = queue;
        }
        //并发分裂队列，将队列分成多个子队列进行同时处理，提高处理效率。
        public Spliterator<E> trySplit() {
            Node<E> p;
            final ConcurrentLinkedQueue<E> q = this.queue;
            int b = batch;
            //n表示数组长度
            //分割批次长度<=0，数组长度为1；
            //分割批次长度>=最大批处理大小，数组长度为最大批处理大小；否则为分割批次长度+1
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            //!exhausted 为true表示队列还有元素
            //((p = current) != null || (p = q.first()) != null) 如果前部分为true表示已经运行过一次，后部分为true表          //示是第一次运行，将队列的头节点赋值给p
            //p.next != null 表示队列后面还有元素,p表示的节点不是最后一个
            if (!exhausted &&
                ((p = current) != null || (p = q.first()) != null) &&
                p.next != null) {
                //临时存储需分割的元素
                Object[] a = new Object[n];
                //已分割的元素计数，只计数item不为null的元素
                //对于ConcurrentLinkedQueue来说，这里不会有item值为null的情况
                int i = 0;
                do {
                    //节点item值赋值临时数组
                    if ((a[i] = p.item) != null)
                        //计数
                        ++i;
                    //当next也指向本身p的时候为true,也就是自引用，重新找头节点
                    if (p == (p = p.next))
                        p = q.first();
                    //直到取完需要的元素数量
                } while (p != null && i < n);
                //如果p等于null了就说明没有元素了
                if ((current = p) == null)
                    exhausted = true;
                //有不为null的元素
                if (i > 0) {
                    batch = i;
                    //创建Spliterator，数组从0-i不包括i，指定了迭代器的特性
                    return Spliterators.spliterator
                        (a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL |
                         Spliterator.CONCURRENT);
                }
            }
            return null;
        }
        //循环遍历当前迭代器中所有没有被移除的节点数据（item不为空）做指定的操作
        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            final ConcurrentLinkedQueue<E> q = this.queue;
            if (!exhausted &&
                ((p = current) != null || (p = q.first()) != null)) {
                exhausted = true;
                do {
                    E e = p.item;
                    if (p == (p = p.next))
                        p = q.first();
                    if (e != null)
                        action.accept(e);
                } while (p != null);
            }
        }
        //获取第一个item不为空的节点数据做指定的操作
        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            final ConcurrentLinkedQueue<E> q = this.queue;
            if (!exhausted &&
                ((p = current) != null || (p = q.first()) != null)) {
                E e;
                do {
                    e = p.item;
                    if (p == (p = p.next))
                        p = q.first();
                } while (e == null && p != null);
                if ((current = p) == null)
                    exhausted = true;
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }

        //返回了一个Long.MAX_VALUE，用于表示队列长度为无限大
        public long estimateSize() { return Long.MAX_VALUE; }
        //返回了Spliterator.ORDERED、Spliterator.NONNULL和Spliterator.CONCURRENT三个标志，
        // 表示该Spliterator是有序、非空且并发处理的。
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL |
                Spliterator.CONCURRENT;
        }
    }
```

# **常用方法**

## **add**

在尾部插入元素，实际调用offer()

```java
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offer(e);
    }
```

## **入列**offer

在尾部插入元素

```java
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        //检查节点是否为null
        checkNotNull(e);
        //创建新节点
        final Node<E> newNode = new Node<E>(e);
        //死循环 直到成功为止
        // t和p默认都为尾结点 q为p的后继节点
        for (Node<E> t = tail, p = t;;) {
            Node<E> q = p.next;
            //q == null 表示p已经是最后一个节点，尝试将新节点加入到队列尾
            if (q == null) {             //判断1
                // p is last node
                //casNext： p节点的next指向新节点，失败则继续循环
                if (p.casNext(null, newNode)) {              //判断2
                    // Successful CAS is the linearization point
                    // for e to become an element of this queue,
                    // and for newNode to become "live".
                    //p！=t node加入节点后会导致tail距离最后一个节点相差大于一个，需要更新tail
                    if (p != t) // hop two nodes at a time          //判断3
                        //将tail指向新节点 ，失败也没事，因为失败了就是有别的线程更新成功了
                        casTail(t, newNode);  // Failure is OK.
                    return true;
                }
                //如果casNext插入失败，则表示其他线程已经修改了p.next的指向
                //当前线程需要重新尝试获取p的下一个节点再次cas操作
                // Lost CAS race to another thread; re-read next
            }
            // p == q 代表着该节点已经被删除了
            // 由于多线程的原因，我们offer()的时候也会poll()，如果offer()的时候正好该节点已经poll()了
            // 那么在poll()方法中的updateHead()方法会将head指向当前的q，而把p.next指向自己，即：p.next == p
            else if (p == q)                //判断4
                // We have fallen off list.  If tail is unchanged, it
                // will also be off-list, in which case we need to
                // jump to head, from which all live nodes are always
                // reachable.  Else the new tail is a better bet.
                // 这样就会导致tail节点滞后head（tail位于head的前面），则需要重新设置p
                //如果当前tail还是没变则取head，head永远是活动的节点，否则tail仍然是最快获取尾结点的方式
                p = (t != (t = tail)) ? t : head;
                //tail并没有指向尾节点,当前有另一个线程改变的结构，则重新获取尾结点。
            else              //判断5
                // Check for tail updates after two hops.
                //则将p指向最后一个节点，继续循环
                //(p != t)  说明执行过 p = q 操作(向后遍历操作)
                //(t != (t = tail))) 说明尾节点在其他的线程发生变化
                p = (p != t && t != (t = tail)) ? t : q;
        }
    }
```

### **1.当队列中只有空节点时（无参初始化）**

ConcurrentLinkedQueue初始化时head、tail存储的元素都为null，且head等于tail：

![image-20230627200204116](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272002813.png)

**这个item为null的节点可以理解为哨兵节点**

### **2.添加第一个元素"张三"**

2.1执行到如下代码处,创建item为张三的新节点

```java
final Node<E> newNode = new Node<E>(e);
```

![image-20230627200239989](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272002491.png)

2.2  p的后继节点q，因为p现在是尾节点，所以q为null、

```java
Node<E> q = p.next;
```

![image-20230627200303469](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272003349.png)

2.3 进入判断1，执行判断2 当p节点的next指针指向null时，cas操作将其重新指向newNode

```java
p.casNext(null, newNode)
```

![image-20230627200325456](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272003596.png)

这里是通过CAS操作把新节点添加到链表中，同一时间只有一个线程可以执行成功。

2.4 跳过判断3 此时 p == q 并未将尾指针指向新节点

```java
if (p != t)    
    casTail(t, newNode);
```

### **3.添加第二个元素"李四"**

3.1 创建item为李四的新节点

```java
final Node<E> newNode = new Node<E>(e);
```

![image-20230627200414693](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272004581.png)

3.2 p的后继节点q，p当前指向头节点，q则指向p的后继节点

```java
Node<E> q = p.next;
```

![image-20230627200440366](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272004916.png)

3.3 到达判断5 ，将p指向了q指针

```java
else
     p = (p != t && t != (t = tail)) ? t : q;
```

![image-20230627200506541](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272005593.png)

第一轮循环结束，进入第二轮循环

3.4 此时 p，q都指向了张三节点，因为p现在是尾节点，所以q为null，

```java
Node<E> q = p.next;
```

![image-20230627200528312](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272005176.png)

3.5 进入判断1，执行判断2，cas操作将p的next节点指向newNode

```java
p.casNext(null, newNode)
```

![image-20230627200546773](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272005880.png)

3.6 判断3 ，此时p != t ,将尾指针指向newNode

```java
if (p != t) 
        casTail(t, newNode);
```

![image-20230627200603968](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272006921.png)

### **4添加第三个元素"王五"**

4.1 创建item为王五的新节点

```java
final Node<E> newNode = new Node<E>(e);
```

![image-20230627200628590](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272006837.png)

4.2  循环条件初始化  t为尾节点， p = t

```java
 for (Node<E> t = tail, p = t;;) 
```

![image-20230627200650198](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272006046.png)

4.3 q为p的后继节点， q为null

```java
Node<E> q = p.next;
```

![image-20230627200713430](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272007587.png)

4.4 进入判断1，执行判断2  cas操作将其重新指向newNode

```java
p.casNext(null, newNode)
```

![image-20230627200744005](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272007803.png)

## **出列**poll

ConcurrentLinkedQueue提供了poll()方法进行出列操作。入列主要是涉及到tail，出列则涉及到head。

```java
    public E poll() {
        //如果出现p被删除的情况需要从head重新开始
        restartFromHead:          //goto
        for (;;) {
            //h为头节点
            for (Node<E> h = head, p = h, q;;) {
                //p节点中的item元素
                E item = p.item;
                //item不为null，则将item设置为null
                if (item != null && p.casItem(item, null)) {     //判断1
                    // Successful CAS is the linearization point
                    // for item to be removed from this queue.
                    //p！= h，则更新head
                    if (p != h) // hop two nodes at a time       判断2
                        //头节点从h修改   p的next不为null， 修改为q，
                                        //      为null， 修改为p
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    return item;
                }
                //p的next 为null， 修改为p
                else if ((q = p.next) == null) {    //判断3
                    updateHead(h, p);
                    return null;
                }
                //当一个线程在poll的时候，另一个线程已经把当前的p从队列中删除——将p.next = p，p已经被移除不能继续，跳出多层循环
                else if (p == q)    //判断4
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }
```

updateHead()，该方法用于CAS更新head节点

```java
    /**
     * Tries to CAS head to p. If successful, repoint old head to itself
     * as sentinel for succ(), below.
     */
    final void updateHead(Node<E> h, Node<E> p) {
        if (h != p && casHead(h, p))
            h.lazySetNext(h);
    }
```

### **1 队列初始化**

![image-20230627200905742](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272009242.png)

### **2循环条件初始化**

```java
for (Node<E> h = head, p = h, q;;)
```

![image-20230627200932112](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272009019.png)

### **3 此时p的item元素肯定为null**

```java
E item = p.item;
```

### **4跳过判断1，到达判断3**

```java
else if ((q = p.next) == null)
```

![image-20230627201008910](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272010777.png)

### **5 显然判断3、4也不满足，到达最后的逻辑**

```java
else
   p = q;
```

![image-20230627201029790](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272010713.png)

开始第二轮循环

### **6 此时p的item是张三**

```java
E item = p.item;
```

### **7**将队列的头部节点（哨兵的下一个节点）的item值设为null，

这是CAS操作，保证同一时间就一个线程可以成功。

![image-20230627201111846](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272011636.png)

### 8   q = p.next

```java
((q = p.next) != null) ? q : p
```

![image-20230627201132766](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272011595.png)

### 9更新头节点  李四所在节点变为头节点

```java
updateHead(h, ((q = p.next) != null) ? q : p);
```

updateHead

```java
final void updateHead(Node<E> h, Node<E> p) {
    if (h != p && casHead(h, p))
        h.lazySetNext(h);
}
```

lazySetNext

```java
void lazySetNext(Node<E> val) {
    UNSAFE.putOrderedObject(this, nextOffset, val);
}
```

UNSAFE.putOrderedObject(this, nextOffset, val)将原哨兵节点的next指针指向了自己，后续会被垃圾回收



![image-20230627201222456](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272014031.png)

### 10返回张三，张三所在节点出列完成

```java
return item;
```

![image-20230627201253391](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306272012225.png)
