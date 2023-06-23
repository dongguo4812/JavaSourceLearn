# **介绍**

CopyOnWriteArrayList是Java并发包中提供的一个并发容器，它是个线程安全且**读操作**无锁的ArrayList，**写操作**则通过创建底层数组的新副本来实现，是一种读写分离的并发策略

1. 在保证并发读取的前提下，确保了写入时的线程安全；
2. 由于每次写入操作时，进行了Copy复制原数组，所以无需扩容；
3. 适合读多写少的应用场景。由于add()、set() 、 remove()等修改操作需要复制整个数组，所以会有内存开销大的问题。
4. CopyOnWriteArrayList由于只在写入时加锁，所以只能保证数据的最终一致性，不能保证数据的实时一致性。

```java
public class CopyOnWriteArrayList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

![image-20230623205955598](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306232100701.png)

# **常量&变量**

```java
    //序列化版本号
    private static final long serialVersionUID = 8673264195747942595L;

    /** The lock protecting all mutators */
    //控制所有函数的同步锁
    final transient ReentrantLock lock = new ReentrantLock();

    /** The array, accessed only via getArray/setArray. */
    //存储数据的数组，设置volatile，保证可见性
    private transient volatile Object[] array;
```

# **构造方法**

```java
    /**
     * Gets the array.  Non-private so as to also be accessible
     * from CopyOnWriteArraySet class.
     */
    final Object[] getArray() {
        return array;
    }

    /**
     * Sets the array.
     */
    final void setArray(Object[] a) {
        array = a;
    }

    /**
     * Creates an empty list.
     * 创建一个空的CopyOnWriteArrayList对象。
     */
    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    /**
     * Creates a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection of initially held elements
     * @throws NullPointerException if the specified collection is null
     *创建一个包含给定集合元素的CopyOnWriteArrayList对象。
     *集合c中的元素按照它们在集合中的顺序被添加到新的CopyOnWriteArrayList对象中。
     */
    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] elements;
        if (c.getClass() == CopyOnWriteArrayList.class)
            elements = ((CopyOnWriteArrayList<?>)c).getArray();
        else {
            elements = c.toArray();
            if (c.getClass() != ArrayList.class)
                elements = Arrays.copyOf(elements, elements.length, Object[].class);
        }
        setArray(elements);
    }

    /**
     * Creates a list holding a copy of the given array.
     *
     * @param toCopyIn the array (a copy of this array is used as the
     *        internal array)
     * @throws NullPointerException if the specified array is null
     *创建一个包含给定数组元素的CopyOnWriteArrayList对象。
     *数组toCopyIn中的元素按照它们在数组中的顺序被添加到新的CopyOnWriteArrayList对象中。
     */
    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

```



# **内部类**

## **COWIterator**

COWIterator表示迭代器，其也有一个Object类型的数组作为CopyOnWriteArrayList数组的快照，这种快照风格的迭代器方法在创建迭代器时使用了对当时数组状态的引用。此数组在迭代器的生存期内不会更改，因此不可能发生冲突，并且迭代器保证不会抛出 ConcurrentModificationException。创建迭代器以后，迭代器就不会反映列表的添加、移除或者更改。在迭代器上进行的元素更改操作（remove、set 和 add）不受支持。这些方法将抛出 UnsupportedOperationException。

```java
    static final class COWIterator<E> implements ListIterator<E> {
        /** Snapshot of the array */
        //快照
        private final Object[] snapshot;
        /** Index of element to be returned by subsequent call to next.  */
        //游标
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }
        //是否有下一个元素
        public boolean hasNext() {
            return cursor < snapshot.length;
        }
        //是否有上一个元素
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (! hasNext())
                throw new NoSuchElementException();
            return (E) snapshot[cursor++];
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            if (! hasPrevious())
                throw new NoSuchElementException();
            return (E) snapshot[--cursor];
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; {@code remove}
         *         is not supported by this iterator.
         *         不支持remove
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; {@code set}
         *         is not supported by this iterator.
         *         不支持set
         */
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; {@code add}
         *         is not supported by this iterator.
         *         不支持add
         */
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] elements = snapshot;
            final int size = elements.length;
            for (int i = cursor; i < size; i++) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                action.accept(e);
            }
            cursor = size;
        }
    }
```

## **COWSubList**

CopyOnWriteArrayList定义了一个子类COWSubList，用于在sublist时返回，为了少写代码，此类继承了AbstractList，它有以下特点：

没有使用modCount来记录是否有其他修改，而是通过一种更巧妙的方式——它记录了对当前数组的引用，如果当前数组的引用和CopyOnWriteArrayList.getArray()结果不同，表示有人创建了新的副本并进行了修改操作

此类没有专门做优化，所以addAll()之类的批量操作方法是通过批量调用add()实现的，增加10个值可能会连续创建10个副本，这里可能有性能问题，

```java
    /**
     * Sublist for CopyOnWriteArrayList.
     * This class extends AbstractList merely for convenience, to
     * avoid having to define addAll, etc. This doesn't hurt, but
     * is wasteful.  This class does not need or use modCount
     * mechanics in AbstractList, but does need to check for
     * concurrent modification using similar mechanics.  On each
     * operation, the array that we expect the backing list to use
     * is checked and updated.  Since we do this for all of the
     * base operations invoked by those defined in AbstractList,
     * all is well.  While inefficient, this is not worth
     * improving.  The kinds of list operations inherited from
     * AbstractList are already so slow on COW sublists that
     * adding a bit more space/time doesn't seem even noticeable.
     */
    private static class COWSubList<E>
        extends AbstractList<E>
        implements RandomAccess
    {
        private final CopyOnWriteArrayList<E> l;
        private final int offset;
        private int size;
        private Object[] expectedArray;

        // only call this holding l's lock
        COWSubList(CopyOnWriteArrayList<E> list,
                   int fromIndex, int toIndex) {
            l = list;
            expectedArray = l.getArray();
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        // only call this holding l's lock
        private void checkForComodification() {
            if (l.getArray() != expectedArray)
                throw new ConcurrentModificationException();
        }

        // only call this holding l's lock
        private void rangeCheck(int index) {
            if (index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ",Size: "+size);
        }

        public E set(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E x = l.set(index+offset, element);
                expectedArray = l.getArray();
                return x;
            } finally {
                lock.unlock();
            }
        }

        public E get(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                return l.get(index+offset);
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return size;
            } finally {
                lock.unlock();
            }
        }

        public void add(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException();
                l.add(index+offset, element);
                expectedArray = l.getArray();
                size++;
            } finally {
                lock.unlock();
            }
        }

        public void clear() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                l.removeRange(offset, offset+size);
                expectedArray = l.getArray();
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        public E remove(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E result = l.remove(index+offset);
                expectedArray = l.getArray();
                size--;
                return result;
            } finally {
                lock.unlock();
            }
        }

        public boolean remove(Object o) {
            int index = indexOf(o);
            if (index == -1)
                return false;
            remove(index);
            return true;
        }

        public Iterator<E> iterator() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return new COWSubListIterator<E>(l, 0, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public ListIterator<E> listIterator(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException("Index: "+index+
                                                        ", Size: "+size);
                return new COWSubListIterator<E>(l, index, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public List<E> subList(int fromIndex, int toIndex) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (fromIndex < 0 || toIndex > size || fromIndex > toIndex)
                    throw new IndexOutOfBoundsException();
                return new COWSubList<E>(l, fromIndex + offset,
                                         toIndex + offset);
            } finally {
                lock.unlock();
            }
        }

        public void forEach(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            for (int i = lo; i < hi; ++i) {
                @SuppressWarnings("unchecked") E e = (E) a[i];
                action.accept(e);
            }
        }

        public void replaceAll(UnaryOperator<E> operator) {
            if (operator == null) throw new NullPointerException();
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                for (int i = lo; i < hi; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) elements[i];
                    newElements[i] = operator.apply(e);
                }
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public void sort(Comparator<? super E> c) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                @SuppressWarnings("unchecked") E[] es = (E[])newElements;
                Arrays.sort(es, lo, hi, c);
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (!c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                         lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                         lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean removeIf(Predicate<? super E> filter) {
            if (filter == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) elements[i];
                        if (!filter.test(e))
                            temp[newSize++] = e;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                         lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public Spliterator<E> spliterator() {
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            return Spliterators.spliterator
                (a, lo, hi, Spliterator.IMMUTABLE | Spliterator.ORDERED);
        }

    }
```

## **COWSubListIterator**

CopyOnWriteArrayList为内部类COWSubList提供了一个用于遍历的子类COWSubListIterator，一个标准实现而已，没啥特别的。

```java
  private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<E> l, int index, int offset, int size) {
            this.offset = offset;
            this.size = size;
            it = l.listIterator(index+offset);
        }

        public boolean hasNext() {
            return nextIndex() < size;
        }

        public E next() {
            if (hasNext())
                return it.next();
            else
                throw new NoSuchElementException();
        }

        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        public E previous() {
            if (hasPrevious())
                return it.previous();
            else
                throw new NoSuchElementException();
        }

        public int nextIndex() {
            return it.nextIndex() - offset;
        }

        public int previousIndex() {
            return it.previousIndex() - offset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            int s = size;
            ListIterator<E> i = it;
            while (nextIndex() < s) {
                action.accept(i.next());
            }
        }
    }

```

# **常用方法**

## **get**

获取指定索引位置的元素

```java
    @SuppressWarnings("unchecked")
    private E get(Object[] a, int index) {
        return (E) a[index];
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * 直接无锁访问数组下标获取数据
     */
    public E get(int index) {
        return get(getArray(), index);
    }
```

getArray方法用于获取当前的快照数组，get方法返回快照数组中指定索引位置的元素

## **add**

将指定元素添加到此列表的尾部

1获取锁，加锁后，获取当前数组，获取当前数组的长度

2根据当前数组赋值出一个长度为len+1的新数组newElements，此时newElements最后一个元素为null

3将元素e添加到newElements数组尾部，再设置当前Object[]为newElements，最终释放锁

```java
    /**
     * Appends the specified element to the end of this list.
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     *  向list中添加元素
     */
    public boolean add(E e) {
        // 获取公共锁
        final ReentrantLock lock = this.lock;
        // 加锁
        lock.lock();
        try {
            // 获取当前数组
            Object[] elements = getArray();
            // 求出数组长度
            int len = elements.length;
            // 扩容拷贝数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            // 将新数组的最后一个元素设置为e
            newElements[len] = e;
            // 设置数组
            setArray(newElements);
            return true;
        } finally {
            // 解锁
            lock.unlock();
        }
    }
```

## **addIfAbsent**

添加一个元素到列表中，但仅当列表中没有相同的元素存在时。如果列表中已经存在相同的元素，则此方法不会添加该元素并返回false



addIfAbsent(E e) 此方法首先获取当前列表的快照，然后检查该元素是否已经存在于列表中。如果不存在，则使用锁保护的addIfAbsent方法将该元素添加到列表中。

```java
    /**
     * Appends the element, if not present.
     *
     * @param e element to be added to this list, if absent
     * @return {@code true} if the element was added
     * 添加元素，如果数组中不存在，添加，存在，则不添加
     */
    public boolean addIfAbsent(E e) {
        //获取当前数组
        Object[] snapshot = getArray();
        return indexOf(e, snapshot, 0, snapshot.length) >= 0 ? false :
            addIfAbsent(e, snapshot);
    }
```

addIfAbsent(E e, Object[] snapshot)这个方法首先检查快照是否与当前列表相同，如果不同则必须再次检查该元素是否已经存在于列表中。最后，如果该元素不存在于列表中，则将其添加到列表中。如果该元素已经存在于列表中，则不会进行添加操作并返回false。

```java
    /**
     * A version of addIfAbsent using the strong hint that given
     * recent snapshot does not contain e.
     */
    private boolean addIfAbsent(E e, Object[] snapshot) {
        //重入锁
        final ReentrantLock lock = this.lock;
        //获取锁
        lock.lock();
        try {
            //获取当前数组
            Object[] current = getArray();
            //数组长度len
            int len = current.length;
            //快照不等于当前数组，对数组进行了修改
            if (snapshot != current) {
                // Optimize for lost race to another addXXX operation
                //二者取最小
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++)
                    //当前数组的元素与快照的元素不相等并且e与当前元素相等
                    if (current[i] != snapshot[i] && eq(e, current[i]))
                        //表示在snapshot与current之间修改了数组，并且设置了数组某一元素为e，已经存在
                        return false;
                if (indexOf(e, current, common, len) >= 0)
                    //在数组中找到了元素e
                        return false;
            }
            //赋值数组
            Object[] newElements = Arrays.copyOf(current, len + 1);
            //索引len的元素赋值为e
            newElements[len] = e;
            //将新数组设置为当前数组
            setArray(newElements);
            return true;
        } finally {
            //释放锁
            lock.unlock();
        }
    }
```

## **set**

替换指定位置的元素

```java
    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * 更新指定下标的元素
     */
    public E set(int index, E element) {
        // 获取公共锁
        final ReentrantLock lock = this.lock;
        // 加锁
        lock.lock();
        try {
            // 获取数组
            Object[] elements = getArray();
            // 获取数组插入位置的元素
            E oldValue = get(elements, index);
            // 当值不相等时进行更新
            if (oldValue != element) {
            //数组长度
                int len = elements.length;
                // 拷贝数组
                Object[] newElements = Arrays.copyOf(elements, len);
                //重新赋值
                newElements[index] = element;
                // 设置数组
                setArray(newElements);
            } else {
                // Not quite a no-op; ensures volatile write semantics
                // 当值相同时，设置数组
                setArray(elements);
            }
            // 返回原来的值
            return oldValue;
        } finally {
            // 解锁
            lock.unlock();
        }
    }

```

set方法的实现非常简单：先复制一份数组，然后在新数组上进行替换操作，最后通过CAS操作将原数组替换为新数组即可。由于CopyOnWriteArrayList是线程安全的，所以操作过程中不需要加锁。

## **remove**

删除指定下标的元素

1.获取锁，获取数组elements，数组长度为length，获取索引的值elements[index] (oldValue)，计算需要移动的元素个数（length - index - 1）,若个数为0，则表示移除的是数组的最后一个元素，复制elements数组，复制长度为length-1，然后设置数组进入步骤3；否则，进入步骤2。

2.先复制index索引前的元素，再复制index索引后的元素，然后设置数组。

3.释放锁，返回旧值。

```java
    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).  Returns the element that was removed from the list.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * 删除指定下标的元素
     */
    public E remove(int index) {
        // 获取公共锁
        final ReentrantLock lock = this.lock;
        // 加锁
        lock.lock();
        try {
            // 获取数组
            Object[] elements = getArray();
            // 获取数组长度
            int len = elements.length;
            // 获取当前下标的元素
            E oldValue = get(elements, index);
            // 计算移动的距离
            int numMoved = len - index - 1;
            if (numMoved == 0)
                // 不需要移动时，代表删除的末尾元素
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                // 实例化新的数组
                Object[] newElements = new Object[len - 1];
                // 先拷贝前一部分
                System.arraycopy(elements, 0, newElements, 0, index);
                // 再拷贝后一部分
                System.arraycopy(elements, index + 1, newElements, index,
                                 numMoved);
                // 赋值
                setArray(newElements);
            }
            // 返回删除的值
            return oldValue;
        } finally {
            // 解锁
            lock.unlock();
        }
    }
```

# 总结

## **优点**

读操作性能很高，因为无需任何同步措施，比较适用于读多写少的并发场景。

Java的list在遍历时，若中途有别的线程对list容器进行修改，则会抛ConcurrentModificationException异常。而CopyOnWriteArrayList由于其"读写分离"的思想，遍历和修改操作分别作用在不同的list容器，所以在使用迭代器进行遍历时候，也就不会抛出ConcurrentModificationException异常了

## **缺点**

内存占用问题，毕竟每次执行写操作都要将原容器拷贝一份，数据量大时，对内存压力较大，可能会引起频繁GC；

无法保证实时性，Vector对于读写操作均加锁同步，可以保证读和写的强一致性。而CopyOnWriteArrayList由于其实现策略的原因，写和读分别作用在新老不同容器上，在写操作执行过程中，读不会阻塞但读取到的却是老容器的数据。