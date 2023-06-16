# **介绍**

AbstractList是AbstractCollection和List的抽象子类，为一些通用的方法提供实现

```java
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> 
```

![image-20230617035004505](F:\note\image\image-20230617035004505.png)

这个抽象类提供了List接口的大多数实现，ArrayList和LinkedList只需要继承这个抽象类并重写抽象方法即可。

# **常量&变量**

```java
    /**
     * The number of times this list has been <i>structurally modified</i>.
     * Structural modifications are those that change the size of the
     * list, or otherwise perturb it in such a fashion that iterations in
     * progress may yield incorrect results.
     *
     * <p>This field is used by the iterator and list iterator implementation
     * returned by the {@code iterator} and {@code listIterator} methods.
     * If the value of this field changes unexpectedly, the iterator (or list
     * iterator) will throw a {@code ConcurrentModificationException} in
     * response to the {@code next}, {@code remove}, {@code previous},
     * {@code set} or {@code add} operations.  This provides
     * <i>fail-fast</i> behavior, rather than non-deterministic behavior in
     * the face of concurrent modification during iteration.
     *
     * <p><b>Use of this field by subclasses is optional.</b> If a subclass
     * wishes to provide fail-fast iterators (and list iterators), then it
     * merely has to increment this field in its {@code add(int, E)} and
     * {@code remove(int)} methods (and any other methods that it overrides
     * that result in structural modifications to the list).  A single call to
     * {@code add(int, E)} or {@code remove(int)} must add no more than
     * one to this field, or the iterators (and list iterators) will throw
     * bogus {@code ConcurrentModificationExceptions}.  If an implementation
     * does not wish to provide fail-fast iterators, this field may be
     * ignored.
     * 操作次数
     */
    protected transient int modCount = 0;
```

# **构造方法**

非公开的无参构造，提供给其子类使用

```java
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractList() {
    }

```

# **内部类**

AbstractList 内部已经提供了 Iterator, ListIterator 迭代器的实现类，分别为 Itr, ListItr

## **Itr**

实现了Iterator接口的迭代器Itr

```java
    private class Itr implements Iterator<E> {
        /**
         * Index of element to be returned by subsequent call to next.
         * 游标 下一次调用next返回的元素索引
         */
        int cursor = 0;

        /**
         * Index of element returned by most recent call to next or
         * previous.  Reset to -1 if this element is deleted by a call
         * to remove.
         * 最近一次调用next或者previous返回的元素索引
         * 调用remove删除元素，会被置为-1，避免连续删除
         */
        int lastRet = -1;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         * 期望修改次数 = 实际修改次数
         * 如果迭代器在使用过程中，检测到这个值（expectedModCount）和list的值(modCount)不一样，则发生并发修改异常
         */
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size();
        }

        public E next() {
            checkForComodification();
            try {
                //下一次next返回索引，迭代前先取出
                int i = cursor;
                //获得下标的元素
                E next = get(i);
                //调用next返回的元素索引i
                lastRet = i;
                //下次迭代的游标
                cursor = i + 1;
                return next;
            } catch (IndexOutOfBoundsException e) {
                //get函数可能抛出IndexOutOfBoundsException
                //可能因为出现并发修改异常导致数组下标越界
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            //再次删除的情况，上一次执行了迭代器的删除后，还没有进行元素的移动，这种情况不允许再次删除，为-1
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                AbstractList.this.remove(lastRet);
                if (lastRet < cursor)
                    cursor--;
                //删除后，为-1 
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
```

## **ListItr**

实现了ListIterator并且继承自Itr的迭代器ListItr

```java
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            cursor = index;
        }
        //只要游标不为0，都可以往前迭代
        public boolean hasPrevious() {
            return cursor != 0;
        }

        public E previous() {
            checkForComodification();
            try {
                //往前迭代
                int i = cursor - 1;
                E previous = get(i);
                //更新下标
                lastRet = cursor = i;
                return previous;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        public void set(E e) {
            //说明执行了remove或add后，还没有进行过next或previous操作
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                AbstractList.this.set(lastRet, e);
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                AbstractList.this.add(i, e);
                lastRet = -1;
                cursor = i + 1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }
```

