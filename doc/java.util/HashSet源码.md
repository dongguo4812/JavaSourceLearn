# **介绍**

继承了AbstractSet，实现了Set, Cloneable,Serializable。内部是依赖HashMap的实现，所以HashSet的结构比较简单。不保证元素的迭代顺序一直不变，允许有一个null。非同步的，可以通过

Set s = Collections.synchronizedSet(new HashSet());

进行包装，实现对外同步。

```java
public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable
```

![image-20230610065930059](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306100659263.png)

# **常量&变量**

```java
    //序列化版本号
    static final long serialVersionUID = -5024744406713321676L;

    //依赖的HashMap实例 HashSet的底层就是由HashMap来进行实现的，就是把key存储在map的key中，value用一个空对象PRESENT填补。
    private transient HashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    //一个static final的空对象，用于填补hashmap的key对应的value
    private static final Object PRESENT = new Object();
```

# **构造方法**

```java
    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * default initial capacity (16) and load factor (0.75).
     * 默认构造，构造一个空set，内部的HashMap实例容量为初始的16，负载因子0.75
     */
    public HashSet() {
        map = new HashMap<>();
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The <tt>HashMap</tt> is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     * 用足够的容量和默认的负载因子构造，包含集合里的元素
     */
    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     *  用给定的容量和负载因子构造
     */
    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and default load factor (0.75).
     *
     * @param      initialCapacity   the initial capacity of the hash table
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero
     *  用给定的容量和默认的负载因子构造
     */
    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    /**
     * Constructs a new, empty linked hash set.  (This package private
     * constructor is only used by LinkedHashSet.) The backing
     * HashMap instance is a LinkedHashMap with the specified initial
     * capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the hash map
     * @param      loadFactor        the load factor of the hash map
     * @param      dummy             ignored (distinguishes this
     *             constructor from other int, float constructor.)
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero, or if the load factor is nonpositive
     */
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }
```

# **常用方法**

HashSet内部是依赖HashMap实现的，调用的是HashMap中的的方法，请结合HashMap源码查看

```java
    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return the number of elements in this set (its cardinality)
     * 返回一个这个set中元素个数。
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>true</tt> if this set contains no elements
     * set是否为空
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }
     /**
     * Removes all of the elements from this set.
     * The set will be empty after this call returns.
     * 清空set
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this set
     * contains an element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this set is to be tested
     * @return <tt>true</tt> if this set contains the specified element
     * 调用HashMap#containsKey查找，因为set内元素被当做key存储在hashmap中
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * More formally, adds the specified element <tt>e</tt> to this set if
     * this set contains no element <tt>e2</tt> such that
     * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
     * If this set already contains the element, the call leaves the set
     * unchanged and returns <tt>false</tt>.
     *
     * @param e element to be added to this set
     * @return <tt>true</tt> if this set did not already contain the specified
     * element
     * 将e当做key插入，因为key是不可重复的，也就保证了e在set中唯一
     */
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }
```

