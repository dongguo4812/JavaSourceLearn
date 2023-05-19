/*
 * Copyright (c) 1994, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Class {@code Object} is the root of the class hierarchy.
 * Every class has {@code Object} as a superclass. All objects,
 * including arrays, implement the methods of this class.
 *
 * @author  unascribed
 * @see     java.lang.Class
 * @since   JDK1.0
 */
public class Object {

    private static native void registerNatives();
    static {
        registerNatives();
    }

    /**
     * Returns the runtime class of this {@code Object}. The returned
     * {@code Class} object is the object that is locked by {@code
     * static synchronized} methods of the represented class.
     *
     * <p><b>The actual result type is {@code Class<? extends |X|>}
     * where {@code |X|} is the erasure of the static type of the
     * expression on which {@code getClass} is called.</b> For
     * example, no cast is required in this code fragment:</p>
     *
     * <p>
     * {@code Number n = 0;                             }<br>
     * {@code Class<? extends Number> c = n.getClass(); }
     * </p>
     *
     * @return The {@code Class} object that represents the runtime
     *         class of this object.
     * @jls 15.8.2 Class Literals
     * 返回此对象的运行时类。返回的Class对象是被所表示类的静态同步方法锁定的对象
     */
    public final native Class<?> getClass();

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hash tables such as those provided by
     * {@link java.util.HashMap}.
     * <p>
     * The general contract of {@code hashCode} is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     *     an execution of a Java application, the {@code hashCode} method
     *     must consistently return the same integer, provided no information
     *     used in {@code equals} comparisons on the object is modified.
     *     This integer need not remain consistent from one execution of an
     *     application to another execution of the same application.
     * <li>If two objects are equal according to the {@code equals(Object)}
     *     method, then calling the {@code hashCode} method on each of
     *     the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     *     according to the {@link java.lang.Object#equals(java.lang.Object)}
     *     method, then calling the {@code hashCode} method on each of the
     *     two objects must produce distinct integer results.  However, the
     *     programmer should be aware that producing distinct integer results
     *     for unequal objects may improve the performance of hash tables.
     * </ul>
     * <p>
     * As much as is reasonably practical, the hashCode method defined by
     * class {@code Object} does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java&trade; programming language.)
     *
     * @return  a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.lang.System#identityHashCode
     * 1.hashCode()的返回值：返回对象的hashCode值，通常是通过将对象的内部地址转换为整数来实现的，
     *  但是Jav语言并没有要求它的实现技术。
     * 2.hashCode()的作用：为了配合基于散列的集合一起正常运行，
     * 这样的散列集合包括HashSet/HashMp以及HashTable。
     * 3.hashCode的一般规定是：
     *    (1) 在Java应用程序中，对同一对象多次调用hashCode()的结果返回相同的整数。
     *        默认的hashCode()的返回值被称作identity hash code，它使用内存地址的整数形式表示。
     *    (2) 如果两个对象相等(使用equals()方法比较)，则两个对象拥有相同的hashCode值。
     *    (3) 如果两个对象不相等(使用equals()方法比较)，那么这两个对象的hashCode值也不相等。不同的
     *        hashCode值可以提高hash表的利用率。
     *4.hashCode生成的六种方式：
     *    (1) 随机生成数字；
     *    (2) 根据对象的内存地址生成；
     *    (3) 对“根据内存地址生成”方式进行硬编码（用于敏感性测试）；
     *    (4) 一个序列；
     *    (5) 对象内存地址，强制转换成int；
     *    (6) 使用线程状态结合xorshift算法生成。
     */
    public native int hashCode();

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     *     {@code x}, {@code x.equals(x)} should return
     *     {@code true}.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     *     {@code x} and {@code y}, {@code x.equals(y)}
     *     should return {@code true} if and only if
     *     {@code y.equals(x)} returns {@code true}.
     * <li>It is <i>transitive</i>: for any non-null reference values
     *     {@code x}, {@code y}, and {@code z}, if
     *     {@code x.equals(y)} returns {@code true} and
     *     {@code y.equals(z)} returns {@code true}, then
     *     {@code x.equals(z)} should return {@code true}.
     * <li>It is <i>consistent</i>: for any non-null reference values
     *     {@code x} and {@code y}, multiple invocations of
     *     {@code x.equals(y)} consistently return {@code true}
     *     or consistently return {@code false}, provided no
     *     information used in {@code equals} comparisons on the
     *     objects is modified.
     * <li>For any non-null reference value {@code x},
     *     {@code x.equals(null)} should return {@code false}.
     * </ul>
     * <p>
     * The {@code equals} method for class {@code Object} implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values {@code x} and
     * {@code y}, this method returns {@code true} if and only
     * if {@code x} and {@code y} refer to the same object
     * ({@code x == y} has the value {@code true}).
     * <p>
     * Note that it is generally necessary to override the {@code hashCode}
     * method whenever this method is overridden, so as to maintain the
     * general contract for the {@code hashCode} method, which states
     * that equal objects must have equal hash codes.
     *
     * @param   obj   the reference object with which to compare.   需要比较的引用对象
     * @return  {@code true} if this object is the same as the obj
     *          argument; {@code false} otherwise.  如果这个对象和传递的参数对象是相同的，返回true; 否则返回false
     * @see     #hashCode()
     * @see     java.util.HashMap
     * 表明其他的类是否“等于”这个类.这个方法实现了对非空对象引用的等价性。
     *   对于任意的非空引用，它有以下几个特性：
     *   (1) 自反性：x.equals(y) 和 y.equals(x)的返回值是相同的;
     *   (2) 对称性：当且仅当 y.equals(x) 返回trues时，x.equals(y) 才返回true，反之亦成立;
     *   (3) 传递性：如果x.equals(y)==true，y.equals(z)==true，那么x.equals(z)==true；
     *              如果x.equals(y)==false，y.equals(z)==false，那么x.equals(z)==false;
     *   (4) 一致性：在x，y没有被修改的前提下，多次调用x.equals(y)的返回结果是相同的;
     *   (5) 另外，对于任意非空引用x，x.equals(null)返回false.
     *
     *   请注意，每当重写此方法时，通常都需要重写hashCode方法，以便维护hashCode方法的一般约定，
     *   该约定声明相等的对象必须具有相等的哈希代码。
     *
     */
    public boolean equals(Object obj) {
        //实质上是==的判断，判断两个对象的内存地址是否相等
        return (this == obj);
    }

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object {@code x}, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be {@code true}, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be {@code true}, this is not an absolute requirement.
     * <p>
     * By convention, the returned object should be obtained by calling
     * {@code super.clone}.  If a class and all of its superclasses (except
     * {@code Object}) obey this convention, it will be the case that
     * {@code x.clone().getClass() == x.getClass()}.
     * <p>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by {@code super.clone} before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by {@code super.clone}
     * need to be modified.
     * <p>
     * The method {@code clone} for class {@code Object} performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface {@code Cloneable}, then a
     * {@code CloneNotSupportedException} is thrown. Note that all arrays
     * are considered to implement the interface {@code Cloneable} and that
     * the return type of the {@code clone} method of an array type {@code T[]}
     * is {@code T[]} where T is any reference or primitive type.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p>
     * The class {@code Object} does not itself implement the interface
     * {@code Cloneable}, so calling the {@code clone} method on an object
     * whose class is {@code Object} will result in throwing an
     * exception at run time.
     *
     * @return     a clone of this instance.
     * @throws  CloneNotSupportedException  if the object's class does not
     *               support the {@code Cloneable} interface. Subclasses
     *               that override the {@code clone} method can also
     *               throw this exception to indicate that an instance cannot
     *               be cloned.
     * @see java.lang.Cloneable
     * 创建并返回对象的副本。创建的副本取决于这个对象的类型，一般情况如下：
     *   1、克隆对象不等于被克隆的对象：x.clone() != x, x.clone().equals(x) == false;
     *   2、克隆对象和被克隆的对象的class是相等的：person.getClass() == clone.getClass()为true;
     *       如果重写时，自定义了对象的clone()方法的返回值内容，
     *         则不能保证克隆对象和被克隆对象的class是相等的;
     *   3、重写clone()方法必须实现Cloneable接口，否则运行时会抛出“CloneNotSupportedException”异常，
     *       所有的数组都实现了Cloneable接口;
     */
    protected native Object clone() throws CloneNotSupportedException;

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return  a string representation of the object.
     * toString()方法返回一个实例对象的字符串表现形式。
     * 在没有重写toString方法的情况下，对象的toString的返回值有以下几种情况：
     * （1）对象为String类型：返回该String类型的值；如String s = "abcd"，s.toString()的值为
     *      String s = "abcd";
     *      System.out.println(s.toString());  // "abc"。
     * （2）封装的基本类型：基本类型的文本形式即为toString的返回值
     *      Integer i = new Integer(0);
     *      System.out.println(i.toString());    // "0"(字符串)
     * （3）对象为数组：
     *      基本类型数组：
     *        int[] intArr1 = new int[0];
     *        System.out.println(intArr1.toString());  // [I@7f31245a, 此值和数组大小无关
     *        int[][] intArr2 = new int[0][0];
     *        System.out.println(intArr2.toString());  // [[I@6d6f6e28
     *      封装数据类型：
     *        Integer[] IntArr = new Integer[0];
     *        System.out.println(IntArr.toString());   // [Ljava.lang.Integer;@135fbaa4
     *        Integer[][] IntArr2 = new Integer[0][];
     *        System.out.println(IntArr2.toString());  // [[Ljava.lang.Integer;@45ee12a7
     *     对象数组类型：
     *        返回值为 描述符+getClass().getName+";"+"@"+对象的hashCode的十六进制形式
     *        Person[] pArr = new Person[1];    // Person是自定义的对象，Java的其他对象同理
     *        System.out.println(pArr.toString());
     *          // [Lcom.david.object.object.Person;@677327b6
     *        Person[][] pArr2 = new Person[1][1];
     *        System.out.println(pArr2.toString());
     *          // [[Lcom.david.object.object.Person;@14ae5a5
     * （4）非数组非基本数据封装类型的对象类型：
     *      返回getClass().getName() + "@" + Integer.toHexString(hashCode())
     *      Person p = new Person();
     *      System.out.println(p.toString());//com.dongguo.object.Person@74a14482
     */
    public String toString() {
        // 返回类的全限定名+"@"+由对象内存地址计算出来的hashCode的十六进制形式
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * Wakes up a single thread that is waiting on this object's
     * monitor. If any threads are waiting on this object, one of them
     * is chosen to be awakened. The choice is arbitrary and occurs at
     * the discretion of the implementation. A thread waits on an object's
     * monitor by calling one of the {@code wait} methods.
     * <p>
     * The awakened thread will not be able to proceed until the current
     * thread relinquishes the lock on this object. The awakened thread will
     * compete in the usual manner with any other threads that might be
     * actively competing to synchronize on this object; for example, the
     * awakened thread enjoys no reliable privilege or disadvantage in being
     * the next thread to lock this object.
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. A thread becomes the owner of the
     * object's monitor in one of three ways:
     * <ul>
     * <li>By executing a synchronized instance method of that object.
     * <li>By executing the body of a {@code synchronized} statement
     *     that synchronizes on the object.
     * <li>For objects of type {@code Class,} by executing a
     *     synchronized static method of that class.
     * </ul>
     * <p>
     * Only one thread at a time can own an object's monitor.
     *
     * @throws  IllegalMonitorStateException  if the current thread is not
     *               the owner of this object's monitor.
     * @see        java.lang.Object#notifyAll()
     * @see        java.lang.Object#wait()
     * 唤醒一个正在等待此对象监听器的线程。如果有多个线程正在等待该线程的监听器，则由实现者决定随机唤
     * 醒其中一个线程，线程通过调用wait方法来等待对象的监视器。一次只能有一个线程拥有对象的监视器。
     * 此方法只能被拥有该对象监听器的对象所调用，对象一般通过以下三种情况成为监听器的所有者：
     * 1、通过执行该对象的同步实例方法;
     * 2、通过执行在对象上同步的synchronized语句的主体;
     * 3、对于Class类型的对象，执行该类的同步静态方法.
     */
    public final native void notify();

    /**
     * Wakes up all threads that are waiting on this object's monitor. A
     * thread waits on an object's monitor by calling one of the
     * {@code wait} methods.
     * <p>
     * The awakened threads will not be able to proceed until the current
     * thread relinquishes the lock on this object. The awakened threads
     * will compete in the usual manner with any other threads that might
     * be actively competing to synchronize on this object; for example,
     * the awakened threads enjoy no reliable privilege or disadvantage in
     * being the next thread to lock this object.
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the {@code notify} method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @throws  IllegalMonitorStateException  if the current thread is not
     *               the owner of this object's monitor.
     * @see        java.lang.Object#notify()
     * @see        java.lang.Object#wait()
     * 唤醒此对象监视器上等待的所有线程。
     */
    public final native void notifyAll();

    /**
     * Causes the current thread to wait until either another thread invokes the
     * {@link java.lang.Object#notify()} method or the
     * {@link java.lang.Object#notifyAll()} method for this object, or a
     * specified amount of time has elapsed.
     * <p>
     * The current thread must own this object's monitor.
     * <p>
     * This method causes the current thread (call it <var>T</var>) to
     * place itself in the wait set for this object and then to relinquish
     * any and all synchronization claims on this object. Thread <var>T</var>
     * becomes disabled for thread scheduling purposes and lies dormant
     * until one of four things happens:
     * <ul>
     * <li>Some other thread invokes the {@code notify} method for this
     * object and thread <var>T</var> happens to be arbitrarily chosen as
     * the thread to be awakened.
     * <li>Some other thread invokes the {@code notifyAll} method for this
     * object.
     * <li>Some other thread {@linkplain Thread#interrupt() interrupts}
     * thread <var>T</var>.
     * <li>The specified amount of real time has elapsed, more or less.  If
     * {@code timeout} is zero, however, then real time is not taken into
     * consideration and the thread simply waits until notified.
     * </ul>
     * The thread <var>T</var> is then removed from the wait set for this
     * object and re-enabled for thread scheduling. It then competes in the
     * usual manner with other threads for the right to synchronize on the
     * object; once it has gained control of the object, all its
     * synchronization claims on the object are restored to the status quo
     * ante - that is, to the situation as of the time that the {@code wait}
     * method was invoked. Thread <var>T</var> then returns from the
     * invocation of the {@code wait} method. Thus, on return from the
     * {@code wait} method, the synchronization state of the object and of
     * thread {@code T} is exactly as it was when the {@code wait} method
     * was invoked.
     * <p>
     * A thread can also wake up without being notified, interrupted, or
     * timing out, a so-called <i>spurious wakeup</i>.  While this will rarely
     * occur in practice, applications must guard against it by testing for
     * the condition that should have caused the thread to be awakened, and
     * continuing to wait if the condition is not satisfied.  In other words,
     * waits should always occur in loops, like this one:
     * <pre>
     *     synchronized (obj) {
     *         while (&lt;condition does not hold&gt;)
     *             obj.wait(timeout);
     *         ... // Perform action appropriate to condition
     *     }
     * </pre>
     * (For more information on this topic, see Section 3.2.3 in Doug Lea's
     * "Concurrent Programming in Java (Second Edition)" (Addison-Wesley,
     * 2000), or Item 50 in Joshua Bloch's "Effective Java Programming
     * Language Guide" (Addison-Wesley, 2001).
     *
     * <p>If the current thread is {@linkplain java.lang.Thread#interrupt()
     * interrupted} by any thread before or while it is waiting, then an
     * {@code InterruptedException} is thrown.  This exception is not
     * thrown until the lock status of this object has been restored as
     * described above.
     *
     * <p>
     * Note that the {@code wait} method, as it places the current thread
     * into the wait set for this object, unlocks only this object; any
     * other objects on which the current thread may be synchronized remain
     * locked while the thread waits.
     * <p>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the {@code notify} method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @param      timeout   the maximum time to wait in milliseconds.
     * @throws  IllegalArgumentException      if the value of timeout is
     *               negative.
     * @throws  IllegalMonitorStateException  if the current thread is not
     *               the owner of the object's monitor.
     * @throws  InterruptedException if any thread interrupted the
     *             current thread before or while the current thread
     *             was waiting for a notification.  The <i>interrupted
     *             status</i> of the current thread is cleared when
     *             this exception is thrown.
     * @see        java.lang.Object#notify()
     * @see        java.lang.Object#notifyAll()
     * 作用：使当前线程一直处于等待状态，直到另一个线程执行了这个对象的notify()方法
     *    或者notifyAll()方法，或者经过了指定的时间。当前线程必须拥有这个对象的监视器。
     *    这个方法导致了当前线程（称为T）把自己放到一个这个对象的等待集合中，然后使线程
     *    放弃了此对象上的所有的同步声明。线程T因线程调用而被禁用，处于休眠状态。
     * 唤醒线程的四种方式：
     *    一、其他一些线程为此对象调用了notify()方法，线程T碰巧被选中为要唤醒的线程；
     *    二、其他一些线程为此对象调用了notifyAll()方法；
     *    三、其他一些线程执行了interrupt()方法；
     *    四、指定的时间已经过去。如果超时时间为0，则不考虑实时性，线程只需要等待被唤醒
     * 描述：线程T将从该对象的等待集合中移除，并重新启用线程调度。然后它以通常的方式开始和
     *    其他线程竞争对象上的同步权；一旦它获取了对该对象的控制权，它对该对象的所有同步声明
     *    将会恢复到原来的状态，也就是说，恢复到可以调用wait()方法时的状态。因此，对象和线程
     *    的同步状态在调用wait()方法时完全相同。一个线程可以在不被通知、中断或者超时的时候唤
     *    醒，即所谓的虚假唤醒。虽然这种情况在实践中很少发生，但应用程序必须通过测试本应导致
     *    线程唤醒的条件，并在条件不满足时继续等待来防范这种情况。换句话说，等待应该以循环的
     *    方式出现，比如以下这种形式：
     *     synchronized (obj) {
     *         while (&lt;condition does not hold&gt;)
     *             obj.wait(timeout);
     *         ... // Perform action appropriate to condition
     *     }
     *     （有关此主题的更多信息，请参阅Doug Lea的“Java并发编程（第二版）”中的第3.2.3节
     *     （Addison-Wesley，2000），或Joshua Bloch的“有效Java编程语言指南”
     *     （Addison-Wesley，2001）中的第50项）。
     *
     *    如果当前线程在等待之前或等待期间被任何线程中断，则会抛出InterruptedException。
     *    在还原此对象的锁定状态（如上所述）之前，不会引发此异常。
     *    注意，wait()方法在将当前线程放入到此对象的等待集合中时，只解锁这个对象；在线程等待期
     *    间，当前线程上可能被同步的的任何其他对象都将保持锁定状态。wait()方法只能由拥有此对象
     *    监视器的线程所调用。请参阅notify()方法来了解线程成为所有者监视器的方式。
     *
     * @param   timeout   等待的超时时间（毫秒）.
     * @throws  IllegalArgumentException      如果超时时间非法则抛出此异常
     * @throws  IllegalMonitorStateException  如果当前线程非此对象的监视器拥有者则抛出此异常
     * @throws  InterruptedException 如果任何线程在当前线程等待通知之前或者期间中断了当前线程
     *          则抛出此异常。引发此异常时，当前线程的中断状态将被清除。
     * @see     java.lang.Object#notify()
     * @see     java.lang.Object#notifyAll()
     */
    public final native void wait(long timeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until another thread invokes the
     * {@link java.lang.Object#notify()} method or the
     * {@link java.lang.Object#notifyAll()} method for this object, or
     * some other thread interrupts the current thread, or a certain
     * amount of real time has elapsed.
     * <p>
     * This method is similar to the {@code wait} method of one
     * argument, but it allows finer control over the amount of time to
     * wait for a notification before giving up. The amount of real time,
     * measured in nanoseconds, is given by:
     * <blockquote>
     * <pre>
     * 1000000*timeout+nanos</pre></blockquote>
     * <p>
     * In all other respects, this method does the same thing as the
     * method {@link #wait(long)} of one argument. In particular,
     * {@code wait(0, 0)} means the same thing as {@code wait(0)}.
     * <p>
     * The current thread must own this object's monitor. The thread
     * releases ownership of this monitor and waits until either of the
     * following two conditions has occurred:
     * <ul>
     * <li>Another thread notifies threads waiting on this object's monitor
     *     to wake up either through a call to the {@code notify} method
     *     or the {@code notifyAll} method.
     * <li>The timeout period, specified by {@code timeout}
     *     milliseconds plus {@code nanos} nanoseconds arguments, has
     *     elapsed.
     * </ul>
     * <p>
     * The thread then waits until it can re-obtain ownership of the
     * monitor and resumes execution.
     * <p>
     * As in the one argument version, interrupts and spurious wakeups are
     * possible, and this method should always be used in a loop:
     * <pre>
     *     synchronized (obj) {
     *         while (&lt;condition does not hold&gt;)
     *             obj.wait(timeout, nanos);
     *         ... // Perform action appropriate to condition
     *     }
     * </pre>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the {@code notify} method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @param      timeout   the maximum time to wait in milliseconds.
     * @param      nanos      additional time, in nanoseconds range
     *                       0-999999.
     * @throws  IllegalArgumentException      if the value of timeout is
     *                      negative or the value of nanos is
     *                      not in the range 0-999999.
     * @throws  IllegalMonitorStateException  if the current thread is not
     *               the owner of this object's monitor.
     * @throws  InterruptedException if any thread interrupted the
     *             current thread before or while the current thread
     *             was waiting for a notification.  The <i>interrupted
     *             status</i> of the current thread is cleared when
     *             this exception is thrown.
     * 作用：使当前线程处于等待状态，直到另外一个线程执行了此对象的notify()方法或者notifyAll()
     *    方法，或者其他的一些线程中断了当前线程，或者已经过了设定的超时时间。此方法类似于
     *    wait(long)方法，在放弃之前等待通知的时间量，它允许更好的控制。实时量（以纳秒为单位）
     *    由下列式子给出：1000000*timeout+nanos。在其他所有方面，这个方法和wait(long)作用
     *    相同。特别地，wait(0,0)和wait(0)意思相同。
     * 当前线程释放此监视器的所有权的两种情况（当前线程必须拥有此对象的监视器）：
     *    一、另外一个线程通过调用notify()方法或者notifyAll()方法通知等待此对象监视器唤醒线程；
     *    二、超时时间（超时毫秒数+纳秒参数）已过。
     *    除了以上两种情况，这个线程将会一直等待，直到它重新获得监视器并恢复执行。
     * 描述：在wait(long)方法中，中断和虚假唤醒是可能发生的，此方法应该始终遵循在循环中使用：
     *     synchronized (obj) {
     *         while (&lt;condition does not hold&gt;)
     *             obj.wait(timeout, nanos);
     *         ... // Perform action appropriate to condition
     *     }
     *    此方法只能被拥有此对象监视器的线程所调用，请参阅notify方法，以了解线程成为
     *    监视器所有者的方式。
     *
     * @param   timeout   等待的超时时间（毫秒）.
     * @param   nanos     额外时间，范围0-999999纳秒
     * @throws  IllegalArgumentException  如果超时时间非法或nanos不在0-999999的范围内
     *                                    则抛出此异常.
     * @throws  IllegalMonitorStateException  如果当前线程非此对象的监视器拥有者则抛出此异常.
     * @throws  InterruptedException 如果任何线程在当前线程等待通知之前或者期间中断了当前线程
     *          则抛出此异常。引发此异常时，当前线程的中断状态将被清除。
     */
    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeout++;
        }

        wait(timeout);
    }

    /**
     * Causes the current thread to wait until another thread invokes the
     * {@link java.lang.Object#notify()} method or the
     * {@link java.lang.Object#notifyAll()} method for this object.
     * In other words, this method behaves exactly as if it simply
     * performs the call {@code wait(0)}.
     * <p>
     * The current thread must own this object's monitor. The thread
     * releases ownership of this monitor and waits until another thread
     * notifies threads waiting on this object's monitor to wake up
     * either through a call to the {@code notify} method or the
     * {@code notifyAll} method. The thread then waits until it can
     * re-obtain ownership of the monitor and resumes execution.
     * <p>
     * As in the one argument version, interrupts and spurious wakeups are
     * possible, and this method should always be used in a loop:
     * <pre>
     *     synchronized (obj) {
     *         while (&lt;condition does not hold&gt;)
     *             obj.wait();
     *         ... // Perform action appropriate to condition
     *     }
     * </pre>
     * This method should only be called by a thread that is the owner
     * of this object's monitor. See the {@code notify} method for a
     * description of the ways in which a thread can become the owner of
     * a monitor.
     *
     * @throws  IllegalMonitorStateException  if the current thread is not
     *               the owner of the object's monitor.
     * @throws  InterruptedException if any thread interrupted the
     *             current thread before or while the current thread
     *             was waiting for a notification.  The <i>interrupted
     *             status</i> of the current thread is cleared when
     *             this exception is thrown.
     * @see        java.lang.Object#notify()
     * @see        java.lang.Object#notifyAll()
     * 作用：使当前线程处于等待状态，直到另外一个线程执行了此对象的notify()方法或者notifyAll()
     *    方法。换句话说，这个方法的本质就是执行了wait(0)这行代码。
     * 描述：当前线程一定有此对象的监视器。直到另一个线程调用了notify()方法或者notifyAll()方法
     *    通知等待此对象监视器唤醒的线程，此线程释才放此监视器的所有权并等待。这个线程将一直
     *    等待，直到它可以重新获得监视器的所有权并运行。
     *    在wait(long)方法中，中断和虚假唤醒是可能发生的，此方法应该始终遵循在循环中使用：
     *     synchronized (obj) {
     *         while (&lt;condition does not hold&gt;)
     *             obj.wait();
     *         ... // Perform action appropriate to condition
     *     }
     *    此方法只能被拥有此对象监视器的线程所调用，请参阅notify方法，以了解线程成为
     *    监视器所有者的方式。
     *
     * @throws  IllegalMonitorStateException 如果当前线程非此对象的监视器拥有者则抛出此异常.
     * @throws  InterruptedException 如果任何线程在当前线程等待通知之前或者期间中断了当前线程
     *          则抛出此异常。引发此异常时，当前线程的中断状态将被清除。
     * @see        java.lang.Object#notify()
     * @see        java.lang.Object#notifyAll()
     */
    public final void wait() throws InterruptedException {
        wait(0);
    }

    /**
     * Called by the garbage collector on an object when garbage collection
     * determines that there are no more references to the object.
     * A subclass overrides the {@code finalize} method to dispose of
     * system resources or to perform other cleanup.
     * <p>
     * The general contract of {@code finalize} is that it is invoked
     * if and when the Java&trade; virtual
     * machine has determined that there is no longer any
     * means by which this object can be accessed by any thread that has
     * not yet died, except as a result of an action taken by the
     * finalization of some other object or class which is ready to be
     * finalized. The {@code finalize} method may take any action, including
     * making this object available again to other threads; the usual purpose
     * of {@code finalize}, however, is to perform cleanup actions before
     * the object is irrevocably discarded. For example, the finalize method
     * for an object that represents an input/output connection might perform
     * explicit I/O transactions to break the connection before the object is
     * permanently discarded.
     * <p>
     * The {@code finalize} method of class {@code Object} performs no
     * special action; it simply returns normally. Subclasses of
     * {@code Object} may override this definition.
     * <p>
     * The Java programming language does not guarantee which thread will
     * invoke the {@code finalize} method for any given object. It is
     * guaranteed, however, that the thread that invokes finalize will not
     * be holding any user-visible synchronization locks when finalize is
     * invoked. If an uncaught exception is thrown by the finalize method,
     * the exception is ignored and finalization of that object terminates.
     * <p>
     * After the {@code finalize} method has been invoked for an object, no
     * further action is taken until the Java virtual machine has again
     * determined that there is no longer any means by which this object can
     * be accessed by any thread that has not yet died, including possible
     * actions by other objects or classes which are ready to be finalized,
     * at which point the object may be discarded.
     * <p>
     * The {@code finalize} method is never invoked more than once by a Java
     * virtual machine for any given object.
     * <p>
     * Any exception thrown by the {@code finalize} method causes
     * the finalization of this object to be halted, but is otherwise
     * ignored.
     *
     * @throws Throwable the {@code Exception} raised by this method
     * @see java.lang.ref.WeakReference
     * @see java.lang.ref.PhantomReference
     * @jls 12.6 Finalization of Class Instances
     * 作用：Object类的finalize()方法不执行特殊操作，它只是正常的返回。子类可以重写此方法。
     * 执行时机：当垃圾回收器确定不再有对对象的引用时，由对象上的垃圾回收器调用finalize()。
     *    子类重写finalize()方法用来释放系统资源或执行其他清理操作。
     * 一般约定是：虚拟机确定不再有任何方法或者任何尚未终止的线程访问此对象时，将调用finalize()
     *    方法。finalize()方法可以执行任何操作，包括使此对象相对于其他线程可用。然而finalize()
     *    方法通常的目的是，在对象永久丢弃之前执行清理操作。比如，表示输入/输出的对象在永久丢弃
     *    之前，它的finalize()方法可能会执行显示的I/O事物去断开连接。
     * 描述：Java编程语言不能保证哪一个线程将会为给定的对象执行finalize()方法，但是它保证，
     *    此线程在调用finalize()方法时，不会持有任何用户可见的同步锁。如果finalize()方法
     *    引发了未捕获的异常，则该异常将会被忽略，该对象的finalize()方法将会被终止。
     *    在对象调用finalize()方法之后，不会有进一步的操作被执行，直到Java虚拟机再次确定
     *    不再有任何方法可以让任何尚未终止的线程访问该对象，包括其他可能已准备好结束的对象或类，
     *    该对象才可能被丢弃。对于任何给定的对象，该方法都不会被执行多次（至多一次）。
     *    finalize()方法引发的任何异常都会导致此对象的终止，如果没有异常则会被忽略。
     */
    protected void finalize() throws Throwable { }
}
