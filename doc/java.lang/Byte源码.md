# **介绍**

```java
public final class Byte extends Number implements Comparable<Byte>
```

![image-20230525190639592](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305251906506.png)

抽象类Number是平台类的超类，可转换为基本类型byte 、 double 、 float 、 int 、 long和short数值。 它更像是一种规范，规定了继承这个抽象类的子类可以互相转换。

因为Byte只有一个字节，所以它和其他数值类型转换都是向上转型。

# **常量&变量**

```java
    /**
     * A constant holding the minimum value a {@code byte} can
     * have, -2<sup>7</sup>.
     * byte最小值
     */
    public static final byte   MIN_VALUE = -128;

    /**
     * A constant holding the maximum value a {@code byte} can
     * have, 2<sup>7</sup>-1.
     * byte最大值
     */
    public static final byte   MAX_VALUE = 127;

    /**
     * The {@code Class} instance representing the primitive type
     * {@code byte}.
     * 基本类型 byte 的 Class 对象，可用于类反射
     */
    @SuppressWarnings("unchecked")
    public static final Class<Byte>     TYPE = (Class<Byte>) Class.getPrimitiveClass("byte");

    /**
     * The value of the {@code Byte}.
     *
     * @serial
     * 用来存储byte
     */
    private final byte value;

    /**
     * The number of bits used to represent a {@code byte} value in two's
     * complement binary form.
     *
     * @since 1.5
     * 以二进制补码形式表示byte值的位数。
     */
    public static final int SIZE = 8;

    /**
     * The number of bytes used to represent a {@code byte} value in two's
     * complement binary form.
     *
     * @since 1.8
     * 以二进制补码形式表示byte值的byte数。就是1。
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    /** use serialVersionUID from JDK 1.1. for interoperability */
    //序列化版本号
    private static final long serialVersionUID = -7183698231559129828L;
```

# **构造方法**

```java
/**
     * Constructs a newly allocated {@code Byte} object that
     * represents the specified {@code byte} value.
     *
     * @param value     the value to be represented by the
     *                  {@code Byte}.
     */
    public Byte(byte value) {
        this.value = value;
    }

    /**
     * Constructs a newly allocated {@code Byte} object that
     * represents the {@code byte} value indicated by the
     * {@code String} parameter. The string is converted to a
     * {@code byte} value in exactly the manner used by the
     * {@code parseByte} method for radix 10.
     *
     * @param s         the {@code String} to be converted to a
     *                  {@code Byte}
     * @throws           NumberFormatException If the {@code String}
     *                  does not contain a parsable {@code byte}.
     * @see        java.lang.Byte#parseByte(java.lang.String, int)
     */
    public Byte(String s) throws NumberFormatException {
        this.value = parseByte(s, 10);
    }
    
    

    /**
     * Parses the string argument as a signed {@code byte} in the
     * radix specified by the second argument. The characters in the
     * string must all be digits, of the specified radix (as
     * determined by whether {@link java.lang.Character#digit(char,
     * int)} returns a nonnegative value) except that the first
     * character may be an ASCII minus sign {@code '-'}
     * ({@code '\u005Cu002D'}) to indicate a negative value or an
     * ASCII plus sign {@code '+'} ({@code '\u005Cu002B'}) to
     * indicate a positive value.  The resulting {@code byte} value is
     * returned.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     * <li> The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li> The radix is either smaller than {@link
     * java.lang.Character#MIN_RADIX} or larger than {@link
     * java.lang.Character#MAX_RADIX}.
     *
     * <li> Any character of the string is not a digit of the
     * specified radix, except that the first character may be a minus
     * sign {@code '-'} ({@code '\u005Cu002D'}) or plus sign
     * {@code '+'} ({@code '\u005Cu002B'}) provided that the
     * string is longer than length 1.
     *
     * <li> The value represented by the string is not a value of type
     * {@code byte}.
     * </ul>
     *
     * @param s         the {@code String} containing the
     *                  {@code byte}
     *                  representation to be parsed
     * @param radix     the radix to be used while parsing {@code s}
     * @return          the {@code byte} value represented by the string
     *                   argument in the specified radix
     * @throws          NumberFormatException If the string does
     *                  not contain a parsable {@code byte}.
     *                   调用integer的parseInt
     */
    public static byte parseByte(String s, int radix)
        throws NumberFormatException {
        int i = Integer.parseInt(s, radix);
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException(
                "Value out of range. Value:\"" + s + "\" Radix:" + radix);
        return (byte)i;
    }
```

## 内部类

```java
    /**
     * 内部类 存储value为-128 - 127的Byte对象。
     */
    private static class ByteCache {
        private ByteCache(){}

        static final Byte cache[] = new Byte[-(-128) + 127 + 1];

        static {
            for(int i = 0; i < cache.length; i++)
                cache[i] = new Byte((byte)(i - 128));
        }
    }
```



# **常用方法**

## **toString**

```java
/**
 * Returns a {@code String} object representing this
 * {@code Byte}'s value.  The value is converted to signed
 * decimal representation and returned as a string, exactly as if
 * the {@code byte} value were given as an argument to the
 * {@link java.lang.Byte#toString(byte)} method.
 *
 * @return  a string representation of the value of this object in
 *          base&nbsp;10.
 *          强转为Integer后toString
 */
public String toString() {
    return Integer.toString((int)value);
}
```

## **valueOf**

```java

    /**
     * Returns a {@code Byte} instance representing the specified
     * {@code byte} value.
     * If a new {@code Byte} instance is not required, this method
     * should generally be used in preference to the constructor
     * {@link #Byte(byte)}, as this method is likely to yield
     * significantly better space and time performance since
     * all byte values are cached.
     *
     * @param  b a byte value.
     * @return a {@code Byte} instance representing {@code b}.
     * @since  1.5
     * 返回缓存对象（注意加上偏移量 128  缓存byte数组（-128， 127）
     * 比如b = 1 ，对应byte数组索引为 1 + 128
     */
    public static Byte valueOf(byte b) {
        final int offset = 128;
        return ByteCache.cache[(int)b + offset];
    }
```



```java
    /**
     * Returns a {@code Byte} object holding the value
     * given by the specified {@code String}. The argument is
     * interpreted as representing a signed decimal {@code byte},
     * exactly as if the argument were given to the {@link
     * #parseByte(java.lang.String)} method. The result is a
     * {@code Byte} object that represents the {@code byte}
     * value specified by the string.
     *
     * <p> In other words, this method returns a {@code Byte} object
     * equal to the value of:
     *
     * <blockquote>
     * {@code new Byte(Byte.parseByte(s))}
     * </blockquote>
     *
     * @param s         the string to be parsed
     * @return          a {@code Byte} object holding the value
     *                  represented by the string argument
     * @throws          NumberFormatException If the {@code String} does
     *                  not contain a parsable {@code byte}.
     *                  10进制
     */
    public static Byte valueOf(String s) throws NumberFormatException {
        return valueOf(s, 10);
    }
```



```java
    /**
     * Returns a {@code Byte} object holding the value
     * extracted from the specified {@code String} when parsed
     * with the radix given by the second argument. The first argument
     * is interpreted as representing a signed {@code byte} in
     * the radix specified by the second argument, exactly as if the
     * argument were given to the {@link #parseByte(java.lang.String,
     * int)} method. The result is a {@code Byte} object that
     * represents the {@code byte} value specified by the string.
     *
     * <p> In other words, this method returns a {@code Byte} object
     * equal to the value of:
     *
     * <blockquote>
     * {@code new Byte(Byte.parseByte(s, radix))}
     * </blockquote>
     *
     * @param s         the string to be parsed
     * @param radix     the radix to be used in interpreting {@code s}
     * @return          a {@code Byte} object holding the value
     *                  represented by the string argument in the
     *                  specified radix.
     * @throws          NumberFormatException If the {@code String} does
     *                  not contain a parsable {@code byte}.
     *    将radix进制的String类型的整数转换为byte类型。
     */
    public static Byte valueOf(String s, int radix)
        throws NumberFormatException {
        return valueOf(parseByte(s, radix));
    }
```

## parseByte

```java
    /**
     * Parses the string argument as a signed {@code byte} in the
     * radix specified by the second argument. The characters in the
     * string must all be digits, of the specified radix (as
     * determined by whether {@link java.lang.Character#digit(char,
     * int)} returns a nonnegative value) except that the first
     * character may be an ASCII minus sign {@code '-'}
     * ({@code '\u005Cu002D'}) to indicate a negative value or an
     * ASCII plus sign {@code '+'} ({@code '\u005Cu002B'}) to
     * indicate a positive value.  The resulting {@code byte} value is
     * returned.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     * <li> The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li> The radix is either smaller than {@link
     * java.lang.Character#MIN_RADIX} or larger than {@link
     * java.lang.Character#MAX_RADIX}.
     *
     * <li> Any character of the string is not a digit of the
     * specified radix, except that the first character may be a minus
     * sign {@code '-'} ({@code '\u005Cu002D'}) or plus sign
     * {@code '+'} ({@code '\u005Cu002B'}) provided that the
     * string is longer than length 1.
     *
     * <li> The value represented by the string is not a value of type
     * {@code byte}.
     * </ul>
     *
     * @param s         the {@code String} containing the
     *                  {@code byte}
     *                  representation to be parsed
     * @param radix     the radix to be used while parsing {@code s}
     * @return          the {@code byte} value represented by the string
     *                   argument in the specified radix
     * @throws          NumberFormatException If the string does
     *                  not contain a parsable {@code byte}.
     *                   调用integer的parseInt
     */
    public static byte parseByte(String s, int radix)
        throws NumberFormatException {
        //parseInt将字符串解析成指定进制的int
        int i = Integer.parseInt(s, radix);
        //边界判断
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException(
                "Value out of range. Value:\"" + s + "\" Radix:" + radix);
        //强转成byte
        return (byte)i;
    }
```

## **decode**

```java
    /**
     * Decodes a {@code String} into a {@code Byte}.
     * Accepts decimal, hexadecimal, and octal numbers given by
     * the following grammar:
     *
     * <blockquote>
     * <dl>
     * <dt><i>DecodableString:</i>
     * <dd><i>Sign<sub>opt</sub> DecimalNumeral</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code 0x} <i>HexDigits</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code 0X} <i>HexDigits</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code #} <i>HexDigits</i>
     * <dd><i>Sign<sub>opt</sub></i> {@code 0} <i>OctalDigits</i>
     *
     * <dt><i>Sign:</i>
     * <dd>{@code -}
     * <dd>{@code +}
     * </dl>
     * </blockquote>
     *
     * <i>DecimalNumeral</i>, <i>HexDigits</i>, and <i>OctalDigits</i>
     * are as defined in section 3.10.1 of
     * <cite>The Java&trade; Language Specification</cite>,
     * except that underscores are not accepted between digits.
     *
     * <p>The sequence of characters following an optional
     * sign and/or radix specifier ("{@code 0x}", "{@code 0X}",
     * "{@code #}", or leading zero) is parsed as by the {@code
     * Byte.parseByte} method with the indicated radix (10, 16, or 8).
     * This sequence of characters must represent a positive value or
     * a {@link NumberFormatException} will be thrown.  The result is
     * negated if first character of the specified {@code String} is
     * the minus sign.  No whitespace characters are permitted in the
     * {@code String}.
     *
     * @param     nm the {@code String} to decode.
     * @return   a {@code Byte} object holding the {@code byte}
     *          value represented by {@code nm}
     * @throws  NumberFormatException  if the {@code String} does not
     *            contain a parsable {@code byte}.
     * @see java.lang.Byte#parseByte(java.lang.String, int)
     * 将String类型的字符串解码为10进制的Byte类型。
     */
    public static Byte decode(String nm) throws NumberFormatException {
        //Integer.decode将String类型的字符串解码为10进制的Integer类型。
        int i = Integer.decode(nm);
        //边界判断
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException(
                    "Value " + i + " out of range from input " + nm);
        return valueOf((byte)i);
    }
```

