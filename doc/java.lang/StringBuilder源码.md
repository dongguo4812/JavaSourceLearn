# **介绍**

1. 一个可变的字符序列
2. 线程不安全
3. StringBuffer就是把StringBuilder的方法都加上了synchronized，不再赘述

该类被涉及用作StringBuffer的一个简易替换，用在字符串缓冲区被单线程使用的时候，如果可能，建议优先使用StringBuilder，因为在大多数实现中，它比StringBuffer要快。

```java
public final class StringBuilder
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence
```

![image-20230524072806690](F:\note\image\image-20230524072806690.png)

# **构造方法**

```java
/**
     * Constructs a string builder with no characters in it and an
     * initial capacity of 16 characters.
     */
    public StringBuilder() {
        super(16);
    }

    /**
     * Constructs a string builder with no characters in it and an
     * initial capacity specified by the {@code capacity} argument.
     *
     * @param      capacity  the initial capacity.
     * @throws     NegativeArraySizeException  if the {@code capacity}
     *               argument is less than {@code 0}.
     */
    public StringBuilder(int capacity) {
        super(capacity);
    }

    /**
     * Constructs a string builder initialized to the contents of the
     * specified string. The initial capacity of the string builder is
     * {@code 16} plus the length of the string argument.
     *
     * @param   str   the initial contents of the buffer.
     */
    public StringBuilder(String str) {
        super(str.length() + 16);
        append(str);
    }

    /**
     * Constructs a string builder that contains the same characters
     * as the specified {@code CharSequence}. The initial capacity of
     * the string builder is {@code 16} plus the length of the
     * {@code CharSequence} argument.
     *
     * @param      seq   the sequence to copy.
     */
    public StringBuilder(CharSequence seq) {
        this(seq.length() + 16);
        append(seq);
    }
```

# **常用方法**

参考   [AbstractStringBuilder.java源码](https://blog.csdn.net/m0_37450089/article/details/130818791?spm=1001.2014.3001.5501)





如文章有问题请留言，谢谢~