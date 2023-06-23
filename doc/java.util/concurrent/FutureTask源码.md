# **介绍**

在创建线程的方式中，我们可以直接继承Thread和实现Callable接口来创建线程，但是这两种创建线程的方式不能返回执行的结果。于是从JDK1.5开始提供了Callable接口和Future接口，这两种创建线程的方式可以在执行完任务之后返回执行结果。

Future模式可以让线程异步获得执行的结果，而不用等到线程等到run()方法里面执行完再执行之后的逻辑。

```java
public class FutureTask<V> implements RunnableFuture<V>
```

![image-20230622200425251](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306222004378.png)

我们看到FutureTask实现了RunnableFuture接口，其中RunnableFuture接口继承了Runnable和Future接口，因此我们可以说FutureTask实现了Future接口和Runnable接口，但是我们需要注意的是，FutureTask也实现了Callable接口，虽然这并没有在继承体系中看出，但是可以从FutureTask的源码中看出。



FutureTask用于异步执行或取消任务的场景。通过传入Runnable的实现类或Callable的实现类给FutureTask对象，

之后将FutureTask对象传给Thread对象或线程池。可以通过FutureTask对象.get()方法可以异步返回执行结果，

如果任务还没有执行完，则调用get()方法的线程会陷入阻塞，直到任务执行完。无论有多少个线程调用get()方法，

FutureTask中的run()逻辑只会执行一次。我们还可以调用FutureTask对象的cancel()方法取消掉当前任务。

# **常量&变量**

```java
    /**
     * The run state of this task, initially NEW.  The run state
     * transitions to a terminal state only in methods set,
     * setException, and cancel.  During completion, state may take on
     * transient values of COMPLETING (while outcome is being set) or
     * INTERRUPTING (only while interrupting the runner to satisfy a
     * cancel(true)). Transitions from these intermediate to final
     * states use cheaper ordered/lazy writes because values are unique
     * and cannot be further modified.
     *
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    //表示当前任务的状态
    private volatile int state;
    //表示新创建状态，任务尚未执行。
    private static final int NEW          = 0;
    //表示当前任务即将结束，但是还未完全结束，返回值还未写入，处于一种临界状态。
    private static final int COMPLETING   = 1;
    //表示当前任务处于正常结束状态（没有发生异常，中断，取消）。
    private static final int NORMAL       = 2;
    // 表示当前任务执行过程中出现了异常，处于非正常结束状态。内部封装的callable.call()向上抛出异常了
    private static final int EXCEPTIONAL  = 3;
    //表示当前任务因调用cancel而处于被取消状态。
    private static final int CANCELLED    = 4;
    //表示当前任务处于中断中，但是还没有完全中断的阶段。
    private static final int INTERRUPTING = 5;
    //表示当前任务处于已完全中断的阶段。
    private static final int INTERRUPTED  = 6;

    /** The underlying callable; nulled out after running */
    // 我们在使用FutureTask对象的时候，会传入一个Callable实现类或Runnable实现类，这个callable存储的就是
    // 传入的Callable实现类或Runnable实现类（Runnable会被使用修饰者设计模式伪装为）callable
    //submit(callable/runnable):其中runnable使用了装饰者设计模式伪装成了callable
    private Callable<V> callable;
    /** The result to return or exception to throw from get() */
    // 正常情况下，outcome保存的是任务的返回结果
    // 不正常情况下，outcome保存的是任务抛出的异常
    private Object outcome; // non-volatile, protected by state reads/writes
    /** The thread running the callable; CASed during run() */
    // 保存的是当前任务执行期间，执行任务的线程的引用
    private volatile Thread runner;
    /** Treiber stack of waiting threads */
    // 因为会有很多线程去get结果，这里把线程封装成WaitNode，一种数据结构：栈，头插头取
    private volatile WaitNode waiters;
    
    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    static final class WaitNode {
        // 线程对象
        volatile Thread thread;
        // 下一个WaitNode结点
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }
```

# **构造方法**

```java
    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     *
     * @param  callable the callable task
     * @throws NullPointerException if the callable is null
     * 传入一个callable，调用get返回的结果就是callable返回的结果
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        // 设置状态为新创建
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and arrange that {@code get} will return the
     * given result on successful completion.
     *
     * @param runnable the runnable task
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     * 传入一个runnable，一个result变量，调用get方法返回的结果就是传入的result变量
     */
    public FutureTask(Runnable runnable, V result) {
        // 装饰者模式，将runnable转化为callable接口
        this.callable = Executors.callable(runnable, result);
        // 设置状态为新创建
        this.state = NEW;       // ensure visibility of callable
    }
```

# **内部类**

## **WaitNode**

WaitNode如下所示以内部类的形式存在于FutureTask中，主要是为了优化get方法阻塞主线程的问题，如果没有WaitNode，那么主线程将会一直轮询重试，如果子任务执行时间较长或者获取get结果的主线程数量过多，肯定会大量占用cpu资源，因此引入WaitNode作为所有等待结果的线程链表或队列。

一旦有线程执行get方法获取结果，并且任务并未完成，就需要新建WaitNode节点添加到等待队列中；后续的任务完成，会依次通知等待队列中的所有线程。WaitNode的数据结构也很简单，一个是当前线程对象，一个是等待链表的next指针。

```java
    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    static final class WaitNode {
        // 线程对象
        volatile Thread thread;
        // 下一个WaitNode结点
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }
```

# **run()方法及与其相关的方法**

## **run()**

调用**Callable对象**的**run()方法任务的具体逻辑**和一些关于**任务状态**、**返回结果**的逻辑。

```java
    public void run() {
        // 当前任务状态不为new或者runner旧值不为null，说明已经启动过了，直接返回，
        // 这里也说明了run()里面的具体逻辑只会被执行一次。
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        // 只有当任务状态为new并且runner旧值为null才会执行到这里
        try {
            // 传入的callable任务
            Callable<V> c = callable;
            // 当任务不为null并且当前任务状态为新建时才会往下执行
            // 条件1：防止空指针异常
            // 条件2：防止外部线程cacle掉当前任务
            if (c != null && state == NEW) {
                // 储存任务的返回结果
                V result;
                // 储存执行是否成功
                boolean ran;
                try {
                    // 调用callable.run()并返回结果
                    result = c.call();
                    // 正常执行设置ran为true
                    ran = true;
                } catch (Throwable ex) {
                    // callable的run()方法抛出了异常
                    // 设置结果为null
                    result = null;
                    // 执行失败设置ran为false
                    ran = false;
                    // 内部设置outcome为抛出的异常，
                    //并且更新任务状态为EXCEPTIONAL（执行过程中出现了异常）并且唤醒阻塞的线程
                    setException(ex);
                }
                // 如果执行成功（正常执行）
                if (ran)
                    // 内部设置outcome为callable执行的结果，并且更新任务的状态为NORMAL（任务正常执行）并且唤醒阻塞的线程
                    set(result);
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // 将当前任务的线程设置为null
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // 当前任务的状态
            int s = state;
            // 如果state>=INTERRUPTING,说明当前任务处于中断中或已中断状态
            if (s >= INTERRUPTING)
                // 如果当前任务处于中，则执行这个方法线程会不断让出cpu直到任务处于已中断状态
                handlePossibleCancellationInterrupt(s);
        }
    }
```

### **setException(Throwable t)**

如果在执行run()方法的过程中出现了异常会执行这个方法，里面具体的逻辑是：

1. 设置任务的状态为EXCEPTIONAL(表示因为出现异常而非正常完成)
2. 设置outcome(返回结果)为Callable对象的run()方法抛出的异常
3. 执行finishCompletion()方法唤醒因为调用get()方法而陷入阻塞的线程。

```java
    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    protected void setException(Throwable t) {
        // 如果当前任务的状态是新建状态，则设置任务状态为临界状态（即将完成）
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 设置outcome（结果）为callable.run()抛出的异常
            outcome = t;
            // 设置当前任务的状态为出现中断异常
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            // 唤醒调用get()的所有等待的线程并清空栈
            finishCompletion();
        }
    }
```

### **set(V v)**

如果执行run()方法**正常结束**(**没有出现异常**)会执行这个方法，里面的具体逻辑是：

1. 设置任务的状态为**NORMAL**(**表示正常结束**)
2. 设置outcome(**返回结果**)为Callable对象调用run()方法**返回的结果**
3. 唤醒因为**调用get()方法**而**陷入阻塞的线程**。

```java
    /**
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     *
     * @param v the value
     */
    protected void set(V v) {
        // 如果当前任务的状态为新建状态，则设置当前任务的状态为临界状态（即将完成）
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 设置outcome（结果）为callable.run()返回的结果
            outcome = v;
            // 设置当前任务的状态为正常结束
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            // 唤醒调用get()的所有等待的线程并清空栈
            finishCompletion();
        }
    }
```

#### **finishCompletion()**

**任务执行完成**(正常结束和非正常结束都代表任务执行完成)会调用这个方法来**唤醒**所有因**调用get()方法**而**陷入阻塞的线程**。

```java
    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     */
    private void finishCompletion() {
        // assert state > COMPLETING;
        // 如果条件成立，说明当前有陷入阻塞的线程
        for (WaitNode q; (q = waiters) != null;) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    // 获取当前节点封装的thread
                    Thread t = q.thread;
                    // 防止空指针异常
                    if (t != null) {
                        // 设置q.thread为null，方便GC
                        q.thread = null;
                        // 唤醒当前节点封装的线程
                        LockSupport.unpark(t);
                    }
                    // 获取下一个WaitNode节点
                    WaitNode next = q.next;
                    // 如果next为空，说明栈现在已经为空，调用get()陷入阻塞的线程已经全部唤醒，直接break
                    if (next == null)
                        break;
                    // 执行到这里说明还有因调用get()而陷入阻塞的线程，自旋接着唤醒
                    // 这里q.next设置为null帮助GC（垃圾回收）
                    q.next = null; // unlink to help gc
                    // 下一个WaitNode节点
                    q = next;
                }
                // 中断
                break;
            }
        }
        // 空方法，子类可以重写
        done();
        // 将callable设置为null，方便GC
        callable = null;        // to reduce footprint
    }
```

### **handlePossibleCancellationInterrupt(int s)**

这个方法在run()方法里可能会执行，当任务的状态为**中断中**时，**抢到**cpu的线程会**释放**cpu资源，直到任务状态更新为**已中断**状态。

```java
    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        //
        // Thread.interrupted();
    }
```

# **get()方法及与其相关的方法**

## **get()**

**get()方法获取的是任务执行完后返回的结果**。对于空参的**get()方法来说，如果任务还没有执行完**就有线程调用**get()方法获取结果，则该线程会陷入阻塞**，阻塞的具体方法是**awaitDone**方法

```java
    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        // 判断当前任务的状态是否小于COMPLETING，如果成立说明当前任务的状态要么为新建状态要么为临界状态
        if (s <= COMPLETING)
            // 条件成立会调用awaitDone方法自旋等待直到任务完成
            s = awaitDone(false, 0L);
        return report(s);
    }
```

对于指定时间含参的**get**方法来说，如果在**指定时间**内**没有返回结果**，则会**抛出**时间超时异常(**TimeoutException**)

```java
    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        return report(s);
    }
```

### **awaitDone(boolean timed, long nanos)**

这个方法是用来**阻塞**所有因**调用get()方法**获取结果但是**FutureTask**任务还没有执行完的**线程**。**awaitDone**方法在**get()**方法里面会被调用。

**awaitDone**用来**阻塞线程**时需要满足的条件：

1. 任务还没有执行完
2. 线程调用了**get()**方法

```java
    /**
     * Awaits completion or aborts on interrupt or timeout.
     *
     * @param timed true if use timed waits
     * @param nanos time to wait, if timed
     * @return state upon completion
     * 这个方法的作用是等待任务被完成（正常完成或出现异常完成都算完成），被中断，或是被超时
     */
    private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        // 这个WaitNode其实存储的是当前线程
        WaitNode q = null;
        // 表示当前线程代表的WaitNode对象是否入栈成功
        boolean queued = false;
        for (;;) {
            // 如果当前线程出现中断异常，则将该线程代表的WaitNode结点移出栈并抛出中断异常
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }
            // 获取当前任务的状态
            int s = state;
            // 如果当前任务状态大于COMPLETING，说明当前任务已经有结果了（任务完成、中断、取消），直接返回任务状态
            if (s > COMPLETING) {
                if (q != null)
                    // 设置q.thread为null，方便GC
                    q.thread = null;
                return s;
            }
            // 当前任务处于临界状态，即将完成，则当前线程释放cpu
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield();
            // 第一次自旋，如果当前WitNode为null，new一个WaitNode结点
            else if (q == null)
                q = new WaitNode();
            // 第二次自旋，如果当前WaitNode节点没有入队，则尝试入队
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                     q.next = waiters, q);
            // 第三次自旋，到这里表示是否定义了超时时间
            else if (timed) {
                nanos = deadline - System.nanoTime();
                // 超出了指定时间，就移除当前节点并返回任务状态
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                // 未超出时间，挂起当前线程一定时间
                LockSupport.parkNanos(this, nanos);
            }
            else
                // 挂起当前线程，该线程会休眠（什么时候该线程会继续执行呢？除非有其他线程调用unpark()或者中断该线程）
                LockSupport.park(this);
        }
    }
```

#### **removeWaiter**

出现**中断**时，**清空**栈中的**结点**。

```java
    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    // 后继节点不为空
                    if (q.thread != null)
                        pred = q;
                    // 前驱结点不为空
                    else if (pred != null) {
                        // 前驱结点的后继结点指向当前结点的后继结点，就相当于将当前节点删去
                        pred.next = s;
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s))
                        continue retry;
                }
                break;
            }
        }
    }
```

### **report(int s)**

这个方法是真正用来**获取任务的返回结果**的，这个方法在**get()**方法里面会被调用，如果该方法被调用，说明任务已经执行完了。

```java
    /**
     * Returns result or throws exception for completed task.
     *
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        // 获取outcome的值
        Object x = outcome;
        // 如果当前任务的状态为正常结束，则返回outcome的值
        if (s == NORMAL)
            return (V)x;
        // 如果当前任务的状态 >= CANCELLED 说明当前任务状态为被取消、或是在中断中、或是已经中断完成
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable)x);
    }
```

# **cancel(boolean mayInterruptIfRunning)**

这个方法可以**取消当前任务**。

```java
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 条件1成立说明当前任务正在运行中或者处于线程池队列中
        // 条件2成立说明CAS成功可以执行下面的逻辑了，否则返回false，代表cancel失败
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {    // in case call to interrupt throws exception
            if (mayInterruptIfRunning) {
                try {
                    // 获取当前正在执行的线程，也有可能是null，是null的情况代表当前任务正在队列中，线程还没有获取到它呢
                    Thread t = runner;
                    // 给Thread一个中断信号，如果你的程序是响应中断的，则走中断逻辑；如果你的程序不是响应中断的，则什么也不做
                    if (t != null)
                        t.interrupt();
                } finally { // final state
                    // 设置任务状态为已中断
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            // 唤醒所有因调用get()而陷入阻塞的线程
            finishCompletion();
        }
        return true;
    }

```

