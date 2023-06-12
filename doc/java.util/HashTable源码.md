# **介绍**

Hashtable是**线程安全**的，因为使用了synchronized关键字来保证线程安全。

Hashtable的key和value都**不能**为null。

Hashtable中数组默认大小11，扩容方式是 old*2+1

```java
public class Hashtable<K,V>
    extends Dictionary<K,V>
    implements Map<K,V>, Cloneable, java.io.Serializable
```

![image-20230612221048771](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306122210895.png)

# **常量&变量**

```java
    /**
     * The hash table data.
     * hash表数组
     */
    private transient Entry<?,?>[] table;

    /**
     * The total number of entries in the hash table.
     * 元素个数
     */
    private transient int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     * @serial
     * 扩容阈值
     */
    private int threshold;

    /**
     * The load factor for the hashtable.
     *
     * @serial
     * 负载因子
     */
    private float loadFactor;

    /**
     * The number of times this Hashtable has been structurally modified
     * Structural modifications are those that change the number of entries in
     * the Hashtable or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the Hashtable fail-fast.  (See ConcurrentModificationException).
     * 修改次数
     */
    private transient int modCount = 0;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    private static final long serialVersionUID = 1421746759512286392L;
```

# **构造方法**

```java
    /**
     * Constructs a new, empty hashtable with the specified initial
     * capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the hashtable.
     * @param      loadFactor        the load factor of the hashtable.
     * @exception  IllegalArgumentException  if the initial capacity is less
     *             than zero, or if the load factor is nonpositive.
     *  使用指定的初始容量和指定的负载因子构造一个新的空哈希表。
     */
    public Hashtable(int initialCapacity, float loadFactor) {
        //如果指定容量小于0，不用存了直接报错
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        //如果负载因子小于0，或者loadFactor是非数字的数
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load: "+loadFactor);
        //如果初始容量为空，则设置为1
        if (initialCapacity==0)
            initialCapacity = 1;
        //负载因子赋值
        this.loadFactor = loadFactor;
        //初始化table
        table = new Entry<?,?>[initialCapacity];
        //扩容阈值赋值
        threshold = (int)Math.min(initialCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    }

    /**
     * Constructs a new, empty hashtable with the specified initial capacity
     * and default load factor (0.75).
     *
     * @param     initialCapacity   the initial capacity of the hashtable.
     * @exception IllegalArgumentException if the initial capacity is less
     *              than zero.
     *  使用指定的初始容量和默认加载因子 (0.75) 构造一个新的空哈希表。
     */
    public Hashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty hashtable with a default initial capacity (11)
     * and load factor (0.75).
     * 构造一个具有默认初始容量 (11) 和负载因子 (0.75) 的空哈希表。
     */
    public Hashtable() {
        this(11, 0.75f);
    }

    /**
     * Constructs a new hashtable with the same mappings as the given
     * Map.  The hashtable is created with an initial capacity sufficient to
     * hold the mappings in the given Map and a default load factor (0.75).
     *
     * @param t the map whose mappings are to be placed in this map.
     * @throws NullPointerException if the specified map is null.
     * @since   1.2
     * 构造一个与给定 Map 具有相同映射关系的新哈希表。
     * 哈希表的初始容量足以容纳给定 Map 中的映射和默认负载因子 (0.75)。
     */
    public Hashtable(Map<? extends K, ? extends V> t) {
        //设置容量为传入map的两倍，如果还小于默认容量就是用默认容量11
        this(Math.max(2*t.size(), 11), 0.75f);
        //将传入哈希表t放入table里
        putAll(t);
    }
```

## **内部类**

### **KeySet**

```java
    //key的set集合
    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return getIterator(KEYS);
        }
        public int size() {
            return count;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return Hashtable.this.remove(o) != null;
        }
        public void clear() {
            Hashtable.this.clear();
        }
    }
```

### **EntrySet**

```java
    //entry的set集合
    private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return getIterator(ENTRIES);
        }

        public boolean add(Map.Entry<K,V> o) {
            return super.add(o);
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
            Object key = entry.getKey();
            Entry<?,?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            for (Entry<?,?> e = tab[index]; e != null; e = e.next)
                if (e.hash==hash && e.equals(entry))
                    return true;
            return false;
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object key = entry.getKey();
            Entry<?,?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            @SuppressWarnings("unchecked")
            Entry<K,V> e = (Entry<K,V>)tab[index];
            for(Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
                if (e.hash==hash && e.equals(entry)) {
                    modCount++;
                    if (prev != null)
                        prev.next = e.next;
                    else
                        tab[index] = e.next;

                    count--;
                    e.value = null;
                    return true;
                }
            }
            return false;
        }

        public int size() {
            return count;
        }

        public void clear() {
            Hashtable.this.clear();
        }
    }
```

### **ValueCollection**

```java
    //value的Collection集合
    private class ValueCollection extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return getIterator(VALUES);
        }
        public int size() {
            return count;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            Hashtable.this.clear();
        }
    }
```

### **Entry**

```java
    /**
     * Hashtable bucket collision list entry
     */
    private static class Entry<K,V> implements Map.Entry<K,V> {
        //key的hash值
        final int hash;
        //key值
        final K key;
        //value值
        V value;
        //哈希冲突产生的链表  下一个节点
        Entry<K,V> next;

        protected Entry(int hash, K key, V value, Entry<K,V> next) {
            this.hash = hash;
            this.key =  key;
            this.value = value;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
        protected Object clone() {
            return new Entry<>(hash, key, value,
                                  (next==null ? null : (Entry<K,V>) next.clone()));
        }

        // Map.Entry Ops

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            if (value == null)
                throw new NullPointerException();

            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;

            return (key==null ? e.getKey()==null : key.equals(e.getKey())) &&
               (value==null ? e.getValue()==null : value.equals(e.getValue()));
        }

        public int hashCode() {
            return hash ^ Objects.hashCode(value);
        }

        public String toString() {
            return key.toString()+"="+value.toString();
        }
    }
```

### **Enumerator**

```java
    // Types of Enumerations/Iterations
    // 用于获取哈希表中的key或者value时指定一个类型
    // 返回相应的枚举器
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    /**
     * A hashtable enumerator class.  This class implements both the
     * Enumeration and Iterator interfaces, but individual instances
     * can be created with the Iterator methods disabled.  This is necessary
     * to avoid unintentionally increasing the capabilities granted a user
     * by passing an Enumeration.
     */
    private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
        Entry<?,?>[] table = Hashtable.this.table;
        int index = table.length;
        Entry<?,?> entry;
        Entry<?,?> lastReturned;
        int type;

        /**
         * Indicates whether this Enumerator is serving as an Iterator
         * or an Enumeration.  (true -> Iterator).
         */
        boolean iterator;

        /**
         * The modCount value that the iterator believes that the backing
         * Hashtable should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        protected int expectedModCount = modCount;

        Enumerator(int type, boolean iterator) {
            this.type = type;
            this.iterator = iterator;
        }

        public boolean hasMoreElements() {
            Entry<?,?> e = entry;
            int i = index;
            Entry<?,?>[] t = table;
            /* Use locals for faster loop iteration */
            while (e == null && i > 0) {
                e = t[--i];
            }
            entry = e;
            index = i;
            return e != null;
        }

        @SuppressWarnings("unchecked")
        public T nextElement() {
            Entry<?,?> et = entry;
            int i = index;
            Entry<?,?>[] t = table;
            /* Use locals for faster loop iteration */
            while (et == null && i > 0) {
                et = t[--i];
            }
            entry = et;
            index = i;
            if (et != null) {
                Entry<?,?> e = lastReturned = entry;
                entry = e.next;
                return type == KEYS ? (T)e.key : (type == VALUES ? (T)e.value : (T)e);
            }
            throw new NoSuchElementException("Hashtable Enumerator");
        }

        // Iterator methods
        public boolean hasNext() {
            return hasMoreElements();
        }

        public T next() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return nextElement();
        }

        public void remove() {
            if (!iterator)
                throw new UnsupportedOperationException();
            if (lastReturned == null)
                throw new IllegalStateException("Hashtable Enumerator");
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            synchronized(Hashtable.this) {
                Entry<?,?>[] tab = Hashtable.this.table;
                int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

                @SuppressWarnings("unchecked")
                Entry<K,V> e = (Entry<K,V>)tab[index];
                for(Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
                    if (e == lastReturned) {
                        modCount++;
                        expectedModCount++;
                        if (prev == null)
                            tab[index] = e.next;
                        else
                            prev.next = e.next;
                        count--;
                        lastReturned = null;
                        return;
                    }
                }
                throw new ConcurrentModificationException();
            }
        }
    }
```

# **常用方法**

## **hash**值和index计算

hash值

- HashTable中的hash值计算直接使用了Object的hashcode()方法计算
- HashMap(1.8中)：

```java
/**
 * Returns the hash code value for this Map as per the definition in the
 * Map interface.
 *
 * @see Map#hashCode()
 * @since 1.2
 */
public synchronized int hashCode() {
    /*
     * This code detects the recursion caused by computing the hash code
     * of a self-referential hash table and prevents the stack overflow
     * that would otherwise result.  This allows certain 1.1-era
     * applets with self-referential hash tables to work.  This code
     * abuses the loadFactor field to do double-duty as a hashCode
     * in progress flag, so as not to worsen the space performance.
     * A negative load factor indicates that hash code computation is
     * in progress.
     */
    int h = 0;
    if (count == 0 || loadFactor < 0)
        return h;  // Returns zero

    loadFactor = -loadFactor;  // Mark hashCode computation in progress
    Entry<?,?>[] tab = table;
    for (Entry<?,?> entry : tab) {
        while (entry != null) {
            h += entry.hashCode();
            entry = entry.next;
        }
    }

    loadFactor = -loadFactor;  // Mark hashCode computation complete

    return h;
}


    /**
 * Hashtable bucket collision list entry
 */
private static class Entry<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Entry<K,V> next;

    protected Entry(int hash, K key, V value, Entry<K,V> next) {
        this.hash = hash;
        this.key =  key;
        this.value = value;
        this.next = next;
    }

    @SuppressWarnings("unchecked")
    protected Object clone() {
        return new Entry<>(hash, key, value,
                              (next==null ? null : (Entry<K,V>) next.clone()));
    }

    // Map.Entry Ops

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        if (value == null)
            throw new NullPointerException();

        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;

        return (key==null ? e.getKey()==null : key.equals(e.getKey())) &&
           (value==null ? e.getValue()==null : value.equals(e.getValue()));
    }

    public int hashCode() {
        return hash ^ Objects.hashCode(value);
    }

    public String toString() {
        return key.toString()+"="+value.toString();
    }
}

```



```java
Objects.java
/**
 * Returns the hash code of a non-{@code null} argument and 0 for
 * a {@code null} argument.
 *
 * @param o an object
 * @return the hash code of a non-{@code null} argument and 0 for
 * a {@code null} argument
 * @see Object#hashCode
 */
public static int hashCode(Object o) {
    return o != null ? o.hashCode() : 0;
}
```

value是什么类型，就调用什么类型的hashcode方法

```java
Object.java

/**
 * Returns a hash code value for the object. This method is
 * supported for the benefit of hash tables such as those provided by
 * {@link java.util.HashMap}.
 * <p>
 * The general contract of {@code hashCode} is:
 * <ul>
 * <li>Whenever it is invoked on the same object more than once during
 *     an execution of a Java application, the {@code hashCode} method
 *     must consistently return the same integer, provided no information
 *     used in {@code equals} comparisons on the object is modified.
 *     This integer need not remain consistent from one execution of an
 *     application to another execution of the same application.
 * <li>If two objects are equal according to the {@code equals(Object)}
 *     method, then calling the {@code hashCode} method on each of
 *     the two objects must produce the same integer result.
 * <li>It is <em>not</em> required that if two objects are unequal
 *     according to the {@link java.lang.Object#equals(java.lang.Object)}
 *     method, then calling the {@code hashCode} method on each of the
 *     two objects must produce distinct integer results.  However, the
 *     programmer should be aware that producing distinct integer results
 *     for unequal objects may improve the performance of hash tables.
 * </ul>
 * <p>
 * As much as is reasonably practical, the hashCode method defined by
 * class {@code Object} does return distinct integers for distinct
 * objects. (This is typically implemented by converting the internal
 * address of the object into an integer, but this implementation
 * technique is not required by the
 * Java&trade; programming language.)
 *
 * @return  a hash code value for this object.
 * @see     java.lang.Object#equals(java.lang.Object)
 * @see     java.lang.System#identityHashCode
 */
public native int hashCode();
```

index

- HashTable中的index下标（对应位桶数组的下标）直接用hash值对表长取余(hash & 0x7FFFFFFF) % tab.length;

## **put**

```java
    /**
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable. Neither the key nor the
     * value can be <code>null</code>. <p>
     *
     * The value can be retrieved by calling the <code>get</code> method
     * with a key that is equal to the original key.
     *
     * @param      key     the hashtable key
     * @param      value   the value
     * @return     the previous value of the specified key in this hashtable,
     *             or <code>null</code> if it did not have one
     * @exception  NullPointerException  if the key or value is
     *               <code>null</code>
     * @see     Object#equals(Object)
     * @see     #get(Object)
     *
     * 存入键值对key-value  键和值都不能是 null。  key重复则执行value值的更新，key不存在则正常添此加键值对。
     * 可以通过使用与原始键相同的键调用 get 方法来检索该值。
     */
    public synchronized V put(K key, V value) {
        // Make sure the value is not null
        //确保该值不为空
        if (value == null) {
            throw new NullPointerException();
        }

        // Makes sure the key is not already in the hashtable.
        //创建tab数组指向table
        Entry<?,?> tab[] = table;
        //计算hash值
        int hash = key.hashCode();
        //key对应的位桶下标(hash值对表长度取余)
        int index = (hash & 0x7FFFFFFF) % tab.length;
        //创建entry指向对应位置的位桶头结点
        @SuppressWarnings("unchecked")
        Entry<K,V> entry = (Entry<K,V>)tab[index];
        //遍历链表中的节点
        for(; entry != null ; entry = entry.next) {
            //判断结点的hash值和key是否与传入的相等  相等则保存新值返回旧值
            if ((entry.hash == hash) && entry.key.equals(key)) {
                
                V old = entry.value;
                //保存新值覆盖旧值
                entry.value = value;
                //返回旧值
                return old;
            }
        }
        //如果没有找到key相等的结点,调用addEntry方法将结点添加进表里
        addEntry(hash, key, value, index);
        //返回空
        return null;
    }
```

## **addEntry**

头插法添加新节点

```java
    private void addEntry(int hash, K key, V value, int index) {
        //修改次数+1
        modCount++;
        //创建指针保存table地址
        Entry<?,?> tab[] = table;
        //如果元素个数>扩容阈值
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            // 如果超过阈值，则重新散列表(扩容)
            rehash();
            //指向新表
            tab = table;
            //重新计算hash值
            hash = key.hashCode();
            //计算索引位置
            index = (hash & 0x7FFFFFFF) % tab.length;
        }
        // 创建新结点
        // 位桶数组index位置
        // 创建辅助指针e指向头结点
        // Creates the new entry.
        @SuppressWarnings("unchecked")
        Entry<K,V> e = (Entry<K,V>) tab[index];
        //头插入  结点放入桶中，新结点的next指针指向e(原头结点)  完成头插
        tab[index] = new Entry<>(hash, key, value, e);
        //元素个数+1
        count++;
    }
```

## **rehash**

实现扩容

```java
/**
 * Increases the capacity of and internally reorganizes this
 * hashtable, in order to accommodate and access its entries more
 * efficiently.  This method is called automatically when the
 * number of keys in the hashtable exceeds this hashtable's capacity
 * and load factor.
 */
@SuppressWarnings("unchecked")
protected void rehash() {
    //保存旧容量
    int oldCapacity = table.length;
    //oldMap保存旧表
    Entry<?,?>[] oldMap = table;

    // overflow-conscious code
    // 新容量为旧容量的两倍+1
    int newCapacity = (oldCapacity << 1) + 1;
    //如果新容量>integer的最大值-8
    if (newCapacity - MAX_ARRAY_SIZE > 0) {
        //如果旧容量为integer的最大值-8
        if (oldCapacity == MAX_ARRAY_SIZE)
            // Keep running with MAX_ARRAY_SIZE buckets
            // 使用旧容量继续跑
            return;
        //旧容量设置为integer的最大值-8
        newCapacity = MAX_ARRAY_SIZE;
    }
    //创建容量为newCapacity的hash表
    Entry<?,?>[] newMap = new Entry<?,?>[newCapacity];
    //修改次数+
    modCount++;
    //重新计算扩容阈值
    threshold = (int)Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    //table指向新表
    table = newMap;
    //遍历旧表进行数据转移
    for (int i = oldCapacity ; i-- > 0 ;) {
        //从最后一个桶开始遍历，old指向链表头结点
        for (Entry<K,V> old = (Entry<K,V>)oldMap[i] ; old != null ; ) {
            //辅助指针e先指向头结点
            Entry<K,V> e = old;
            //old后移
            old = old.next;
            //计算新位置
            int index = (e.hash & 0x7FFFFFFF) % newCapacity;
            //e的next指向新表对应位置的头结点
            e.next = (Entry<K,V>)newMap[index];
            //头插法  (e已经连接了原头结点）
            newMap[index] = e;
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
     * {@code k} to a value {@code v} such that {@code (key.equals(k))},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     * @throws NullPointerException if the specified key is null
     * @see     #put(Object, Object)
     * 返回指定键映射到的值，如果此映射不包含键的映射，则返回 null
     */
    @SuppressWarnings("unchecked")
    public synchronized V get(Object key) {
        Entry<?,?> tab[] = table;
        //计算key的hash值
        int hash = key.hashCode();
        //计算下标
        int index = (hash & 0x7FFFFFFF) % tab.length;
        //遍历对应位置的桶，从头结点开始
        for (Entry<?,?> e = tab[index] ; e != null ; e = e.next) {
            //如果找到hash值和key与传入的相等的则返回对应的值
            if ((e.hash == hash) && e.key.equals(key)) {
                return (V)e.value;
            }
        }
        //找不到返回空
        return null;
    }
```

## **remove**

```java
    /**
     * Removes the key (and its corresponding value) from this
     * hashtable. This method does nothing if the key is not in the hashtable.
     *
     * @param   key   the key that needs to be removed
     * @return  the value to which the key had been mapped in this hashtable,
     *          or <code>null</code> if the key did not have a mapping
     * @throws  NullPointerException  if the key is <code>null</code>
     * 从此哈希表中删除key（及其相应的value）。
     * 如果key不在哈希表中，则此方法不执行任何操作。
     */
    public synchronized V remove(Object key) {
        Entry<?,?> tab[] = table;
        //计算hash值
        int hash = key.hashCode();
        //计算位置
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        //辅助指针指向头结点
        Entry<K,V> e = (Entry<K,V>)tab[index];
        //prev记录前一个结点,e为当前结点
        //遍历桶
        for(Entry<K,V> prev = null ; e != null ; prev = e, e = e.next) {
            //如果hash值和key和结点对应相等
            if ((e.hash == hash) && e.key.equals(key)) {
                //修改次数+1
                modCount++;
                if (prev != null) {
                    //前一个结点指针当前结点的后一个结点(越过当前结点，相当于删除)
                    prev.next = e.next;
                } else {
                    //如果是头结点为要删除的结点，将桶头结点设置为头结点的下一个结点
                    tab[index] = e.next;
                }
                //元素个数-1
                count--;
                //保存旧值
                V oldValue = e.value;
                //将e的值设为空
                e.value = null;
                //返回旧值
                return oldValue;
            }
        }
        //如果没找到对应的 返回空
        return null;
    }
```

