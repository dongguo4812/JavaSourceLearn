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
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A counting semaphore.  Conceptually, a semaphore maintains a set of
 * permits.  Each {@link #acquire} blocks if necessary until a permit is
 * available, and then takes it.  Each {@link #release} adds a permit,
 * potentially releasing a blocking acquirer.
 * However, no actual permit objects are used; the {@code Semaphore} just
 * keeps a count of the number available and acts accordingly.
 *
 * <p>Semaphores are often used to restrict the number of threads than can
 * access some (physical or logical) resource. For example, here is
 * a class that uses a semaphore to control access to a pool of items:
 *  <pre> {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}</pre>
 *
 * <p>Before obtaining an item each thread must acquire a permit from
 * the semaphore, guaranteeing that an item is available for use. When
 * the thread has finished with the item it is returned back to the
 * pool and a permit is returned to the semaphore, allowing another
 * thread to acquire that item.  Note that no synchronization lock is
 * held when {@link #acquire} is called as that would prevent an item
 * from being returned to the pool.  The semaphore encapsulates the
 * synchronization needed to restrict access to the pool, separately
 * from any synchronization needed to maintain the consistency of the
 * pool itself.
 *
 * <p>A semaphore initialized to one, and which is used such that it
 * only has at most one permit available, can serve as a mutual
 * exclusion lock.  This is more commonly known as a <em>binary
 * semaphore</em>, because it only has two states: one permit
 * available, or zero permits available.  When used in this way, the
 * binary semaphore has the property (unlike many {@link java.util.concurrent.locks.Lock}
 * implementations), that the &quot;lock&quot; can be released by a
 * thread other than the owner (as semaphores have no notion of
 * ownership).  This can be useful in some specialized contexts, such
 * as deadlock recovery.
 *
 * <p> The constructor for this class optionally accepts a
 * <em>fairness</em> parameter. When set false, this class makes no
 * guarantees about the order in which threads acquire permits. In
 * particular, <em>barging</em> is permitted, that is, a thread
 * invoking {@link #acquire} can be allocated a permit ahead of a
 * thread that has been waiting - logically the new thread places itself at
 * the head of the queue of waiting threads. When fairness is set true, the
 * semaphore guarantees that threads invoking any of the {@link
 * #acquire() acquire} methods are selected to obtain permits in the order in
 * which their invocation of those methods was processed
 * (first-in-first-out; FIFO). Note that FIFO ordering necessarily
 * applies to specific internal points of execution within these
 * methods.  So, it is possible for one thread to invoke
 * {@code acquire} before another, but reach the ordering point after
 * the other, and similarly upon return from the method.
 * Also note that the untimed {@link #tryAcquire() tryAcquire} methods do not
 * honor the fairness setting, but will take any permits that are
 * available.
 *
 * <p>Generally, semaphores used to control resource access should be
 * initialized as fair, to ensure that no thread is starved out from
 * accessing a resource. When using semaphores for other kinds of
 * synchronization control, the throughput advantages of non-fair
 * ordering often outweigh fairness considerations.
 *
 * <p>This class also provides convenience methods to {@link
 * #acquire(int) acquire} and {@link #release(int) release} multiple
 * permits at a time.  Beware of the increased risk of indefinite
 * postponement when these methods are used without fairness set true.
 *
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * a "release" method such as {@code release()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful "acquire" method such as {@code acquire()}
 * in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class Semaphore implements java.io.Serializable {
    //序列化版本号
    private static final long serialVersionUID = -3222578661600680210L;
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    /** 所有机制通过AbstractQueuedSynchronizer子类实现 */
    private final Sync sync;

    /**
     * Synchronization implementation for semaphore.  Uses AQS state
     * to represent permits. Subclassed into fair and nonfair
     * versions.
     * 信号量的同步实现。使用AQS状态表示许可证。子类分为公平和非公平版本。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        //非公平模式获取许可
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
        //尝试释放许可
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                if (compareAndSetState(current, next))
                    return true;
            }
        }
        //减少许可
        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next))
                    return;
            }
        }
        //清空许可（许可证数置为0）
        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }

    /**
     * NonFair version
     * 非公平版本
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * Fair version
     * 公平版本
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            //自旋
            for (;;) {
                if (hasQueuedPredecessors())
                    return -1;
                //资源数
                int available = getState();
                //剩余资源数
                int remaining = available - acquires;
                //资源不够-1 直接返回
                if (remaining < 0 ||
                        //修改state
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }

    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and nonfair fairness setting.
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     *                指定许可证个数，采用非公平版本
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and the given fairness setting.
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     * @param fair {@code true} if this semaphore will guarantee
     *        first-in first-out granting of permits under contention,
     *        else {@code false}
     *     指定许可证个数，指定是否公平版本
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available, or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    /**
     * 从这个信号量获取一个许可，阻塞直到有一个可用，或者线程被中断。
     *
     * 1.获取一个许可证(如果有)并立即返回，将可用许可证的数量减少一个。
     * 2.如果没有可用的许可，那么当前线程将出于线程调度目的而被禁用，并处于休眠状态，直到发生以下两种情况之一:
     * 其他线程为这个信号量调用  release 方法，当前线程下一个将被分配一个许可; 或其他线程中断当前线程。
     * 3.如果当前线程: 在进入此方法时设置其中断状态; 或当等待线程中断时，线程中断 。然后 InterruptedException 被抛出，当前线程的中断状态被清除。
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it will continue to wait, but the
     * time at which the thread is assigned a permit may change compared to
     * the time it would have received the permit had no interruption
     * occurred.  When the thread does return from this method its interrupt
     * status will be set.
     */
    /**
     * 从这个信号量获取一个许可，阻塞直到一个可用。
     *
     * 1.获取一个许可证(如果有)并立即返回，将可用许可证的数量减少一个。
     * 2.如果没有可用的许可，则当前线程出于线程调度的目的被禁用，并处于休眠状态，直到其他线程调用这个信号量的 release 方法，当前线程下一个将被分配一个许可。
     * 3.如果当前线程在等待许可时被中断，那么它将继续等待，但与未发生中断时线程收到许可证的时间相比，分配给线程的许可证时间可能会发生变化。当线程从这个方法返回时，它的中断状态将被设置。
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * Acquires a permit from this semaphore, only if one is available at the
     * time of invocation.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>Even when this semaphore has been set to use a
     * fair ordering policy, a call to {@code tryAcquire()} <em>will</em>
     * immediately acquire a permit if one is available, whether or not
     * other threads are currently waiting.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting, then use
     * {@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    /**
     * 仅当调用时可用时，才从此信号量获取许可。
     *
     * 1.获取一个许可(如果有)并立即返回，值为  true ，将可用的许可数量减少1。
     * 2.如果没有可用的许可，则该方法将立即返回值  false 。
     * 3.即使这个信号量被设置为使用公平排序策略，如果有一个许可可用，调用 tryAcquire() 将立即获得一个许可，无论当前是否有其他线程正在等待。
     *
     * 这个“闯入“ 行为在某些情况下是有用的，即使它破坏了公平。
     * 如果你想遵守公平性设置，那么使用  tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit. seconds)，这几乎是等效的(它也检测中断)。
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * Acquires a permit from this semaphore, if one becomes available
     * within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If a permit is acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    /**
     * 如果在给定的等待时间内有一个信号量可用，并且当前线程没有被中断，则从这个信号量获得一个许可。
     *
     * 1.获取一个许可(如果有)并立即返回，值为  true ，将可用的许可数量减少 1 。
     * 2.如果没有可用的许可，那么当前线程将出于线程调度目的而被禁用，并处于休眠状态，直到发生以下三种情况之一:
     *     其他线程为这个信号量调用 release 方法，当前线程下一个将被分配一个许可; 或一些其他线程中断当前线程; 或等待时间已过。
     * 3.如果获得了许可，则返回值  true 。
     * 4.如果当前线程:在进入此方法时设置其中断状态; 或在等待获取许可证时被中断。 然后  InterruptedException 被抛出，当前线程的中断状态被清除。
     * 5.如果指定的等待时间过去了，则返回值  false 。如果时间小于或等于零，该方法将根本不等待。
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * Releases a permit, returning it to the semaphore.
     *
     * <p>Releases a permit, increasing the number of available permits by
     * one.  If any threads are trying to acquire a permit, then one is
     * selected and given the permit that was just released.  That thread
     * is (re)enabled for thread scheduling purposes.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link #acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     */
    /**
     * 释放一个许可，并将其返回给信号量。
     *
     * 1.释放一个许可证，可用的许可证数量增加一个。如果有任何线程试图获得许可证，那么将选择一个线程并授予刚刚释放的许可证。出于线程调度的目的，该线程已（重新）启用。
     * 2.没有要求释放许可的线程必须通过调用  acquire 来获得该许可。信号量的正确用法是通过应用程序中的编程约定来确定的。
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available,
     * or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread are instead
     * assigned to other threads trying to acquire permits, as if
     * permits had been made available by a call to {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    /**
     * 从这个信号量中获取给定数量的许可，阻塞直到所有许可都可用，或者线程被中断。
     *
     * 1.获取给定数量的许可证(如果它们可用)，并立即返回，将可用许可证的数量减少给定的数量。
     * 2.如果可用的许可不足，则当前线程将出于线程调度目的而被禁用，并处于休眠状态，直到发生以下两种情况之一:
     *    其他线程调用这个信号量的  release() 方法之一，当前线程是下一个被分配许可的线程，可用的许可数量满足这个请求; 或其他线程中断当前线程。
     * 3.如果当前线程:在进入该方法时设置其中断状态； 或在等待许可时被中断。 然后 InterruptedException 被抛出，当前线程的中断状态被清除。
     *
     * 任何分配给该线程的许可都会分配给其他试图获取许可的线程，就像通过调用 release（）获得了许可一样。
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its
     * position in the queue is not affected.  When the thread does return
     * from this method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    /**
     * 从这个信号量中获取给定数量的许可，直到所有许可都可用为止。
     *
     * 1.获取给定数量的许可证(如果它们可用)，并立即返回，将可用许可证的数量减少给定的数量。
     * 2.如果可用的许可数量不足，则当前线程将出于线程调度目的而被禁用，并处于休眠状态，直到其他线程调用此信号量的 release 方法之一，当前线程将被分配许可，并且可用许可的数量满足此请求。
     * 3.如果当前线程在等待允许时是被中断，那么它将继续等待，并且它在队列中的位置不受影响。当线程从这个方法返回时，它的中断状态将被设置。
     */
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * Acquires the given number of permits from this semaphore, only
     * if all are available at the time of invocation.
     *
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * <p>Even when this semaphore has been set to use a fair ordering
     * policy, a call to {@code tryAcquire} <em>will</em>
     * immediately acquire a permit if one is available, whether or
     * not other threads are currently waiting.  This
     * &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to
     * honor the fairness setting, then use {@link #tryAcquire(int,
     * long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    /**
     * 仅当调用时所有许可都可用时，才从此信号量获取给定数量的许可。
     *
     * 1.获取给定数量的许可(如果它们可用)，并立即返回值 true，将可用许可的数量减少给定的数量。
     * 2.如果可用的许可数量不足，则该方法将立即返回值  false，可用的许可数量不变。
     * 3.即使这个信号量已经被设置为使用公平排序策略，如果有一个许可可用，调用  tryAcquire 将立即获得一个许可，无论当前是否有其他线程正在等待。
     * 这个“闯入”行为在某些情况下是有用的，即使它破坏了公平。
     * 如果你想遵守公平性设置，那么使用 tryAcquire(int, long, TimeUnit) tryAcquire(permissions, 0, TimeUnit. seconds) ，这几乎是等效的(它也检测中断)。
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * Acquires the given number of permits from this semaphore, if all
     * become available within the given waiting time and the current
     * thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the permits are acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire the permits,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread, are instead
     * assigned to other threads trying to acquire permits, as if
     * the permits had been made available by a call to {@link #release()}.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.  Any permits that were to be assigned to this
     * thread, are instead assigned to other threads trying to acquire
     * permits, as if the permits had been made available by a call to
     * {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    /**
     * 从这个信号量中获取给定数量的许可，如果所有许可都在给定的等待时间内可用，并且当前线程没有被线程中断。
     *
     * 1.获取给定数量的许可，如果它们可用并立即返回，值为  true，将可用许可的数量减少给定的数量。
     * 2.如果可用的许可不足，则当前线程将出于线程调度目的而被禁用，并处于休眠状态，直到发生以下三种情况之一:
     *     其他线程调用这个信号量的 release() release 方法之一，当前线程是下一个被分配许可的线程，可用的许可数量满足这个请求; 或一些其他线程中断当前线程; 或等待时间已过。
     * 3.如果获得了许可，则返回值  true。
     * 4.如果当前线程: 在进入此方法时设置其中断状态; 或在等待获取许可时，线程中断。然后 InterruptedException 被抛出，当前线程的中断状态被清除。
     *     任何分配给这个线程的许可，都被分配给了其他试图获得许可的线程，就好像这些许可是通过调用 release() 获得的一样。
     * 5.如果指定的等待时间过去了，则返回值  false 。如果时间小于或等于零，该方法将根本不等待。任何分配给这个线程的许可，都被分配给了其他试图获得许可的线程，就好像这些许可是通过调用  release() 获得的一样。
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * Releases the given number of permits, returning them to the semaphore.
     *
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount.
     * If any threads are trying to acquire permits, then one
     * is selected and given the permits that were just released.
     * If the number of available permits satisfies that thread's request
     * then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available.
     * If there are still permits available
     * after this thread's request has been satisfied, then those permits
     * are assigned in turn to other threads trying to acquire permits.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link Semaphore#acquire acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    /**
     * 释放给定数量的许可，并将它们返回给信号量。
     *
     * 1.释放给定数量的许可证，将可用许可证的数量增加该数量。如果有任何线程试图获得许可，那么将选择一个线程并授予刚刚释放的许可。
     * 如果可用许可的数量满足线程的请求，那么该线程将(重新)为线程调度目的启用;否则线程将等待，直到有足够的许可可用。
     * 如果在这个线程的请求被满足之后，仍然有可用的许可证，那么这些许可证将依次分配给试图获得许可证的其他线程。
     * 2.没有要求释放许可证的线程必须通过调用 acquire 来获得许可证。信号量的正确用法是通过应用程序中的编程约定来确定的。
     */
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * Returns the current number of permits available in this semaphore.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     * 返回此信号量中可用的许可证的当前数量。（此方法通常用于调试和测试目的。）
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * Acquires and returns all permits that are immediately available.
     *
     * @return the number of permits acquired
     * 获取并返回所有立即可用的许可。
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * Shrinks the number of available permits by the indicated
     * reduction. This method can be useful in subclasses that use
     * semaphores to track resources that become unavailable. This
     * method differs from {@code acquire} in that it does not block
     * waiting for permits to become available.
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException if {@code reduction} is negative
     * 将可用许可证的数量按指定的减少量缩小。此方法在使用信号量跟踪不可用资源的子类中非常有用。
     * 此方法与  acquire 的不同之处在于，它不会阻塞等待许可证变得可用。
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * Returns {@code true} if this semaphore has fairness set true.
     *
     * @return {@code true} if this semaphore has fairness set true
     * 如果这个信号量的公平性设置为true，则返回 true 。
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     * 查询是否有线程正在等待获取。请注意，因为取消可能在任何时候发生，返回  true 并不保证任何其他线程将获得。这种方法主要用于监视系统状态。
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire.
     * The value is only an estimate because the number of threads may
     * change dynamically while this method traverses internal data
     * structures.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     * 返回等待获取的线程数的估计值。该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态更改。此方法设计用于监视系统状态，而不是用于同步控制。
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a best-effort
     * estimate.  The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     * 返回一个包含可能正在等待获取的线程的集合。因为在构造此结果时，实际的线程集可能会动态更改，因此返回的集合只是最佳估计。
     * 返回集合的元素没有特定的顺序。设计此方法是为了便于构造提供更广泛监视功能的子类。
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Returns a string identifying this semaphore, as well as its state.
     * The state, in brackets, includes the String {@code "Permits ="}
     * followed by the number of permits.
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
