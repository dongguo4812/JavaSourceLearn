# **介绍**

- Vector是矢量队列，继承于AbstractList，实现了List, RandomAccess, Cloneable和Serializable接口
- Vector继承了AbstractList，实现了List接口，所以它是一个队列，支持相关的添加、删除、修改、遍历等功能
- Vector实现了RandomAccess接口，即提供了随机访问功能。在Vector中，我们可以通过元素的序号快速获取元素对象，这就是快速随机访问。
- Vector实现了Cloneable接口，即实现了clone()方法，可以被克隆
- 和ArrayList不同，Vector中的操作是线程安全的

```java
public class Vector<E>
    extends AbstractList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

# **常量&变量**

```java
    /**
     * The array buffer into which the components of the vector are
     * stored. The capacity of the vector is the length of this array buffer,
     * and is at least large enough to contain all the vector's elements.
     *
     * <p>Any array elements following the last element in the Vector are null.
     *
     * @serial
     * 数组缓冲区
     */
    protected Object[] elementData;

    /**
     * The number of valid components in this {@code Vector} object.
     * Components {@code elementData[0]} through
     * {@code elementData[elementCount-1]} are the actual items.
     *
     * @serial
     * 元素个数
     */
    protected int elementCount;

    /**
     * The amount by which the capacity of the vector is automatically
     * incremented when its size becomes greater than its capacity.  If
     * the capacity increment is less than or equal to zero, the capacity
     * of the vector is doubled each time it needs to grow.
     *
     * @serial
     * 容量增量 如果容量增量小于或等于零，则每次需要增长时，向量的容量将增加一倍。
     */
    protected int capacityIncrement;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    private static final long serialVersionUID = -2767605614048989439L;

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 存储元素最大值
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```

# **构造方法**

```java
    /**
     * The amount by which the capacity of the vector is automatically
     * incremented when its size becomes greater than its capacity.  If
     * the capacity increment is less than or equal to zero, the capacity
     * of the vector is doubled each time it needs to grow.
     *
     * @serial
     * 容量增量 如果容量增量小于或等于零，则每次需要增长时，向量的容量将增加一倍。
     */
    protected int capacityIncrement;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    private static final long serialVersionUID = -2767605614048989439L;

    /**
     * Constructs an empty vector with the specified initial capacity and
     * capacity increment.
     *
     * @param   initialCapacity     the initial capacity of the vector
     * @param   capacityIncrement   the amount by which the capacity is
     *                              increased when the vector overflows
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     *         构造具有指定的初始容量和容量增量的空向量。
     */
    public Vector(int initialCapacity, int capacityIncrement) {
        super();
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        this.elementData = new Object[initialCapacity];
        this.capacityIncrement = capacityIncrement;
    }

    /**
     * Constructs an empty vector with the specified initial capacity and
     * with its capacity increment equal to zero.
     *
     * @param   initialCapacity   the initial capacity of the vector
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     *         构造具有指定初始容量并且其容量增量等于零的空向量。
     */
    public Vector(int initialCapacity) {
        this(initialCapacity, 0);
    }

    /**
     * Constructs an empty vector so that its internal data array
     * has size {@code 10} and its standard capacity increment is
     * zero.
     * 构造一个空向量，使其内部数据数组的大小为 10 ，标准容量增量为零。
     */
    public Vector() {
        this(10);
    }

    /**
     * Constructs a vector containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this
     *       vector
     * @throws NullPointerException if the specified collection is null
     * @since   1.2
     * 构造一个包含指定集合元素的向量，按照集合的迭代器返回的顺序。
     */
    public Vector(Collection<? extends E> c) {
        Object[] a = c.toArray();
        elementCount = a.length;
        //c为ArrayList类型 直接赋值
        if (c.getClass() == ArrayList.class) {
            elementData = a;
        } else {
            //拷贝
            elementData = Arrays.copyOf(a, elementCount, Object[].class);
        }
    }
```

## 内部类

参考ArrayList源码

# **添加**

## add(E e)

将元素插入到Vector末尾

```java
    /**
     * Appends the specified element to the end of this Vector.
     *
     * @param e element to be appended to this Vector
     * @return {@code true} (as specified by {@link Collection#add})
     * @since 1.2
     */
    public synchronized boolean add(E e) {
        modCount++;
        // 确定插入后的容量没有超过最大容量，否则对Vector进行扩容
        ensureCapacityHelper(elementCount + 1);
        // 将e赋值给elementData[elementCount]，然后将Vector元素数量加1
        elementData[elementCount++] = e;
        return true;
    }
```



## add(int index, E element)

将元素插入到指定的index处

```java
    /**
     * Inserts the specified element at the specified position in this Vector.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     * @since 1.2
     */
    public void add(int index, E element) {
        // 直接调用insertElementAt方法
        insertElementAt(element, index);
    }
```

## insertElementAt(E obj, int index)

```java
    /**
     * Inserts the specified object as a component in this vector at the
     * specified {@code index}. Each component in this vector with
     * an index greater or equal to the specified {@code index} is
     * shifted upward to have an index one greater than the value it had
     * previously.
     *
     * <p>The index must be a value greater than or equal to {@code 0}
     * and less than or equal to the current size of the vector. (If the
     * index is equal to the current size of the vector, the new element
     * is appended to the Vector.)
     *
     * <p>This method is identical in functionality to the
     * {@link #add(int, Object) add(int, E)}
     * method (which is part of the {@link List} interface).  Note that the
     * {@code add} method reverses the order of the parameters, to more closely
     * match array usage.
     *
     * @param      obj     the component to insert
     * @param      index   where to insert the new component
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public synchronized void insertElementAt(E obj, int index) {
        modCount++;
        if (index > elementCount) {
            throw new ArrayIndexOutOfBoundsException(index
                                                     + " > " + elementCount);
        }
        // 确定插入后的容量没有超过最大容量，否则对Vector进行扩容
        ensureCapacityHelper(elementCount + 1);
        // 使用System.arraycopy将elementData[index]及其之后的元素向后移动一个位置
        System.arraycopy(elementData, index, elementData, index + 1, elementCount - index);
        // 将obj赋值给elementData[index]
        elementData[index] = obj;
        // Vector元素数量加1
        elementCount++;
    }
```

# **删除**

## remove(int index)

删除指定index上的元素

```java
    /**
     * Removes the element at the specified position in this Vector.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).  Returns the element that was removed from the Vector.
     *
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     * @param index the index of the element to be removed
     * @return element that was removed
     * @since 1.2
     */
    public synchronized E remove(int index) {
        modCount++;
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);
        // 获取index处原先的值
        E oldValue = elementData(index);

        int numMoved = elementCount - index - 1;
        if (numMoved > 0)
            // 使用System.arraycopy将elementData[index]之后的元素向前移动一个位置
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        // 将Vector最后一个元素设置为null（以便进行gc），然后将Vector元素数量减1
        elementData[--elementCount] = null; // Let gc do its work
        // 返回index处原先的值
        return oldValue;
    }
```

## remove(Object o)

删除特定元素o

```java
    /**
     * Removes the first occurrence of the specified element in this Vector
     * If the Vector does not contain the element, it is unchanged.  More
     * formally, removes the element with the lowest index i such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))} (if such
     * an element exists).
     *
     * @param o element to be removed from this Vector, if present
     * @return true if the Vector contained the specified element
     * @since 1.2
     */
    public boolean remove(Object o) {
        // 直接调用removeElement方法
        return removeElement(o);
    }
```

## removeElement

```java
    /**
     * Removes the first (lowest-indexed) occurrence of the argument
     * from this vector. If the object is found in this vector, each
     * component in the vector with an index greater or equal to the
     * object's index is shifted downward to have an index one smaller
     * than the value it had previously.
     *
     * <p>This method is identical in functionality to the
     * {@link #remove(Object)} method (which is part of the
     * {@link List} interface).
     *
     * @param   obj   the component to be removed
     * @return  {@code true} if the argument was a component of this
     *          vector; {@code false} otherwise.
     */
    public synchronized boolean removeElement(Object obj) {
        modCount++;
        // 获取元素obj的索引
        // 根据indexOf的源码，若Vector中存在多个obj，将返回遍历Vector得到的第一个obj的索引
        int i = indexOf(obj);
        // 若元素obj存在
        if (i >= 0) {
            // 调用removeElementAt删除指定索引的元素
            // removeElementAt方法与上面的remove(int index)方法基本一致
            removeElementAt(i);
            return true;
        }
        return false;
    }
```

# **查找**

## get(int index)

获取指定index处的元素

```java
    /**
     * Returns the element at the specified position in this Vector.
     *
     * @param index index of the element to return
     * @return object at the specified index
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *            ({@code index < 0 || index >= size()})
     * @since 1.2
     */
    public synchronized E get(int index) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);
        // 直接调用elementData方法
        return elementData(index);
    }
```

# **修改**

## set(int index, E element)

修改index处的元素

```java
    /**
     * Replaces the element at the specified position in this Vector with the
     * specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     * @since 1.2
     */
    public synchronized E set(int index, E element) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);
        // 获取index处原先的值
        E oldValue = elementData(index);
        // 将element赋值给elementData[index]
        elementData[index] = element;
        // 返回index处原先的值
        return oldValue;
    }
```

# **扩容**

## ensureCapacityHelper(int minCapacity)

确保容量

```java
    /**
     * This implements the unsynchronized semantics of ensureCapacity.
     * Synchronized methods in this class can internally call this
     * method for ensuring capacity without incurring the cost of an
     * extra synchronization.
     *
     * @see #ensureCapacity(int)
     */
    private void ensureCapacityHelper(int minCapacity) {
        // overflow-conscious code
        // 当传入容量大于当前容量时，进行扩容
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }
```

## grow(int minCapacity)

扩容

```java
    private void grow(int minCapacity) {
        // overflow-conscious code
        // 获取当前容量
        int oldCapacity = elementData.length;
        // 当已达到上限时，直接修改为最大容量，否则修改为当前容量+设置的增长容量
        int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                                         capacityIncrement : oldCapacity);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // 使用Arrays.copyOf方法将原数组元素复制到容量为newCapacity的新数组中
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
```

