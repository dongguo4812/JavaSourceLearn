# 介绍

LinkedHashSet是HashSet的子类

LinkedHashSet底层是一个LinkedHashMap，底层维护了一个数组+双向链表

LinkedHashSet根据元素的hashCOde值来决定元素的存储位置，同时使用链表维护元素的次序，这使元素看起来以插入顺序保存的

LinkedHashSet不允许添加重复元素

LinkedHashSet 是在 HashSet 的基础上，维护了元素的插入顺序。虽然 LinkedHashSet 使用了 HashSet 的实现，但其却调用了 LinkedHashMap 作为最终实现，从而实现了对插入元素顺序的维护。

**由于底层维护着是一个双向链表和数组，所以插入和取出的顺序是一致的,原因是用了链表维护元素添加的顺序**

```java
public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable 
```

![image-20230612072054442](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306120721486.png)

# **构造方法**

参考hashset源码

```java
    /**
     * Constructs a new, empty linked hash set with the specified initial
     * capacity and load factor.
     *
     * @param      initialCapacity the initial capacity of the linked hash set
     * @param      loadFactor      the load factor of the linked hash set
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive
     *   构造具有指定的初始容量和负载因子的新的，空的链接散列集。
     */
    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    /**
     * Constructs a new, empty linked hash set with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param   initialCapacity   the initial capacity of the LinkedHashSet
     * @throws  IllegalArgumentException if the initial capacity is less
     *              than zero
     *   构造一个具有指定初始容量和默认负载因子（0.75）的新的，空的链接散列集。
     */
    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    /**
     * Constructs a new, empty linked hash set with the default initial
     * capacity (16) and load factor (0.75).
     * 构造一个具有默认初始容量（16）和负载因子（0.75）的新的，空的链接散列集。
     */
    public LinkedHashSet() {
        super(16, .75f, true);
    }

    /**
     * Constructs a new linked hash set with the same elements as the
     * specified collection.  The linked hash set is created with an initial
     * capacity sufficient to hold the elements in the specified collection
     * and the default load factor (0.75).
     *
     * @param c  the collection whose elements are to be placed into
     *           this set
     * @throws NullPointerException if the specified collection is null
     * 构造与指定集合相同的元素的新的链接散列集。
     */
    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }
```

在LinkedHashSet中维护一个hash表和双向链表【LinkedHashSet有head和tail】

每一个节点有before和after属性，这样可以形成双向链表

在添加一个元素时，先求hash值，在求索引，确定该元素在hashtable的位置，然后将添加的元素加入到双向链表【如果已经存在，则不添加【原则和HashSet一样】