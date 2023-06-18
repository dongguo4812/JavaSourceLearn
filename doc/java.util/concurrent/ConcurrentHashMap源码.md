HashTable是一个线程安全的类，它使用synchronized来锁住整张Hash表来实现线程安全，即每次锁住整张表让线程独占，相当于所有线程进行读写时都去竞争一把锁，导致效率非常低下。

# **介绍**

ConcurrentHashMap的底层原理和HashMap是比较相似的，比较大的区别就是在保证线程安全方面

- 线程安全
- 比Hashtable锁粒度更细

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
    implements ConcurrentMap<K,V>, Serializable
```

![image-20230618214405027](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306182144984.png)

- 虽然名字中也有 HashMap，但是并没有继承 HashMap。
- ConcurrentHashMap 继承 AbstractMap，实现 ConcurrentMap、Serializable，即表示是一个可序列化的 Map

# **常量&变量**

```java
    //序列化版本号
    private static final long serialVersionUID = 7249069246763182397L;
    
   /**
     * The largest possible table capacity.  This value must be
     * exactly 1<<30 to stay within Java array allocation and indexing
     * bounds for power of two table sizes, and is further required
     * because the top two bits of 32bit hash fields are used for
     * control purposes.
     * 数组的最大容量(少使用两次幂，前两位用于32位hash)
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The default initial table capacity.  Must be a power of 2
     * (i.e., at least 1) and at most MAXIMUM_CAPACITY.
     * 默认初始化容量，必须是2的倍数，最大为MAXIMUM_CAPACITY
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The largest possible (non-power of two) array size.
     * Needed by toArray and related methods.
     * 最大数组大小
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The default concurrency level for this table. Unused but
     * defined for compatibility with previous versions of this class.
     * 表的默认并发级别，已经不使用，为了兼容以前的版本
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * The load factor for this table. Overrides of this value in
     * constructors affect only the initial table capacity.  The
     * actual floating point value isn't normally used -- it is
     * simpler to use expressions such as {@code n - (n >>> 2)} for
     * the associated resizing threshold.
     * 负载因子
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2, and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     * 链表转化为红黑树的阈值
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     * 红黑树转化为链表的阈值，扩容时才可能发生
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * The value should be at least 4 * TREEIFY_THRESHOLD to avoid
     * conflicts between resizing and treeification thresholds.
     * 进行树化的最小容量，防止在调整容量和形态时发生冲突
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Minimum number of rebinnings per transfer step. Ranges are
     * subdivided to allow multiple resizer threads.  This value
     * serves as a lower bound to avoid resizers encountering
     * excessive memory contention.  The value should be at least
     * DEFAULT_CAPACITY.
     * 作为下界以避免遇到过多的内存争用
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * The number of bits used for generation stamp in sizeCtl.
     * Must be at least 6 for 32bit arrays.
     *  用于sizeCtl产生标记的bit数量
     */
    private static int RESIZE_STAMP_BITS = 16;

    /**
     * The maximum number of threads that can help resize.
     * Must fit in 32 - RESIZE_STAMP_BITS bits.
     * 可帮助调整的最大线程数
     */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * The bit shift for recording size stamp in sizeCtl.
     *  sizeCtl移位大小标记
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /*
     * Encodings for Node hash fields. See above for explanation.
     */
    //转移的hash
    static final int MOVED     = -1; // hash for forwarding nodes
    // 树根的hash
    static final int TREEBIN   = -2; // hash for roots of trees
    // ReservationNode的hash
    static final int RESERVED  = -3; // hash for transient reservations
    // 可用普通节点的hash
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /** Number of CPUS, to place bounds on some sizings */
    // 当前cpu可用的数量
    static final int NCPU = Runtime.getRuntime().availableProcessors();
```

# **构造方法**

```java
 /**
     * Creates a new, empty map with the default initial table size (16).
     */
    public ConcurrentHashMap() {
    }

    /**
     * Creates a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     */
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                   MAXIMUM_CAPACITY :
                   tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    /**
     * Creates a new map with the same mappings as the given map.
     *
     * @param m the map
     */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}) and
     * initial table density ({@code loadFactor}).
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative or the load factor is nonpositive
     *
     * @since 1.6
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}), table
     * density ({@code loadFactor}), and number of concurrently
     * updating threads ({@code concurrencyLevel}).
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation may use this value as
     * a sizing hint.
     * @throws IllegalArgumentException if the initial capacity is
     * negative or the load factor or concurrencyLevel are
     * nonpositive
     */
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
            MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }
    
     /**
     * Returns a power of two table size for the given desired capacity.
     * See Hackers Delight, sec 3.2
     */
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
```

# **内部类**

## **Node**

```java
    /**
     * Key-value entry.  This class is never exported out as a
     * user-mutable Map.Entry (i.e., one supporting setValue; see
     * MapEntry below), but can be used for read-only traversals used
     * in bulk tasks.  Subclasses of Node with a negative hash field
     * are special, and contain null keys and values (but are never
     * exported).  Otherwise, keys and vals are never null.
     * 链表节点Node
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        //哈希值
        final int hash;
        final K key;
        volatile V val;
        //链表下一个节点
        volatile Node<K,V> next;

        Node(int hash, K key, V val, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey()       { return key; }
        public final V getValue()     { return val; }
        public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
        public final String toString(){ return key + "=" + val; }
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }

        /**
         * Virtualized support for map.get(); overridden in subclasses.
         */
        Node<K,V> find(int h, Object k) {
            Node<K,V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }
```

## **TreeNode**

```java
    /**
     * Nodes for use in TreeBins
     * 红黑树节点
     */
    static final class TreeNode<K,V> extends Node<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, V val, Node<K,V> next,
                 TreeNode<K,V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        Node<K,V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        /**
         * Returns the TreeNode (or null if not found) for the given key
         * starting at given root.
         */
        final TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                TreeNode<K,V> p = this;
                do  {
                    int ph, dir; K pk; TreeNode<K,V> q;
                    TreeNode<K,V> pl = p.left, pr = p.right;
                    if ((ph = p.hash) > h)
                        p = pl;
                    else if (ph < h)
                        p = pr;
                    else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                        return p;
                    else if (pl == null)
                        p = pr;
                    else if (pr == null)
                        p = pl;
                    else if ((kc != null ||
                              (kc = comparableClassFor(k)) != null) &&
                             (dir = compareComparables(kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;
                    else if ((q = pr.findTreeNode(h, k, kc)) != null)
                        return q;
                    else
                        p = pl;
                } while (p != null);
            }
            return null;
        }
    }
```

## **ForwardingNode**

只有在扩容迁移节点的时候才会出现，用来标记当前桶正在进行迁移

```java
    /**
     * A node inserted at head of bins during transfer operations.
     * 转移节点
     */
    static final class ForwardingNode<K,V> extends Node<K,V> {
        //下一个数组，只有在进行转移时才会出现
        final Node<K,V>[] nextTable;
        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        Node<K,V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer: for (Node<K,V>[] tab = nextTable;;) {
                Node<K,V> e; int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                    (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (;;) {
                    int eh; K ek;
                    if ((eh = e.hash) == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable;
                            continue outer;
                        }
                        else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }
```

# **常用方法**

## **put**

```java
    /**
     * Maps the specified key to the specified value in this table.
     * Neither the key nor the value can be null.
     *
     * <p>The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key or value is null
     */
    public V put(K key, V value) {
        // 直接调用putVal
        return putVal(key, value, false);
    }
```

## **putVal**

```java


    /** Implementation for put and putIfAbsent */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        // 当key或者value为空时直接抛出空指针异常
        if (key == null || value == null) throw new NullPointerException();
        // 获取二次hash后的值
        int hash = spread(key.hashCode());
        // 操作次数
        int binCount = 0;
        // 死循环，直到插入成功
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            // 判断是否已经初始化，未初始化则进行初始化
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            // 获取该位置的节点，当为空时，没有发生碰撞，直接CAS进行存储，操作成功则退出死循环
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            // 为fh赋值，当检测到正在进行扩容，帮助扩容
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                // 对Node(hash值相同的链表的头节点)上锁(1.7中锁segement)
                synchronized (f) {
                    // 双重检测
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            // 操作次数
                            binCount = 1;
                            // 死循环更新value，并增加操作数量
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                // 判断传入元素的hash和冲突节点的hash是否相同，key是否相同
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    // 将发生冲突的值赋值给oldVal
                                    oldVal = e.val;
                                    // putIfAbsent()方法中onlyIfAbsent为true
                                    if (!onlyIfAbsent)
                                        // 包含则赋值
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                // 查找下一个节点，并判断是否为空为，当为空时进行实例化
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        // 当为树型结构时
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            // 当向树型结构赋值成功时设置oldVal
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                // 当已经进行过节点赋值后，判断一下是否需要将链表转化为红黑树
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }
```

### **为什么 key 和 value 不允许为 null**

在 HashMap 中，key 和 value 都是可以为 null 的，但是在 ConcurrentHashMap 中却不允许，这是为什么呢？

作者 Doug Lea 本身对这个问题有过回答，在并发编程中，null 值容易引来歧义， 假如先调用 get(key) 返回的结果是 null，那么我们无法确认是因为当时这个 key 对应的 value 本身放的就是 null，还是说这个 key 值根本不存在，这会引起歧义，如果在非并发编程中，可以进一步通过调用 containsKey 方法来进行判断，但是并发编程中无法保证两个方法之间没有其他线程来修改 key 值，所以就直接禁止了 null 值的存在。

而且作者 Doug Lea 本身也认为，假如允许在集合，如 map 和 set 等存在 null 值的话，即使在非并发集合中也有一种公开允许程序中存在错误的意思，这也是 Doug Lea 和 Josh Bloch（HashMap作者之一） 在设计问题上少数不同意见之一，而 ConcurrentHashMap 是 Doug Lea 一个人开发的，所以就直接禁止了 null 值的存在。

## **initTable**

使用了 cas 操作来保证安全性

```java
    /**
     * Initializes table, using the size recorded in sizeCtl.
     */
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        //判断数组是否初始化  没有初始化， 死循环执行初始化操作
        while ((tab = table) == null || tab.length == 0) {
            //sizeCtl < 0 表示其他线程正在初始化
            if ((sc = sizeCtl) < 0)
                //放弃当前cpu资源
                Thread.yield(); // lost initialization race; just spin
            //cas操作将SIZECTL修改为-1，表示该线程开始进行初始化
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    //双重检测 数组是否初始化
                    if ((tab = table) == null || tab.length == 0) {
                        //确定容量
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        //创建哈希表
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        // n(1-0.25) = 0.75n = 更新阈值
                        sc = n - (n >>> 2);
                    }
                } finally {
                    //初始化成功，sizeCtl记录的是阈值
                    //初始化失败，则还原sizeCtl
                    sizeCtl = sc;
                }
                break;
            }
            //数组未初始化，准备去初始化，结果有其他线程去执行 初始化。
            //并不知道是否完成初始化，重新循环检查
        }
        return tab;
    }
```

## **treeifyBin**

根据阈值判断是否需要链表转红黑树，还是进行扩容

```java
    /**
     * Replaces all linked nodes in bin at given index unless table is
     * too small, in which case resizes instead.
     */
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            //数组长度小于64，进行扩容
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);
            //否则链表转换红黑树
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K,V> hd = null, tl = null;
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                new TreeNode<K,V>(e.hash, e.key, e.val,
                                                  null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<K,V>(hd));
                    }
                }
            }
        }
    }
```

## **tryPresize**

多线程并发扩容（允许其他线程来协助扩容）：多个线程对数据进行分段迁移

扩容的本质是创建一个新的数组，数组大小是之前的两倍

```java
    /**
     * Tries to presize table to accommodate the given number of elements.
     *
     * @param size number of elements (doesn't need to be perfectly accurate)
     */
    private final void tryPresize(int size) {
        // 如果大小已经大于等于最大容量的一半，直接扩容到最大容量，否则*1.5倍+1并且向上取到二次幂
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
            tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K,V>[] tab = table; int n;
            // 当节点还未初始化时
            if (tab == null || (n = tab.length) == 0) {
                // 取sc和c的较大值
                n = (sc > c) ? sc : c;
                // 通过cas修改SIZECTL为-1，表示正在初始化
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        // 判断是否没有被其他线程修改
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        // 最后将sizeCtl设置为sc
                        sizeCtl = sc;
                    }
                }
            }
            // 如果扩容大小没有达到阈值，或者超过最大容量时退出
            else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;
            else if (tab == table) {
                // 重新生成戳
                int rs = resizeStamp(n);
                // 当线程在进行扩容时
                if (sc < 0) {
                    Node<K,V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                }
                // 未进行扩容时，cas修改sizeCtl值
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                             (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }
```

## **addCount**

```java
   private final void addCount(long x, int check) {
        // as 表示 LongAdder.cells
        // b 表示 LongAdder.base
        // s 表示当前map.table中元素的数量
        CounterCell[] as; long b, s;
        //条件一：true-> 表示cells 已经初始化了，当前线程应该去使用hash寻址找到合适的cell 去累加数据
        //      false-> 表示当前线程应该将数据累加到base
        //条件二：false-> 表示写base成功，数据累加到base 中了，当前竞争不强烈，不需要创建cells
        //      true-> 表示写base失败，与其它线程在base上发生竞争，当前线程应该去尝试创建cells
        /**
         * LongAdder 中的cekks数组，当baseCount 发生竞争后，会创建cells数组
         * 线程会通过计算hash值 取道自己的cell，将增量累加到指定cell中
         * 总数 = sum(cells) + baseCount
         *
         * private transient volatile CounterCell[] counterCells;
         */
        if ((as = counterCells) != null ||
            !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            //有几种情况进入到if中
            // 1. true-> 表示cells 已经初始化了，当前线程应该去使用hash寻址找到合适的cell 去累加数据
            // 2. true-> 表示写base失败，与其它线程在base上发生竞争，当前线程应该去尝试创建cells

            //a 表示当前线程hash寻址命中的cell
            CounterCell a;
            //v 表示当前线程写cell 时的期望值
            long v;
            //m 表示当前cells 数组的长度
            int m;
            //true-> 未竞争 false->发生竞争
            boolean uncontended = true;

            //条件一：as == null || (m = as.length - 1) < 0
            //      表示写base竞争失败，然后进入if块，需要调用fullAddCount进行扩容 或者重试 LongAdder.longAccumulate
            //条件二：(a = as[ThreadLocalRandom.getProbe() & m]) == null
            //      前置条件：cells已经初始化了，
            //      true-> 表示当前线程命中的cell表格是个空，需要当前线程进入fullAddCount方法去初始化 cells，放入当前位置
            //条件三：!(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
            //      false->取反得到false，表示当前线程使用cas方式更新当前命中的cell成功
            //      true-> 取反得到true，表示当前线程使用cas方式更新命中的cell失败，需要进入fullAddCount 进行重试或者扩容cells
            if (as == null || (m = as.length - 1) < 0 ||
                    //getProbe() 获取当前线程的hash值
                (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
                !(uncontended =
                  U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                // 和LongAdder的 longAccumulate一样
                fullAddCount(x, uncontended);
                //考虑到fullAddCount 里面的事情太多，就让当前线程不参与到扩容相关的逻辑了
                return;
            }
            //  check是 是否扩容的主要标识
            // putVal方法中调用addCount时， 里面传进来的binCount = check
            // binCount >= 1  当前命中的桶位的链表的长度，是1时也可能代表key相同，发生冲突
            // binCount == 0  当前命中的桶位是null，直接将节点放到桐中
            // binCount == 2  桶位下已经树化
            // remove() 方法中调用addCount时， 里面传进来的 check=-1
            if (check <= 1)
                return;
            // 获取当前散列表的元素个数，期望值
            s = sumCount();
        }
        // 表示一定是一个put 操作调用的addCount （只有添加元素时才会扩容）
        if (check >= 0) {
            // tab 代表 map.table
            // nt 代表 map.nextTable
            /**
             * 扩容过程中，会将扩容中的新table 赋值给 nextTable 保持引用，扩容结束之后，这里会被设置为Null
             *  private transient volatile Node<K, V>[] nextTable;
             */
            // n 代表table 数组的长度
            // sc 代表sizeCtl 的临时值
            Node<K,V>[] tab, nt; int n, sc;

            /**     sizeCtl < 0
             *  X   1. -1 表示当前table正在初始化（有线程在创建table数组），当前线程需要自旋等待..
             * 可能  2. 表示当前mao正在进行扩容 高16位表示：扩容的标识戳   低16位表示：（1 + nThread） 当前参与并发扩容的线程数量
             *
             *      sizeCtl = 0
             *  X   表示创建table数组时，使用 DEFAULT_CAPACITY 为大小
             *
             *      sizeCtl > 0
             *  X   1.如果table 未初始化，表示初始化大小
             * 可能  2.如果已经初始化，表示下次扩容时的 触发条件（阈值）
             */
            // 自旋
            // 条件一：s >= (long)(sc = sizeCtl)
            // true：1.当前sizeCtl 为一个负数，表示正在扩容中。
            //       2.当前sizeCtl 是一个正数，表示扩容阈值
            // false: 表示当前table 尚未达到扩容条件
            // 条件二； (tab = table) != null 恒成立
            // 条件三： (n = tab.length) < MAXIMUM_CAPACITY
            //          当前table长度小于最大值限制，则可以进行扩容
            while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
                   (n = tab.length) < MAXIMUM_CAPACITY) {
                //当前扩容标识戳,之前讲过
                //16 -> 32 标识戳：32768
                int rs = resizeStamp(n);
                // 条件成立：当前table正在扩容
                //   当前线程理论上应该协助table 完成扩容
                if (sc < 0) {
                    // 条件一：(sc >>> RESIZE_STAMP_SHIFT) != rs
                    //          true-> 说明当前线程获取到的扩容唯一标识戳 非 本次扩容
                    //          false-> 说明当前线程获取到的扩容唯一标识戳 是 本次扩容
                    // 条件二：jdk1.8中有bug_jira：其实想表达的是：sc == (rs << 16) + 1
                    //          true-> 表示扩容完毕，当前线程不需要再参与进来了
                    //          false-> 扩容还在进行时，当前线程可以参与进来
                    // 条件三：jdk1.8中有bug_jira：应该是：sc == rs << 16 + MAX_RESIZERS
                    //          true-> 表示当前参与并发扩容的线程达到最大值 65535 - 1
                    //          false-> 表示当前线程可以参与进来
                    // 条件四：(nt = nextTable) == null
                    //          true-> 表示本次扩容结束
                    //          false-> 扩容正在进行
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                        break;
                    // 前置条件：当前table 正在执行扩容中，当前线程有机会参与扩容
                    //   条件成立：说明当前线程成功参与到扩容任务中，并且将sc低16位加1，表示多了一个线程参与工作
                    //   条件失败：说明参与工作的线程比较多，cas修改失败，下次自旋  大概率还会来到这里
                    //   条件失败：1.当前很多线程都在此处尝试修改sizeCtl，有其它一个线程修成功，导致你的sc期望值与内存中的值不一致，修改失败
                    //           2.transfer  任务内部的线程也修改了sizeCtl
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        // 协助扩容线程，持有nextTable 参数
                        //在transfer 方法中，需要做一些扩容准备工作
                        transfer(tab, nt);
                }
                //RESIZE_STAMP_SHIFT = 16
                //      1000 0000 0001 1011   0000 0000 0000 0000 + 2
                // =>   1000 0000 0001 1011   0000 0000 0000 0010
                // 条件成立：说明当前线程是触发扩容的第一个线程，在transfer 方法中，需要做一些扩容准备工作
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                             (rs << RESIZE_STAMP_SHIFT) + 2))
                    // 触发扩容条件的线程，不持有nextTable 参数
                    transfer(tab, null);

                // 再次获取当前散列表的元素个数，期望值，再次自旋
                s = sumCount();
            }
        }
    }


```

## **transfer**

数据迁移 ，支持多线程协助做数据扩容，记录线程的数量（sizeCtl），对数据进行分段，每个线程负责一段数据的迁移，当每个线程完成数据迁移之后，退出的时候减掉协助扩容的线程数量。

```java
    /**
     * Moves and/or copies the nodes in each bin to new table. See
     * above for explanation.
     * 移动或复制
     */
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        // 判断当前可用线程是否大于1，大于1时则进行并行操作
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            try {
                @SuppressWarnings("unchecked")
                // 构造一个原来容量两倍的对象
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
        int nextn = nextTab.length;
        // 正在迁移的Node，该节点hash为MOVED，作为标志使用
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
        // 表示是否已经完成迁移
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab
        // i位置索引，bound边界
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;
            while (advance) {
                int nextIndex, nextBound;
                // 判断是否已经处理过
                if (--i >= bound || finishing)
                    advance = false;
                // 原数组的所有位置都有相应的线程去处理
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }
                else if (U.compareAndSwapInt
                         (this, TRANSFERINDEX, nextIndex,
                          nextBound = (nextIndex > stride ?
                                       nextIndex - stride : 0))) {
                    // 赋值迁移边界
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                // 迁移工作完成
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                // 使用cas来修改数量，代表完成当前任务
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    // 所有任务都完成
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            }
            // 如果位置i是空的，没有任何节点，放入刚刚实例化的 ForwardingNode
            else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);
            // 该位置是ForwardingNode，代表已经完成过迁移
            else if ((fh = f.hash) == MOVED)
                advance = true; // already processed
            else {
                // 对当前位置节点加锁
                synchronized (f) {
                    // 获取头节点
                    if (tabAt(tab, i) == f) {
                        Node<K,V> ln, hn;
                        // 头节点hash>=0代表为链表
                        if (fh >= 0) {
                            // 将链表进行划分，分成两部分进行迁移
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            // 分别存入两个链表中
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            // 其中的一个链表放在新数组的i位置
                            setTabAt(nextTab, i, ln);
                            // 另一个链表放在新数组的i+n位置(n为原长度)
                            setTabAt(nextTab, i + n, hn);
                            // 将原数组该位置处设置为 fwd，代表该位置已经处理完毕
                            setTabAt(tab, i, fwd);
                            // 迁移完成
                            advance = true;
                        }
                        // 当为红黑树时，开始树型迁移
                        else if (f instanceof TreeBin) {
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            // 进行划分，分为两部分迁移
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>
                                    (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            // 如果一分为二后，节点数少于8，那么将红黑树转换回链表
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            // 其中的一部分放在新数组的i位置
                            setTabAt(nextTab, i, ln);
                            // 另一部分放在新数组的i+n位置
                            setTabAt(nextTab, i + n, hn);
                            // 将原数组该位置处设置为 fwd，代表该位置已经处理完毕
                            setTabAt(tab, i, fwd);
                            // 迁移完毕
                            advance = true;
                        }
                    }
                }
            }
        }
    }
```

## **get**

```java
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key.equals(k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @throws NullPointerException if the specified key is null
     */
    public V get(Object key) {
        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
        // 获取二次hash后的值
        int h = spread(key.hashCode());
        // 当所有节点不为空，并且能找到对应节点时进入操作否则直接返回null
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (e = tabAt(tab, (n - 1) & h)) != null) {
            // 获取头节点，判断hash值是否相等
            if ((eh = e.hash) == h) {
                // hash值相等时需要判断key是否相等(解决碰撞问题)
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            }
            // 代表此时为树型结构，进行树的查找
            else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            // 遍历查找
            while ((e = e.next) != null) {
                if (e.hash == h &&
                    ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }
```

