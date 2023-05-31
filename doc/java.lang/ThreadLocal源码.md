# **介绍**

ThreadLocal是一个线程的本地变量，也就意味着这个变量是线程独有的，是不能与其他线程共享的。这样就可以避免资源竞争带来的多线程的问题。

但是，这种解决多线程安全问题的方式和加锁方式（synchronized、Lock) 是有本质的区别的，区别如下所示：

1> 关于资源的管理

当资源是多个线程共享时，访问的时候可以通过加锁的方式，逐一访问资源。

ThreadLocal是每个线程都有一个资源副本，是不需要加锁的。

2> 关于实现方式

锁是通过时间换空间的做法。

ThreadLocal是通过空间换时间的做法。

由于使用场景的不同，我们可以选择不同的技术手段，关键还是要看你的应用场景对于资源的管理是需要多线程之间共享还是单线程内部独享。

## **说明**

- 它能让线程拥有了自己内部独享的变量
- 每一个线程可以通过get、set方法去进行操作
- 可以覆盖initialValue方法指定线程独享的值
- 通常会用来修饰类里private static final的属性，为线程设置一些状态信息，例如user ID或者Transaction ID
- 每一个线程都有一个指向threadLocal实例的弱引用，只要线程一直存活或者该threadLocal实例能被访问到，都不会被垃圾回收清理掉

# **常量&变量**

```java
    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).  The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
     * 对象私有常量
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out. Updated atomically. Starts at
     * zero.
     * 共享原子对象
     */
    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     * 魔数0x61c88647，利用一定算法实现了元素的完美散列  对应十进制=1640531527。
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Returns the next hash code.
     * 获取下一个hashcode值
     */
    private static int nextHashCode() {
        // 获取并增加，原子操作
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }
```

# **构造方法**

```java

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     *
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * @return the initial value for this thread-local
     * 返回当前线程的这个线程局部变量的“初始值”  由子类重写该方法
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     * 创建线程局部变量
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * Creates a thread local variable.
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }

```

## 内部类

### SuppliedThreadLocal

```java
    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {
        //函数接口
        private final Supplier<? extends T> supplier;
        //在构造函数中赋值该函数接口
        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }
        //开接口，触发函数接口实现调用
        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }
```



### **ThreadLocalMap**

​    数据结构仅仅是**数组**

​    通过**开放地址法**来解决hash冲突的问题

​    Entry内部类中的key是**弱引用**，value是**强引用**

```java
    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two.
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
         */
        private Entry[] table;

        /**
         * The number of entries in the table.
         */
        private int size = 0;

        /**
         * The next size value at which to resize.
         */
        private int threshold; // Default to 0

        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
         * 阈值 = 容量 * 2/3 即负载因子为2/3
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * Increment i modulo len.
         * 从指定的下标i开始，向后获取下一个位置的下标值
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         * 从指定的下标i开始，前向获取上一个位置的下标值。
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         *  ThreadLocalMap构造函数
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            //table数组的默认大小 16
            table = new Entry[INITIAL_CAPACITY];
            //插入数组的下标
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            //创建待插入的entry对象
            table[i] = new Entry(firstKey, firstValue);
            //设置数组table中entry元素的个数为1
            size = 1;
            //设置数组table的阈值
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
            // 计算entry table索引
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                // 当entry的值不存在时
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    // 找到entry
                    return e;
                if (k == null)
                    // 移除过期条目
                    expungeStaleEntry(i);
                else
                    // 向下扫描
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.
            //table数组
            Entry[] tab = table;
            //table数组的长度
            int len = tab.length;
            //计算数组的下标  待插入entry的下标
            int i = key.threadLocalHashCode & (len-1);
            //通过哈希码和数组长度找到数组下标，从i开始往后寻找相等的ThreadLocal对象，没有就下一个index
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();
                //如果是相同的ThreadLocal对象，则将value值覆盖更新
                if (k == key) {
                    e.value = value;
                    return;
                }
                //key为null，表示ThreadLocal对象已经被GC回收 （虚引用）
                if (k == null) {
                    //替换待清除的entry，并清除历史key = null 的垃圾数据
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
            //没有找到 创建新的entry 插入到i的索引位置
            tab[i] = new Entry(key, value);
            //插入完entry后，最新的table数组长度
            int sz = ++size;
            //如果超过阈值，就需要rehash
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                //对table数组和元素进行整理（扩容等操作）
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    //调用entry的clear方法  这里是其父类  抽象类Reference的方法
                    //将弱引用的对象置null，有利于GC回收内存
                    e.clear();
                    //清除陈旧数据
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         *                   替换待清除的entry
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            //以slotToExpunge为中轴，向左遍历到的第一个空位置和向右遍历遇到的第一个空位置 之间，
            // 最左的第一个为旧的entry的下标
            int slotToExpunge = staleSlot;
            //从staleSlot位置向前遍历，将旧的entry空间释放
            //如果所有的槽位都被占满了，一直循环，直到有空的位置
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                //空位置
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            //从staleSlot位置向后遍历，
            //遇到与key相同的entry时，执行清除操作，返回，不再继续遍历
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                //找到相同的key，替换旧的值并且和前面那个过期的对象进行位置交换
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    //向左遍历没有找到旧的entry，将staleSlot位置的entry作为旧的
                    //向右遍历已经与旧的对象进行位置交换，待清理的位置为i
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    //清理旧数据
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                //slotToExpunge == staleSlot 向左遍历找到了空位置，那么会在向右遍历过程中寻找旧的entry
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            //将staleSlot位置的entry的value设置为GC可回收，
            tab[staleSlot].value = null;
            //在staleSlot位置创建一个新的entry
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            //如果有过期的对象，进行清理
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         * 清除陈旧的entry
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            //设置下标staleSlot对应的entry可被GC回收
            //置空value
            tab[staleSlot].value = null;
            //置空entry
            tab[staleSlot] = null;
            //数组元素减1
            size--;

            // Rehash until we encounter null
            //遍历指定删除节点的后续节点
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                //key为null，清除对应槽位的元素 size减1
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    //key不为null，后面的元素向前移动，重新获得下标
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        //如果不在同一个位置，位置发生了改变，就将原来的旧位置entry = null
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            //如果新的下标所在位置不为空，则从h开始往后遍历，直到找到空节点，插入
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         * 旧的entry是否已被清除
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
                //n=n/2
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         * 对table数组和元素进行整理
         */
        private void rehash() {
            //清除表中的陈旧数据
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            //清理完陈旧数据，如果仍然大于阈值的3/4，就执行扩容,
            if (size >= threshold - threshold / 4)
                //扩容
                resize();
        }

        /**
         * Double the capacity of the table.
         * 扩容  将table扩容2倍，并把老数据重新哈希散列进新的table
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            //扩容后的数组  2倍
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;
            //遍历旧的entry数组
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        //如果key = null，将value也置为null，有助于GC回收，防止内存泄漏
                        e.value = null; // Help the GC
                    } else {
                        //key不为空
                        //重新hash计算旧entry在新的table数组中的位置
                        int h = k.threadLocalHashCode & (newLen - 1);
                        //如果该位置已经被其他entry占用，向右（后）查找空位 直到找到一个没有使用的位置
                        while (newTab[h] != null)
                            //h递增
                            h = nextIndex(h, newLen);
                        //在找到的第一个空节点塞入e
                        newTab[h] = e;
                        //计数++ 记录保存的元素个数
                        count++;
                    }
                }
            }
            //扩容后，设置新的table数组的阈值   newLen的2/3
            setThreshold(newLen);
            //设置ThreadLocalMap的元素个数
            size = count;
            //将新table赋值给ThreadLocalMap中的entry[] table
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         * 清除表中的陈旧条目
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            //遍历
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                //entry不为空，但是entry的key为null
                if (e != null && e.get() == null)
                    //删除该下标对应的陈旧的entry
                    expungeStaleEntry(j);
            }
        }
    }
```

## **ThreadLocal、Thread、ThreadLocalMap、Entry之间的关系**

![image-20230531190833007](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305311908265.png)

# **常用方法**

## **get**

![image-20230531200415802](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305312004942.png)

```java
    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        // 获得当前线程
        Thread t = Thread.currentThread();
        // 从当前线程中获得ThreadLocalMap对象
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            // 获得对应entry对象
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                // 不为空，获取value正常返回
                return result;
            }
        }
        // 当ThreadLocalMap不存在时 说明还未初始化，初始化ThreadLocalMap并返回
        return setInitialValue();
    }
```

### 1.3getEntry

```java
        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
            // 计算entry table索引
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                // 当entry的值不存在时
                return getEntryAfterMiss(key, i, e);
        }
```

#### getEntryAfterMiss

```java
        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    // 找到entry
                    return e;
                if (k == null)
                    // 移除过期条目
                    expungeStaleEntry(i);
                else
                    // 向下扫描
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;
        }
```

##### expungeStaleEntry

```java
        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         * 清除陈旧的entry
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            //设置下标staleSlot对应的entry可被GC回收
            //置空value
            tab[staleSlot].value = null;
            //置空entry
            tab[staleSlot] = null;
            //数组元素减1
            size--;

            // Rehash until we encounter null
            //遍历指定删除节点的后续节点
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                //key为null，清除对应槽位的元素 size减1
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    //key不为null，后面的元素向前移动，重新获得下标
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        //如果不在同一个位置，位置发生了改变，就将原来的旧位置entry = null
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            //如果新的下标所在位置不为空，则从h开始往后遍历，直到找到空节点，插入
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }
```



### 1.4setInitialValue

```java
    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * @return the initial value
     * 设置初始化值
     */
    private T setInitialValue() {
        //该方法默认返回null，用户可以自定义
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            //如果map不为null，设置初始值value
            map.set(this, value);
        else
            //如果map为null，则创建一个map，设置初始化value
            createMap(t, value);
        return value;
    }
```

#### 1.4.5createMap

```java
    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     *     创建ThreadLocalMap并赋值
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }
```

##### ThreadLocalMap

```java
        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         *  ThreadLocalMap构造函数
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            //table数组的默认大小 16
            table = new Entry[INITIAL_CAPACITY];
            //插入数组的下标
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            //创建待插入的entry对象
            table[i] = new Entry(firstKey, firstValue);
            //设置数组table中entry元素的个数为1
            size = 1;
            //设置数组table的阈值
            setThreshold(INITIAL_CAPACITY);
        }
```

###### setThreshold

```java
        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
         * 阈值 = 容量 * 2/3 即负载因子为2/3
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }
```



## **set**



![image-20230531200602746](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305312006225.png)



```java
    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            //调用ThreadLocalMap的set方法 赋值
            map.set(this, value);
        else
            //map为空，创建新的ThreadLocalMap
            createMap(t, value);
    }
```

### 1.3set

==ThreadLocalMap.java==

```java
        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.
            //table数组
            Entry[] tab = table;
            //table数组的长度
            int len = tab.length;
            //计算数组的下标  待插入entry的下标
            int i = key.threadLocalHashCode & (len-1);
            //通过哈希码和数组长度找到数组下标，从i开始往后寻找相等的ThreadLocal对象，没有就下一个index
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();
                //如果是相同的ThreadLocal对象，则将value值覆盖更新
                if (k == key) {
                    e.value = value;
                    return;
                }
                //key为null，表示ThreadLocal对象已经被GC回收 （虚引用）
                if (k == null) {
                    //替换待清除的entry，并清除历史key = null 的垃圾数据
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
            //没有找到 创建新的entry 插入到i的索引位置
            tab[i] = new Entry(key, value);
            //插入完entry后，最新的table数组长度
            int sz = ++size;
            //如果超过阈值，就需要rehash
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                //对table数组和元素进行整理（扩容等操作）
                rehash();
        }
```

#### **replaceStaleEntry**

```java
        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         *                   替换待清除的entry
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            //以slotToExpunge为中轴，向左遍历到的第一个空位置和向右遍历遇到的第一个空位置 之间，
            // 最左的第一个为旧的entry的下标
            int slotToExpunge = staleSlot;
            //从staleSlot位置向前遍历，将旧的entry空间释放
            //如果所有的槽位都被占满了，一直循环，直到有空的位置
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                //空位置
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            //从staleSlot位置向后遍历，
            //遇到与key相同的entry时，执行清除操作，返回，不再继续遍历
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                //找到相同的key，替换旧的值并且和前面那个过期的对象进行位置交换
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    //向左遍历没有找到旧的entry，将staleSlot位置的entry作为旧的
                    //向右遍历已经与旧的对象进行位置交换，待清理的位置为i
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    //清理旧数据
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                //slotToExpunge == staleSlot 向左遍历找到了空位置，那么会在向右遍历过程中寻找旧的entry
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            //将staleSlot位置的entry的value设置为GC可回收，
            tab[staleSlot].value = null;
            //在staleSlot位置创建一个新的entry
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            //如果有过期的对象，进行清理
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }
```

##### **nextIndex和prevIndex**

```java
        /**
         * Increment i modulo len.
         * 从指定的下标i开始，向后获取下一个位置的下标值
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         * 从指定的下标i开始，前向获取上一个位置的下标值。
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }
```

##### **cleanSomeSlots**

```java
        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         * 旧的entry是否已被清除
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
                //n=n/2
            } while ( (n >>>= 1) != 0);
            return removed;
        }
```

##### rehash

```java
        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         * 对table数组和元素进行整理
         */
        private void rehash() {
            //清除表中的陈旧数据
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            //清理完陈旧数据，如果仍然大于阈值的3/4，就执行扩容,
            if (size >= threshold - threshold / 4)
                //扩容
                resize();
        }
```

###### expungeStaleEntries

```java
        /**
         * Expunge all stale entries in the table.
         * 清除表中的陈旧条目
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            //遍历
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                //entry不为空，但是entry的key为null
                if (e != null && e.get() == null)
                    //删除该下标对应的陈旧的entry
                    expungeStaleEntry(j);
            }
        }
```

###### resize

```java
        /**
         * Double the capacity of the table.
         * 扩容  将table扩容2倍，并把老数据重新哈希散列进新的table
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            //扩容后的数组  2倍
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;
            //遍历旧的entry数组
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        //如果key = null，将value也置为null，有助于GC回收，防止内存泄漏
                        e.value = null; // Help the GC
                    } else {
                        //key不为空
                        //重新hash计算旧entry在新的table数组中的位置
                        int h = k.threadLocalHashCode & (newLen - 1);
                        //如果该位置已经被其他entry占用，向右（后）查找空位 直到找到一个没有使用的位置
                        while (newTab[h] != null)
                            //h递增
                            h = nextIndex(h, newLen);
                        //在找到的第一个空节点塞入e
                        newTab[h] = e;
                        //计数++ 记录保存的元素个数
                        count++;
                    }
                }
            }
            //扩容后，设置新的table数组的阈值   newLen的2/3
            setThreshold(newLen);
            //设置ThreadLocalMap的元素个数
            size = count;
            //将新table赋值给ThreadLocalMap中的entry[] table
            table = newTab;
        }
```



## remove

![image-20230531200732361](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305312007515.png)

```java
    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     *
     * @since 1.5
     */
     public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             //调用ThreadLocalMap的remove删除
             m.remove(this);
     }
```

### 1.3remove

==ThreadLocalMap.java==

```java
        /**
         * Remove the entry for key.
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    //调用entry的clear方法  这里是其父类  抽象类Reference的方法
                    //将弱引用的对象置null，有利于GC回收内存
                    e.clear();
                    //清除陈旧数据
                    expungeStaleEntry(i);
                    return;
                }
            }
        }
```



# **答疑**

### **ThreadLocal内存溢出问题**

通过上面的分析，我们知道expungeStaleEntry() 方法是帮助垃圾回收的，根据源码，我们可以发现 get 和set 方法都可能触发清理方法expungeStaleEntry()，所以正常情况下是不会有内存溢出的。

但是如果我们没有调用get和set的时候就会可能面临着内存溢出。

养成好习惯在使用的时候调用remove()，加快垃圾回收，避免内存溢出。

退一步说，就算我们没有调用get和set和remove方法，线程结束的时候，也就没有强引用再指向ThreadLocal中的ThreadLocalMap了，这样ThreadLocalMap和里面的元素也会被回收掉。

但是有一种危险是，如果线程是线程池的，在线程执行完代码的时候并没有结束，只是归还给线程池，这个时候ThreadLocalMap和里面的元素是不会回收掉的，可能导致内存溢出。