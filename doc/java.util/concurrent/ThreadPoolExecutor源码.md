# **介绍**

ThreadPoolExecutor 是 Java 中实现线程池的一个类，它是 ExecutorService 接口的一个实现类。线程池可以用来优化线程的使用，避免频繁地创建和销毁线程，以及限制并发线程的数量，从而提高应用程序的性能。

```java
public class ThreadPoolExecutor extends AbstractExecutorService 
```

![image-20230619221856084](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306192219000.png)

# **常量&变量**

```java
 /**
     * The main pool control state, ctl, is an atomic integer packing
     * two conceptual fields
     *   workerCount, indicating the effective number of threads
     *   runState,    indicating whether running, shutting down etc
     *
     * In order to pack them into one int, we limit workerCount to
     * (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2
     * billion) otherwise representable. If this is ever an issue in
     * the future, the variable can be changed to be an AtomicLong,
     * and the shift/mask constants below adjusted. But until the need
     * arises, this code is a bit faster and simpler using an int.
     *
     * The workerCount is the number of workers that have been
     * permitted to start and not permitted to stop.  The value may be
     * transiently different from the actual number of live threads,
     * for example when a ThreadFactory fails to create a thread when
     * asked, and when exiting threads are still performing
     * bookkeeping before terminating. The user-visible pool size is
     * reported as the current size of the workers set.
     *
     * The runState provides the main lifecycle control, taking on values:
     *
     *   RUNNING:  Accept new tasks and process queued tasks
     *   SHUTDOWN: Don't accept new tasks, but process queued tasks
     *   STOP:     Don't accept new tasks, don't process queued tasks,
     *             and interrupt in-progress tasks
     *   TIDYING:  All tasks have terminated, workerCount is zero,
     *             the thread transitioning to state TIDYING
     *             will run the terminated() hook method
     *   TERMINATED: terminated() has completed
     *
     * The numerical order among these values matters, to allow
     * ordered comparisons. The runState monotonically increases over
     * time, but need not hit each state. The transitions are:
     *
     * RUNNING -> SHUTDOWN
     *    On invocation of shutdown(), perhaps implicitly in finalize()
     * (RUNNING or SHUTDOWN) -> STOP
     *    On invocation of shutdownNow()
     * SHUTDOWN -> TIDYING
     *    When both queue and pool are empty
     * STOP -> TIDYING
     *    When pool is empty
     * TIDYING -> TERMINATED
     *    When the terminated() hook method has completed
     *
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     *
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     * 存储线程池状态和线程池大小，那么用前3位表示线程池状态，后29位表示：线程池大小，即线程池线程数
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    //常量29 用于计算的位数
    private static final int COUNT_BITS = Integer.SIZE - 3;
    //最大支持线程数2^29-1
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    //以下为线程池的四个状态，用32位中的前三位表示
    // 111 00000 00000000 00000000 00000000  线程运行中 【running状态值为负数最小】
    private static final int RUNNING    = -1 << COUNT_BITS;
    // 000 拒绝新的任务提交,会将队列中的任务执行完,正在执行的任务继续执行.
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    // 001 拒绝新的任务提交,清空在队列中的任务
    private static final int STOP       =  1 << COUNT_BITS;
    // 010 所有任务都销毁了,workCount=0的时候,线程池的装填在转换为TIDYING是,会执行钩子方法terminated()
    private static final int TIDYING    =  2 << COUNT_BITS;
    // 011 terminated() 方法执行完成后,线程池的状态会转为TERMINATED.
    private static final int TERMINATED =  3 << COUNT_BITS;
    
 /**
     * The queue used for holding tasks and handing off to worker
     * threads.  We do not require that workQueue.poll() returning
     * null necessarily means that workQueue.isEmpty(), so rely
     * solely on isEmpty to see if the queue is empty (which we must
     * do for example when deciding whether to transition from
     * SHUTDOWN to TIDYING).  This accommodates special-purpose
     * queues such as DelayQueues for which poll() is allowed to
     * return null even if it may later return non-null when delays
     * expire.
     * 任务队列
     * 保存不能马上执行的Runnable任务。
     * 执行shutdownNow()时，会返回还在队列的任务
     */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * Lock held on access to workers set and related bookkeeping.
     * While we could use a concurrent set of some sort, it turns out
     * to be generally preferable to use a lock. Among the reasons is
     * that this serializes interruptIdleWorkers, which avoids
     * unnecessary interrupt storms, especially during shutdown.
     * Otherwise exiting threads would concurrently interrupt those
     * that have not yet interrupted. It also simplifies some of the
     * associated statistics bookkeeping of largestPoolSize etc. We
     * also hold mainLock on shutdown and shutdownNow, for the sake of
     * ensuring workers set is stable while separately checking
     * permission to interrupt and actually interrupting.
     *  主锁，对workers、largestPoolSize、completedTaskCount的访问都必须先获取该锁
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * Set containing all worker threads in pool. Accessed only when
     * holding mainLock.
     * 包含池中的所有工作线程的集合。持有mainLock访问
     *  创建Worker时，添加到集合
     *  线程结束时，从集合移除
     *  调用shutdown()时，从该集合中找到空闲线程并中断
     *  调用shutdownNow()时，从该集合中找到已启动的线程并中断
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * Wait condition to support awaitTermination
     * 线程通信手段, 用于支持awaitTermination方法：等待所有任务完成,并支持设置超时时间,返回值代表是不是超时.
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * Tracks largest attained pool size. Accessed only under
     * mainLock.
     *  记录workers历史以来的最大值。持有mainLock访问
     *  每次增加worker的时候,都会判断当前workers.size()是否大于最大值，大于则更新
     *  用于线程池监控的，作为重要指标
     */
    private int largestPoolSize;

    /**
     * Counter for completed tasks. Updated only on termination of
     * worker threads. Accessed only under mainLock.
     * 计数所有已完成任务,持有mainLock访问
     * 每个worker都有一个自己的成员变量 completedTasks 来记录当前 worker 执行的任务次数,
     * 当前线worker工作线程终止的时候, 才会将worker中的completedTasks的数量加入到 completedTaskCount 指标中.
     */
    private long completedTaskCount;

    /*
     * All user control parameters are declared as volatiles so that
     * ongoing actions are based on freshest values, but without need
     * for locking, since no internal invariants depend on them
     * changing synchronously with respect to other actions.
     */

    /**
     * Factory for new threads. All threads are created using this
     * factory (via method addWorker).  All callers must be prepared
     * for addWorker to fail, which may reflect a system or user's
     * policy limiting the number of threads.  Even though it is not
     * treated as an error, failure to create threads may result in
     * new tasks being rejected or existing ones remaining stuck in
     * the queue.
     *
     * We go further and preserve pool invariants even in the face of
     * errors such as OutOfMemoryError, that might be thrown while
     * trying to create threads.  Such errors are rather common due to
     * the need to allocate a native stack in Thread.start, and users
     * will want to perform clean pool shutdown to clean up.  There
     * will likely be enough memory available for the cleanup code to
     * complete without encountering yet another OutOfMemoryError.
     * 线程工厂
     */
    private volatile ThreadFactory threadFactory;

    /**
     * Handler called when saturated or shutdown in execute.
     * 拒绝策略,默认四种AbortPolicy、CallerRunsPolicy、DiscardPolicy、DiscardOldestPolicy,
     * 建议自己实现,增加监控指标
     */
    private volatile RejectedExecutionHandler handler;


    // keepAliveTime和allowCoreThreadTimeOut 是关于线程空闲是否会被销毁的配置

    /**
     * Timeout in nanoseconds for idle threads waiting for work.
     * Threads use this timeout when there are more than corePoolSize
     * present or if allowCoreThreadTimeOut. Otherwise they wait
     * forever for new work.
     *  关于空闲的说明：
     *  1、线程池在没有关闭之前，会一直向任务队列（workqueue）获取任务执行，如果任务队列是空的，在新任务提交上来之前，就会产生一个等待时间，期间，线程处于空闲状态
     *  2、向任务队列获取任务用：workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS)，表示阻塞式获取元素，等待超时，则终止等待并返回false。通过判断poll()方法是true/falle来判定线程是否超时
     *
     *  获取任务的等待时间 ，以下两种情况会使用到该值
     * 1、如果启用allowCoreThreadTimeOut，那表示核心线程的空闲时间
     *  2、当线程池内线程数超过corePoolSize，表示线程获取任务的等待时间
     */
    private volatile long keepAliveTime;

    /**
     * If false (default), core threads stay alive even when idle.
     * If true, core threads use keepAliveTime to time out waiting
     * for work.
     *  核心线程是否开启超时
     *  false：表示核心线程一旦启动，会一直运行，直至关闭线程池。默认该值
     *  true：表示核心线程处于空闲且时间超过keepAliveTime，核心线程结束后，将不再创建新线程
     *  （默认的构造函数没有设置这个属性，需要手工调用allowCoreThreadTimeOut()方法来设置)
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     * Core pool size is the minimum number of workers to keep alive
     * (and not allow to time out etc) unless allowCoreThreadTimeOut
     * is set, in which case the minimum is zero.
     * 核心线程数量
     * 核心线程是指：线程会一直存活在线程池中，不会被主动销毁【如果核心线程开启超时，有可能被被销毁】
     */
    private volatile int corePoolSize;

    /**
     * Maximum pool size. Note that the actual maximum is internally
     * bounded by CAPACITY.
     * 配置的线程池最大线程数
     */
    private volatile int maximumPoolSize;

    /**
     * The default rejected execution handler
     * 默认拒绝策略 AbortPolicy
     */
    private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();

    /**
     * Permission required for callers of shutdown and shutdownNow.
     * We additionally require (see checkShutdownAccess) that callers
     * have permission to actually interrupt threads in the worker set
     * (as governed by Thread.interrupt, which relies on
     * ThreadGroup.checkAccess, which in turn relies on
     * SecurityManager.checkAccess). Shutdowns are attempted only if
     * these checks pass.
     *
     * All actual invocations of Thread.interrupt (see
     * interruptIdleWorkers and interruptWorkers) ignore
     * SecurityExceptions, meaning that the attempted interrupts
     * silently fail. In the case of shutdown, they should not fail
     * unless the SecurityManager has inconsistent policies, sometimes
     * allowing access to a thread and sometimes not. In such cases,
     * failure to actually interrupt threads may disable or delay full
     * termination. Other uses of interruptIdleWorkers are advisory,
     * and failure to actually interrupt will merely delay response to
     * configuration changes so is not handled exceptionally.
     * 安全控制访问（主要用于shutdown和 shutdownNow方法
     */
    private static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread");

    /* The context to be used when executing the finalizer, or null. */
    //在threadPoolExecutor初始化的时候赋值,acc对象是指当前调用上下文的快照，
    // 其中包括当前线程继承的AccessControlContext和任何有限的特权范围，
    // 使得可以在稍后的某个时间点(可能在另一个线程中)检查此上下文。
    private final AccessControlContext acc;
```

# **构造方法**

```java
    // Public constructors and methods

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler.
     * It may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        //初始值的合法性校验
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
                //最大线程数必须大于核心线程数
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        //成员变量赋初值
        this.acc = System.getSecurityManager() == null ?
                null :
                AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        //默认使用SynchronousQueue<Runnable>
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        //默认使用DefaultThreadFactory
        this.threadFactory = threadFactory;
        this.handler = handler;
    }
```

- corePoolSize：线程池中的核心线程数；

- maximumPoolSize：线程池最大线程数，它表示在线程池中最多能创建多少个线程；

- keepAliveTime：线程池中**非核心线程闲置超时时长**（准确来说应该是没有任务执行时的回收时间，后面会分析）；

- - 一个非核心线程，如果不干活(闲置状态)的时长超过这个参数所设定的时长，就会被销毁掉
  - 如果设置allowCoreThreadTimeOut(boolean value)，则会作用于核心线程

- TimeUnit：时间单位。可选的单位有分钟（MINUTES），秒（SECONDS），毫秒(MILLISECONDS) 等；

- workQueue：任务的阻塞队列，缓存将要执行的Runnable任务，由各线程轮询该任务队列获取任务执行。可以选择以下几个阻塞队列。

- - ArrayBlockingQueue：是一个基于数组结构的有界阻塞队列，此队列按 FIFO（先进先出）原则对元素进行排序。
  - LinkedBlockingQueue：一个基于链表结构的阻塞队列，此队列按FIFO （先进先出） 排序元素，吞吐量通常要高于ArrayBlockingQueue。静态工厂方法Executors.newFixedThreadPool()使用了这个队列。
  - SynchronousQueue：一个不存储元素的阻塞队列。每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态，吞吐量通常要高于LinkedBlockingQueue，静态工厂方法Executors.newCachedThreadPool使用了这个队列。
  - PriorityBlockingQueue：一个具有优先级的无限阻塞队列。

- ThreadFactory：线程创建的工厂。可以进行一些属性设置，比如线程名，优先级等等，有默认实现。

- RejectedExecutionHandler：任务拒绝策略，当运行线程数已达到maximumPoolSize，队列也已经装满时会调用该参数拒绝任务，默认情况下是AbortPolicy，表示无法处理新任务时抛出异常。以下是JDK1.5提供的四种策略。

- - AbortPolicy：直接抛出异常。

  - CallerRunsPolicy：只用调用者所在线程来运行任务。

  - DiscardOldestPolicy：丢弃队列里最近的一个任务，并执行当前任务。

  - DiscardPolicy：不处理，丢弃掉。

    当然也可以根据应用场景需要来实现

RejectedExecutionHandler接口自定义策略。如记录日志或持久化不能处理的任务。

# **内部类**

## **Worker**

Worker其实是ThreadPoolExecutor线程池中保存的工作线程，线程池主要是使用Worker来执行任务。

Worker类继承自AQS，并且实现了Runnable接口。它主要维持运行任务线程的中断控制状态，以及其他次要的信息。

它继承自AQS主要用于每次执行任务时简化锁的获取和释放。这可以防止中断用于唤醒等待任务的工作线程，而不是中断正在运行的任务。

```java
    /**
     * Class Worker mainly maintains interrupt control state for
     * threads running tasks, along with other minor bookkeeping.
     * This class opportunistically extends AbstractQueuedSynchronizer
     * to simplify acquiring and releasing a lock surrounding each
     * task execution.  This protects against interrupts that are
     * intended to wake up a worker thread waiting for a task from
     * instead interrupting a task being run.  We implement a simple
     * non-reentrant mutual exclusion lock rather than use
     * ReentrantLock because we do not want worker tasks to be able to
     * reacquire the lock when they invoke pool control methods like
     * setCorePoolSize.  Additionally, to suppress interrupts until
     * the thread actually starts running tasks, we initialize lock
     * state to a negative value, and clear it upon start (in
     * runWorker).
     */
    private final class Worker
        extends AbstractQueuedSynchronizer
        implements Runnable
    {
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** Thread this worker is running in.  Null if factory fails. */
        //线程类型的属性：thread，线程池启动工作线程，就是启动这个thread。
        // 1、通过this.thread=getThreadFactory().newThread(this)，初始化了属性thread，this就是指Worker对象
        //2、因为Worker类实现了Runnable接口，所以thread启动后，会运行Worker的run()方法，然后就去执行runWorker(this)方法
        final Thread thread;
        /** Initial task to run.  Possibly null. */
        //线程要执行的第1个任务（可能为 null)  它表示这个任务立即执行，不需要放到任务队列。在工作线程数<核心线程数时，这种场景会出现
        Runnable firstTask;
        /** Per-thread task counter */
        //保存Worker线程池执行过的任务数，在runWorker()的finally中累加更新。任务执行成功与否都会更新
        volatile long completedTasks;

        /**
         * Creates with given first task and thread from ThreadFactory.
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {
            // AQS父类的state。设为-1
            setState(-1); // inhibit interrupts until runWorker
            //firstTask赋初值
            this.firstTask = firstTask;
            //属性thread赋值
            this.thread = getThreadFactory().newThread(this);
        }

        /** Delegates main run loop to outer runWorker  */
        public void run() {
            //调用runWorkder方法：将Worker对象传递给调用者，这样就可以访问firstTask、thread等属性以及lock()相关方法
            runWorker(this);
        }

        // Lock methods
        //
        // The value 0 represents the unlocked state.
        // The value 1 represents the locked state.
        // state 的值说明
        // -1：worker初始化；  1 ：锁被独占； 0：锁空闲

        //是否持有锁 AQS父类方法的实现
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        //以独占方式获取锁，将state设为1  AQS父类方法的实现
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            //假如state=1，那么cas失败，返回false，线程就会进入AQS队列等待
            return false;
        }

        //释放锁。state设为0  AQS父类方法的实现
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        //提供加锁和解锁的方法
        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        //向线程发起中断请求
        // 符合：1、运行中的；2、没有处于中断   才能中断
        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }
```

AbortPolicy、CallerRunsPolicy、DiscardOldestPolicy、DiscardPolicy表示任务的拒绝策略，当线程池的线程数量达到最大值并且阻塞队列已满时，根据这些不同的策略对新提交的任务进行不同的处理。它们都实现了RejectedExecutionHandler接口。

## **CallerRunsPolicy**

直接在调用execute方法的线程中运行被拒绝的任务，除非执行器已关闭，在这种情况下，该任务被丢弃

```java
    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     * 
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() { }

        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         *          在调用线程中执行任务，除非执行器已经被关闭，此时这个任务将被丢弃
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            //如果线程池没有关闭，直接在该线程中运行此方法
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }
```

## **AbortPolicy**

当线程和阻塞队列均到达最大容量时，直接拒绝执行任务，并抛出RejectedExecutionException异常

```java
    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     * 直接抛出RejectedExecutionException异常
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                                                 " rejected from " +
                                                 e.toString());
        }
    }
```

## **DiscardPolicy**

不做任何事情，相当于直接丢弃任务。

```java
    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() { }

        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         *          不做任何事情，相当于直接丢弃任务
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }
```

## **DiscardOldestPolicy**

丢弃最老的任务，并运行最新提交的任务。

```java
        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         *          获取并且忽略线程池下一个要执行的任务
         *          线程池可用的话，就调用execute执行新的任务
         *          线程池关闭的话，新的任务将被丢弃
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                //队列中位于头部的任务出栈
                e.getQueue().poll();
                //执行新的任务
                e.execute(r);
            }
        }
```



# 线程池流程图:

![image-20230619222900973](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306192229717.png)

# **常用方法**

## **execute**

![image-20230619222352560](https://gitee.com/dongguo4812_admin/image/raw/master/image/202306192223200.png)

```java
    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         */
        //计算当前线程池的状态及线程数
        int c = ctl.get();
        // 1、线程池线程数小于配置的核心线程数
        if (workerCountOf(c) < corePoolSize) {
            // 将任务提交给核心线程执行
            if (addWorker(command, true))
                return;
            //失败的情况：1、线程池已经被关闭、2、线程池线程数大于等于核心线程数 （不能以true的方式提交了 ）
            // 重新获取线程池状态
            c = ctl.get();
        }
        // 2、无空闲核心线程或者开启核心线程失败，尝试将任务加入队列
        // 再次确认线程池为RUNNING状态，将任务加入队列【非阻塞式，队列满了会立即返回false】
        if (isRunning(c) && workQueue.offer(command)) {
            //任务加入队列成功
            //再次获取当前线程池状态(线程池可能被其它线程关闭了)
            int recheck = ctl.get();
            //判断当前线程池状态是不是RUNNING状态,不是就从workQueue中删除command任务
            if (! isRunning(recheck) && remove(command))
                reject(command);
            //如果当前线程数是0（那证明还没有其他工作线程去处理这个任务）,那么刚刚的任务肯定在阻塞队列里面了
            else if (workerCountOf(recheck) == 0)
                //开启一个没有任务的Worker线程去执行队列的任务
                addWorker(null, false);
        }
        // 3 workQueue添加worker失败，即队列满了导致添加任务失败
        //创建新的非核心线程并执行任务
        //如果线程创建失败，说明要么是线程池当前状态!=RUNNING，或者是任务队列已满且线程总数达到最大线程数了
        else if (!addWorker(command, false))
            //执行拒绝策略.
            reject(command);
    }
```

**execute()总结**

进行三次addWorker的尝试：

1. addWorker(command, true)：**创建任务并以核心线程执行**
2. 核心线程数达到上限， **创建任务添加到任务队列，不创建线程**
3. addWorker(null, false) ：任务添加到队列后，接着线程池被关闭，并且从队列移除该任务失败，并且线程池线程数为0，这时**创建任务并以非核心线程执行**

addWorker(command, false) ：**任务队列已满，创建非核心线程并执行**

任务提交失败情况：**线程池非RUNNING状态** 并且 **任务队列已满并且线程池线程数达到最大线程数（maximumPoolSize）**

**FixedThreadPool**，因为它配置的corePoolSize与maximumPoolSize相等，所以不会执行到情况3，并且因为workQueue为默认的LinkedBlockingQueue，其长度为Integer.MAX_VALUE，几乎不可能出现任务无法被添加到workQueue的情况，所以FixedThreadPool的所有任务执行在核心线程中。

而**CachedThreadPool**的corePoolSize为0，表示它不会执行到情况1，因为它的maximumPoolSize为Integer.MAX_VALUE，所以几乎没有线程数量上限，因为它的workQueue为SynchronousQueue，所以当线程池里没有闲置的线程SynchronousQueue就会添加任务失败，因此会执行到情况3添加新的线程执行任务。

## **addWorker**

```java
    /**
     * Checks if a new worker can be added with respect to current
     * pool state and the given bound (either core or maximum). If so,
     * the worker count is adjusted accordingly, and, if possible, a
     * new worker is created and started, running firstTask as its
     * first task. This method returns false if the pool is stopped or
     * eligible to shut down. It also returns false if the thread
     * factory fails to create a thread when asked.  If the thread
     * creation fails, either due to the thread factory returning
     * null, or due to an exception (typically OutOfMemoryError in
     * Thread.start()), we roll back cleanly.
     *
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     *                  指定新增线程执行的第一个任务或者不执行任务
     *
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     * @return true if successful
     * 创建新的线程执行当前任务
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        //外循环：
        retry:
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            // 如果线程池状态是SHUTDOWN、STOP、TIDYING、TERMINATED就不允许提交。
            // && 后面的特殊情况，线程池的状态是SHUTDOWN并且要要执行的任务为Null并且队列不是空，
            // 这种情况下是允许增加一个线程来帮助队列中的任务跑完的，因为shutdown状态下，允许执行完成阻塞队里中的任务
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   //execute()有addWorkder(null,false)的场景
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;
            //内循环：cas修改工作线程数，同时判断能否添加work
            for (;;) {
                int wc = workerCountOf(c);
                //添加任务前，线程池线程数已达到上限，此时不允许添加。上限分这三种情况：
                // 1、最大支持线程数
                // 2、以core=true提交时，配置的核心线程数。（返回false后，会以core=false再提交一次）
                // 3、以core=false提交时，配置的线程池可容纳最大线程数。
                if (wc >= CAPACITY ||
                        //使用core则上限为核心线程数，否则最大线程数
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                //没超过上限，通过CAS的方式增加worker的数量（+1），增加成功就跳出外层循环
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                //获取最新的线程池状态，与刚开始的状态比较
                c = ctl.get();  // Re-read ctl
                // - 变了，就从外层循环重新执行，重新进行状态的检查。
                // - 没变，从当前循环重新执行，重新执行CAS操作。
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            //创建Worker，并给firstTask赋初值
            w = new Worker(firstTask);
            //拿到属性thread
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                //此处加锁：因为涉及属性：workers、largestPoolSize（可能） 更新
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    //获取线程池最新状态
                    int rs = runStateOf(ctl.get());

                    //如果当前状态是<SHUTDOWN也就是RUNNING状态
                    if (rs < SHUTDOWN ||
                        //或者状态是SHUTDOWN并且当前任务是空的（比如前面说的场景：阻塞队里里面还有，但当前已经是不允许提交的状态了）
                            //  检查Worker线程已经开始跑了。（thread.start()变为alive）
                        (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        //增加worker
                        workers.add(w);
                        //获取最新worker的总数，比较并更新largestPoolSize
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        //表示添加worker成功
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    //启动worker线程。该线程会一直循环执行getTask()，直至返回null，线程才结束
                    //执行runWorker()
                    t.start();
                    //表示线程已经跑起来了
                    workerStarted = true;
                }
            }
        } finally {
            if (! workerStarted)
                //worker线程没成功启动，进入失败处理逻辑
                addWorkerFailed(w);
        }
        //返回当前worker是否启动成功。
        return workerStarted;
    }
```

**addWorker()总结：**

1. 检查线程池状态以确定能否提交任务
2. 校验能否以核心线程的方式提交任务
3. 线程池的状态是SHUTDOWN并且任务队列不是空，允许增加一个线程来帮助队列中的任务跑完，但不会提交任务
4. 更新线程池线程数
5. 超过线程池线程数峰值则**更新峰值（largestPoolSize）**
6. 加锁（mainLock）来更新
7. 启动worker线程

### **runWorker**

```java
    /**
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     *
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     *
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * @param w the worker
     *          执行任务
     */
    final void runWorker(Worker w) {
        //runWorker()是由Worker.run()调用，因此wt就是worker线程
        Thread wt = Thread.currentThread();
        //拿到firstTask并赋值给局部变量task
        Runnable task = w.firstTask;
        //firstTask置空
        w.firstTask = null;
        //将state设置为0。因为构造函数设成-1，在执行任务前置为0。
        w.unlock(); // allow interrupts
        //标识任务是不是立刻就完成了。
        boolean completedAbruptly = true;
        try {
            //循环：先执行firstTask（不为空），后续通过getTask()获取任务。
            while (task != null || (task = getTask()) != null) {
                //任务执行前加锁，任务完成后解锁。
                //任何地方可通过判断锁状态来确认worker是否执行中
                //加锁。防止任务在执行过程中被中断。
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                //判断目的：确保线程池当状态值大于等于 STOP 时有向线程发起过中断请求【调用了shutdownNow()】
                // 两种情况:
                //1)如果当前线程池的状态是>=Stop的，并且当前线程没有被中断，那么就要执行中断。
                //2)或者当前线程目前是已中断的状态并且线程池的状态也是>=Stop的（注意Thread.interrupted是会擦除中断标识符的），
                // 那么因为中断标识符已经被擦除了，那么!wt.isInterrupted()一定返回true，这个时候还是要将当前线程中断。
                // 第二次执行runStateAtLeast(ctl.get(), STOP)相当于一个二次检查
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    //中断worker线程 。因为线程池将要终止了，所以这里没有从workerSet移除当前线程
                    wt.interrupt();
                try {
                    //前置操作，空方法，可以业务自己实现
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        //执行任务：就是执行通过execute()提交的Runnable
                        //第一个是firstTask，后面的是通过getTask()拿到的任务
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        //后置操作,空方法,可以业务自己实现
                        afterExecute(task, thrown);
                    }
                } finally {
                    //最后将task置为null，触发while循环的条件getTask()
                    task = null;
                    //已完成的任务计数器+1
                    w.completedTasks++;
                    //释放当前线程的独占锁
                    w.unlock();
                }
            }
            //当第一个try的代码块有异常， completedAbruptly = false 不生效。
            // 最后completedAbruptly为true表示发生未知异常了
            completedAbruptly = false;
        } finally {
            //getTask返回null时，执行任务退出
            //completedAbruptly=true表示是突然退出的
            processWorkerExit(w, completedAbruptly);
        }
    }

```

**runWorker()总结**：

执行任务前**先判断线程池是否是STOPING状态**，是则中断worker线程。

执行任务：**先执行firstTask，再从任务队列获取执行**

**如果没有任务，调用processWorkerExit()来执行线程退出**的工作。

只要还有任务，worker线程就一直执行任务，并刷新completedTasks

## **getTask**

```java
    /**
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     *    a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     *    workers are subject to termination (that is,
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *    both before and after the timed wait, and if the queue is
     *    non-empty, this worker is not the last thread in the pool.
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?

        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            //1、先判断能否获取到任务

            // 1)如果线程池的状态是>=STOP状态,这个时候不再处理队列中的任务,并且减少worker记录数量,
            // 返回的任务为null,这个时候在runRWorker方法中会执行processWorkerExit进行worker的退出操作.
            // 2)如果线程池的状态是>=SHUTDOWN并且workQueue为空,就说明处于SHOTdown以上的状态下,
            // 且没有任务在等待,那么也属于获取不到任务,getTask返回null.
            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                //扣减线程池线程数，在processWorkerExit()处理线程退出
                decrementWorkerCount();
                return null;
            }
            //获取当前wokrer的数量
            int wc = workerCountOf(c);

            // Are workers subject to culling?
            //以下涉及空闲线程是否会被线程池销毁的处理逻辑

            // 线程超时处理前置条件：开启核心线程超时 或 线程池线程数大于核心线程数
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            //线程超时处理的进一步判断：
            // 线程池线程数超过maximumPoolSize 或者 线程设置允许超时且当前worker取任务超时
            //并且
            // 线程池大小不是零或阻塞队列是空的),这种就返回null,并减少线程池线程计数

            // 1、 (wc>maximumPoolSize)  && (wc>1)  一般情况，线程池线程数会少于配置的最大线程数，
            // 但在addWork中 状态=shutdown且队列不为空时，会创建一个Worker，此时可能导致wc>maximumPoolSize，这里同时限定wc>1。因此线程池减少1个线程也不影响任务的执行【processWorkerExit()会保证还有任务就至少留有1个worker线程】。
            // 2、 (wc>maximumPoolSize) && (workQueue.isEmpty()) 没有任务了，扣减更不影响
            // 3 、(timed && timedOut) && (wc > 1) 超时了，先扣减再说
            // 4 、(timed && timedOut) && (workQueue.isEmpty()) 超时了&队列没有任务，必须要扣减
            if ((wc > maximumPoolSize || (timed && timedOut))
                && (wc > 1 || workQueue.isEmpty())) {
                //这里为啥不用decrementWorkerCount()呢,上面使用decrementWorkerCount()是因为确定不管是什么情况下,数量都要减,多减一次也没事,因为这个时候就是要关闭线程池释放资源
                //这里不一样,线程池的状态可能是RUNNING状态,多减一次,可能导致获取不到worker去跑
                if (compareAndDecrementWorkerCount(c))
                    //扣减线程池线程数，在processWorkerExit()处理线程退出
                    return null;
                //扣减失败， 跳出本次循环重新检查
                continue;
            }
            //从队列中获取任务
            //符合【线程超时处理前置条件】时用poll设置超时时间，不符合就使用take（阻塞直至有返回）
            try {
                Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();
                if (r != null)
                    //task不为空，此处返回task
                    return r;
                timedOut = true;
                // 此处，r == null，肯定是poll操作超时了（注意，不代表队列空了），继续for循环，
                // 回到if ((wc > maximumPoolSize || (timed && timedOut)) 这个地方退出循环
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }
    
    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     */
    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }
```

**getTask()总结：**

从**workQueue中获取一个任务并返回**

**没有获取到任务就扣减线程池线程数**。获取不到任务的四种情况：

​       线程池的状态是>=STOP

​       线程池的状态是SHUTDOWN并且任务队列为空

​       获取任务超时

​       线程池线程数大于maximumPoolSize并且队列为空

## **processWorkerExit**

```java
/**
 * Performs cleanup and bookkeeping for a dying worker. Called
 * only from worker threads. Unless completedAbruptly is set,
 * assumes that workerCount has already been adjusted to account
 * for exit.  This method removes thread from worker set, and
 * possibly terminates the pool or replaces the worker if either
 * it exited due to user task exception or if fewer than
 * corePoolSize workers are running or queue is non-empty but
 * there are no workers.
 *
 * @param w the worker
 * @param completedAbruptly if the worker died due to user exception
 *  worker线程没有拿到任务，成为空闲线程。该方法对空闲线程进一步处理
 */
private void processWorkerExit(Worker w, boolean completedAbruptly) {
    //如果completedAbruptly为true，则说明线程执行时出现异常，需要将workerCount数量减一
    //如果completedAbruptly为false，说明在getTask方法中已经对workerCount进行减一，这里不用再减
    if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
        decrementWorkerCount();

    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //更新已完成任务的数量的统计项
        completedTaskCount += w.completedTasks;
        //从worker集合中移除该worker
        workers.remove(w);
    } finally {
        mainLock.unlock();
    }
    //尝试关闭线程池,但如果是正常运行状态,就不会关闭
    tryTerminate();

    int c = ctl.get();
    //1、线程池是SHUTDOWN或RUNNING（如果不是这两个状态,说明线程已经停止了，不做任何操作）
    if (runStateLessThan(c, STOP)) {
        //2、线程正常结束
        if (!completedAbruptly) {
            // 如果没有开启核心线程超时配置，则至少保留corePoolSize个线程；
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
            //如果允许核心线程超时并且当前队列里面还有任务没跑，必须留1个线程,不能全死掉.
            if (min == 0 && ! workQueue.isEmpty())
                min = 1;
            // 如果线程池数量>=最少预留线程数
            if (workerCountOf(c) >= min)
                // 线程自然结束了，不用补充worker
                return; // replacement not needed
        }
        // 1、执行任务异常结束的，补充worker
        // 2、如果线程池数量<最少预留线程数，补充worker
        //异常结束 增加worker
        addWorker(null, false);
        //注: 别问我为啥上面要删除worker,还要再加,不删是不是不用加了.
        // 明确下那个任务已经退出getTask那块的死循环了,永远回不去了,只能新增worker.
    }
}
```

**processWorkerExit()方法总结：**

当Worker线程结束前，完成以下工作：扣减线程池线程数（**ctl**）、更新已完成任务数（**completedTaskCount**）、Worker集合中移除一个Worker（**workers**）、**尝试终止线程池**、计算线程池的**最少保留线程数**、根据最少保留线程数来确定**是否补充一个Worker**。

关于最少保留线程数：**如果没有开启核心线程超时配置，则至少保留corePoolSize个线程；如果开启核心线程超时并且当前队列里面还有任务，只需保留1个线程**；

需要**补充worker**的两种情况：1、**线程池线程数<最少保留线程数** 2、**任务执行异常结束**

## **tryTerminate**

```java
    /**
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     * 尝试终止线程池
     */
    final void tryTerminate() {
        //cas自旋 确保更新成功
        for (;;) {
            int c = ctl.get();
            //RUNNING状态,不能终止线程池
            //线程池状态是TIDYING或TERMINATED说明线程池已经处于正在终止的路上,不用再终止了.
            //状态为SHUTDOWN，但是任务队列不为空,也不能终止线程池
            if (isRunning(c) ||
                runStateAtLeast(c, TIDYING) ||
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                return;
            //调用shutdown()或者shutdownNow()方法时，执行以下处理

            //工作线程数量不等于0，中断一个空闲的工作线程并返回
            //这个时候线程池一定是 1、STOP的状态或者 2、SHUTDOW且队列为空  这两种情况中断一个空闲worker
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {

                // 设置线程池状态为TIDYING，如果设置成功，则调用terminated()
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        //钩子方法，子类实现。默认什么都不做
                        terminated();
                    } finally {
                        // 设置状态为TERMINATED
                        ctl.set(ctlOf(TERMINATED, 0));
                        //唤醒阻塞等待的线程 （future的场景）
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }
```

**tryTerminate()总结**

1. 尝试终止线程池

2. 不能终止线程池：

3. 1. 状态是RUNNING，不能直接终止（如果是调用shutdown()，shutdownNow()，会先将状态改为SHUTDOWN）
   2. 状态是TIDYING或者TERMINATED，不能终止（因为已经处于终止过程中）
   3. 状态是SHUTDOWN并且任务队列不为空，不能终止（因为还有任务要处理）

4. 可以终止线程池：

5. 1. 状态是SHUTDOWN并且任务队列为空
   2. 状态是STOP

6. 符合可以终止线程池的条件下，如果线程池线程数不等于0，那就中断1个Worker线程，不修改线程池状态

7. 符合可以终止线程池的条件下，并且线程池线程数等于0，那就将线程池状态改为TIDYING，执行完钩子方法terminated()后状态再改为TERMINATED

interruptIdleWorkers(ONLY_ONE); 是否好奇为啥这里只中断一个worker呢, 这里就涉及到了线程池的优雅退出了.

当执行到 interruptIdleWorkers(ONLY_ONE) 前面的时候, 线程池只能处于两种状态:

1. STOP 状态 , 这个时候 workQueue 可能是有值的 , workQueue 在清空的过程中了.
2. SHUTDOWN 状态并且 workQueue 是空的 .

这两种状态都是说明, 线程池即将关闭, 或者说空闲的线程此时已经没用了,这个时候随手关一个, 反正要关,早关晚关而已

## **interruptIdleWorker**

```java
    /**
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     * called only from tryTerminate when termination is otherwise
     * enabled but there are still other workers.  In this case, at
     * most one waiting worker is interrupted to propagate shutdown
     * signals in case all threads are currently waiting.
     * Interrupting any arbitrary thread ensures that newly arriving
     * workers since shutdown began will also eventually exit.
     * To guarantee eventual termination, it suffices to always
     * interrupt only one idle worker, but shutdown() interrupts all
     * idle workers so that redundant workers exit promptly, not
     * waiting for a straggler task to finish.
     *  中断一个或多个线程
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //遍历worker，根据onlyOne判断，如果为ture只中断一个线程
            for (Worker w : workers) {
                Thread t = w.thread;
                //线程没有被中断并且线程是空闲状态
                //通过tryLock实现：不能中断还没有开始执行或者还在执行中的worker线程。
                //线程未启动：-1 ，线程正在执行：1  ，trylock：0->1 ; 
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        //中断操作，之后该线程就结束了
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }
```

**interruptIdleWorker()总结：**

1. 从worker集合中遍历并**中断worker线程**
2. 只有worker线程状态是0的，才能够中断（**不能中断未启动或者还在执行中的Worker线程**）

## **shutdown**

```java
    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException {@inheritDoc}
     * 初始化一个有序的关闭，之前提交的任务都会被执行，但是新提交的任务则不会被允许放入任务队列中。
     * 如果之前被调用过了的话，那么再次调用也没什么用
     */
    public void shutdown() {
        //mainLock是全局变量，加锁确保不会并发关闭线程池
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //安全策略判断。方法检查每一个线程池的线程是否有可以ShutDown的权限。
            checkShutdownAccess();
            //CAS自旋把ctl中的状态从RUNNING变为SHUTDOWN
            advanceRunState(SHUTDOWN);
            //中断所有空闲线程
            interruptIdleWorkers();
            // 方法告知子类，线程池要处于ShutDown状态了 ，ScheduledThreadPoolExecutor预留的钩子
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        //尝试终止线程池
        tryTerminate();
    }
```

**shutdown()方法总结**

1. 执行shutdown()方法：**关闭线程池，不再接受新的任务，已提交执行的任务继续执行**。
2. 调用interruptIdleWorkers()先**中断所有空闲线程**
3. 调用tryTerminate()尝试终止线程池
4. shutdown()**将线程池状态改为SHUTDOWN但不是STOP**

## **shutdownNow**

```java
    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * @throws SecurityException {@inheritDoc}
     * 关闭线程池，不再接受新的任务，正在执行的任务尝试终止
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            //线程池的状态置为STOP
            advanceRunState(STOP);
            interruptWorkers();
            //将剩余任务返回
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }
    
    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //循环所有的worker
            for (Worker w : workers)
                //已经启动的线程直接执行中断
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }
```

`Worker.java  `

```java
 Worker.java  
    
      //向线程发起中断请求
    // 符合：1、运行中的；2、没有处于中断   才能中断
    void interruptIfStarted() {
        Thread t;
        //只有刚刚构建的worker的时候,状态state值是-1(这里也能体现刚构建的worker无法被中断),其他情况都是>=0的
        if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
            try {
                t.interrupt();
            } catch (SecurityException ignore) {
            }
        }
    }
```

ShutDownNow()方法总结

- 1. 关闭线程池，不再接受新的任务，**中断已经启动的Worker线程**
  2. 将线程池状态改为STOP
  3. 返回未完成的任务队列

## **isShutdown**

```java
    //确认线程池是否关闭。判断状态是不是RUNNING.
    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }
```

## **prestartCoreThread**

```java
    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);
    }
```

1. 启动一个空闲的线程作为核心线程
2. 如果核心线程数已到阈值, 会加入失败, 返回false, 如果线程池处于SHUTDOWN以上的状态也返回false
3. 只有真正这个线程调用start方法跑起来, 才会返回true

## **prestartAllCoreThreads**

```java
    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     *
     * @return the number of threads started
     * 启动所有核心线程，使他们等待获取任务
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        //null代表空闲线程,true代表是增加的是核心线程
        while (addWorker(null, true))
            //死循环增加空闲 worker 而已
            ++n;
        return n;
    }
```

