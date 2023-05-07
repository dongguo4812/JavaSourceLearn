/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 *
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     *
     * Style note: As usual, we bypass overhead of using
     * AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     */

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

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state != NEW;
    }

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

    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    protected void done() { }

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

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

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

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
