# **介绍**

HashMap基于哈希表的Map接口实现，是以key-value存储形式存在，即主要用来存放键值对。HashMap 的实现不是同步的，这意味着它不是线程安全的。它的key、value都可以为null。此外，HashMap中的映射不是有序的。

## **特点：**

1.存取无序的

2.键和值位置都可以是null，但是键位置只能是一个null

3.键位置是唯一的，底层的数据结构控制键的

4.jdk1.8前数据结构是：链表 + 数组 jdk1.8之后是 ： 链表 + 数组 + 红黑树

5.阈值(边界值) > 8 并且数组长度大于64，才将链表转换为红黑树，变为红黑树的目的是为了高效的查询。

HashMap继承于AbstractMap，实现了Map、Cloneable、Serializable接口。

在JDK1.8 之前 HashMap 由 **数组+链表** 数据结构组成的，数组是 HashMap 的主体，链表则是主要为了解决哈希冲突(两个对象调用的hashCode方法计算的哈希码值一致导致计算的数组索引值相同)而存在的  

在JDK1.8 之后 HashMap 由 **数组+链表 +红黑树**数据结构组成的，解决哈希冲突时有了较大的变化，**当链表长度大于阈值（或者红黑树的边界值，默认为 8）并且当前数组的长度大于64时，此时此索引位置上的所有数据改为使用红黑树存储。**

补充：将链表转换成红黑树前会判断，即使阈值大于8，但是数组长度小于64，此时并不会将链表变为红黑树。而是选择进行数组扩容。

这样做的目的是因为数组比较小，尽量避开红黑树结构，这种情况下变为红黑树结构，反而会降低效率，因为红黑树需要进行左旋，右旋，变色这些操作来保持平衡 。同时数组长度小于64时，搜索时间相对要快些。所以综上所述为了提高性能和减少搜索时间，底层在阈值大于8并且数组长度大于64时，链表才转换为红黑树。具体可以参考 treeifyBin方法。

当然虽然增了红黑树作为底层数据结构，结构变得复杂了，但是阈值大于8并且数组长度大于64时，链表转换为红黑树时，效率也变的更高效。

```java
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable
```

- Cloneable 空接口，表示可以克隆。 创建并返回HashMap对象的一个副本。
- Serializable 序列化接口。属于标记性接口。HashMap对象可以被序列化和反序列化。
- AbstractMap 父类提供了Map实现接口。以最大限度地减少实现此接口所需的工作。

![image-20230608205119208](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082051387.png)

补充：通过上述继承关系我们发现一个很奇怪的现象， 就是HashMap已经继承了AbstractMap而AbstractMap类实现了Map接口，那为什么HashMap还要在实现Map接口呢？同样在ArrayList中LinkedList中都是这种结构。

据 java 集合框架的创始人Josh Bloch描述，这样的写法是一个失误。在java集合框架中，类似这样的写法很多，最开始写java集合框架的时候，他认为这样写，在某些地方可能是有价值的，直到他意识到错了。JDK的维护者后来不认为这个小小的失误值得去修改，所以就这样存在下来了。

# **常量&变量**

```java
    //序列化版本号
     private static final long serialVersionUID = 362498820763181265L;
    
     /**
     * The default initial capacity - MUST be a power of two.
     *  默认初始容量必须是2的幂，这里是16
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     *  集合最大容量（必须是2的幂且小于2的30次方，如果在构造函数中传入过大的容量参数将被这个值替换）
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     * 默认负载因子，啥叫负载因子呢，HashMap通过负载因子与桶的数量计算得到所能容纳的最大元素数量
     * 计算公式为threshold = capacity * loadFactor
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     * 1.8中链表转化为红黑树的阈值
     *  当桶(bucket)上的节点数大于这个值时会转成红黑树
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     * 红黑树转化为链表的阈值，扩容时才可能发生
     * 当桶(bucket)上的节点数小于这个值时树转链表
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     * 进行树化的最小容量，防止在调整容量和形态时发生冲突
     * 当Map里面的数量超过这个值时，表中的桶才能进行树形化 ，否则桶内元素太多时会扩容，而不是树形化 
     * 为了避免进行扩容、树形化选择的冲突，这个值不能小于 4 * TREEIFY_THRESHOLD (8)
     */
    static final int MIN_TREEIFY_CAPACITY = 64;
    
    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     * 存储元素的数组  用来初始化(必须是二的n次幂)
     * table在JDK1.8中我们了解到HashMap是由数组加链表加红黑树来组成的结构
     * 其中table就是HashMap中的数组，jdk8之前数组类型是Entry<K,V>类型。
     * 从jdk1.8之后是Node<K,V>类型。只是换了个名字，都实现了一样的接口：Map.Entry<K,V>。
     * 负责存储键值对数据的。
     */
    transient Node<K,V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     * 用来存放缓存  存放具体元素的集合
     */
    transient Set<Map.Entry<K,V>> entrySet;

    /**
     * The number of key-value mappings contained in this map.
     *  HashMap中存放元素的个数 size为HashMap中K-V的实时数量，不是数组table的长度
     */
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     * 用来记录HashMap的修改次数  每次扩容和更改map结构都要增加1
     */
    transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
    // 临界值 当实际大小(容量*负载因子)超过临界值时，会进行扩容
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     * 加载因子
     */
    final float loadFactor;

```

# **构造方法**

```java
    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity  指定的容量
     * @param  loadFactor      the load factor   指定的加载因子
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     *   构造一个具有指定的初始容量和负载因子的 HashMap
     */
     public HashMap(int initialCapacity, float loadFactor) {
        //判断初始化容量
        if (initialCapacity < 0)
            //如果小于0，则抛出非法的参数异常
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        //判断初始化容量initialCapacity是否大于集合的最大容量MAXIMUM_CAPACITY-》2的30次幂
        if (initialCapacity > MAXIMUM_CAPACITY)
            //如果超过MAXIMUM_CAPACITY，会将MAXIMUM_CAPACITY赋值给initialCapacity
            initialCapacity = MAXIMUM_CAPACITY;
        //判断负载因子loadFactor是否小于等于0或者是否是一个非数值
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            //如果满足上述其中之一，则抛出非法的参数异常IllegalArgumentException
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        //将指定的加载因子赋值给HashMap成员变量的负载因子loadFactor
        this.loadFactor = loadFactor;
        /**
         * tableSizeFor(initialCapacity) 判断指定的初始化容量是否是2的n次幂，
         * 如果不是   那么会变为比指定初始化容量大的最小的2的n次幂。
         * 但是注意，在tableSizeFor方法体内部将计算后的数据返回给调用这里了，并且直接赋值给threshold边界值了。
         * 有些人会觉得这里是一个bug,应该这样书写：  this.threshold = tableSizeFor(initialCapacity) * this.loadFactor;
         * 这样才符合threshold的意思（当HashMap的size到达threshold这个阈值时会扩容）。
         * 但是，请注意，在jdk8以后的构造方法中，并没有对table这个成员变量进行初始化，
         * table的初始化被推迟到了put方法中，在put方法中会对threshold重新计算，put方法的具体实现我们下面会进行讲解
         */
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     * 构造一个具有指定的初始容量和默认负载因子（0.75
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     * 构造一个空的 HashMap ，默认初始容量（16）和默认负载因子（0.75）
     *
     */
    public HashMap() {
        // 将默认的加载因子0.75赋值给loadFactor，默认初始化容量16，
        // 但并没有创建数组，在put时当数组为空，以默认容量16构造一个数组。
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     * 构造一个映射关系与指定Map 相同的新HashMap
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        //负载因子loadFactor变为默认的负载因子0.75
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }
```

## **initialCapacity为什么必须是2的n次幂？**

当向HashMap中添加一个元素的时候，需要根据key的hash值，去确定其在数组中的具体位置。 HashMap为了存取高效，要尽量减少碰撞，就是要尽量把数据分配均匀，每个链表长度大致相同，这个实现就在把数据存到哪个链表中的算法。

这个算法实际就是取模，hash%length，计算机中直接求余效率不如位移运算。所以源码中做了优化,使用 hash&(length-1)，而实际上hash%length等于hash&(length-1)的前提是length是2的n次幂。

### **为什么这样能均匀分布减少碰撞呢？**

2的n次方二进制表现实际就是1后面n个0，2的n次方-1 实际就是n个1；

**hash&(length-1)**

**例如长度为8时候，hash码分别为2,3**

长度length为8时候，8是2的3次幂。二进制是：1000

​                                           length-1 二进制是： 1000 - 1   =  0111

hash码2的二进制是  0010

hash&(length-1) :

0010     2

0111     7

\----------

0010    2

hash码3的二进制是  0011

hash&(length-1) :

0011     3

0111     7

\----------

0011    3

数组下标说明：上述计算结果是不同位置上，不碰撞；

**例如长度为9时候，hash码分别为2,3**

长度length为9时候，9不是2的n次幂。二进制是：1001

   length-1 二进制是： 1001- 1 = 1000

hash码2的二进制是  0010

hash&(length-1) :

0010     2

1000     8

\----------

000     0

hash码3的二进制是  0011

hash&(length-1) :

0011     3

1000     8

\----------

0000    0

数组下标说明：上述计算结果都在0上，碰撞了；

**注意： 当然如果不考虑效率直接求余即可（就不需要要求长度必须是2的n次方了）**

### **initialCapacity如果输入值不是2的幂比如10会怎么样？**

```java
     /**
     * Returns a power of two size for the given target capacity.
     * 返回比指定初始化容量大的最小的2的n次
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
```

当在实例化HashMap实例时，如果给定了initialCapacity(假设是10)，由于HashMap的capacity必须都是2的幂，因此这个方法用于找到大于等于initialCapacity(假设是10)的最小的2的幂（initialCapacity如果就是2的幂，则返回的还是这个数）。

下面分析这个算法：

\1) 首先，为什么要对cap做减1操作。int n = cap - 1;

这是为了防止，cap已经是2的幂。如果cap已经是2的幂， 又没有执行这个减1操作，则执行完后面的几条无符号右移操作之后，返回的capacity将是这个cap的2倍。

2）如果n这时为0了（经过了cap-1之后），则经过后面的几次无符号右移依然是0，最后返回的capacity是 1（最后有个n+1的操作）。

3）注意：**|（按位或运算）：运算规则：相同的二进制数位上，都是0的时候，结果为0，否则为1。**

 **第一次右移** ：

```java
int n = cap - 1;
//cap=10  n=9n |= n >>> 1; 
00000000 00000000 00000000 00001001    9 
00000000 00000000 00000000 00000100    4 
00000000 00000000 00000000 00001101    13 
//按位异或之后是13
```

由于n不等于0，则n的二进制表示中总会有一bit为1，这时考虑最高位的1。通过无符号右移1位，则将最高位的1右移了1位，再做或操作，使得n的二进制表示中与最高位的1紧邻的右边一位也为1，如：

00000000 00000000 00000000 00001101

**第二次右移** ：

```java
 n |= n >>> 2;
 //n通过第一次右移变为了：n=13 
 00000000 00000000 00000000 00001101  13
00000000 00000000 00000000 00000011    3
 //13右移之后变为3------------------------------------------------- 
 00000000 00000000 00000000 00001111   15 
 //按位异或之后是15
```

注意，这个n已经经过了n |= n >>> 1; 操作。假设此时n为00000000 00000000 00000000 00001101 ，则n无符号右移两位，会将最高位两个连续的1右移两位，然后再与原来的n做或操作，这样n的二进制表示的高位中会有4个连续的1。如：

00000000 00000000 00000000 00001111 //按位异或之后是15

**第三次右移** :

```java
n |= n >>> 4;
//n通过第一、二次右移变为了：n=15 
00000000 00000000 00000000 00001111  15
00000000 00000000 00000000 00000000  0
//15右移之后变为0------------------------------------------------- 
00000000 00000000 00000000 00001111   15 
//按位异或之后是15
```

这次把已经有的高位中的连续的4个1，右移4位，再做或操作，这样n的二进制表示的高位中正常会有8个连续的1。

如00001111 1111xxxxxx 。

以此类推

注意，容量最大也就是32bit的正数，因此最后n |= n >>> 16; ，最多也就32个1（但是这已经是负数了。在执行tableSizeFor之前，对initialCapacity做了判断，如果大于MAXIMUM_CAPACITY(2 ^ 30)，则取MAXIMUM_CAPACITY。如果等于MAXIMUM_CAPACITY(2 ^ 30)，会执行移位操作。所以这里面的移位操作之后，最大30个1，不会大于等于MAXIMUM_CAPACITY。30个1，加1之后得2 ^ 30） 。

小结：

 1.由上面可以看出，当我们根据key的hash确定其在数组的位置时，如果n为2的幂次方，可以保证数据的均匀插入，如果n不是2的幂次方，可能数组的一些位置永远不会插入数据，浪费数组的空间，加大hash冲突。

 2.另一方面，一般我们可能会想通过 % 求余来确定位置，这样也可以，只不过性能不如 & 运算。要求n是2的幂次方：hash & (length - 1) == hash % length

 3.因此，HashMap 容量为2次幂的原因，就是为了数据的的均匀分布，减少hash冲突，毕竟hash冲突越大，代表数组中一个链的长度越大，这样的话会降低hashmap的性能

 **4.如果创建HashMap对象时，输入的数组长度是10，不是2的幂，HashMap通过多次位移运算和或运算得到的肯定是2的幂次数，并且是离那个数最近的数字。**



## **加载因子loadFactor**

1.**loadFactor**加载因子，是用来衡量 HashMap 满的程度，**表示HashMap的疏密程度，影响hash操作到同一个数组位置的概率**，计算HashMap的实时加载因子的方法为：size/capacity，而不是占用桶的数量去除以capacity。capacity 是桶的数量，也就是 table 的长度length。

**loadFactor太大导致查找元素效率低，太小导致数组的利用率低，存放的数据会很分散。loadFactor的默认值为0.75f是官方给出的一个比较好的临界值**。

**当HashMap里面容纳的元素已经达到HashMap数组长度的75%时，表示HashMap太挤了，需要扩容，而扩容这个过程涉及到 rehash、复制数据等操作，非常消耗性能。，所以开发中尽量减少扩容的次数，可以通过创建HashMap集合对象时指定初始容量来尽量避免。**

**同时在HashMap的构造器中可以定制loadFactor。**

```java
//构造一个带指定初始容量和加载因子的空 HashMap。
HashMap(int initialCapacity, float loadFactor)
```

### **为什么加载因子设置为0.75,初始化临界值是12？**

loadFactor越趋近于1，那么 数组中存放的数据(entry)也就越多，也就越密，也就是会让链表的长度增加，loadFactor越小，也就是趋近于0，数组中存放的数据(entry)也就越少，也就越稀疏。

如果希望链表尽可能少些。要提前扩容，有的数组空间有可能一直没有存储数据。加载因子尽可能小一些。

举例：

例如：加载因子是0.4。 那么16*0.4--->6 如果数组中满6个空间就扩容会造成数组利用率太低了。

​	  加载因子是0.9。 那么16*0.9---->14 那么这样就会导致链表有点多了。导致查找元素效率低。

所以既兼顾数组利用率又考虑链表不要太多，经过大量测试0.75是最佳方案。

- threshold计算公式：capacity(数组长度默认16) * loadFactor(负载因子默认0.75)。这个值是当前已占用数组长度的最大值。**当Size>=threshold**的时候，那么就要考虑对数组的resize(扩容)，也就是说，这个的意思就是 **衡量数组是否需要扩增的一个标准**。 扩容后的 HashMap 容量是之前容量的两倍.

## **内部类**

### **Node**

Node类是HashMap的一个静态内部类，它实现了 Map 结构顶层规范的 Map.Entry （以键值对的形式储存数据的结构），在桶数据树化之前都用这个来存储数据，树化之后就改用了 TreeNode 了

```java
    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        //hash码，表示元素在桶的位置
        final int hash;
        //Node节点对应的key
        final K key;
        //Node节点对应value
        V value;
        //链表结构   表示下一个节点
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        //判断hashmap中是否存在该元素 键值都相等
        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }
```

### **KeySet、Values、EntrySet**

KeySet、Values、EntrySet 操作其实都差不多，其内部并不存储任何数据，这三个内部内都是封装了方便外部对于 table 的 key，value，[node](https://so.csdn.net/so/search?q=node&spm=1001.2101.3001.7020) 的遍历和操作

#### **KeySet**

HashMap 中所有 key 的集合

```java
    final class KeySet extends AbstractSet<K> {
        //元素个数
        public final int size()                 { return size; }
        //清空
        public final void clear()               { HashMap.this.clear(); }
        //用于迭代KeySet的迭代器
        public final Iterator<K> iterator()     { return new KeyIterator(); }
        //判断是否存在key为o的元素
        public final boolean contains(Object o) { return containsKey(o); }
        //通过key删除元素
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        //分割迭代器
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        //函数式编程 参数可以是接口的实现，也可以是Lambda表达式
        public final void forEach(Consumer<? super K> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }
```

#### **Values**

HashMap 中所有 value 的集合

```java
    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<V> iterator()     { return new ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super V> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }
```

#### **EntrySet**

```java
    final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }
```

### **迭代器**Iterator

HashMap中全部的迭代器都继承了抽象类HashIterator.

#### **HashIterator**

HashIterator是HashMap中所有迭代器的基类。通过抽象类的方式实现了大多数方法，如hashnext、nextNode、remove等。但是需要注意的是，HashIterator并没用实现Iterator接口。而是各自的迭代器在各自实现这个接口。

```java
    abstract class HashIterator {
        //下次迭代的元素
        Node<K,V> next;        // next entry to return
        //当前迭代的元素
        Node<K,V> current;     // current entry
        //期望的修改次数
        int expectedModCount;  // for fast-fail
        //下标
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }
        //是否有下一个元素
        public final boolean hasNext() {
            return next != null;
        }
        //下一个元素节点
        final Node<K,V> nextNode() {
            Node<K,V>[] t;
            Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }
        //移除当前节点
        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }
```

#### **KeyIterator、ValueIterator、EntryIterator**

继承HashIterator，实现各自对应的Iterator

```java
    final class KeyIterator extends HashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    final class ValueIterator extends HashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class EntryIterator extends HashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }
```

### **Spliterator**

Spliterator是java1.8新增的接口，即为splitable iterator的意思，接口是java为了并行遍历数据源中的元素而设计的。与iterator相比，一个是顺序遍历，一个是将数据拆分为若干部分之后进行并行遍历。以配合Stream的并行流。

Spliterator的一个特点是每次将元素拆分出去一半。对于HashMap，由于hashMap底层是链表，如果要完全精确到元素，势必会造成算法的复杂和性能的低下。因此，Spliterator在HashMap的实现过程中，直接是按bucket进行处理，这样会导致每次拆分的数据并不均匀。HashMap中实际上是按照bucket的数量平均拆分，只是可能每个bucket上面Node的数量不一致，另外有的bucket可能为空。

#### **HashMapSpliterator**

HashMapSpliterator是一个基类，keySpliterator和ValueSpiterator、EntrySpliterator都继承了这个类

```java
    static class HashMapSpliterator<K,V> {
        //需要拆分的map
        final HashMap<K,V> map;
        //当前节点
        Node<K,V> current;          // current node
        //bucket的下标
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        //期望的修改次数
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(HashMap<K,V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        /**
         * 初始化方法
         * @return
         */
        final int getFence() { // initialize fence and size on first use
            int hi;
            //如果完成了初始化，则fence不为-1 只有new的时候传入的fence为-1
            if ((hi = fence) < 0) {
                HashMap<K,V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K,V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        /**
         * 确保初始化方法被调用
         * @return
         */
        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }
```

#### **KeySpliterator**

继承HashMapSpliterator并实现了Spliterator接口，消除很多冗余代码。

构造函数中直接调用了super方法。

```java
    static final class KeySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        /**
         * 对spliterator进行分割 分治法
         * 如果Spliterator可以被分割，那么会返回一个新的Spliterator，在传统的情况下，比如list，如果为偶数，则拆分一半，如果为奇数，则会少一个。
         * 对于HashMap，实际上bucket是2的幂，那么直接位移就能实现。
         * @return
         */
        public KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                                        expectedModCount);
        }

        /**
         * 对Spliterator执行遍历
         * @param action The action
         */
        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            //map中的bucket
            Node<K,V>[] tab = m.table;
            //fence < 0,说明没有进行初始化
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    //如果p为空，移动到下一个bucket
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
        
        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }
```

#### **ValueSpliterator**

同**KeySpliterator类似**

#### **EntrySpliterator**

同**KeySpliterator类似**

### **TreeNode**

TreeNode继承Node，具有Node的单向链表的特性

每一个TreeNode都是红黑树中的一个节点，TreeNode具有以下特点：

1、每个节点的颜色非黑即红；

2、根节点是黑色的；

3、每个叶节点（NIL）是黑色的；

4、父节点与子节点的颜色不同，即父节点是红，两个子节点就是黑，反之亦然；

5、对每个节点，从该节点到其所有后代叶节点的最佳路径上，均包含相同数量的黑节点。

![image-20230608205950746](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082059759.png)

```java
    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        //父节点
        TreeNode<K,V> parent;  // red-black tree links
        //左子树
        TreeNode<K,V> left;
        //右子树
        TreeNode<K,V> right;
        //前驱节点  删除后需要取消链接
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        //颜色属性
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         * Returns root of tree containing this node.
         * 返回当前节点所在的根节点TreeNode
         */
        final TreeNode<K,V> root() {
            for (TreeNode<K,V> r = this, p;;) {
                //如果节点的父节点不存在就返回该节点
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * Ensures that the given root is the first node of its bin.
         * 确保根节点是bucket（数组tab的其中一个元素）中的第一个节点,如果不是，则进行操作，将根节点放到tab数组上
         */
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                if (root != first) {
                    Node<K,V> rn;
                    tab[index] = root;
                    TreeNode<K,V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         * 从当前节点开始使用给定的hash和key查找到对应的节点，只会查询遍历以当前节点为根节点的局部树结构的节点
         */
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            TreeNode<K,V> p = this;
            do {
                int ph, dir; K pk;
                TreeNode<K,V> pl = p.left, pr = p.right, q;
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if (pl == null)
                    p = pr;
                else if (pr == null)
                    p = pl;
                else if ((kc != null ||
                          (kc = comparableClassFor(k)) != null) &&
                         (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                else
                    p = pl;
            } while (p != null);
            return null;
        }

        /**
         * Calls find for root node.
         * 根据key和key的hash 查找对应的树节点，找不到返回null
         */
        final TreeNode<K,V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         * 比较两个对象的大小
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                (d = a.getClass().getName().
                 compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                     -1 : 1);
            return d;
        }

        /**
         * Forms tree of the nodes linked from this node.
         * 链表转红黑树
         */
        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null;
            for (TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }

        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         * 将树转换为链表结构，将TreeNode转化为Node
         */
        final Node<K,V> untreeify(HashMap<K,V> map) {
            Node<K,V> hd = null, tl = null;
            for (Node<K,V> q = this; q != null; q = q.next) {
                Node<K,V> p = map.replacementNode(q, null);
                if (tl == null)
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * Tree version of putVal.
         * 向红黑树插入 or 更新数据（键值对），遍历红黑树，找到与新数据key相同的节点，新数据value替换旧数据的value，找不到相同的key则创建新节点并插入
         */
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                                       int h, K k, V v) {
            //定义k的类信息
            Class<?> kc = null;
            boolean searched = false;
            //红黑树的根节点
            TreeNode<K,V> root = (parent != null) ? root() : this;
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;
                //存入key的hash和root根节点比较
                if ((ph = p.hash) > h)
                    //小于，即放置在节点的左侧
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                             (q = ch.find(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    Node<K,V> xpn = xp.next;
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K,V>)xpn).prev = x;
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         * 红黑树的节点移除，还要根据movable判断删除时是否移动其他节点。movable - 如果为false，则在删除时不移动其他节点
         */
        final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
            if (pred == null)
                tab[index] = first = succ;
            else
                pred.next = succ;
            if (succ != null)
                succ.prev = pred;
            if (first == null)
                return;
            if (root.parent != null)
                root = root.root();
            if (root == null
                || (movable
                    && (root.right == null
                        || (rl = root.left) == null
                        || rl.left == null))) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            TreeNode<K,V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                TreeNode<K,V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                TreeNode<K,V> sr = s.right;
                TreeNode<K,V> pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                }
                else {
                    TreeNode<K,V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            }
            else if (pl != null)
                replacement = pl;
            else if (pr != null)
                replacement = pr;
            else
                replacement = p;
            if (replacement != p) {
                TreeNode<K,V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }

            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

            if (replacement == p) {  // detach
                TreeNode<K,V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map the map
         * @param tab the table for recording bin heads
         * @param index the index of the table being split
         * @param bit the bit of hash to split on
         *            红黑树拆分
         */
        final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
            TreeNode<K,V> b = this;
            // Relink into lo and hi lists, preserving order
            TreeNode<K,V> loHead = null, loTail = null;
            TreeNode<K,V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;
            for (TreeNode<K,V> e = b, next; e != null; e = next) {
                next = (TreeNode<K,V>)e.next;
                e.next = null;
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null)
                        loHead = e;
                    else
                        loTail.next = e;
                    loTail = e;
                    ++lc;
                }
                else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            if (loHead != null) {
                if (lc <= UNTREEIFY_THRESHOLD)
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    if (hiHead != null) // (else is already treeified)
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR
        //左旋操作
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                              TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }
        //右旋操作
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                               TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }
        //插入节点之后进行平衡调整，x为新添加的节点，root为树的根节点，返回根节点
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            x.red = true;
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }
                else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }
        //删除节点后自平衡操作，x是删除节点的替换节点
        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                                   TreeNode<K,V> x) {
            for (TreeNode<K,V> xp, xpl, xpr;;) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                else if (x.red) {
                    x.red = false;
                    return root;
                }
                else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                            (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        }
                        else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                    null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                }
                else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                            (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        }
                        else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                    null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         * 对整棵树进行红黑树一致性的检查 目前仅在检查root是否落在table上时调用，满足红黑树的特性以及节点指向的正确性
         */
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                tb = t.prev, tn = (TreeNode<K,V>)t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }
    }
```

# **常用方法**

## **hash**

```java
    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    static final int hash(Object key) {
        int h;
        // 1）如果key等于null：可以看到当key等于null的时候也是有哈希值的，返回的是0.   
        // 2）如果key不等于null：首先计算出key的hashCode赋值给h,然后与h无符号右移16位后的二进制进行按位异或得到最后的hash值  
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
```

**解读上述hash方法：**

key的哈希值是通过上述方法计算出来的。

当key为null时直接返回0，key的hash值高16位不变，低16位与高16位异或作为key的最终hash值。如此设置的原因是因为下标的计算是：n = table.length; index = (n-1) & hash;

table的长度都是2的幂，因此index仅与hash值的低n位有关，hash值的高位都被与操作置为0了，所以异或降低冲突

![image-20230608210031777](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082100660.png)

![image-20230608210037262](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082100135.png)

### **HashMap中hash函数是怎么实现的？还有哪些hash函数的实现方式？**

对于key的hashCode做hash操作，无符号右移16位然后做异或运算。

还有平方取中法，伪随机数法和取余数法。这三种效率都比较低。而无符号右移16位异或运算效率是最高的。

## **put**

添加元素

![image-20230608210105331](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082101550.png)

```java
 /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        // 求出key的hash值，并直接调用putVal
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        // 步骤一：如果table为空或length=0，则调用resize扩容
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 步骤二：根据key的hash值得到插入的数组索引i，如果table[i]==null，直接新建节点并添加，转向步骤六
        // 如果table[i]不为空，转向步骤三
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
            Node<K,V> e; K k;
            // 步骤三：判断table[i]的首个元素是否等于key，若相等直接覆盖value，若不相等转向步骤四
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            // 步骤四：判断table[i]是否为treeNode，
            // 即table[i]是否为红黑树，如果是红黑树直接在树中插入键值对，否则转向步骤五
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                // 步骤五：遍历table[i]
                for (int binCount = 0; ; ++binCount) {
                    // 若链表长度小于8，执行链表的插入操作
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        // 若链表长度大于等于8，将链表转换为红黑树并执行插入操作
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    // 在遍历过程中，若发现key已经存在则直接覆盖value
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            // 若key已经存在，用新的value替换原先的value，并将原先的value返回
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        // modCount+1，modCount用于实现fail-fast机制
        ++modCount;
        // 步骤六：插入成功后，判断实际存在的键值对数量size是否超过最大容量threshold，如果超过则调用resize扩容
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }
```

put方法是比较复杂的，实现步骤大致如下：

1）先通过hash值计算出key映射到哪个桶；

2）如果桶上没有碰撞冲突，则直接插入；

3）如果出现碰撞冲突了，则需要处理冲突：

 a:如果该桶使用红黑树处理冲突，则调用红黑树的方法插入数据；

 b:否则采用传统的链式方法插入。如果链的长度达到临界值并且数组长度超过64，则把链转变为红黑树；

4）如果桶中存在重复的键，则为该键替换新值value；

5）如果size大于阈值threshold，则进行扩容；

HashMap中的putVal()添加元素方法会触发一系列的TreeNode类的方法，依次为：putTreeVal()、root()、find()

### **TreeNode.putTreeVal()**

向红黑树插入 or 更新数据（键值对），遍历红黑树，如果找到与新数据key相同的节点，则直接返回该节点；如果找不到相同的key，则创建新节点并插入，然后重新平衡红黑树。

putTreeVal的两种情况：

key已经存在这个红黑树中当中了，就直接放回对应的那个节点；

从红黑树的root节点开始遍历，定位到要插入的叶子节点，插入新节点；

putTreeVal除了要维护红黑树的平衡外，还需要维护节点之间的前后关系，也就是同时在维护双向链表关系。

`TreeNode.putTreeVal`

```java
        /**
         * Tree version of putVal.
         * 向红黑树插入 or 更新数据（键值对），遍历红黑树，找到与新数据key相同的节点，新数据value替换旧数据的value，找不到相同的key则创建新节点并插入
         */
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                                       int h, K k, V v) {
            //定义k的类信息
            Class<?> kc = null;
            //是否调用find方法进行查找，默认没调用
            boolean searched = false;
            //红黑树的根节点  索引位置的头节点并不一定为红黑树的根节点，所以需要通过调用root()方法来遍历红黑树结构进而找到根节点
            TreeNode<K,V> root = (parent != null) ? root() : this;
            //将根节点赋值给p节点，从根节点开始遍历红黑树，在内部终止遍历
            for (TreeNode<K,V> p = root;;) {
                // dir：表示向哪个子树查找，-1左，1右；
                // p：当前节点，ph：当前树节点的hash，
                // pk：当前树节点的key
                int dir, ph; K pk;
                //存入p的hash和root根节点的hash比较
                if ((ph = p.hash) > h)
                    //小于，即放置在节点的左侧(左子节点)
                    dir = -1;
                else if (ph < h)
                    //右子节点
                    dir = 1;
                //当前树节点的key等于新数据的key，直接返回当前节点
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;

                /**
                 * 如果kc为null，说明当前kc还没有被赋值，需要继续执行后面的代码对kc进行赋值，因为它的后面是与运算符，如果kc已经被赋值了，说明已经执行过后面的语句了，就不用再执行后面comparableClassFor和compareComparables了
                 * kc==null&&(kc = comparableClassFor(k)) == null是同一个与运算表达式，继续执行后面comparableClassFor来判断key是不是实现了comparable接口，如果返回null，说明key没有实现comparable接口，也就无法使用compareComparables来比较大小了。整个与运算表达式结果为true，也就直接进入到if分支内部了，因为它们的后面是或运算
                 * 如果实现了comparable接口接口，则继续调用compareComparables来比较大小，如果返回值不为0，则说明通过compareComparables比较出了大小，将比较结果直接赋值给dir，也就不用执行if分支内部的语句来比较大小了。
                 * 如果返回值为0，说明compareComparables方法也没有比较出两者的大小关系，则需要继续进入到if分支内部去用别的方法继续进行比较。
                 */
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    //还没有调用find方法进行查找
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        // 改为已经调用find方法进行查找了，
                        searched = true;
                        // 从p节点的左节点和右节点分别调用find方法进行查找, 如果查找到目标节点则并终止循环，返回q；
                        if (((ch = p.left) != null &&
                             (q = ch.find(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.find(h, k, kc)) != null))
                            // 找到了相同key的节点，直接返回该节点
                            return q;
                    }
                    //比较p节点的key和新数据的key大小, 用来决定向左还是向右查找
                    //dir<0 则代表 k<pk，则向p左边查找；反之亦然
                    dir = tieBreakOrder(k, pk);
                }
                // x表示新插入元素构建出来的树节点
                // xp赋值为x的父节点，中间变量，用于下面给x的父节点赋值
                TreeNode<K,V> xp = p;
                // dir<=0则向p左边查找，否则向p右边查找，如果为null,说明已经到达了叶子节点，红黑树插入新节点都会插入到叶子结点的位置，遍历到了null则代表该位置即为新插入节点x的应该插入的位置，进入if分支内进行插入操作
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    // 已经找到x要插入的位置，只需将x放到该位置即可
                    Node<K,V> xpn = xp.next;
                    // 创建新的节点, 其中x的next节点为xpn, 即将x节点插入xp与xpn之间
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        //如果时dir <= 0, 则代表x节点为xp的左节点
                        xp.left = x;
                    else
                        // 如果时dir> 0, 则代表x节点为xp的右节点
                        xp.right = x;
                    // 将xp的next节点设置为x
                    xp.next = x;
                    // 将x的parent和prev节点设置为xp
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        // 如果xpn不为空,则将xpn的prev节点设置为x节点,与上文的x节点的next节点对应
                        ((TreeNode<K,V>)xpn).prev = x;
                    // 进行红黑树的插入平衡调整，调用了balanceInsertion和moveRootToFront
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }
```

### **TreeNode.root()**

查找红黑树的根节点。向上层遍历，通过判断有没有父节点来找出根节点

```java
        /**
         * Returns root of tree containing this node.
         * 返回当前节点所在的根节点TreeNode
         */
        final TreeNode<K,V> root() {
            for (TreeNode<K,V> r = this, p;;) {
                //如果节点的父节点不存在就返回该节点
                if ((p = r.parent) == null)
                    return r;

                // 当前遍历到的节点设置为其父节点，实现向上层遍历
                r = p;
            }
        }
```

### **TreeNode.find()**

从调用此方法的节点开始查找, 对左右子树进行递归遍历，通过hash值和key找到对应的节点。查找过程是比较hash，判断往左找还是往右找，特殊情况就是一边为空，那就只往另一边找，比较key是否相等，递归遍历直到找到相等的key时，就代表找到了。

```java
        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         * 从当前节点开始使用给定的hash和key查找到对应的节点，只会查询遍历以当前节点为根节点的局部树结构的节点
         */
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            //1.将p节点赋值为调用此方法的节点，即为红黑树根节点
            TreeNode<K,V> p = this;

            // 2.从p节点开始向下遍历
            do {
                // ph p的hash
                // pk p的key
                int ph, dir; K pk;
                TreeNode<K,V> pl = p.left, pr = p.right, q;
                // 3.如果传入的hash值小于p节点的hash值，则往p节点的左边遍历
                if ((ph = p.hash) > h)
                    p = pl;
                // 4.如果传入的hash值大于p节点的hash值，则往p节点的右边遍历
                else if (ph < h)
                    p = pr;
                // 5.如果传入的hash值和key值等于p节点的hash值和key值,则p节点为目标节点,返回p节点
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                // 6.p节点的左节点为空则将向右遍历
                else if (pl == null)
                    p = pr;
                // 7.p节点的右节点为空则向左遍历
                else if (pr == null)
                    p = pl;
                // 8.如何k的hash值等于pk的hash值，但是k和pk不相等，则将继续用其他的方法对p节点与k进行比较
                else if ((kc != null ||
                          //8.1 kc不为空代表k实现了Comparable
                          (kc = comparableClassFor(k)) != null) &&
                          // 8.2 k<pk则dir<0, k>pk则dir>0
                         (dir = compareComparables(kc, k, pk)) != 0)
                    // 8.3 k<pk则向左遍历(p赋值为p的左节点), 否则向右遍历
                    p = (dir < 0) ? pl : pr;
                // 9.key所属类没有实现Comparable, 向p的右边遍历查找，继续递归调用find()
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                // 10.代表上一步的向右边没找到“pr.find(h, k, kc)”为空, 因此向左遍历
                else
                    p = pl;
            } while (p != null);
            //没有找到
            return null;
        }
```

## **get**

![image-20230608210326030](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082103165.png)

```java
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    public V get(Object key) {
        Node<K,V> e;
        // 求出key的hash值，并直接调用getNode
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * Implements Map.get and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @return the node, or null if none
     */
    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        // 当table不为空，并且经过计算得到的插入位置table[i]也不为空时继续操作，否则返回null
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            // 判断table[i]的首个元素是否等于key，若相等将其返回
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.next) != null) {
                // 若存储结构为红黑树，则执行红黑树中的查找操作
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    // 若存储结构仍为链表，则遍历链表，找到key所在的位置
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
```

HashMap中的getNode()查找元素方法会触发一系列的TreeNode类的方法，依次为：getTreeNode()、root()、find()

### **TreeNode.getTreeNode**()

在红黑树中根据key和key的hash 查找对应的树节点，找不到返回null，这里要先找到根节点，然后从根节点再去查找树节点

```java
        /**
         * Calls find for root node.
         * 根据key和key的hash 查找对应的树节点，找不到返回null
         */
        final TreeNode<K,V> getTreeNode(int h, Object k) {
            // 如果当前调用该方法的红黑树节点还有父级节点，说明该红黑树节点不是根节点，所以需要调用 root() 方法找到根节点，
            // 如果当前调用该方法的红黑树节点没有父级节点，说明该红黑树节点就是根节点，
            // 找到根节点后，根节点调用find方法去查找目标节点
            return ((parent != null) ? root() : this).find(h, k, null);
        }
```

## **remove**

```java
    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V remove(Object key) {
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }
```

### **removeNode**

```java
    /**
     * Implements Map.remove and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
            Node<K,V> node = null, e; K k; V v;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            else if ((e = p.next) != null) {
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            //找到目标节点
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
                // 如果目标节点是红黑树
                if (node instanceof TreeNode)
                    // 调用红黑树的删除节点方法
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                else if (node == p)
                    tab[index] = node.next;
                else
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }
```

HashMap中的removeNode()删除元素方法会触发一系列的TreeNode类的方法，依次为：removeTreeNode()、untreeify()、balanceDeletion()、rotateLeft()、rotateRight()

### **TreeNode.removeTreeNode**

```java

/**
 * 红黑树的节点移除
 * @param movable 如果为false，则在删除后不移动其他节点，不用执行moveRootToFront()方法。
 */
final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                          boolean movable) {
    // --- 链表的处理start ---
    int n;
    // 1.table为空或者length为0直接返回
    if (tab == null || (n = tab.length) == 0)
        return;
    // 2.根据调用者的hash计算出索引的位置，也就是 根据将要被移除的node节点的hash进行计算
    int index = (n - 1) & hash;
    // 3.first：当前index位置的节点，root：当前index位置的节点，作为根节点，rl：root的左节点
    TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
    // 4.succ：目标节点(也就是this节点，调用removeTreeNode方法要删除掉的节点)node.next节点，pred：目标节点node.prev节点
    TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
    // 下面的5-7步的操作就是在链表结构上对node目标节点进行删除
    // 5.如果pred节点为空，则代表目标节点node为头节点，
    if (pred == null){
        // 则将table索引位置和first都指向succ节点(node.next节点)
        tab[index] = first = succ;
    }
    // 6.否则将pred的next属性指向succ节点(node.next节点)
    else{
        // 这块有一点点饶，主要是因为他这个变量搞得，其实等同于 node.prev.next = node.next;
        // 原来是 pred.next = node -> node.next = succ
        // 现在是 pred.next = node.next = succ，跳过了node，也就相当于把node删除了（前驱节点与后继节点相连，跳过node节点）
        pred.next = succ;
    }
    // 7.如果succ节点(node.next节点)不为空，
    if (succ != null){
        // 则将succ.prev(node.next.prev)节点设置为pred(node.prev), 与前面对应（后继节点与前驱节点相连，跳过node节点）
        // 等同于 node.next.prev = node.prev
        succ.prev = pred;
    }
    // 8.如果first节点为null，则代表该索引位置没有节点则直接返回
    // 这个if其实可以放在上方第3点后面，第4点前面，因为直接判断索引位置就是null，压根不用在找下个节点
    if (first == null){
        return;
    }
    // 9.如果root的父节点不为空，说明该节点并不是真正的红黑树根节点，需要重新查找根节点
    if (root.parent != null){
        // 从该root节点开始去查找根节点，得到根节点之后，将root指向真正的红黑树根节点
        root = root.root();
    }
    // 10.通过root节点来判断此红黑树是否太小, 如果是太小了则调用untreeify方法转为链表节点并返回
    // (转链表后就无需再进行下面的红黑树处理)
    // 太小的判定依据：根节点为null，或者根的右节点为null，或者根的左节点为null，或者根的左节点的左节点为null
    // 这里并没有遍历整个红黑树去统计节点数是否小于等于阈值6，而是直接判断这几种情况，来决定要不要转换为链表，因为这几种情况一般就涵盖了节点数小于6的情况，这样执行效率也会变高
    if (root == null || root.right == null ||
        (rl = root.left) == null || rl.left == null) {
        tab[index] = first.untreeify(map);  // too small
        return;
    }
    // --- 链表的处理end ---
    // --- 红黑树的处理start ---
    // 11.p：目标节点node，pl：p的左节点，pr：p的右节点，replacement：用来替换掉被删除节点的节点
    TreeNode<K,V> p = this, pl = left, pr = right, replacement;
    // 12.如果p的左和右节点都不为空时。这种情况是最复杂的
    if (pl != null && pr != null) {
        // 12.1-12.3操作就是用来定位节点p和节点s，为第一次调整做准备
        /**
         * 这里说明一下p和s的作用
         * p就是目标节点，也就是我们要删除的节点，这个没什么可说的。
         * 但是p节点不能直接在原位置将其删除，因为如果P有左右子树的话，将p删除后应该对其子树进行移动处理，需要在其中选出合适的节点放置在p原有的位置来代替p，并且重新维护搜索二叉树结构。毕竟删除后p的原有位置不能是空的，需要有新的节点来顶替上。
         * 如果直接在p的原位置删除的话，后续的处理操作有很大的难度，从两颗子树中找到合适的节点来代替p原有的位置是一件很麻烦的事情，因为有很多个可选择的节点，这个选择标准不好把控，而且就算是选出来也需要进行一些列的操作来维护二叉搜索树结构（需要照顾到两棵子树的结构）。所以最好是将p移动到一个对后续操作更加简单位的位置上
         * s节点就是要在删除p前，应该将p移动到的位置，将p与s节点进行位置交换，后续的处理就会更加简单
         * 那么什么样的位置能让删除p后，能更加简单的进行后续处理呢，也就是我们该如何选定s节点？
         * 其实很容易就能想到，当要删除的节点p只有一棵子树甚至没有子树的时候，我们对后续的操作越方便。想想看如果p没有子树，那么删除了p之后，也不需要做其他操作来维护二叉搜索树结构了，只需要后续进行红黑树平衡操作即可，如果p只有一棵子树，那也很简单，直接将这颗子树的根节点替换到p的原有位置即可维持二叉搜索树结构
         * 所以在选择s节点的时候，就按照让p的子树越少越好的原则来选
         * 在removeTreeNode中选择s节点的标准就是选择刚刚大于p节点的节点，因为这个节点必定没有左子树了，如果有左子树的话它也不会是刚刚大于p节点的节点，这样也就满足了该位置最多只有一颗子树的条件。
         * 并且s节点如果刚刚大于p节点，那么将s和p节点交换位置，其他的节点都不需要变换位置，因为s是刚刚大于p节点的节点，所以将s放到p的位置，能让他继续保持和p节点一样的与pp、pl、pr等其他节点相同的位置关系。虽然交换后s和p的位置关系有问题（s应该是大于p的，交换之后p却成了s的左子树节点），但是后续删除p节点即可。具体的变化可以对比第一次调整前和第二次调整后的两张图片
         * 
         */
        // 12.1 将s指向pr(p的右节点)，这是为了保证查找s节点是找到的节点都是比p大的    sl：s的左节点
        TreeNode<K,V> s = pr, sl;
        // 12.2 从p节点的右子节点开始，向左一直查找，跳出循环时，s为没有左节点的节点。这也就保证了最后定位到的s节点是刚刚比p大的节点
        while ((sl = s.left) != null){
            s = sl;
        }
        // 12.3 交换p节点和s节点的颜色
        boolean c = s.red; s.red = p.red; p.red = c;
        // s的右节点
        TreeNode<K,V> sr = s.right; 
        // p的父节点
        TreeNode<K,V> pp = p.parent; 
        // --- 第一次调整和第二次调整：将p节点和s节点进行了位置调换，并且选出要替换掉 p 节点的 replacement ---
        // 12.4 第一次调整：将 p 节点和 s 节点进行了位置调换
        // 如果p的右节点即为s节点，则将p和s交换位置，原先是s.parent = p；p.right = s；
        if (s == pr) {
            p.parent = s;
            s.right = p;
        }
        else {
            // 将sp指向s的父节点
            TreeNode<K,V> sp = s.parent;
            // 将sp作为p的父节点
            if ((p.parent = sp) != null) {
                // 如果s节点为sp的左节点，则将sp的左节点指向p，此时sp的的左节点s变成了p节点
                if (s == sp.left){
                    sp.left = p;
                }
                // 否则s节点为sp的右节点，则将sp的右节点指向p，此时sp的的右节点s变成了p节点
                else{
                    sp.right = p;
                }
                // 完成了p和s的交换位置
            }
            // s的右节点指向p的右节点
            if ((s.right = pr) != null)
                // 如果pr不为空，则将pr的父节点指向s，此时p的右节点变成了s的右节点
                pr.parent = s;
        }
        // 12.5 第二次调整：将第一次调整后独立出来的节点再次插入新构造出来的红黑树结构的对应位置，并且选出要替换掉 p 节点的 replacement
        // 12.5.1 将第一次调整后独立出来的节点再次插入新构造出来的红黑树结构的对应位置
        // 将p的左节点赋值为空，pl已经保存了该节点
        p.left = null;
        // 将p节点的右节点指向sr，如果sr不为空，则将sr的父节点指向p节点，此时s的右节点变成了p的右节点
        if ((p.right = sr) != null)
            sr.parent = p;
        // 将s节点的左节点赋值为pl，如果pl不为空，则将pl的父节点指向s节点，此时p的左节点变成了s的左节点
        if ((s.left = pl) != null)
            pl.parent = s;
        // 将s的父节点赋值为p的父节点pp
        // 如果pp为空，则p节点为root节点, 交换后s成为新的root节点
        if ((s.parent = pp) == null)
            root = s;
        // 如果p不为root节点, 并且p是pp的左节点，则将pp的左节点赋值为s节点
        else if (p == pp.left)
            pp.left = s;
        // 如果p不为root节点, 并且p是pp的右节点，则将pp的右节点赋值为s节点
        else
            pp.right = s;
        // 12.5.2 寻找replacement节点，用来替换掉p节点。removeTreeNode的规则就是取s和p交换位置前s的右子节点sr作为要用来替代p节点的replacement节点,如果sr节点为null，则直接将p删除即可
        // 12.5.2.1 如果sr不为空，则replacement节点为sr，因为s没有左节点，所以使用s的右节点来替换p的位置
        if (sr != null)
            replacement = sr;
        // 12.5.2.1 如果sr为空，则s为叶子节点，replacement为p本身，只需要将p节点直接去除即可
        else
            replacement = p;
    }
    // 13.承接12点的判断，如果p的左节点不为空，右节点为空，replacement节点为p的左节点，原理上面也讲过了，因为只有一颗子树，直接将子树的根节点替换到p的位置即可，不需要重新维护二叉搜索树结构
    else if (pl != null)
        replacement = pl;
    // 14.如果p的右节点不为空,左节点为空，replacement节点为p的右节点，原理同上
    else if (pr != null)
        replacement = pr;
    // 15.如果p的左右节点都为空, 即p为叶子节点, replacement节点为p节点本身，直接将p删除即可
    else
        replacement = p;
    // 16.第三次调整：使用replacement节点替换掉p节点的位置，将p节点移除
    // 16.1 如果p节点不是叶子节点（上面只有当p没有子树的时候，才会将replacement指向p），则将p删除后需要再将replacement节点替换到p的位置
    if (replacement != p) { 
        // 16.1.1 将p节点的父节点（此时p的父节点是已经交换完位置后p的父节点，也就是第三张图中的sp节点）赋值给replacement节点的父节点, 同时赋值给pp节点
        TreeNode<K,V> pp = replacement.parent = p.parent;
        // 16.1.2 如果p没有父节点, 即p为root节点，则将root节点赋值为replacement节点即可
        if (pp == null)
            root = replacement;
        // 16.1.3 如果p不是root节点, 并且p为pp（第三张图的sp节点）的左节点，则将pp的左节点赋值为替换节点replacement
        else if (p == pp.left)
            pp.left = replacement;
        // 16.1.4 如果p不是root节点, 并且p为pp的右节点，则将pp的右节点赋值为替换节点replacement
        else
            pp.right = replacement;
        // 16.1.5 p节点的位置已经被完整的替换为replacement, 将p节点清空, 以便垃圾收集器回收
        p.left = p.right = p.parent = null;
    }
    // 16.2 完成了p节点的删除并将替代节点放置到p的位置后，判断如果p节点不为红色，则进行红黑树删除平衡调整(如果删除的节点是红色则不会破坏红黑树的平衡无需调整，在红黑树的文章中讲过)
    TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);
    // 16.3 如果p节点为叶子节点（即无子树）, 则简单的将p节点去除即可，无需做其他操作
    if (replacement == p) {
        TreeNode<K,V> pp = p.parent;
        // 16.3.1 将p的parent属性设置为空
        p.parent = null;
        if (pp != null) {
            // 16.3.2 如果p节点为父节点的左节点，则将父节点的左节点赋值为空
            if (p == pp.left)
                pp.left = null;
            // 16.3.3 如果p节点为父节点的右节点，则将父节点的右节点赋值为空
            else if (p == pp.right)
                pp.right = null;
        }
    }
    // 根据movable判断删除节点后是否要将红黑树根节点root放到数组桶中
    if (movable)
        // 18.将root节点移到数组桶中
        moveRootToFront(tab, r);   
    // --- 红黑树的处理end ---
}
```

## **putMapEntries**

```java
    /**
     * Implements Map.putAll and Map constructor.
     *
     * @param m the map
     * @param evict false when initially constructing this map, else
     * true (relayed to method afterNodeInsertion).
     *  将指定map中的所有元素添加至HashMap中
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        //获取参数集合的长度
        int s = m.size();
        //判断参数集合的长度是否大于0，说明必须大于0
        if (s > 0) {
            // 判断table是否已经初始化
            if (table == null) { // pre-size
                // 未初始化，s为m的实际元素个数
                float ft = ((float)s / loadFactor) + 1.0F;
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                         (int)ft : MAXIMUM_CAPACITY);
                // 计算得到的t大于阈值，则初始化阈值
                if (t > threshold)
                    threshold = tableSizeFor(t);
            }
            // 已初始化，并且m元素个数大于阈值，进行扩容处理
            else if (s > threshold)
                resize();
            // 将m中的所有元素添加至HashMap中
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }
```

注意：

**float ft = ((float)s / loadFactor) + 1.0F;这一行代码中为什么要加1.0F ？**

s/loadFactor的结果是小数，加1.0F后转为int相当于是对小数做一个向上取整以尽可能的保证更大容量，更大的容量能够减少resize的调用次数。所以 + 1.0F是为了获取更大的容量。

例如：原来集合的元素个数是6个，那么6/0.75是8，是2的n次幂，那么新的数组大小就是8了。然后原来数组的数据就会存储到长度是8的新的数组中了，这样会导致在存储元素的时候，容量不够，还得继续扩容，那么性能降低了，而如果+1呢，数组长度直接变为16了，这样可以减少数组的扩容。

## **resize**

![image-20230608210533529](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082105487.png)

```java
    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    final Node<K,V>[] resize() {
        //得到当前数组
        Node<K,V>[] oldTab = table;
        //如果当前数组等于null长度返回0，否则返回当前数组的长度
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        //当前阀值点 默认是12(16*0.75)
        int oldThr = threshold;
        int newCap, newThr = 0;
        //如果老的数组长度大于0
        // 开始计算扩容后的大小
        if (oldCap > 0) {
            // 如果扩容前的数组大小超过最大容量  超过最大值就不再扩充了，就只好随你碰撞去吧
            if (oldCap >= MAXIMUM_CAPACITY) {
                // 修改resize阈值为int的最大值(2^31-1)，这样以后就不会扩容了
                threshold = Integer.MAX_VALUE;
                // 直接将原数组返回
                return oldTab;
            }
            // 没超过最大容量，就将容量扩充为原来的两倍
            //1.扩大到2倍之后容量要小于最大容量  2.原数组长度大于等于数组初始化长度
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                // 将新的resize阈值也扩充为原来的两倍
                newThr = oldThr << 1; // double threshold
        }
        //老阈值大于0 直接赋值
        else if (oldThr > 0) // initial capacity was placed in threshold
            // 老阈值赋值给新的数组长度
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            // 使用默认值
            newCap = DEFAULT_INITIAL_CAPACITY;
            //0.75*16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        // 计算新的resize阈值
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        //新的阀值 默认原来是12 乘以2之后变为24
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        // 使用resize后的容量新建一个空的table
        //newCap是新的数组长度--》32
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        //判断旧数组是否等于空
        if (oldTab != null) {
            // 遍历旧的哈希表的每个桶 重新计算桶里元素的新位置
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    // 原来的数据赋值为null 便于GC回收  （for循环后，旧的table不再引用任何对象）
                    oldTab[j] = null;
                    //判断数组是否有下一个引用    若oldTab[j]只包含一个元素
                    if (e.next == null)
                        //没有下一个引用，说明不是链表，当前桶上只有一个键值对，直接插入
                        // 直接将这一个元素放到newTab合适的位置
                        newTab[e.hash & (newCap - 1)] = e;
                    // 判断是否是红黑树  若oldTab[j]存储结构为红黑树，执行红黑树中的调整操作
                    else if (e instanceof TreeNode)
                        //说明是红黑树来处理冲突的，则调用相关方法把树分开
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        // 若oldTab[j]存储结构为链表 采用链表处理冲突
                        // 这一波操作比较巧妙，与JDK 1.7相比，既不需要重新计算hash，也避免了链表元素倒置的情况
                        // 不需要重新计算元素在数组中的位置，采用原始位置加原数组长度的方法计算得到位置
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        //计算节点的新位置
                        do {
                            // 原索引
                            next = e.next;
                            // 通过位操作可以得到元素在数组中的位置是否需要移动
                            //如果等于true e这个节点在resize之后不需要移动位
                            if ((e.hash & oldCap) == 0) {
                                // 链表为空时，当前节点设置为头节点
                                if (loTail == null)
                                    loHead = e;
                                else
                                    // 不为空时，将尾节点的下一个设置为当前节点
                                    loTail.next = e;
                                // 将尾节点设置为当前节点，移动指针
                                loTail = e;
                            }
                            // 需要移动时 // 原索引+oldCap
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        // 原索引放到bucket里
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        // 原索引+oldCap放到bucket里  
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```

# **答疑**

## **什么是哈希碰撞,如何解决哈希碰撞？**

只要两个元素的key计算的哈希码值相同就会发生哈希碰撞。jdk8前使用链表解决哈希碰撞。jdk8之后使用链表+红黑树解决哈希碰撞。

主流解决哈希冲突的方式有两种:开发寻址法，分裂链接法(拉链法)

## **什么时候才需要扩容**

当HashMap中的元素个数超过数组大小(数组长度)*loadFactor(负载因子)时，就会进行数组扩容，loadFactor的默认值(DEFAULT_LOAD_FACTOR)是0.75,这是一个折中的取值。也就是说，默认情况下，数组大小为16，那么当HashMap中的元素个数超过16×0.75=12(这个值就是阈值或者边界值threshold值)的时候，就把数组的大小扩展为2×16=32，即扩大一倍，然后重新计算每个元素在数组中的位置，而这是一个非常耗性能的操作，所以如果我们已经预知HashMap中元素的个数，那么预知元素的个数能够有效的提高HashMap的性能。

**补充：**

**当HashMap中的其中一个链表的对象个数如果达到了8个，此时如果数组长度没有达到64，那么HashMap会先扩容解决，如果已经达到了64，那么这个链表会变成红黑树，节点类型由Node变成TreeNode类型。当然，如果映射关系被移除后，下次执行resize方法时判断树的节点个数低于6，也会再把树转换为链表。**

HashMap扩容可以分为三种情况：

第一种：使用默认构造方法初始化HashMap。从前文可以知道HashMap在一开始初始化的时候会返回一个空的table，并且thershold为0。因此第一次扩容的容量为默认值DEFAULT_INITIAL_CAPACITY也就是16。同时threshold = DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR = 12。

第二种：指定初始容量的构造方法初始化HashMap。那么从下面源码可以看到初始容量会等于threshold，接着threshold = 当前的容量（threshold） * DEFAULT_LOAD_FACTOR。

第三种：HashMap不是第一次扩容。如果HashMap已经扩容过的话，那么每次table的容量以及threshold量为原有的两倍。

这边也可以引申到一个问题HashMap是先插入还是先扩容：HashMap初始化后首次插入数据时，先发生resize扩容再插入数据，之后每当插入的数据个数达到threshold时就会发生resize，此时是先插入数据再resize。

## **为什么阿里巴巴建议初始化 HashMap 的容量大小？**

![image-20230608210630126](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306082106963.png)

HashMap 有扩容机制，就是当达到扩容条件时会进行扩容。HashMap 的扩容条件就是当 HashMap中的元素个数（size）超过临界值（threshold）时就会自动扩容。在 HashMap 中，threshold = loadFactor * capacity。

所以，如果我们没有设置初始容量大小，随着元素的不断增加，HashMap 会发生多次扩容，而 HashMap 中的扩容机制决定了每次扩容都需要重建 hash 表，是非常影响性能的。

## **为什么Map桶中节点个数超过8才转为红黑树？**

8这个阈值定义在HashMap中，针对这个成员变量，在源码的注释中只说明了8是bin（bin就是bucket(桶)）从链表转成树的阈值，但是并没有说明为什么是8：

在HashMap源码中有一段注释说明： 我们继续往下看 :

```java
       Because TreeNodes are about twice the size of regular nodes, we
       use them only when bins contain enough nodes to warrant use
       (see TREEIFY_THRESHOLD). And when they become too small (due to
       removal or resizing) they are converted back to plain bins.  In
       usages with well-distributed user hashCodes, tree bins are
       rarely used.  Ideally, under random hashCodes, the frequency of
       nodes in bins follows a Poisson distribution
```

TreeNodes占用空间是普通Nodes的两倍，所以只有当bin包含足够多的节点时才会转成TreeNodes，而是否足够多就是由TREEIFY_THRESHOLD的值决定的。当bin中节点数变少时，又会转成普通的bin。并且我们查看源码的时候发现，链表长度达到8就转成红黑树，当长度降到6就转成普通bin。

这样就解释了为什么不是一开始就将其转换为TreeNodes，而是需要一定节点数才转为TreeNodes，说白了就是权衡，空间和时间的权衡。

这段内容还说到：当hashCode离散性很好的时候，树型bin用到的概率非常小，因为数据均匀分布在每个bin中，几乎不会有bin中链表长度会达到阈值。但是在随机hashCode下，离散性可能会变差，然而JDK又不能阻止用户实现这种不好的hash算法，因此就可能导致不均匀的数据分布。不过理想情况下随机hashCode算法下所有bin中节点的分布频率会遵循泊松分布，我们可以看到，一个bin中链表长度达到8个元素的概率为0.00000006，几乎是不可能事件。所以，之所以选择8，不是随便决定的，而是根据概率统计决定的。由此可见，发展将近30年的Java每一项改动和优化都是非常严谨和科学的。

也就是说：选择8因为符合泊松分布，超过8的时候，概率已经非常小了，所以我们选择8这个数字。

红黑树的平均查找长度是log(n)，如果长度为8，平均查找长度为log(8)=3，链表的平均查找长度为n/2，当长度为8时，平均查找长度为8/2=4，这才有转换成树的必要；链表长度如果是小于等于6，6/2=3，而log(6)=2.6，虽然速度也很快的，但是转化为树结构和生成树的时间并不会太短。

注意:当链表长度超过8并且数组容量超过64时才会将链表转换为红黑树

链表长度超过8但是数组容量没有超过64会优先进行扩容