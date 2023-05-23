# **介绍**

可变的字符序列，可以对字符串进行增删，不会产生新的对象；线程安全的，将所有方法重写并用synchronized关键字修饰，效率低；底层使用 byte[] 存储。

被 **final** 所修饰，不能被继承。

```java
 public final class StringBuffer
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence
```

![image-20230524072522709](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305240731400.png)

# **常量&变量**

```java
    /**
     * A cache of the last value returned by toString. Cleared
     * whenever the StringBuffer is modified.
     * 用来缓存toString()方法返回的最近一次的value数组中的字符。
     * 当修改StringBuffer对象时会被清除
     */
    private transient char[] toStringCache;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    static final long serialVersionUID = 3388685877147921107L;


    /**
     * Serializable fields for StringBuffer.
     * StringBuffer的可序列化字段
     *
     * @serialField value  char[]
     *              The backing character array of this StringBuffer.
     * @serialField count int
     *              The number of characters in this StringBuffer.
     * @serialField shared  boolean
     *              A flag indicating whether the backing array is shared.
     *              The value is ignored upon deserialization.
     */
    private static final java.io.ObjectStreamField[] serialPersistentFields =
    {
            //用来存储字符序列中的字符。value是一个动态的数组，当存储容量不足时，会对它进行扩容
        new java.io.ObjectStreamField("value", char[].class),
            //表示value数组中已存储的字符数
        new java.io.ObjectStreamField("count", Integer.TYPE),
            //是否共享
        new java.io.ObjectStreamField("shared", Boolean.TYPE),
    };
```

# **构造方法**

```java
    /**
     * Constructs a string buffer with no characters in it and an
     * initial capacity of 16 characters.
     * 默认构造方法  数组的初始容量为16。
     */
    public StringBuffer() {
        super(16);
    }

    /**
     * Constructs a string buffer with no characters in it and
     * the specified initial capacity.
     *
     * @param      capacity  the initial capacity.
     * @exception  NegativeArraySizeException  if the {@code capacity}
     *               argument is less than {@code 0}.
     *  指定数组的初始容量大小
     */
    public StringBuffer(int capacity) {
        super(capacity);
    }

    /**
     * Constructs a string buffer initialized to the contents of the
     * specified string. The initial capacity of the string buffer is
     * {@code 16} plus the length of the string argument.
     *
     * @param   str   the initial contents of the buffer.
     *                接受一个String对象作为参数，设置了value数组的初始容量为String对象的长+16，
     *                并把String对象中的字符添加到value数组中。
     */
    public StringBuffer(String str) {
        super(str.length() + 16);
        append(str);
    }

    /**
     * Constructs a string buffer that contains the same characters
     * as the specified {@code CharSequence}. The initial capacity of
     * the string buffer is {@code 16} plus the length of the
     * {@code CharSequence} argument.
     * <p>
     * If the length of the specified {@code CharSequence} is
     * less than or equal to zero, then an empty buffer of capacity
     * {@code 16} is returned.
     *
     * @param      seq   the sequence to copy.
     * @since 1.5
     * 接受一个CharSequence对象作为参数，设置了value数组的初始容CharSequence对象的长度+16，
     * 并把CharSequence对象中的字符添加到value数组中。
     */
    public StringBuffer(CharSequence seq) {
        this(seq.length() + 16);
        append(seq);
    }
```

当append和insert时,原value数组长度不够时，进行扩容

# **常用方法**

参考   [AbstractStringBuilder.java源码](https://blog.csdn.net/m0_37450089/article/details/130818791?spm=1001.2014.3001.5501)





如文章有问题请留言，谢谢~