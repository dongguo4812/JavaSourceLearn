# **介绍**

- ArrayList非线程安全。
- ArrayList基于动态数组，是一种线性表。随机访问友好，插入和删除效率低。

​		增删慢：每次删除元素，都需要改变数组长度、拷贝以及移动数组长度

​		查询快：由于数组在内存中是一块连续空间，因此可以根据地址+索引的方式快速获取对应位置上的元素。

存储的数据元素有序、可以重复

- 容量动态调节，有一套扩容和优化空间的机制（`基于数组存储数据的局限性：长度不可变，使用List替代数组`）

ArrayList继承了AbstractList，实现了List、RandomAccess、Cloneable、Serializable接口。



```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```



## Serializable标记性接口

类的序列化需要实现java.io.Serializable接口，不实现此接口的类将不会使用任何状态序列化或反序列化。可序列化的所有子类都是可序列化的。序列化接口没有方法或字段，仅用于表示可串行化的语义

## **Cloneable标记性接口**

一个类实现Cloneable接口并重写clone()方法，实现克隆

在未实现Cloneable接口的实例上调用对象的克隆方法会导致CloneNotSupportedException异常

## **RandomAccess标记性接口**

RandomAccess接口是个空接口，作为一个标志使用，当一个类支持随机访问的时候（数组是很典型的），就可以标记这个接口。

## **AbstractList抽象类**

ArrayList继承重写该类

# **常量&变量**

```java
    //序列化的VersionUID
    private static final long serialVersionUID = 8683452581122892189L;

    /**
     * Default initial capacity.
     * 默认初始容量 10，注意：调用无参构造函数时，初始容量为DEFAULTCAPACITY_EMPTY_ELEMENTDATA
     */
    private static final int DEFAULT_CAPACITY = 10;

    /**
     * Shared empty array instance used for empty instances.
     * 指定初始容量为0时，返回该空数组
     */
    private static final Object[] EMPTY_ELEMENTDATA = {};

    /**
     * Shared empty array instance used for default sized empty instances. We
     * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
     * first element is added.
     * 不指定初始容量时，返回该空数组。
     * 用于与EMPTY_ELEMENTDATA进行区分，在第一次添加元素时容量扩容为多少   
     */
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /**
     * The array buffer into which the elements of the ArrayList are stored.
     * The capacity of the ArrayList is the length of this array buffer. Any
     * empty ArrayList with elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
     * will be expanded to DEFAULT_CAPACITY when the first element is added.
     * 数组缓冲区  用于存储元素的容器，ArrayList的容量就是这个缓冲区的容量。
     * 当elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA时，第一次添加元素后，扩容至默认容量10。
     * 另外虽然这里用了transient修饰，但是其实现了readObject和writeObject，查看源码可知,是实现了序列化
     */
    transient Object[] elementData; // non-private to simplify nested class access

    /**
     * The size of the ArrayList (the number of elements it contains).
     * 元素个数，并不一定是容量
     * @serial
     */
    private int size;
    
    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 最大容量，避免在某些虚拟机下可能引起的OutOfMemoryError，
     * 减8的原因：数组作为一个对象，需要一定的内存存储对象头信息，对象头信息最大占用内存不可超过8字节
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

```

# **构造方法**

```java
    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     * 根据给定的初始容量创建数组
     * ①指定初始容量大于0时，创建给定大小的数组
     * ②指定初始容量等于0时，创建一个空数组EMPTY_ELEMENTDATA
     * ③否则抛出异常IllegalArgumentException
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }

    /**
     * Constructs an empty list with an initial capacity of ten.
     * 在没有指定初始容量时，创建一个空数组DEFAULTCAPACITY_EMPTY_ELEMENTDATA
     * 并非创建长度为10的数组         
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     * 按照指定集合的迭代器返回元素的顺序，构造一个包含指定集合元素的list
     * 构建一个Object[] 赋值给数组缓冲区 
     */
    public ArrayList(Collection<? extends E> c) {  
        //将传递过来的集合转成Object数组     
        Object[] a = c.toArray();
        if ((size = a.length) != 0) {
            //传入的集合是ArrayList类型
            if (c.getClass() == ArrayList.class) {
                //直接将c的元素赋值给数组缓冲区
                elementData = a;
            } else {
                //传入的集合不是ArrayList，Arrays.copyOf复制一个Object[] 赋值给数组缓冲区
                elementData = Arrays.copyOf(a, size, Object[].class);
            }
        } else {
            // replace with empty array.
            //传入的集合长度为0，创建一个空数组
            elementData = EMPTY_ELEMENTDATA;
        }
    }
```

# **常用方法**

## **1.添加**