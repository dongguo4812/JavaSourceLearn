# **介绍**

Throwable类是Java语言中所有错误（errors）和异常（exceptions）的父类，直接子类为 Error 和 Exception。只有继承于Throwable的类或子类才能被抛出，还有一种是Java中的@throw注解类也可以抛出。

```java
public class Throwable implements Serializable
```

# **常量&变量**

```java
    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    private static final long serialVersionUID = -3042686055658047285L;

    /**
     * Native code saves some indication of the stack backtrace in this slot.
     * 保存栈信息的轨迹
     */
    private transient Object backtrace;

    /**
     * Specific details about the Throwable.  For example, for
     * {@code FileNotFoundException}, this contains the name of
     * the file that could not be found.
     *
     * @serial
     * 存储异常的简要说明，可以通过e.getMessage()获得。
     */
    private String detailMessage;

    /**
     * A shared value for an empty stack.
     *   stackTrace的初始值 空数组

     */
    private static final StackTraceElement[] UNASSIGNED_STACK = new StackTraceElement[0];

    /*
     * To allow Throwable objects to be made immutable and safely
     * reused by the JVM, such as OutOfMemoryErrors, fields of
     * Throwable that are writable in response to user actions, cause,
     * stackTrace, and suppressedExceptions obey the following
     * protocol:
     *
     * 1) The fields are initialized to a non-null sentinel value
     * which indicates the value has logically not been set.
     *
     * 2) Writing a null to the field indicates further writes
     * are forbidden
     *
     * 3) The sentinel value may be replaced with another non-null
     * value.
     *
     * For example, implementations of the HotSpot JVM have
     * preallocated OutOfMemoryError objects to provide for better
     * diagnosability of that situation.  These objects are created
     * without calling the constructor for that class and the fields
     * in question are initialized to null.  To support this
     * capability, any new fields added to Throwable that require
     * being initialized to a non-null value require a coordinated JVM
     * change.
     */

    /**
     * The throwable that caused this throwable to get thrown, or null if this
     * throwable was not caused by another throwable, or if the causative
     * throwable is unknown.  If this field is equal to this throwable itself,
     * it indicates that the cause of this throwable has not yet been
     * initialized.
     *
     * @serial
     * @since 1.4
     * 表示产生当前异常的根本原因。可以使用构造方法，或者initCause方法进行设置。
     */
    private Throwable cause = this;

    /**
     * The stack trace, as returned by {@link #getStackTrace()}.
     *
     * The field is initialized to a zero-length array.  A {@code
     * null} value of this field indicates subsequent calls to {@link
     * #setStackTrace(StackTraceElement[])} and {@link
     * #fillInStackTrace()} will be be no-ops.
     *
     * @serial
     * @since 1.4
     * 存储异常栈信息
     * 代码的行号会在源码编译时一起编译到字节码中。
     * 在运行发生异常时，方法调用栈会写到stackTrace字段中。
     * 栈的顶端是异常发生的位置，即throw的位置；栈的底端是线程开始的位置。
     * 我们可以通过e.printStackTrace、e.getStackTrace等方法查看异常栈，来分析程序出错的位置在源码中的位置。
     */
    private StackTraceElement[] stackTrace = UNASSIGNED_STACK;

    // Setting this static field introduces an acceptable
    // initialization dependency on a few java.util classes.
    //特殊量（sentinel）来表示特定的状态。比如，stackTrace为UNASSIGNED_STACK代表初始值，为null代表不可访问。
    private static final List<Throwable> SUPPRESSED_SENTINEL =
        Collections.unmodifiableList(new ArrayList<Throwable>(0));

    /**
     * The list of suppressed exceptions, as returned by {@link
     * #getSuppressed()}.  The list is initialized to a zero-element
     * unmodifiable sentinel list.  When a serialized Throwable is
     * read in, if the {@code suppressedExceptions} field points to a
     * zero-element list, the field is reset to the sentinel value.
     *
     * @serial
     * @since 1.7
     * 用来存储其他的不是强关联的异常，防止异常的丢失。
     */
    private List<Throwable> suppressedExceptions = SUPPRESSED_SENTINEL;

    /** Message for trying to suppress a null exception. */
    //空异常
    private static final String NULL_CAUSE_MESSAGE = "Cannot suppress a null exception.";

    /** Message for trying to suppress oneself. */
    private static final String SELF_SUPPRESSION_MESSAGE = "Self-suppression not permitted";

    /** Caption  for labeling causative exception stack traces */
    private static final String CAUSE_CAPTION = "Caused by: ";

    /** Caption for labeling suppressed exception stack traces */
    private static final String SUPPRESSED_CAPTION = "Suppressed: ";
    
    //空的异常数组
    private static final Throwable[] EMPTY_THROWABLE_ARRAY = new Throwable[0];
```

# **构造方法**

```java
    /**
     * Constructs a new throwable with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * <p>The {@link #fillInStackTrace()} method is called to initialize
     * the stack trace data in the newly created throwable.
     * 构造一个新的可抛出的null作为其详细信息。 原因未初始化，可以随后通过调用initCause(java.lang.Throwable)进行初始化 。
     */
    public Throwable() {
        fillInStackTrace();
    }

    /**
     * Constructs a new throwable with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * <p>The {@link #fillInStackTrace()} method is called to initialize
     * the stack trace data in the newly created throwable.
     *
     * @param   message   the detail message. The detail message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     *   构造一个具有指定的详细信息的新的throwable。 原因未初始化，可以随后通过调用initCause(java.lang.Throwable)进行初始化 。
     */
    public Throwable(String message) {
        fillInStackTrace();
        detailMessage = message;
    }

    /**
     * Constructs a new throwable with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this throwable's detail message.
     *
     * <p>The {@link #fillInStackTrace()} method is called to initialize
     * the stack trace data in the newly created throwable.
     *
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A {@code null} value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     * 构造一个具有指定的详细信息和原因的新的throwable。
     */
    public Throwable(String message, Throwable cause) {
        //填充堆栈信息
        fillInStackTrace();
        detailMessage = message;
        this.cause = cause;
    }

    /**
     * Constructs a new throwable with the specified cause and a detail
     * message of {@code (cause==null ? null : cause.toString())} (which
     * typically contains the class and detail message of {@code cause}).
     * This constructor is useful for throwables that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * <p>The {@link #fillInStackTrace()} method is called to initialize
     * the stack trace data in the newly created throwable.
     *
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A {@code null} value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     * 构造具有指定的原因和详细消息的新throwable
     */
    public Throwable(Throwable cause) {
        fillInStackTrace();
        //cause允许为空值，表示原因不存在或未知。
        detailMessage = (cause==null ? null : cause.toString());
        this.cause = cause;
    }

    /**
     * Constructs a new throwable with the specified detail message,
     * cause, {@linkplain #addSuppressed suppression} enabled or
     * disabled, and writable stack trace enabled or disabled.  If
     * suppression is disabled, {@link #getSuppressed} for this object
     * will return a zero-length array and calls to {@link
     * #addSuppressed} that would otherwise append an exception to the
     * suppressed list will have no effect.  If the writable stack
     * trace is false, this constructor will not call {@link
     * #fillInStackTrace()}, a {@code null} will be written to the
     * {@code stackTrace} field, and subsequent calls to {@code
     * fillInStackTrace} and {@link
     * #setStackTrace(StackTraceElement[])} will not set the stack
     * trace.  If the writable stack trace is false, {@link
     * #getStackTrace} will return a zero length array.
     *
     * <p>Note that the other constructors of {@code Throwable} treat
     * suppression as being enabled and the stack trace as being
     * writable.  Subclasses of {@code Throwable} should document any
     * conditions under which suppression is disabled and document
     * conditions under which the stack trace is not writable.
     * Disabling of suppression should only occur in exceptional
     * circumstances where special requirements exist, such as a
     * virtual machine reusing exception objects under low-memory
     * situations.  Circumstances where a given exception object is
     * repeatedly caught and rethrown, such as to implement control
     * flow between two sub-systems, is another situation where
     * immutable throwable objects would be appropriate.
     *
     * @param  message the detail message.
     * @param cause the cause.  (A {@code null} value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be
     *                           writable
     *
     * @see OutOfMemoryError
     * @see NullPointerException
     * @see ArithmeticException
     * @since 1.7
     * 构造一个具有指定原因和详细信息的新的throwable，启用或禁用suppression和启用或禁用可写栈跟踪。
     * 禁用Suppression只应在特殊情况下存在特殊要求，例如虚拟机在低内存情况下重用异常对象。 给定异常对象被重复捕获并重新引导的情况，例如在两个子系统之间实现控制流的情况是另一种情况，即不可变的可抛出对象是合适的。
     */
    protected Throwable(String message, Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace) {
        if (writableStackTrace) {
            //初始化堆栈跟踪数据
            fillInStackTrace();
        } else {
            //禁用写入堆栈跟踪数据
            stackTrace = null;
        }
        detailMessage = message;
        this.cause = cause;
        if (!enableSuppression)
            //禁用Suppression
            suppressedExceptions = null;
    }
```

# **常用方法**

## fillInStackTrace

初始化新创建的throwable中的堆栈跟踪数据

```java
    /**
     * Fills in the execution stack trace. This method records within this
     * {@code Throwable} object information about the current state of
     * the stack frames for the current thread.
     *
     * <p>If the stack trace of this {@code Throwable} {@linkplain
     * Throwable#Throwable(String, Throwable, boolean, boolean) is not
     * writable}, calling this method has no effect.
     *
     * @return  a reference to this {@code Throwable} instance.
     * @see     java.lang.Throwable#printStackTrace()
     * 初始化新创建的throwable中的堆栈跟踪数据
     */
    public synchronized Throwable fillInStackTrace() {
        //判断stackTrace、backtrace是否为null
        if (stackTrace != null ||
            backtrace != null /* Out of protocol state */ ) {
            //将当前线程的栈帧信息记录到此Throwable中
            fillInStackTrace(0);
            stackTrace = UNASSIGNED_STACK;
        }
        return this;
    }
    //native
    private native Throwable fillInStackTrace(int dummy);
```

## **printStackTrace()**

```java
    /**
     * Prints this throwable and its backtrace to the
     * standard error stream. This method prints a stack trace for this
     * {@code Throwable} object on the error output stream that is
     * the value of the field {@code System.err}. The first line of
     * output contains the result of the {@link #toString()} method for
     * this object.  Remaining lines represent data previously recorded by
     * the method {@link #fillInStackTrace()}. The format of this
     * information depends on the implementation, but the following
     * example may be regarded as typical:
     * <blockquote><pre>
     * java.lang.NullPointerException
     *         at MyClass.mash(MyClass.java:9)
     *         at MyClass.crunch(MyClass.java:6)
     *         at MyClass.main(MyClass.java:3)
     * </pre></blockquote>
     * This example was produced by running the program:
     * <pre>
     * class MyClass {
     *     public static void main(String[] args) {
     *         crunch(null);
     *     }
     *     static void crunch(int[] a) {
     *         mash(a);
     *     }
     *     static void mash(int[] b) {
     *         System.out.println(b[0]);
     *     }
     * }
     * </pre>
     * The backtrace for a throwable with an initialized, non-null cause
     * should generally include the backtrace for the cause.  The format
     * of this information depends on the implementation, but the following
     * example may be regarded as typical:
     * <pre>
     * HighLevelException: MidLevelException: LowLevelException
     *         at Junk.a(Junk.java:13)
     *         at Junk.main(Junk.java:4)
     * Caused by: MidLevelException: LowLevelException
     *         at Junk.c(Junk.java:23)
     *         at Junk.b(Junk.java:17)
     *         at Junk.a(Junk.java:11)
     *         ... 1 more
     * Caused by: LowLevelException
     *         at Junk.e(Junk.java:30)
     *         at Junk.d(Junk.java:27)
     *         at Junk.c(Junk.java:21)
     *         ... 3 more
     * </pre>
     * Note the presence of lines containing the characters {@code "..."}.
     * These lines indicate that the remainder of the stack trace for this
     * exception matches the indicated number of frames from the bottom of the
     * stack trace of the exception that was caused by this exception (the
     * "enclosing" exception).  This shorthand can greatly reduce the length
     * of the output in the common case where a wrapped exception is thrown
     * from same method as the "causative exception" is caught.  The above
     * example was produced by running the program:
     * <pre>
     * public class Junk {
     *     public static void main(String args[]) {
     *         try {
     *             a();
     *         } catch(HighLevelException e) {
     *             e.printStackTrace();
     *         }
     *     }
     *     static void a() throws HighLevelException {
     *         try {
     *             b();
     *         } catch(MidLevelException e) {
     *             throw new HighLevelException(e);
     *         }
     *     }
     *     static void b() throws MidLevelException {
     *         c();
     *     }
     *     static void c() throws MidLevelException {
     *         try {
     *             d();
     *         } catch(LowLevelException e) {
     *             throw new MidLevelException(e);
     *         }
     *     }
     *     static void d() throws LowLevelException {
     *        e();
     *     }
     *     static void e() throws LowLevelException {
     *         throw new LowLevelException();
     *     }
     * }
     *
     * class HighLevelException extends Exception {
     *     HighLevelException(Throwable cause) { super(cause); }
     * }
     *
     * class MidLevelException extends Exception {
     *     MidLevelException(Throwable cause)  { super(cause); }
     * }
     *
     * class LowLevelException extends Exception {
     * }
     * </pre>
     * As of release 7, the platform supports the notion of
     * <i>suppressed exceptions</i> (in conjunction with the {@code
     * try}-with-resources statement). Any exceptions that were
     * suppressed in order to deliver an exception are printed out
     * beneath the stack trace.  The format of this information
     * depends on the implementation, but the following example may be
     * regarded as typical:
     *
     * <pre>
     * Exception in thread "main" java.lang.Exception: Something happened
     *  at Foo.bar(Foo.java:10)
     *  at Foo.main(Foo.java:5)
     *  Suppressed: Resource$CloseFailException: Resource ID = 0
     *          at Resource.close(Resource.java:26)
     *          at Foo.bar(Foo.java:9)
     *          ... 1 more
     * </pre>
     * Note that the "... n more" notation is used on suppressed exceptions
     * just at it is used on causes. Unlike causes, suppressed exceptions are
     * indented beyond their "containing exceptions."
     *
     * <p>An exception can have both a cause and one or more suppressed
     * exceptions:
     * <pre>
     * Exception in thread "main" java.lang.Exception: Main block
     *  at Foo3.main(Foo3.java:7)
     *  Suppressed: Resource$CloseFailException: Resource ID = 2
     *          at Resource.close(Resource.java:26)
     *          at Foo3.main(Foo3.java:5)
     *  Suppressed: Resource$CloseFailException: Resource ID = 1
     *          at Resource.close(Resource.java:26)
     *          at Foo3.main(Foo3.java:5)
     * Caused by: java.lang.Exception: I did it
     *  at Foo3.main(Foo3.java:8)
     * </pre>
     * Likewise, a suppressed exception can have a cause:
     * <pre>
     * Exception in thread "main" java.lang.Exception: Main block
     *  at Foo4.main(Foo4.java:6)
     *  Suppressed: Resource2$CloseFailException: Resource ID = 1
     *          at Resource2.close(Resource2.java:20)
     *          at Foo4.main(Foo4.java:5)
     *  Caused by: java.lang.Exception: Rats, you caught me
     *          at Resource2$CloseFailException.&lt;init&gt;(Resource2.java:45)
     *          ... 2 more
     * </pre>
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints this throwable and its backtrace to the specified print stream.
     *
     * @param s {@code PrintStream} to use for output
     */
    public void printStackTrace(PrintStream s) {
        printStackTrace(new WrappedPrintStream(s));
    }

    //把Throwable对象和他的异常栈信息打印到标准错误流中
    private void printStackTrace(PrintStreamOrWriter s) {
        // Guard against malicious overrides of Throwable.equals by
        // using a Set with identity equality semantics.
        Set<Throwable> dejaVu =
            Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        dejaVu.add(this);

        synchronized (s.lock()) {
            // Print our stack trace
            // 打印当前异常的详细信息
            s.println(this);
            // 打印当前堆栈中的栈帧信息
            StackTraceElement[] trace = getOurStackTrace();
            for (StackTraceElement traceElement : trace)
                s.println("\tat " + traceElement);

            // Print suppressed exceptions, if any
            //打印出suppress异常信息
            for (Throwable se : getSuppressed())
                se.printEnclosedStackTrace(s, trace, SUPPRESSED_CAPTION, "\t", dejaVu);

            // Print cause, if any
            //// 递归打印出引起当前异常的异常信息
            Throwable ourCause = getCause();
            if (ourCause != null)
                ourCause.printEnclosedStackTrace(s, trace, CAUSE_CAPTION, "", dejaVu);
        }
    }
```

