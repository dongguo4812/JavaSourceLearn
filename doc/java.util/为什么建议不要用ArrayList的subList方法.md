# 源码

subList 是 List 接口中定义的一个方法，该方法主要用于返回一个集合中的一段、可以理解为截取一个集合中的部分元素，他的返回值也是一个 List。

可以看到 SubList 和 ArrayList 的继承体系非常类似，都实现了 RandomAccess 接口 继承自 AbstarctList。

但是SubList 和 ArrayList 并没有继承关系，因此 ArrayList 的 SubList 并不能强转为 ArrayList 。

![image-20230604085531106](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040855697.png)



```java
    /**
     * Returns a view of the portion of this list between the specified
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
     * {@code fromIndex} and {@code toIndex} are equal, the returned list is
     * empty.)  The returned list is backed by this list, so non-structural
     * changes in the returned list are reflected in this list, and vice-versa.
     * The returned list supports all of the optional list operations.
     *
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * Similar idioms may be constructed for {@link #indexOf(Object)} and
     * {@link #lastIndexOf(Object)}, and all of the algorithms in the
     * {@link Collections} class can be applied to a subList.
     *
     * <p>The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned list.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * 取子list,返回Sublist这个ArrayList的内部类,
     * 这是个坑，注意SubList和其他List实现类的区别
     */
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList(this, 0, fromIndex, toIndex);
    }
```

`ArrayList.SubList`

```java
    private class SubList extends AbstractList<E> implements RandomAccess {
        // 父类的引用
        private final AbstractList<E> parent;
        /*
         * 父类集合中的位置，如果使用SubList中的subList方法，
         * 则此时父类为SubList类，不是ArrayList
         */
        private final int parentOffset;
        // 子类List在父类 ArrayList 中的下标位置
        private final int offset;
        // 视图集合的size
        int size;

        /**
         * 构造方法，参数offset表示父类集合的下标偏移量
         * @param parent
         * @param offset
         * @param fromIndex
         * @param toIndex
         */
        SubList(AbstractList<E> parent,
                int offset, int fromIndex, int toIndex) {
            this.parent = parent;
            this.parentOffset = fromIndex;
            this.offset = offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = ArrayList.this.modCount;
        }

        public E set(int index, E e) {
            // 检查下标是否越界
            rangeCheck(index);
            // 检查是否有其他线程修改了父类集合
            checkForComodification();
            //获取该索引的旧值
            E oldValue = ArrayList.this.elementData(offset + index);
            // 调用父类方法替换元素，所以本质上还是在父类集合中替换元素
            ArrayList.this.elementData[offset + index] = e;
            return oldValue;
        }

        public E get(int index) {
            rangeCheck(index);
            checkForComodification();
            // 调用父类方法获取元素
            return ArrayList.this.elementData(offset + index);
        }

        public int size() {
            checkForComodification();
            return this.size;
        }

        public void add(int index, E e) {
            rangeCheckForAdd(index);
            checkForComodification();
            // 使用父类方法添加元素，
            parent.add(parentOffset + index, e);
            // 父类add()方法修改了modCount的值，更新subList的modCount值
            this.modCount = parent.modCount;
            this.size++;
        }

        /**
         * 根据下标移除元素
         * @param index the index of the element to be removed
         * @return
         */
        public E remove(int index) {
            rangeCheck(index);
            checkForComodification();
            E result = parent.remove(parentOffset + index);
            this.modCount = parent.modCount;
            this.size--;
            return result;
        }

        /**
         * 移除指定区间的元素
         * @param fromIndex index of first element to be removed
         * @param toIndex index after last element to be removed
         */
        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            parent.removeRange(parentOffset + fromIndex,
                               parentOffset + toIndex);
            this.modCount = parent.modCount;
            this.size -= toIndex - fromIndex;
        }

        public boolean addAll(Collection<? extends E> c) {
            return addAll(this.size, c);
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if (cSize==0)
                return false;

            checkForComodification();
            parent.addAll(parentOffset + index, c);
            this.modCount = parent.modCount;
            this.size += cSize;
            return true;
        }

        /**
         *  subList 中迭代器使用ListIterator()
         * @return
         */
        public Iterator<E> iterator() {
            return listIterator();
        }

        public ListIterator<E> listIterator(final int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            final int offset = this.offset;

            return new ListIterator<E>() {
                int cursor = index;
                int lastRet = -1;
                int expectedModCount = ArrayList.this.modCount;

                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    checkForComodification();
                    int i = cursor;
                    if (i >= SubList.this.size)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i + 1;
                    return (E) elementData[offset + (lastRet = i)];
                }

                public boolean hasPrevious() {
                    return cursor != 0;
                }

                @SuppressWarnings("unchecked")
                public E previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if (i < 0)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i;
                    return (E) elementData[offset + (lastRet = i)];
                }

                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super E> consumer) {
                    Objects.requireNonNull(consumer);
                    final int size = SubList.this.size;
                    int i = cursor;
                    if (i >= size) {
                        return;
                    }
                    final Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    while (i != size && modCount == expectedModCount) {
                        consumer.accept((E) elementData[offset + (i++)]);
                    }
                    // update once at end of iteration to reduce heap write traffic
                    lastRet = cursor = i;
                    checkForComodification();
                }

                public int nextIndex() {
                    return cursor;
                }

                public int previousIndex() {
                    return cursor - 1;
                }

                public void remove() {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void set(E e) {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        ArrayList.this.set(offset + lastRet, e);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void add(E e) {
                    checkForComodification();

                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                final void checkForComodification() {
                    if (expectedModCount != ArrayList.this.modCount)
                        throw new ConcurrentModificationException();
                }
            };
        }

        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList(this, offset, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+this.size;
        }

        /**
         * 检查是否有多线程修改集合
         */
        private void checkForComodification() {
            if (ArrayList.this.modCount != this.modCount)
                throw new ConcurrentModificationException();
        }

        public Spliterator<E> spliterator() {
            checkForComodification();
            return new ArrayListSpliterator<E>(ArrayList.this, offset,
                                               offset + this.size, this.modCount);
        }
    }
```



ArrayList.subList()这个方法返回了一个 SubList，这个类是 ArrayList 中的一个内部类。SubList 这个类中单独定义了 set、get、size、add、remove 等方法。当我们调用 subList 方法的时候，会通过调用 SubList 的构造函数创建一个SubList，

SubList 并没有重新创建一个 List，而是直接引用了原有的 List（返回了父类的视图），只是指定了一下他要使用的元素的范围而已（从 fromIndex（包含），到 toIndex（不包含））

```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        List subList = names.subList(0, 1);
        System.out.println(subList);
    }
```

![image-20230604083454786](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040834721.png)

# ArrayList的subList结果不可强转成ArrayList

阿里巴巴 Java 开发手册:

![image-20230604082622808](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040826121.png)

![image-20230604083507397](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040835976.png)

如果我们想将List强转为ArrayList时，就会报ClassCastException

```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
//        List subList = names.subList(0, 1);
        ArrayList subList = (ArrayList) names.subList(0, 1);
        System.out.println(subList);
    }
```

![image-20230604083538139](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040835183.png)

# 对集合的操作

## 对集合的修改set

当我们尝试通过 set ()方法，改变 子集合中某个元素的值得时候，我们发现，原集合 中对应元素的值也发生了改变。

```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        List subList = names.subList(0, 1);
        System.out.println(subList);
        subList.set(0,"zhaoliu");
        System.out.println(names);
        System.out.println(subList);
    }
```

![image-20230604084617717](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040846001.png)

同理，如果我们使用同样的方法，对 子集合中的某个元素进行修改，那么原集合中对应的值也会发生改变。

```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        List subList = names.subList(0, 1);
        System.out.println(subList);
        names.set(0,"zhaoliu");
        System.out.println(names);
        System.out.println(subList);
    }
```

![image-20230604084729579](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040847798.png)

## 对集合的修改add/delete

我们尝试对子集合的结构进行改变，比如向其追加元素，那么得到的结果是原集合的结构也同样发生了改变

```java
@Test
public void testSubList(){
    List<String> names = new ArrayList<String>() {{
        add("zhangsan");
        add("lisi");
        add("wangwu");
    }};
    List subList = names.subList(0, 1);
    System.out.println(subList);
    subList.add("zhaoliu");
    System.out.println(names);
    System.out.println(subList);
}
```

![image-20230604084938144](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040849132.png)



当我们尝试对原集合的结构进行改变，比如向其追加元素，结果发现抛出了ConcurrentModificationException。

```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        List subList = names.subList(0, 1);
        System.out.println(subList);
        names.add("zhaoliu");
        System.out.println(names);
        System.out.println(subList);
    }
```

![image-20230604084434495](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040844970.png)

阿里巴巴 Java 开发手册:

![image-20230604083609484](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040836842.png)



# ConcurrentModificationException的原因

通过debug我们会看到，修改原集合后，再输出子集合时，会调用ArrayList的listIterator()方法

![image-20230604085016288](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040850236.png)

listIterator()会调用checkForComodification()检查集合的修改次数modCount 

![image-20230604085029006](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040850748.png)

可以看到在创建subList的时 this.modCount = ArrayList.this.modCount;

而此时原集合添加了元素，修改了modCount

![image-20230604085046684](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040850704.png)

ArrayList.this.modCount != this.modCount 抛出ConcurrentModificationException

![image-20230604085106514](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040851466.png)

modCount表示列表被修改的次数，一般用于在并发场景下，实现fail-fast机制，



而对子集合的修改操作不会修改modCount的值，所以不会报ConcurrentModificationException

![image-20230604085128983](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040851924.png)

# **总结**

List 的 subList 方法并没有创建一个新的 List，而是使用了原 List 的视图，这个视图使用内部类 SubList 表示。所以，我们不能把 subList 方法返回的 List 强制转换成 ArrayList 等类，因为他们之间没有继承关系。

另外，视图和原 List 的修改还需要注意几点，尤其是他们之间的相互影响：

1. 父 (names) 子 (subList)List 做 的 非 结 构 性 修 改（non-structuralchanges），都会影响到彼此。
2. 对子 List 做结构性修改，操作同样会反映到父 List 上。
3. 对父 List 做结构性修改，会抛出异常 ConcurrentModificationException。



如果你的list只是用来遍历，那么可以用ArrayList.subList()获得，但是如果你的list还想用添加、删除方法，那么就要放弃Arrays.asList(),可以创建 subList 的一个拷贝

对于 Java 8 使用 Stream 的 skip 和 limit API 来跳过流中的元素，以及限制 流中元素的个数，同样可以达到 SubList 切片的目的。

```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        List<String> subList = names.stream().skip(0).limit(1).collect(Collectors.toList());
        System.out.println(subList);

        names.add("zhaoliu");
        System.out.println(names);
        System.out.println(subList);
    }
```

![image-20230604090952767](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040909675.png)

不直接使用 subList 方法返回的 SubList，而是重新使用 new ArrayList，在构 造方法传入 SubList，来构建一个独立的 ArrayList；

```java
List<String> subList = new ArrayList<>(list.subList(0,1));
```



```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        List<String> subList = new ArrayList<>(names.subList(0,1));
        System.out.println(subList);

        names.add("zhaoliu");
        System.out.println(names);
        System.out.println(subList);
    }
```

![image-20230604091044313](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040910349.png)



当然还有一种方法，直接使用原集合接收

```java
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        names = names.subList(0,1);
        System.out.println(names);
        names.add("zhaoliu");
        System.out.println(names);
    }
```

可以看到，names的类型从ArrayList变为ArrayList.SubList

![image-20230604085424072](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040854176.png)



![image-20230604085433410](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040854539.png)



![image-20230604091322436](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306040913623.png)