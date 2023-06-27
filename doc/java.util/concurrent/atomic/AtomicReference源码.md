# **介绍**

AtomicReference和AtomicInteger非常类似，不同之处就在于AtomicInteger是对整数的封装，而AtomicReference则对应普通的对象引用。

AtomicReference是Java中的一个原子类，它提供了一个能够原子性地更新引用类型变量的操作。具体来说，AtomicReference的主要作用是提供了一个线程安全的引用变量，支持原子性的读写操作。它可以保证在多线程环境下变量的值能够正确地被更新和访问，避免了多线程操作引用类型变量时的一些常见问题，如竞态条件、死锁等。

AtomicReference类提供了一些方法用于原子性地更新引用类型变量，包括get()、set()、getAndSet()、compareAndSet()等。其中，compareAndSet()方法是最常见的方法之一，它可以基于当前值进行比较，并在比较成功的情况下更新该值。如果当前值与期望值不同，则该操作将不会进行更新操作，该方法返回false。

AtomicReference类通常用于解决CAS（比较并交换）的问题，也就是在CAS操作中需要传入一个共享变量的引用类型时，可以使用AtomicReference来保证变量在多线程环境下的线程安全性。

# **常量&变量**

```java
    //版本序列号
    private static final long serialVersionUID = -1848883965231344442L;
    //unsafe常量，设置为使用Unsafe.compareAndSwapInt进行更新
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    //AtomicReference的值在内存地址的偏移量
    private static final long valueOffset;
    //objectFieldOffset是一个本地方法，返回属性相对于对象的偏移量，这里使用反射获取属性。
    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                (AtomicReference.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    //AtomicReference当前值
    private volatile V value;
```

## **构造方法**

```java
    /**
     * Creates a new AtomicReference with the given initial value.
     *
     * @param initialValue the initial value
     *创建一个AtomicReference对象，初始值为指定的initialValue。
     */
    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    /**
     * Creates a new AtomicReference with null initial value.
     *创建一个AtomicReference对象，初始值为null
     */
    public AtomicReference() {
    }

```

# **常用方法**

1. 大部分的方法都是final的。
2. AtomicReference与AtomicInteger等不一样的是其是利用泛型来对数据进行操作而不是确定的数据类型。
3. AtomicReference中的内部方法的实现都是利用他的Unsafe变量来进行操作的。

## get

返回AtomicReference对象中保存的引用值，如果对象为空，则返回null

```java
    /**
     * Gets the current value.
     *
     * @return the current value
     */
    public final V get() {
        return value;
    }
```

## set

设置`AtomicReference`对象中的引用值

```java

    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    public final void set(V newValue) {
        value = newValue;
    }
```

## lazySet

`lazySet`方法和`set`方法类似，也用于设置`AtomicReference`对象中的引用值，但是其操作是“懒惰”的，即可能会被延迟执行，不保证立即可见性。不适用于需要立即对其他线程可见的场景。

```java
    /**
     * Eventually sets to the given value.
     *
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(V newValue) {
        unsafe.putOrderedObject(this, valueOffset, newValue);
    }
```

## compareAndSet

用于比较并设置对象引用的值，如果当前对象引用的值等于给定的预期值，就将其更新为新的值。

```java
    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }
```

## weakCompareAndSet

`weakCompareAndSet`方法与`compareAndSet`方法类似，也是用于比较并设置对象引用的值。但是，`weakCompareAndSet`方法使用的是“弱比较并设置”的方式，也就是如果当前对象引用的值等于给定的预期值，它只能保证进行一次更新尝试，而无法保证更新一定成功。

`weakCompareAndSet`方法并不总是比`compareAndSet`方法性能更好，因为它采用的是“弱比较并设置”的方式，因此更新成功的概率更低，可能需要进行多次更新尝试。所以，在使用这两个方法时需要根据具体情况进行选择。

```java
    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     */
    public final boolean weakCompareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }
```

## getAndSet

用于先获取当前对象引用的值，然后将其设置为新的值，相当于原子操作的"先获取再设置"操作。

```java
    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    @SuppressWarnings("unchecked")
    public final V getAndSet(V newValue) {
        return (V)unsafe.getAndSetObject(this, valueOffset, newValue);
    }
```

## getAndUpdate

先获取当前对象引用的值，然后通过给定的函数更新该值，并返回更新前的值，相当于原子操作的"先获取再更新"操作。

```java
    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get();
            next = updateFunction.apply(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }
```

## updateAndGet

用于先通过给定的函数更新当前对象引用的值，然后返回更新后的值，相当于原子操作的"先更新再获取"操作。

```java
    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get();
            next = updateFunction.apply(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }
```

## getAndAccumulate

用于先获取当前对象引用的值，然后将其与给定参数使用给定函数进行计算，最后返回计算结果，相当于原子操作的"先获取再计算"操作。

```java
    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final V getAndAccumulate(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get();
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }
```

## accumulateAndGet

用于先将当前对象引用的值与给定参数使用给定函数进行计算，然后返回计算结果，相当于原子操作的"先计算再获取"操作。

```java
    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final V accumulateAndGet(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get();
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }
```



