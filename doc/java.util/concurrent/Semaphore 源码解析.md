
# semaphore 实现

使用 Semaphore 限流，在访问高峰期时，让请求线程阻塞，高峰期过去再释放许可，当然它只适合限制单机线程数量，并且仅是限制线程数，而不是限制资源数（例如连接数，请对比 Tomcat LimitLatch 的实现可以限制连接的资源个数）
用 Semaphore 实现简单连接池，对比使用object的wait notify，性能和可读性显然更好，

```java
package com.dongguo.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


@Slf4j(topic = "d.SemaphoreTest")
public class SemaphoreTest {
    public static void main(String[] args) {
        // 1. 创建 semaphore 对象
        Semaphore semaphore = new Semaphore(3);

        // 2. 10个线程同时运行
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    log.debug("running...");
                    TimeUnit.SECONDS.sleep(1);
                    log.debug("end...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
            }).start();
        }
    }
}
运行结果
21:10:45 [Thread-0] d.SemaphoreTest - running...
21:10:45 [Thread-1] d.SemaphoreTest - running...
21:10:45 [Thread-2] d.SemaphoreTest - running...
21:10:46 [Thread-1] d.SemaphoreTest - end...
21:10:46 [Thread-3] d.SemaphoreTest - running...
21:10:46 [Thread-0] d.SemaphoreTest - end...
21:10:46 [Thread-8] d.SemaphoreTest - running...
21:10:46 [Thread-2] d.SemaphoreTest - end...
21:10:46 [Thread-9] d.SemaphoreTest - running...
21:10:47 [Thread-3] d.SemaphoreTest - end...
21:10:47 [Thread-8] d.SemaphoreTest - end...
21:10:47 [Thread-6] d.SemaphoreTest - running...
21:10:47 [Thread-7] d.SemaphoreTest - running...
21:10:47 [Thread-9] d.SemaphoreTest - end...
21:10:47 [Thread-5] d.SemaphoreTest - running...
21:10:48 [Thread-7] d.SemaphoreTest - end...
21:10:48 [Thread-6] d.SemaphoreTest - end...
21:10:48 [Thread-4] d.SemaphoreTest - running...
21:10:48 [Thread-5] d.SemaphoreTest - end...
21:10:49 [Thread-4] d.SemaphoreTest - end...
```

# 介绍
Semaphore，俗称信号量，它是操作系统PV操作原语在JDK中的实现，同样，它也是基于AbstractQueuedSynchronizer来实现的。

Semaphore通俗理解就是常说的共享锁，是一个控制访问多个共享资源的计数器,它可以定义共享资源的个数，只要共享资源还有，其他线程就可以执行，否则就会被阻塞。
Semaphore，在 API 是这么介绍的：
一个计数信号量。从概念上讲，信号量维护了一个许可集。如有必要，在许可可用前会阻塞每一个 acquire，然后再获取该许可。每个 release 添加一个许可，从而可能释放一个正在阻塞的获取者。但是，不使用实际的许可对象，Semaphore 只对可用许可的号码进行计数，并采取相应的行动。

Semaphore 通常用于限制可以访问某些资源（物理或逻辑的）的线程数目。

```java
public class Semaphore implements java.io.Serializable
```
# 常量&变量

```java
    //序列化版本号
    private static final long serialVersionUID = -3222578661600680210L;
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    /** 所有机制通过AbstractQueuedSynchronizer子类实现 */
    private final Sync sync;
```
# 构造方法
Semaphore 提供了两个构造函数：
1.Semaphore(int permits) ：创建具有给定的许可数和非公平的公平设置的 Semaphore 。
2.Semaphore(int permits, boolean fair) ：创建具有给定的许可数和给定的公平设置的 Semaphore 

```java
    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and nonfair fairness setting.
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     *                指定许可证个数，默认采用非公平版本
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
```
# 内部类
**Semaphore 内部包含公平锁（FairSync）和非公平锁（NonfairSync），继承内部类 Sync ，其中 Sync 继承 AQS**

## Sync

```java
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
```
## NonfairSync

```java
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
```
## FairSync

```java
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
            for (;;) {
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }
```

# 加锁解锁流程  

下面我们以一个停车场的简单例子来阐述 Semaphore ：
为了简单起见我们假设停车场仅有 3 个停车位。一开始停车场没有车辆所有车位全部空着，然后先后到来两辆车，停车场车位够，安排进去停车，然后又来三辆，这个时候由于只有一个停车位，所有只能停一辆，其余两辆必须在外面候着，直到停车场有空车位。当然，以后每来一辆都需要在外面候着。当停车场有车开出去，里面有空位了，则安排一辆车进去（至于是哪辆，要看选择的机制是公平还是非公平）。
从程序角度看，停车场就相当于信号量 Semaphore ，其中许可数为 3 ，车辆就相对线程。当来一辆车时，许可数就会减 1 。当停车场没有车位了（许可数 == 0 ），其他来的车辆需要在外面等候着。如果有一辆车开出停车场，许可数 + 1，然后放进来一辆车。
信号量 Semaphore 是一个非负整数（ >=0 ）。当一个线程想要访问某个共享资源时，它必须要先获取 Semaphore。当 Semaphore > 0 时，获取该资源并使 Semaphore – 1 。如果Semaphore 值 = 0，则表示全部的共享资源已经被其他线程全部占用，线程必须要等待其他线程释放资源。当线程释放资源时，Semaphore 则 +1 。
模拟停车场有3个停车位，有5辆车要去停车

```java
public class SemaphoreTest {

    static class Parking {
    
        //信号量
        private Semaphore semaphore;

        Parking(int count) {
            semaphore = new Semaphore(count);
        }

        public void park() {
            try {
                //获取信号量
                semaphore.acquire();
                long time = (long) (Math.random() * 10);
                System.out.println(Thread.currentThread().getName() + "进入停车场，停车" + time + "秒..." );
                Thread.sleep(time);
                System.out.println(Thread.currentThread().getName() + "开出停车场...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
    }


    static class Car extends Thread {
        Parking parking ;

        Car(Parking parking){
            this.parking = parking;
        }

        @Override
        public void run() {
            parking.park();     //进入停车场
        }
    }

    public static void main(String[] args){
        Parking parking = new Parking(3);

        for(int i = 0 ; i < 5 ; i++){
            new Car(parking).start();
        }
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/22fade914f794438af9fb07eb308c3f6.png)
 刚开始，permits（state）为 3，这时 5 个线程来获取资源
该构造方法默认为非公平锁


```java
public Semaphore(int permits) {//3
    sync = new NonfairSync(permits);
}
```

非公平锁NonfairSync构造方法

```java
static final class NonfairSync extends Sync {
	NonfairSync(int permits) {
    	super(permits);
	}
    ...
}
```

调用父类的Sync构造方法

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
	Sync(int permits) {
    	setState(permits);
	}
    ...
}
```



![image-20210913155708185](https://img-blog.csdnimg.cn/img_convert/4b34f07c43a0f004f9c9214095803e64.png)

## 获得许可acquire

Semaphore 提供了 acquire() 方法，来获取一个许可
```java
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
```

### acquireSharedInterruptibly
内部调用AbstractQueuedSynchronizer的acquireSharedInterruptibly(int arg)该方法以共享模式获取同步状态
```java
   AbstractQueuedSynchronizer.java
     /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */

    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        //判断线程是否被中断
        if (Thread.interrupted())
            throw new InterruptedException();
        //尝试获取共享锁
        if (tryAcquireShared(arg) < 0)
            //当state=-1
            doAcquireSharedInterruptibly(arg);
    }
```
在 acquireSharedInterruptibly(int arg) 方法中，会调用 tryAcquireShared(int arg) 方法。而 tryAcquireShared(int arg) 方法，由Semaphore的内部类来实现。对于 Semaphore 而言，如果我们选择非公平模式，则调用 NonfairSync 的tryAcquireShared(int arg) 方法，否则调用 FairSync 的 tryAcquireShared(int arg) 方法。若 tryAcquireShared(int arg) 方法返回 < 0 时，则会阻塞等待，从而实现 Semaphore 信号量不足时的阻塞
#### tryAcquireShared(arg)
获取资源失败返回-1，获取资源成功返回剩余资源数
**Semaphore非公平锁实现**

```java
static final class NonfairSync extends Sync {    
    ...
        
    protected int tryAcquireShared(int acquires) {
        return nonfairTryAcquireShared(acquires);
    }
}
```



```java
final int nonfairTryAcquireShared(int acquires) {
    for (;;) {//自旋
        int available = getState();//资源数
        int remaining = available - acquires;//剩余资源数
        //资源不够-1 直接返回 
        if (remaining < 0 ||
            //修改state
            compareAndSetState(available, remaining))
            return remaining;//返回
    }
}
```

获取资源失败返回-1，获取资源成功返回剩余资源数

**Semaphore公平锁实现**
公平锁需要判断当前线程是否位于CLH同步队列列头

```java
        protected int tryAcquireShared(int acquires) {
            //自旋
            for (;;) {
                //判断该线程是否位于CLH队列的列头，从而实现公平锁
                if (hasQueuedPredecessors())
                    return -1;
                //获取当前的信号量许可  资源数
                int available = getState();
                //设置“获得acquires个信号量许可之后，剩余的信号量许可数”剩余资源数
                int remaining = available - acquires;
                //许可资源不够-1 直接返回
                if (remaining < 0 ||
                        //修改state
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
```
##### hasQueuedPredecessors
判断该线程是否位于CLH队列的列头

```java
    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        //尾结点
        Node t = tail; // Read fields in reverse initialization order
        //头节点
        Node h = head;
        Node s;  
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/530e18476eac4fb3abb62e9f4406ce42.png)
返回false的两种情况
①h!=t 为false 即 h == t
表示队列为空或者只有一个元素
只有一个元素：当线程一在执行时，线程二创建了队列，当线程一执行完后唤醒被阻塞的线程二，线程二CAS获取了锁，将自己设置为第一个节点并置空，并将head和tail指针都指向自己，此时队列中只有一个元素。
②h!=t 为true，但是(s = h.next) == null返回false并且s.thread != Thread.currentThread()也返回false
(s = h.next) == null返回false说明头节点的下一个节点不为空，
s.thread != Thread.currentThread()返回false说明当期执行的节点就是头节点的下一个节点


返回true的两种情况
① h != t返回true，(s = h.next) == null返回true
这种情况可能是：线程一获取锁，线程二获取失败，加入到队列中，在构建节点时，将prev指针指向头节点，还没有执行到next指针指向该节点，此时线程一释放锁，线程三尝试获取锁并且调用了hasQueuedPredecessors 
 这里prev指针已经指向了头节点，而next指针未指向该节点，造成队列h != t，但是head.next = null的场景，也就是看起来大于一个节点实际上只有一个节点的假象。

还有一种情况：线程一拿到锁，线程二没有，于是创建队列，线程一释放锁，唤醒线程二，线程二拿到锁，将自己设置为队列头部，并且置空，此时h和t都指向线程二的节点，然后此时线程三来了，在没有拿到锁的情况下开始入队，于是执行addWaiter方法，将线程三节点的prev设置为头结点，将t指向的节点从线程二的空节点指向线程三，此时头部节点还是线程二的空节点，但是t已经指向线程三。下一步就是将首部空节点的next设置为线程三节点，就在这时，线程二也执行完了，释放了锁，又恰好线程四来到tryAcquire，这时state为0无锁，线程四尝试不排队拿锁，此时满足了h != t，且head.next = null的情况，这时线程四就返回了true，表示我必须入队。
②h != t返回true，(s = h.next) == null返回false，s.thread !=Thread.currentThread()返回true。
h != t返回true表示队列中至少有两个不同节点存在。(s = h.next) == null返回false表示首节点是有后继节点的，也就是队列的长度大于1。而s.thread != Thread.currentThread()返回true意味着头结点的下一个节点的线程不是当前执行的线程，所以那当前线程排队还没排到，必须入队。
#### doAcquireSharedInterruptibly(arg)
AbstractQueuedSynchronizer.java
```java
    /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        //①创建一个等待节点  ，共享模式
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            //自旋
            for (;;) {
                // ② 前驱节点
                final Node p = node.predecessor();
                if (p == head) {
                    //尝试获取资源
                    int r = tryAcquireShared(arg);
                    //获取成功
                    if (r >= 0) {
                        //将该节点设置为头节点
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                //tryAcquireShared 返回小于 0 时，则会阻塞等待
                if (shouldParkAfterFailedAcquire(p, node) &&
           
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

#####  addWaiter(Node mode)

```java
    /**
     * Creates and enqueues node for current thread and given mode.
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    private Node addWaiter(Node mode) {
        //创建节点
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        //如果队列没有节点 创建一个哨兵节点
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        //当前节点入队
        enq(node);
        return node;
    }
```

###### enq(node)

```java
    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     * @param node the node to insert
     * @return node's predecessor
     */
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            //尾节点为null 即等待队列为空
            if (t == null) { // Must initialize
                //创建哨兵节点
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }
```

##### shouldParkAfterFailedAcquire(p, node)

```java
    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        //获取当前节点的前置节点的状态
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
            //如果是SIGNAL（-1）状态直接返回true,代表此节点可以挂起
            //因为前置节点状态为SIGNAL在适当状态 会唤醒后继节点
            return true;
        //表示请求取消了，
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            //如果是cancelled
            do {
                //则从后往前依此跳过cancelled状态的节点
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            //将找到的符合标准的节点的后置节点指向当前节点
            pred.next = node;
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            //则将前置节点等待状态设置为SIGNAL-1
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
```

##### parkAndCheckInterrupt()

```java
    /**
     * Convenience method to park and then check if interrupted
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        //阻塞
        LockSupport.park(this);
        return Thread.interrupted();
    }
```

假设其中 Thread-1，Thread-2，Thread-4 cas 竞争成功，而 Thread-0 和 Thread-3 竞争失败，进入 AQS 队列park 阻塞  

![image-20210913155729026](https://img-blog.csdnimg.cn/img_convert/59ad5f03ddc597cacd6b5fafd0551901.png)

# 释放许可release

```java
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
```

## releaseShared(int arg)
内部调用AbstractQueuedSynchronizerreleaseShared方法，释放同步状态
```java
    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     * 共享模式下的释放。如果『tryReleaseShared』返回true的话，会使一个或多个线程重新启动
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            //唤醒阻塞等待许可的线程
            doReleaseShared();
            return true;
        }
        return false;
    }
```

#### tryReleaseShared(arg)

```java
        //尝试释放许可
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                //信号量的许可数 = 当前信号许可数 + 释放的信号许可数
                int next = current + releases;
                //说明信号量的许可书超出最大值
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                //cas修改信号量的许可数    
                if (compareAndSetState(current, next))
                    //表示释放同步状态成功
                    return true;
            }
        }
```

### doReleaseShared()
AbstractQueuedSynchronizer.java
```java
    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        //自旋
        for (;;) {
            //哨兵节点
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    //唤醒后继节点Thread-0
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }
```

#### unparkSuccessor(h)

```java
    /**
     * Wakes up node's successor, if one exists.
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            //唤醒Thread-0
            LockSupport.unpark(s.thread);
    }
```

这时 Thread-4 释放了 permits，状态如下  

![image-20210913155746057](https://img-blog.csdnimg.cn/img_convert/8b957007c34ffb16356fc264ab6dba62.png)

# 唤醒Thread-0
## parkAndCheckInterrupt()

```java
    /**
     * Convenience method to park and then check if interrupted
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        //    Thread-0被唤醒
        LockSupport.park(this);
        return Thread.interrupted();
    }
```
Thread-0在LockSupport.park(this);阻塞，直至被唤醒

## 返回 doAcquireSharedInterruptibly

```java
    /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        //①创建一个等待节点  ，共享模式
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            //自旋
            for (;;) {
                // ② 前驱节点
                final Node p = node.predecessor();
                if (p == head) {
                    //尝试获取资源
                    int r = tryAcquireShared(arg);
                    //获取成功
                    if (r >= 0) {
                        //将node节点设置为新的哨兵节点，并清除node信息
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                //③//前驱节点waitStatus设置为-1
                if (shouldParkAfterFailedAcquire(p, node) &&
                        //进入阻塞
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

### setHeadAndPropagate(node, r)

```java
    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        //将node节点设置为新的哨兵节点，并清除node信息
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            //获取后继节点
            Node s = node.next;
            //后继节点为共享模式
            if (s == null || s.isShared())
                //唤醒后继节点
                doReleaseShared();
        }
    }
```

#### doReleaseShared()

```java
    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        //自旋
        for (;;) {
            //哨兵节点
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    //唤醒后继节点
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }
```

##### unparkSuccessor

```java
    /**
     * Wakes up node's successor, if one exists.
     *
     * @param node the node
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            //唤醒
            LockSupport.unpark(s.thread);
    }
```

# 唤醒Thread-3
## parkAndCheckInterrupt()
```java
private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);//Thread-3被唤醒
    return Thread.interrupted();
}
```
此处Thread-3阻塞，直至被唤醒
## 返回 doAcquireSharedInterruptibly

```java
    /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        //①创建一个等待节点  ，共享模式
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            //自旋
            for (;;) {
                // ② 前驱节点
                final Node p = node.predecessor();
                if (p == head) {
                    //尝试获取资源
                    int r = tryAcquireShared(arg);
                    //获取成功
                    if (r >= 0) {
                        //将node节点设置为新的哨兵节点，并清除node信息
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                //③//前驱节点waitStatus设置为-1
                if (shouldParkAfterFailedAcquire(p, node) &&
                        //进入阻塞
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```





接下来 Thread-0 竞争成功，permits 再次设置为 0，设置自己为 head 节点，断开原来的 head 节点，unpark 接下来的 Thread-3 节点，但由于 permits 是 0，因此 Thread-3 在尝试不成功后再次进入 park 状态  

![image-20210913155806601](https://img-blog.csdnimg.cn/img_convert/bcdac9487d5d54f7c30f56eb22c85b29.png)


