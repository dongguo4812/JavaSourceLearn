# **介绍**

Collections 是java集合框架中的一个工具类，主要用于Collectiont提供的通用算法，比如：**排序**(sort)、**二分查找**(binarySearch)、**洗牌**(shuffle)、**旋转**(rotate)

# **常量&变量**

```java
    /*
     * Tuning parameters for algorithms - Many of the List algorithms have
     * two implementations, one of which is appropriate for RandomAccess
     * lists, the other for "sequential."  Often, the random access variant
     * yields better performance on small sequential access lists.  The
     * tuning parameters below determine the cutoff point for what constitutes
     * a "small" sequential access list for each algorithm.  The values below
     * were empirically determined to work well for LinkedList. Hopefully
     * they should be reasonable for other sequential access List
     * implementations.  Those doing performance work on this code would
     * do well to validate the values of these parameters from time to time.
     * (The first word of each tuning parameter name is the algorithm to which
     * it applies.)
     */
    /** 一些算法所用到的常量，主要是些阈值 **/
    private static final int BINARYSEARCH_THRESHOLD   = 5000;
    private static final int REVERSE_THRESHOLD        =   18;
    private static final int SHUFFLE_THRESHOLD        =    5;
    private static final int FILL_THRESHOLD           =   25;
    private static final int ROTATE_THRESHOLD         =  100;
    private static final int COPY_THRESHOLD           =   10;
    private static final int REPLACEALL_THRESHOLD     =   11;
    private static final int INDEXOFSUBLIST_THRESHOLD =   35;
```

# **构造方法**

私有无参构造

```java
    // Suppresses default constructor, ensuring non-instantiability.
    private Collections() {
    }
```

## **内部类**

Collections中提供了大量的集合类，以代理的方式对现有的集合进行了功能上的修改。

总共有以下几类集合：

Unmodifiable：不可修改的集合类

Synchronized：同步的集合类

Checked：类型安全的集合类

Empty：空集合类

Empty：只有单个元素的集合类

Collections中的内部集合类为传入的类作为代理，以改变传入集合的行为。

### **UnmodifiableCollection**

UnmodifiableCollection是所有Unmodifiable类的父类

UnmodifiableCollection用一个成员变量保存传入对象，然后为该对象提供代理

**UnmodifiableCollection将传入的集合包装成一个不可变的集合。**

```java
    /**
     * @serial include
     */
    static class UnmodifiableCollection<E> implements Collection<E>, Serializable {
        //序列化版本号
        private static final long serialVersionUID = 1820017752578914078L;
        //存储的集合类
        final Collection<? extends E> c;

        UnmodifiableCollection(Collection<? extends E> c) {
            if (c==null)
                throw new NullPointerException();
            this.c = c;
        }
        // 实现行为相同的方法，以代理的方式访问传入的集合类
        public int size()                   {return c.size();}
        public boolean isEmpty()            {return c.isEmpty();}
        public boolean contains(Object o)   {return c.contains(o);}
        public Object[] toArray()           {return c.toArray();}
        public <T> T[] toArray(T[] a)       {return c.toArray(a);}
        public String toString()            {return c.toString();}

        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private final Iterator<? extends E> i = c.iterator();

                public boolean hasNext() {return i.hasNext();}
                public E next()          {return i.next();}
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                @Override
                public void forEachRemaining(Consumer<? super E> action) {
                    // Use backing collection version
                    i.forEachRemaining(action);
                }
            };
        }

        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> coll) {
            return c.containsAll(coll);
        }
        public boolean addAll(Collection<? extends E> coll) {
            throw new UnsupportedOperationException();
        }
        public boolean removeAll(Collection<?> coll) {
            throw new UnsupportedOperationException();
        }
        public boolean retainAll(Collection<?> coll) {
            throw new UnsupportedOperationException();
        }
        public void clear() {
            throw new UnsupportedOperationException();
        }

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            c.forEach(action);
        }
        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }
        @SuppressWarnings("unchecked")
        @Override
        public Spliterator<E> spliterator() {
            return (Spliterator<E>)c.spliterator();
        }
        @SuppressWarnings("unchecked")
        @Override
        public Stream<E> stream() {
            return (Stream<E>)c.stream();
        }
        @SuppressWarnings("unchecked")
        @Override
        public Stream<E> parallelStream() {
            return (Stream<E>)c.parallelStream();
        }
    }
```

### **SynchronizedCollection**

SynchronizedCollection应该算是一个比较重的的集合，他**会把传入的集合包装成一个线程安全的集合类**

```java
    /**
     * Returns a synchronized (thread-safe) collection backed by the specified
     * collection.  In order to guarantee serial access, it is critical that
     * <strong>all</strong> access to the backing collection is accomplished
     * through the returned collection.<p>
     *
     * It is imperative that the user manually synchronize on the returned
     * collection when traversing it via {@link Iterator}, {@link Spliterator}
     * or {@link Stream}:
     * <pre>
     *  Collection c = Collections.synchronizedCollection(myCollection);
     *     ...
     *  synchronized (c) {
     *      Iterator i = c.iterator(); // Must be in the synchronized block
     *      while (i.hasNext())
     *         foo(i.next());
     *  }
     * </pre>
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * <p>The returned collection does <i>not</i> pass the {@code hashCode}
     * and {@code equals} operations through to the backing collection, but
     * relies on {@code Object}'s equals and hashCode methods.  This is
     * necessary to preserve the contracts of these operations in the case
     * that the backing collection is a set or a list.<p>
     *
     * The returned collection will be serializable if the specified collection
     * is serializable.
     *
     * @param  <T> the class of the objects in the collection
     * @param  c the collection to be "wrapped" in a synchronized collection.
     * @return a synchronized view of the specified collection.
     * 返回一个被synchronized包装的集合，方法全部使用synchronized怼代码块加锁
     */
    public static <T> Collection<T> synchronizedCollection(Collection<T> c) {
        return new SynchronizedCollection<>(c);
    }
```

### **CheckedCollection**

**CheckedCollection必须指定类型，将传入对象包装成一个类型安全的集合，**CheckedCollection插入时会检查类型，确保集合是类型安全的

```java
    /**
     * @serial include
     */
    static class CheckedCollection<E> implements Collection<E>, Serializable {
        private static final long serialVersionUID = 1578914078182001775L;

        final Collection<E> c;
        final Class<E> type;

        @SuppressWarnings("unchecked")
        E typeCheck(Object o) {
            if (o != null && !type.isInstance(o))
                throw new ClassCastException(badElementMsg(o));
            return (E) o;
        }

        private String badElementMsg(Object o) {
            return "Attempt to insert " + o.getClass() +
                " element into collection with element type " + type;
        }
        //必须指定类型
        CheckedCollection(Collection<E> c, Class<E> type) {
            this.c = Objects.requireNonNull(c, "c");
            this.type = Objects.requireNonNull(type, "type");
        }

        public int size()                 { return c.size(); }
        public boolean isEmpty()          { return c.isEmpty(); }
        public boolean contains(Object o) { return c.contains(o); }
        public Object[] toArray()         { return c.toArray(); }
        public <T> T[] toArray(T[] a)     { return c.toArray(a); }
        public String toString()          { return c.toString(); }
        public boolean remove(Object o)   { return c.remove(o); }
        public void clear()               {        c.clear(); }

        public boolean containsAll(Collection<?> coll) {
            return c.containsAll(coll);
        }
        public boolean removeAll(Collection<?> coll) {
            return c.removeAll(coll);
        }
        public boolean retainAll(Collection<?> coll) {
            return c.retainAll(coll);
        }

        public Iterator<E> iterator() {
            // JDK-6363904 - unwrapped iterator could be typecast to
            // ListIterator with unsafe set()
            final Iterator<E> it = c.iterator();
            return new Iterator<E>() {
                public boolean hasNext() { return it.hasNext(); }
                public E next()          { return it.next(); }
                public void remove()     {        it.remove(); }};
        }

        public boolean add(E e)          { return c.add(typeCheck(e)); }

        private E[] zeroLengthElementArray; // Lazily initialized

        private E[] zeroLengthElementArray() {
            return zeroLengthElementArray != null ? zeroLengthElementArray :
                (zeroLengthElementArray = zeroLengthArray(type));
        }

        @SuppressWarnings("unchecked")
        Collection<E> checkedCopyOf(Collection<? extends E> coll) {
            Object[] a;
            try {
                E[] z = zeroLengthElementArray();
                a = coll.toArray(z);
                // Defend against coll violating the toArray contract
                if (a.getClass() != z.getClass())
                    a = Arrays.copyOf(a, a.length, z.getClass());
            } catch (ArrayStoreException ignore) {
                // To get better and consistent diagnostics,
                // we call typeCheck explicitly on each element.
                // We call clone() to defend against coll retaining a
                // reference to the returned array and storing a bad
                // element into it after it has been type checked.
                a = coll.toArray().clone();
                for (Object o : a)
                    typeCheck(o);
            }
            // A slight abuse of the type system, but safe here.
            return (Collection<E>) Arrays.asList(a);
        }

        public boolean addAll(Collection<? extends E> coll) {
            // Doing things this way insulates us from concurrent changes
            // in the contents of coll and provides all-or-nothing
            // semantics (which we wouldn't get if we type-checked each
            // element as we added it)
            return c.addAll(checkedCopyOf(coll));
        }

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> action) {c.forEach(action);}
        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            return c.removeIf(filter);
        }
        @Override
        public Spliterator<E> spliterator() {return c.spliterator();}
        @Override
        public Stream<E> stream()           {return c.stream();}
        @Override
        public Stream<E> parallelStream()   {return c.parallelStream();}
    }
```

### **EmptyList**

EmptyList提供一个空的对象，对象不可修改。

Collections中有一个EMPTY_LIST成员变量，该变量为static

例如：某个函数返回一个List，如果返回的List不存在。那么我们不应该返回null，而是使用一个空对象代替。这时候我们可以使用return Collections.EMPTY_LIST;

```java
    /**
     * @serial include
     */
    private static class EmptyList<E>
        extends AbstractList<E>
        implements RandomAccess, Serializable {
        private static final long serialVersionUID = 8842843931221139166L;

        public Iterator<E> iterator() {
            return emptyIterator();
        }
        public ListIterator<E> listIterator() {
            return emptyListIterator();
        }

        public int size() {return 0;}
        public boolean isEmpty() {return true;}

        public boolean contains(Object obj) {return false;}
        public boolean containsAll(Collection<?> c) { return c.isEmpty(); }

        public Object[] toArray() { return new Object[0]; }

        public <T> T[] toArray(T[] a) {
            if (a.length > 0)
                a[0] = null;
            return a;
        }

        public E get(int index) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }

        public boolean equals(Object o) {
            return (o instanceof List) && ((List<?>)o).isEmpty();
        }

        public int hashCode() { return 1; }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter);
            return false;
        }
        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
        }
        @Override
        public void sort(Comparator<? super E> c) {
        }

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
        }

        @Override
        public Spliterator<E> spliterator() { return Spliterators.emptySpliterator(); }

        // Preserves singleton property
        private Object readResolve() {
            return EMPTY_LIST;
        }
    }
```

### **SingletonList**

**SingletonList内只允许存储一个元素**，该元素在List初始化时确定。

```java
    /**
     * @serial include
     */
    private static class SingletonList<E>
        extends AbstractList<E>
        implements RandomAccess, Serializable {

        private static final long serialVersionUID = 3093736618740652951L;

        private final E element;

        // 初始化时传入一个元素
        SingletonList(E obj)                {element = obj;}

        public Iterator<E> iterator() {
            return singletonIterator(element);
        }

        public int size()                   {return 1;}

        public boolean contains(Object obj) {return eq(obj, element);}
        // 只允许返回第一个元素
        public E get(int index) {
            if (index != 0)
              throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
            return element;
        }

        // Override default methods for Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            action.accept(element);
        }
        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void sort(Comparator<? super E> c) {
        }
        @Override
        public Spliterator<E> spliterator() {
            return singletonSpliterator(element);
        }
    }
```

# **常用方法**

## **sort**

Collections.sort集合排序最终调用的是java.util包下Arrays类的 Arrays.sort(T[] a, Comparator c)，这个方法根据有没有传入Compararot c 调用快速排序(sort())和优化的归并排序(TimSort.sort())

```java
    /**
     * Sorts the specified list into ascending order, according to the
     * {@linkplain Comparable natural ordering} of its elements.
     * All elements in the list must implement the {@link Comparable}
     * interface.  Furthermore, all elements in the list must be
     * <i>mutually comparable</i> (that is, {@code e1.compareTo(e2)}
     * must not throw a {@code ClassCastException} for any elements
     * {@code e1} and {@code e2} in the list).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * <p>The specified list must be modifiable, but need not be resizable.
     *
     * @implNote
     * This implementation defers to the {@link List#sort(Comparator)}
     * method using the specified list and a {@code null} comparator.
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list to be sorted.
     * @throws ClassCastException if the list contains elements that are not
     *         <i>mutually comparable</i> (for example, strings and integers).
     * @throws UnsupportedOperationException if the specified list's
     *         list-iterator does not support the {@code set} operation.
     * @throws IllegalArgumentException (optional) if the implementation
     *         detects that the natural ordering of the list elements is
     *         found to violate the {@link Comparable} contract
     * @see List#sort(Comparator)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        // 直接调用list的排序方法
        list.sort(null);
    }
    
     
       /**
     * Sorts the specified list according to the order induced by the
     * specified comparator.  All elements in the list must be <i>mutually
     * comparable</i> using the specified comparator (that is,
     * {@code c.compare(e1, e2)} must not throw a {@code ClassCastException}
     * for any elements {@code e1} and {@code e2} in the list).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * <p>The specified list must be modifiable, but need not be resizable.
     *
     * @implNote
     * This implementation defers to the {@link List#sort(Comparator)}
     * method using the specified list and comparator.
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list to be sorted.
     * @param  c the comparator to determine the order of the list.  A
     *        {@code null} value indicates that the elements' <i>natural
     *        ordering</i> should be used.
     * @throws ClassCastException if the list contains elements that are not
     *         <i>mutually comparable</i> using the specified comparator.
     * @throws UnsupportedOperationException if the specified list's
     *         list-iterator does not support the {@code set} operation.
     * @throws IllegalArgumentException (optional) if the comparator is
     *         found to violate the {@link Comparator} contract
     * @see List#sort(Comparator)
     * 排序，传入集合和比较器
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void sort(List<T> list, Comparator<? super T> c) {
        // 直接调用ArrayList的排序方法
        list.sort(c);
    }

```

ArrayList的sort()方法调用Arrays的sort(),请参考Arrays源码

## **binarySearch**

二分查找的前提是集合有序，否则不能满足二分算法的查找过程。

```java
    /**
     * Searches the specified list for the specified object using the binary
     * search algorithm.  The list must be sorted into ascending order
     * according to the {@linkplain Comparable natural ordering} of its
     * elements (as by the {@link #sort(List)} method) prior to making this
     * call.  If it is not sorted, the results are undefined.  If the list
     * contains multiple elements equal to the specified object, there is no
     * guarantee which one will be found.
     *
     * <p>This method runs in log(n) time for a "random access" list (which
     * provides near-constant-time positional access).  If the specified list
     * does not implement the {@link RandomAccess} interface and is large,
     * this method will do an iterator-based binary search that performs
     * O(n) link traversals and O(log n) element comparisons.
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list to be searched.
     * @param  key the key to be searched for.
     * @return the index of the search key, if it is contained in the list;
     *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the list: the index of the first
     *         element greater than the key, or <tt>list.size()</tt> if all
     *         elements in the list are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws ClassCastException if the list contains elements that are not
     *         <i>mutually comparable</i> (for example, strings and
     *         integers), or the search key is not mutually comparable
     *         with the elements of the list.
     */
    public static <T>
    int binarySearch(List<? extends Comparable<? super T>> list, T key) {
        // 当list为随机访问列表或者长度小于阈值5000时，使用索引二叉搜索
        if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
            return Collections.indexedBinarySearch(list, key);
        else
            // 否则使用迭代二叉搜索
            return Collections.iteratorBinarySearch(list, key);
    }

    //索引二叉搜索

    private static <T>
    int indexedBinarySearch(List<? extends Comparable<? super T>> list, T key) {
        //初始低位为0
        int low = 0;
        //初始高位为 list.size()-1
        int high = list.size()-1;

        while (low <= high) {
            // 位运算获取中间下标
            int mid = (low + high) >>> 1;
            //获取中间下标元素
            Comparable<? super T> midVal = list.get(mid);
            // 调用compareTo方法, 中间元素和寻找元素做对比 大于返回大于0, 小于返回小于0, 相等返回0
            int cmp = midVal.compareTo(key);
            // 更新上界和下界，缩小查找范围
            if (cmp < 0)
                // 小于0说明中间元素小于寻找元素, 说明寻找元素在右边, 低位 = 中间下标 + 1
                low = mid + 1;
            else if (cmp > 0)
                // 大于0说明中间元素大于寻找元素, 说明寻找元素在左边, 高位 = 中间下标 - 1
                high = mid - 1;
            else
                // 相等 找到该值 返回中间下标
                return mid; // key found
        }
        // 当为查找到时，返回负的最小下标+1
        return -(low + 1);  // key not found
    }

    //迭代器二叉搜索  迭代器二分查找通过迭代器获取元素, 时间复杂度为O(n)
    private static <T>
    int iteratorBinarySearch(List<? extends Comparable<? super T>> list, T key)
    {
        // 初始低位为0
        int low = 0;
        // 初始高位为集合大小 - 1
        int high = list.size()-1;
        // 与索引搜索不同，使用迭代器
        ListIterator<? extends Comparable<? super T>> i = list.listIterator();

        while (low <= high) {
            // 通过位运算获取中间下标
            int mid = (low + high) >>> 1;
            // 通过迭代器获取中间下标元素值
            Comparable<? super T> midVal = get(i, mid);
            // 中间值和寻找值做对于 大于返回大于0, 小于返回小于0, 相等返回等于0
            int cmp = midVal.compareTo(key);

            if (cmp < 0)
                // 中间值小于寻找值, 说明寻找值在右边, 低位 = 中间下标 + 1
                low = mid + 1;
            else if (cmp > 0)
                // 中间值大于寻找值, 说明寻找值在左边, 高位 = 中间下标 - 1
                high = mid - 1;
            else
                // 找到该值 返回下标
                return mid; // key found
        }
        // 没有找到返回 -(low + 1)
        return -(low + 1);  // key not found
    }
```

## **shuffle**

洗牌算法就是将List集合中的元素打乱，一般可以用于抽奖、摇号、洗牌等场景。

```java
    /**
     * Randomly permutes the specified list using a default source of
     * randomness.  All permutations occur with approximately equal
     * likelihood.
     *
     * <p>The hedge "approximately" is used in the foregoing description because
     * default source of randomness is only approximately an unbiased source
     * of independently chosen bits. If it were a perfect source of randomly
     * chosen bits, then the algorithm would choose permutations with perfect
     * uniformity.
     *
     * <p>This implementation traverses the list backwards, from the last
     * element up to the second, repeatedly swapping a randomly selected element
     * into the "current position".  Elements are randomly selected from the
     * portion of the list that runs from the first element to the current
     * position, inclusive.
     *
     * <p>This method runs in linear time.  If the specified list does not
     * implement the {@link RandomAccess} interface and is large, this
     * implementation dumps the specified list into an array before shuffling
     * it, and dumps the shuffled array back into the list.  This avoids the
     * quadratic behavior that would result from shuffling a "sequential
     * access" list in place.
     *
     * @param  list the list to be shuffled.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> operation.
     */
    public static void shuffle(List<?> list) {
        Random rnd = r;
        // 没有传入固定随机种子则初始化
        if (rnd == null)
            r = rnd = new Random(); // harmless race.
        // 洗牌算法
        shuffle(list, rnd);
    }
    //随机值
    private static Random r;
    
    /**
     * Randomly permute the specified list using the specified source of
     * randomness.  All permutations occur with equal likelihood
     * assuming that the source of randomness is fair.<p>
     *
     * This implementation traverses the list backwards, from the last element
     * up to the second, repeatedly swapping a randomly selected element into
     * the "current position".  Elements are randomly selected from the
     * portion of the list that runs from the first element to the current
     * position, inclusive.<p>
     *
     * This method runs in linear time.  If the specified list does not
     * implement the {@link RandomAccess} interface and is large, this
     * implementation dumps the specified list into an array before shuffling
     * it, and dumps the shuffled array back into the list.  This avoids the
     * quadratic behavior that would result from shuffling a "sequential
     * access" list in place.
     *
     * @param  list the list to be shuffled.
     * @param  rnd the source of randomness to use to shuffle the list.
     * @throws UnsupportedOperationException if the specified list or its
     *         list-iterator does not support the <tt>set</tt> operation.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void shuffle(List<?> list, Random rnd) {
        //集合容量
        int size = list.size();
        // 集合容量小于洗牌阙值5 或者 是RandomAccess
        if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
            //调用交换算法
            for (int i=size; i>1; i--)
                swap(list, i-1, rnd.nextInt(i));
        } else {
            // 集合容量大于洗牌阙值5, 且不是RandomAccess, 比如LindedList
            // 转成数组,对数组做调换操作, 不调用list的set方法主要是因为LindedList的set方法要获取传入下标的元素,获取元素
            // 时间复杂度为O(n)性能低
            Object[] arr = list.toArray();

            // Shuffle array
            //对数组做交换操作
            for (int i=size; i>1; i--)
                swap(arr, i-1, rnd.nextInt(i));

            // Dump array back into list
            // instead of using a raw type here, it's possible to capture
            // the wildcard but it will require a call to a supplementary
            // private method
            // 通过迭代器进行元素的调换, 使用迭代器的set方法性能就是很高,因为可以通过迭代器获取元素
            ListIterator it = list.listIterator();
            for (int i=0; i<arr.length; i++) {
                it.next();
                it.set(arr[i]);
            }
        }
    }
```

### **swap**

```java
    /**
     * Swaps the elements at the specified positions in the specified list.
     * (If the specified positions are equal, invoking this method leaves
     * the list unchanged.)
     *
     * @param list The list in which to swap elements.
     * @param i the index of one element to be swapped.
     * @param j the index of the other element to be swapped.
     * @throws IndexOutOfBoundsException if either <tt>i</tt> or <tt>j</tt>
     *         is out of range (i &lt; 0 || i &gt;= list.size()
     *         || j &lt; 0 || j &gt;= list.size()).
     * @since 1.4
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void swap(List<?> list, int i, int j) {
        // instead of using a raw type here, it's possible to capture
        // the wildcard but it will require a call to a supplementary
        // private method
        final List l = list;
        // 通过list.set(int index, E e)方法进行元素调换, 该方法返回原元素
        // 比如list(1, 2, 3, 4, 5, 6, 7, 8) i = 7, j = 0
        // 首先 l.set(j, l.get(i)) 将下标为7的元素调到下位为0的位置,并返回元素1, 此时list为(8,2,3,4,5,6,7,8)
        // l.set(i, 1), 将元素1设置到下标7, 此时list为(8,2,3,4,5,6,7,1)
        // 这样子就完成了元素调换
        l.set(i, l.set(j, l.get(i)));
    }
    
   /**
     * Swaps the two specified elements in the specified array.
     */
    private static void swap(Object[] arr, int i, int j) {
        //完成下标i,j元素的调换
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }
```

## **rotate**

rotate旋转算法可以把ArrayList、LinkedList从指定的位置开始，进行顺时针或者逆时针旋转操作。

```java
    /**
     * Rotates the elements in the specified list by the specified distance.
     * After calling this method, the element at index <tt>i</tt> will be
     * the element previously at index <tt>(i - distance)</tt> mod
     * <tt>list.size()</tt>, for all values of <tt>i</tt> between <tt>0</tt>
     * and <tt>list.size()-1</tt>, inclusive.  (This method has no effect on
     * the size of the list.)
     *
     * <p>For example, suppose <tt>list</tt> comprises<tt> [t, a, n, k, s]</tt>.
     * After invoking <tt>Collections.rotate(list, 1)</tt> (or
     * <tt>Collections.rotate(list, -4)</tt>), <tt>list</tt> will comprise
     * <tt>[s, t, a, n, k]</tt>.
     *
     * <p>Note that this method can usefully be applied to sublists to
     * move one or more elements within a list while preserving the
     * order of the remaining elements.  For example, the following idiom
     * moves the element at index <tt>j</tt> forward to position
     * <tt>k</tt> (which must be greater than or equal to <tt>j</tt>):
     * <pre>
     *     Collections.rotate(list.subList(j, k+1), -1);
     * </pre>
     * To make this concrete, suppose <tt>list</tt> comprises
     * <tt>[a, b, c, d, e]</tt>.  To move the element at index <tt>1</tt>
     * (<tt>b</tt>) forward two positions, perform the following invocation:
     * <pre>
     *     Collections.rotate(l.subList(1, 4), -1);
     * </pre>
     * The resulting list is <tt>[a, c, d, b, e]</tt>.
     *
     * <p>To move more than one element forward, increase the absolute value
     * of the rotation distance.  To move elements backward, use a positive
     * shift distance.
     *
     * <p>If the specified list is small or implements the {@link
     * RandomAccess} interface, this implementation exchanges the first
     * element into the location it should go, and then repeatedly exchanges
     * the displaced element into the location it should go until a displaced
     * element is swapped into the first element.  If necessary, the process
     * is repeated on the second and successive elements, until the rotation
     * is complete.  If the specified list is large and doesn't implement the
     * <tt>RandomAccess</tt> interface, this implementation breaks the
     * list into two sublist views around index <tt>-distance mod size</tt>.
     * Then the {@link #reverse(List)} method is invoked on each sublist view,
     * and finally it is invoked on the entire list.  For a more complete
     * description of both algorithms, see Section 2.3 of Jon Bentley's
     * <i>Programming Pearls</i> (Addison-Wesley, 1986).
     *
     * @param list the list to be rotated.
     * @param distance the distance to rotate the list.  There are no
     *        constraints on this value; it may be zero, negative, or
     *        greater than <tt>list.size()</tt>.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> operation.
     * @since 1.4
     * 翻转一定距离
     */
    public static void rotate(List<?> list, int distance) {
        // 为随机访问列表或者数量小于100时
        if (list instanceof RandomAccess || list.size() < ROTATE_THRESHOLD)
            rotate1(list, distance);
        else
            rotate2(list, distance);
    }

    private static <T> void rotate1(List<T> list, int distance) {
        //集合容量
        int size = list.size();
        if (size == 0)
            return;
        // 计算旋转的距离
        distance = distance % size;
        // 将过短的距离增加
        if (distance < 0)
            distance += size;
        if (distance == 0)
            return;
        // 将元素进行移动distance距离
        for (int cycleStart = 0, nMoved = 0; nMoved != size; cycleStart++) {
            //旋转的元素
            T displaced = list.get(cycleStart);
            // 旋转的元素下标
            int i = cycleStart;
            do {
                i += distance;
                // 将后半部分元素前移动
                if (i >= size)
                    i -= size;
                // 通过list.set方法旋转元素
                displaced = list.set(i, displaced);
                nMoved ++;
            } while (i != cycleStart);
        }
    }

    private static void rotate2(List<?> list, int distance) {
        int size = list.size();
        if (size == 0)
            return;
        int mid =  -distance % size;
        // 防止越界
        if (mid < 0)
            mid += size;
        if (mid == 0)
            return;
        // 通过mid进行划分，分别逆置
        reverse(list.subList(0, mid));
        reverse(list.subList(mid, size));
        // 逆置所有元素
        reverse(list);
    }
```

### **reverse**

```java
    /**
     * Reverses the order of the elements in the specified list.<p>
     *
     * This method runs in linear time.
     *
     * @param  list the list whose elements are to be reversed.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> operation.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void reverse(List<?> list) {
        int size = list.size();
        // 当数量小于18或者为随机访问列表时，直接对头尾依次交换
        if (size < REVERSE_THRESHOLD || list instanceof RandomAccess) {
            for (int i=0, mid=size>>1, j=size-1; i<mid; i++, j--)
                swap(list, i, j);
        } else {
            // instead of using a raw type here, it's possible to capture
            // the wildcard but it will require a call to a supplementary
            // private method
            // 使用迭代器进行交换
            ListIterator fwd = list.listIterator();
            ListIterator rev = list.listIterator(size);
            for (int i=0, mid=list.size()>>1; i<mid; i++) {
                Object tmp = fwd.next();
                fwd.set(rev.previous());
                rev.set(tmp);
            }
        }
    }
```

## **fill**

```java
    /**
     * Replaces all of the elements of the specified list with the specified
     * element. <p>
     *
     * This method runs in linear time.
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list to be filled with the specified element.
     * @param  obj The element with which to fill the specified list.
     * @throws UnsupportedOperationException if the specified list or its
     *         list-iterator does not support the <tt>set</tt> operation.
     *  将元素填入list每一个位置
     */
    public static <T> void fill(List<? super T> list, T obj) {
        int size = list.size();
        // 当数量小于25并且时是随机访问列表时，直接使用set进行赋值
        if (size < FILL_THRESHOLD || list instanceof RandomAccess) {
            for (int i=0; i<size; i++)
                list.set(i, obj);
        } else {
            // 使用迭代器进行赋值
            ListIterator<? super T> itr = list.listIterator();
            for (int i=0; i<size; i++) {
                itr.next();
                itr.set(obj);
            }
        }
    }
```

## **min**

```java
    /**
     * Returns the minimum element of the given collection, according to the
     * <i>natural ordering</i> of its elements.  All elements in the
     * collection must implement the <tt>Comparable</tt> interface.
     * Furthermore, all elements in the collection must be <i>mutually
     * comparable</i> (that is, <tt>e1.compareTo(e2)</tt> must not throw a
     * <tt>ClassCastException</tt> for any elements <tt>e1</tt> and
     * <tt>e2</tt> in the collection).<p>
     *
     * This method iterates over the entire collection, hence it requires
     * time proportional to the size of the collection.
     *
     * @param  <T> the class of the objects in the collection
     * @param  coll the collection whose minimum element is to be determined.
     * @return the minimum element of the given collection, according
     *         to the <i>natural ordering</i> of its elements.
     * @throws ClassCastException if the collection contains elements that are
     *         not <i>mutually comparable</i> (for example, strings and
     *         integers).
     * @throws NoSuchElementException if the collection is empty.
     * @see Comparable
     * 求最小值，最大值类似
     */
    public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> coll) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();
        // 依次进行比较，选出最小的元素
        while (i.hasNext()) {
            T next = i.next();
            if (next.compareTo(candidate) < 0)
                candidate = next;
        }
        return candidate;
    }

```

## **replaceAll**

```java
    /**
     * Replaces all occurrences of one specified value in a list with another.
     * More formally, replaces with <tt>newVal</tt> each element <tt>e</tt>
     * in <tt>list</tt> such that
     * <tt>(oldVal==null ? e==null : oldVal.equals(e))</tt>.
     * (This method has no effect on the size of the list.)
     *
     * @param  <T> the class of the objects in the list
     * @param list the list in which replacement is to occur.
     * @param oldVal the old value to be replaced.
     * @param newVal the new value with which <tt>oldVal</tt> is to be
     *        replaced.
     * @return <tt>true</tt> if <tt>list</tt> contained one or more elements
     *         <tt>e</tt> such that
     *         <tt>(oldVal==null ?  e==null : oldVal.equals(e))</tt>.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> operation.
     * @since  1.4
     */
    public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) {
        boolean result = false;
        int size = list.size();
        // 当大小小于11或者为随机访问列表时
        if (size < REPLACEALL_THRESHOLD || list instanceof RandomAccess) {
            // 老的值为空时
            if (oldVal==null) {
                // 遍历修改值
                for (int i=0; i<size; i++) {
                    if (list.get(i)==null) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            } else {
                // 当不为空时，通过equals判断是否相等，然后遍历赋值
                for (int i=0; i<size; i++) {
                    if (oldVal.equals(list.get(i))) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            }
        } else {
            ListIterator<T> itr=list.listIterator();
            if (oldVal==null) {
                for (int i=0; i<size; i++) {
                    if (itr.next()==null) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            } else {
                for (int i=0; i<size; i++) {
                    if (oldVal.equals(itr.next())) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            }
        }
        return result;
    }
```

## **indexOfSubList**

```java
    /**
     * Returns the starting position of the first occurrence of the specified
     * target list within the specified source list, or -1 if there is no
     * such occurrence.  More formally, returns the lowest index <tt>i</tt>
     * such that {@code source.subList(i, i+target.size()).equals(target)},
     * or -1 if there is no such index.  (Returns -1 if
     * {@code target.size() > source.size()})
     *
     * <p>This implementation uses the "brute force" technique of scanning
     * over the source list, looking for a match with the target at each
     * location in turn.
     *
     * @param source the list in which to search for the first occurrence
     *        of <tt>target</tt>.
     * @param target the list to search for as a subList of <tt>source</tt>.
     * @return the starting position of the first occurrence of the specified
     *         target list within the specified source list, or -1 if there
     *         is no such occurrence.
     * @since  1.4
     * 查找子字符串的下标
     */
    public static int indexOfSubList(List<?> source, List<?> target) {
        int sourceSize = source.size();
        int targetSize = target.size();
        // 最大下标
        int maxCandidate = sourceSize - targetSize;
        // 当源list长度小于35或源list和目标list都是随机访问列表时
        if (sourceSize < INDEXOFSUBLIST_THRESHOLD ||
            (source instanceof RandomAccess&&target instanceof RandomAccess)) {
            // 当元素不相同时直接跳出，继续判断下一个元素，当满足条件时，则返回maxCandidate
        nextCand:
            for (int candidate = 0; candidate <= maxCandidate; candidate++) {
                for (int i=0, j=candidate; i<targetSize; i++, j++)
                    if (!eq(target.get(i), source.get(j)))
                        continue nextCand;  // Element mismatch, try next cand
                return candidate;  // All elements of candidate matched target
            }
        } else {  // Iterator version of above algorithm
            ListIterator<?> si = source.listIterator();
        nextCand:
            for (int candidate = 0; candidate <= maxCandidate; candidate++) {
                ListIterator<?> ti = target.listIterator();
                for (int i=0; i<targetSize; i++) {
                    if (!eq(ti.next(), si.next())) {
                        // Back up source iterator to next candidate
                        for (int j=0; j<i; j++)
                            si.previous();
                        continue nextCand;
                    }
                }
                return candidate;
            }
        }
        // 未匹配时返回-1
        return -1;  // No candidate matched the target
    }
```

## **copy**

```java

    /**
     * Copies all of the elements from one list into another.  After the
     * operation, the index of each copied element in the destination list
     * will be identical to its index in the source list.  The destination
     * list must be at least as long as the source list.  If it is longer, the
     * remaining elements in the destination list are unaffected. <p>
     *
     * This method runs in linear time.
     *
     * @param  <T> the class of the objects in the lists
     * @param  dest The destination list.
     * @param  src The source list.
     * @throws IndexOutOfBoundsException if the destination list is too small
     *         to contain the entire source List.
     * @throws UnsupportedOperationException if the destination list's
     *         list-iterator does not support the <tt>set</tt> operation.
     */
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        // 源list长度
        int srcSize = src.size();
        // 当源list长度大于目标list长度时，直接抛出
        if (srcSize > dest.size())
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        // 当源list长度小于10或者两个list都是随机访问列表时
        if (srcSize < COPY_THRESHOLD ||
            (src instanceof RandomAccess && dest instanceof RandomAccess)) {
            // 遍历赋值
            for (int i=0; i<srcSize; i++)
                dest.set(i, src.get(i));
        } else {
            // 使用迭代器进行赋值
            ListIterator<? super T> di=dest.listIterator();
            ListIterator<? extends T> si=src.listIterator();
            for (int i=0; i<srcSize; i++) {
                di.next();
                di.set(si.next());
            }
        }
    }
```

## **nCopies**

```java
    /**
     * Returns an immutable list consisting of <tt>n</tt> copies of the
     * specified object.  The newly allocated data object is tiny (it contains
     * a single reference to the data object).  This method is useful in
     * combination with the <tt>List.addAll</tt> method to grow lists.
     * The returned list is serializable.
     *
     * @param  <T> the class of the object to copy and of the objects
     *         in the returned list.
     * @param  n the number of elements in the returned list.
     * @param  o the element to appear repeatedly in the returned list.
     * @return an immutable list consisting of <tt>n</tt> copies of the
     *         specified object.
     * @throws IllegalArgumentException if {@code n < 0}
     * @see    List#addAll(Collection)
     * @see    List#addAll(int, Collection)
     * 生成一个含有n个o的不可变list
     */
    public static <T> List<T> nCopies(int n, T o) {
        // n小于0时直接抛出
        if (n < 0)
            throw new IllegalArgumentException("List length = " + n);
        // 生成一个复制list(无set方法)
        return new CopiesList<>(n, o);
    }
```

## **frequency**

```java
    /**
     * Returns the number of elements in the specified collection equal to the
     * specified object.  More formally, returns the number of elements
     * <tt>e</tt> in the collection such that
     * <tt>(o == null ? e == null : o.equals(e))</tt>.
     *
     * @param c the collection in which to determine the frequency
     *     of <tt>o</tt>
     * @param o the object whose frequency is to be determined
     * @return the number of elements in {@code c} equal to {@code o}
     * @throws NullPointerException if <tt>c</tt> is null
     * @since 1.5
     * 求list中某一元素的出现频率
     */
    public static int frequency(Collection<?> c, Object o) {
        int result = 0;
        // 当对象为null时直接判断null的数量，否则使用equals进行判断
        if (o == null) {
            for (Object e : c)
                if (e == null)
                    result++;
        } else {
            for (Object e : c)
                if (o.equals(e))
                    result++;
        }
        return result;
    }
```

