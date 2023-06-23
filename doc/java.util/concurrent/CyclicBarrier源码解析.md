# 介绍

CyclicBarrier ，一个同步辅助类，在 AP I中是这么介绍的：
它允许一组线程互相等待，直到到达某个公共屏障点 (Common Barrier Point)。在涉及一组固定大小的线程的程序中，这些线程必须不时地互相等待，此时 CyclicBarrier 很有用。因为该 Barrier 在释放等待线程后可以重用，所以称它为循环( Cyclic ) 的 屏障( Barrier ) 。
通俗点讲就是：让一组线程到达一个屏障时被阻塞，直到最后一个线程到达屏障时，屏障才会开门，所有被屏障拦截的线程才会继续干活。
`CyclicBarrier`栅栏，与`CountDownLatch`类似，但不是基于AQS实现的同步器，用于多个线程之间等待。`CyclicBarrier`每次使用完之后可以重置，`CountDownLatch`不可重置，`CyclicBarrier`同步一组线程，

![img](https://img-blog.csdnimg.cn/img_convert/b5e6a0619408eba25504034aee00232f.gif)

在CyclicBarrier类的内部有一个计数器，每个线程在到达屏障点的时候都会调用await方法将自己阻塞，此时计数器会减1，当计数器减为0的时候所有因调用await方法而被阻塞的线程将被唤醒。这就是实现一组线程相互等待的原理，

# 常量&变量

```java
    /** The lock for guarding barrier entry */
    //同步操作锁
    private final ReentrantLock lock = new ReentrantLock();
    /** Condition to wait on until tripped */
    //线程拦截器
    private final Condition trip = lock.newCondition();
    /** The number of parties */
    //每次拦截的线程数
    private final int parties;
    /* The command to run when tripped */
    //换代前执行的任务
    private final Runnable barrierCommand;
    /** The current generation */
    //表示栅栏的当前代 CyclicBarrier 的更新换代
    private Generation generation = new Generation();


```

可以看到CyclicBarrier内部是通过条件队列trip来对线程进行阻塞的，并且其内部维护了两个int型的变量parties和count，parties表示每次拦截的线程数，该值在构造时进行赋值。count是内部计数器，它的初始值和parties相同，以后随着每次await方法的调用而减1，直到减为0就将所有线程唤醒

barrierCommand表示换代前执行的任务，当count减为0时表示本局游戏结束，需要转到下一局。在转到下一局游戏之前会将所有阻塞的线程唤醒，在唤醒所有线程之前你可以通过指定barrierCommand来执行自己的任务。
# 构造方法
它有两个构造函数：
CyclicBarrier(int parties)：创建一个新的 CyclicBarrier，它将在给定数量的参与者（线程）处于等待状态时启动，但它不会在启动 barrier 时执行预定义的操作。
CyclicBarrier(int parties, Runnable barrierAction)：创建一个新的 CyclicBarrier，它将在给定数量的参与者（线程）处于等待状态时启动，并在启动 barrier 时执行给定的屏障操作，该操作由最后一个进入 barrier 的线程执行。

```java
    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and which
     * will execute the given barrier action when the barrier is tripped,
     * performed by the last thread entering the barrier.
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @param barrierAction the command to execute when the barrier is
     *        tripped, or {@code null} if there is no action
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        //拦截线程的总数量
        this.parties = parties;
        //拦截线程的剩余需要数量
        this.count = parties;
        //barrierAction ：为 CyclicBarrier 接收的 Runnable 命令，用于在线程到达屏障时，优先执行 barrierAction ，用于处理更加复杂的业务场景。
        this.barrierCommand = barrierAction;
    }

    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and
     * does not perform a predefined action when the barrier is tripped.
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties) {
        this(parties, null);
    }
```

# 内部类Generation

```java
    /**
     * Each use of the barrier is represented as a generation instance.
     * The generation changes whenever the barrier is tripped, or
     * is reset. There can be many generations associated with threads
     * using the barrier - due to the non-deterministic way the lock
     * may be allocated to waiting threads - but only one of these
     * can be active at a time (the one to which {@code count} applies)
     * and all the rest are either broken or tripped.
     * There need not be an active generation if there has been a break
     * but no subsequent reset.

     */
    private static class Generation {
        // broker = true意味着所有线程已经全部到达，可重置`CyclicBarrier`
        boolean broken = false;
    }
```

Generation 是 CyclicBarrier 内部静态类，描述了 CyclicBarrier 的更新换代。在CyclicBarrier中，同一批线程属于同一代。当有 parties 个线程全部到达 barrier 时，generation 就会被更新换代。其中 broken 属性，标识该当前 CyclicBarrier 是否已经处于中断状态。


# 常用方法

## wait()
### 阻塞等待
在 CyclicBarrier 中最重要的方法莫过于 await() 方法，在所有参与者都已经在此 barrier 上调用 await 方法之前，将一直等待。
或者说，每个线程调用 await() 方法，告诉 CyclicBarrier 我已经到达了屏障，然后当前线程被阻塞。当所有线程都到达了屏障，结束阻塞，所有线程可继续执行后续逻辑。
```java
    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while waiting,
     * then all other waiting threads will throw
     * {@link BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     *        立即阻塞
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            //理论上，不会出现TimeoutException异常，如果出现，直接抛出error
            throw new Error(toe); // cannot happen
        }
    }
```



### 等待超时

```java
    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier, or the specified waiting time elapses.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>The specified timeout elapses; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link TimeoutException}
     * is thrown. If the time is less than or equal to zero, the
     * method will not wait at all.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while
     * waiting, then all other waiting threads will throw {@link
     * BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @param timeout the time to wait for the barrier
     * @param unit the time unit of the timeout parameter
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws TimeoutException if the specified timeout elapses.
     *         In this case the barrier will be broken.
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was broken
     *         when {@code await} was called, or the barrier action (if
     *         present) failed due to an exception
     *         超时指定时间后阻塞
     */
    public int await(long timeout, TimeUnit unit)
        throws InterruptedException,
               BrokenBarrierException,
               TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }
```



CyclicBarrier类最主要的功能就是使先到达屏障点的线程阻塞并等待后面的线程，其中它提供了两种等待的方法，分别是定时等待和非定时等待。可以看到不管是定时等待还是非定时等待，它们都调用了dowait方法，只不过是传入的参数不同而已。下面我们就来看看dowait方法都做了些什么。

## dowait()

```java
    /**
     * Main barrier code, covering the various policies.
     * 核心等待方法
     */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
        //获取锁
        lock.lock();
        try {
            //分代
            final Generation g = generation;
            //当前generation“已损坏”，抛出BrokenBarrierException异常
            //抛出该异常一般都是某个线程在等待某个处于“断开”状态的CyclicBarrie
            if (g.broken)
                //当某个线程试图等待处于断开状态的 barrier 时，或者 barrier 进入断开状态而线程处于等待状态时，抛出该异常
                throw new BrokenBarrierException();
            //检查当前线程是否被中断
            if (Thread.interrupted()) {
                //如果当前线程被中断会做以下三件事
                //1.打翻当前栅栏
                //2.唤醒拦截的所有线程
                //3.抛出中断异常
                breakBarrier();
                throw new InterruptedException();
            }
            //进来一个线程，都将计数器的值减1
            int index = --count;
            //计数器的值减为0，表示所有线程均已到位，则  触发Runnable任务 唤醒所有线程并转换到下一代
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    //唤醒所有线程前先执行指定的任务
                    final Runnable command = barrierCommand;
                    //执行任务
                    if (command != null)
                        command.run();
                    ranAction = true;
                    //唤醒所有线程并更新generation转到下一代
                    nextGeneration();
                    return 0;
                } finally {
                    //未执行，说明 barrierCommand 执行报错，或者线程打断等等情况。确保在任务未成功执行时能将所有线程唤醒
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // loop until tripped, broken, interrupted, or timed out
            //如果计数器不为0则执行此循环
            for (;;) {
                try {
                    //根据传入的参数来决定是定时等待还是非定时等待
                    if (!timed)
                        //如果不是超时等待，则调用Condition.await()方法等待
                        trip.await();
                    else if (nanos > 0L)
                        //超时等待，调用Condition.awaitNanos()方法等待
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    //若当前线程在等待期间被中断则打翻栅栏唤醒其他线程
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        //若在捕获中断异常前已经完成在栅栏上的等待, 则直接调用中断操作
                        Thread.currentThread().interrupt();
                    }
                }
                //如果线程因为打翻栅栏操作而被唤醒则抛出异常
                if (g.broken)
                    throw new BrokenBarrierException();
                //如果线程因为换代操作而被唤醒则返回计数器的值
                if (g != generation)
                    return index;
                //如果线程因为时间到了而被唤醒则打翻栅栏（终止CyclicBarrier）并抛出异常
                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            //释放锁
            lock.unlock();
        }
    }
```

可以看到在dowait方法中每次都将count减1，减完后立马进行判断看看是否等于0，如果等于0的话就会先去执行之前指定好的任务，执行完之后再调用nextGeneration方法将栅栏转到下一代，在该方法中会将所有线程唤醒，将计数器的值重新设为parties，最后会重新设置栅栏代次，在执行完nextGeneration方法之后就意味着游戏进入下一局。

如果计数器此时还不等于0的话就进入for循环，根据参数来决定是调用trip.awaitNanos(nanos)还是trip.await()方法，这两方法对应着定时和非定时等待。

如果在等待过程中当前线程被中断就会执行breakBarrier方法，该方法叫做打破栅栏，意味着游戏在中途被掐断，设置generation的broken状态为true并唤醒所有线程。同时这也说明在等待过程中有一个线程被中断整盘游戏就结束，所有之前被阻塞的线程都会被唤醒。线程醒来后会执行下面三个判断，看看是否因为调用breakBarrier方法而被唤醒，如果是则抛出异常；看看是否是正常的换代操作而被唤醒，如果是则返回计数器的值；看看是否因为超时而被唤醒，如果是的话就调用breakBarrier打破栅栏并抛出异常。

这里还需要注意的是，如果其中有一个线程因为等待超时而退出，那么整盘游戏也会结束，其他线程都会被唤醒。



下面贴出nextGeneration方法和breakBarrier方法的具体代码。

###  nextGeneration()
当所有线程都已经到达 barrier 处（index == 0），则会通过 nextGeneration() 方法，进行更新换代操作。在这个步骤中，做了三件事：
1.唤醒所有线程。
2.重置 count 。
3.重置 generation 。
```java
    /**
     * Updates state on barrier trip and wakes up everyone.
     * Called only while holding lock.
     * 切换栅栏到下一代
     */
    private void nextGeneration() {
        // signal completion of last generation
        //唤醒条件队列所有线程
        trip.signalAll();
        // set up next generation
        //设置计数器的值为需要拦截的线程数
        count = parties;
        //重新设置栅栏代次
        generation = new Generation();
    }
```

### breakBarrier()
当 barrier 损坏了，或者有一个线程中断了，则通过 breakBarrier() 方法，来终止所有的线程

```java
    /**
     * Sets current barrier generation as broken and wakes up everyone.
     * Called only while holding lock.
     * 打翻当前栅栏
     */
    private void breakBarrier() {
        //将当前栅栏状态设置为打翻
        generation.broken = true;
        //设置计数器的值为需要拦截的线程数
        count = parties;
        //唤醒所有线程
        trip.signalAll();
    }
```
在 breakBarrier() 方法中，中除了将 broken设置为 true ，还会调用 signalAll() 方法，将在 CyclicBarrier 处于等待状态的线程全部唤醒
## 重置
重置 barrier 到初始化状态

```java
    /**
     * Resets the barrier to its initial state.  If any parties are
     * currently waiting at the barrier, they will return with a
     * {@link BrokenBarrierException}. Note that resets <em>after</em>
     * a breakage has occurred for other reasons can be complicated to
     * carry out; threads need to re-synchronize in some other way,
     * and choose one to perform the reset.  It may be preferable to
     * instead create a new barrier for subsequent use.
     * 重置栅栏
     * 1、打破栅栏
     * 2、生成新一代
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }
```
通过组合 breakBarrier() 和 nextGeneration() 方法来实现


## 已到达等待线程数

```java
    /**
     * Returns the number of parties currently waiting at the barrier.
     * This method is primarily useful for debugging and assertions.
     *
     * @return the number of parties currently blocked in {@link #await}
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            //返回阻塞的线程数
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
```

## isBroken
判断 CyclicBarrier 是否处于中断

```java
public boolean isBroken() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return generation.broken;
    } finally {
        lock.unlock();
    }
}
```
# 总结
CyclicBarrier 适用于多线程结果合并的操作，用于多线程计算数据，最后合并计算结果的应用场景。


