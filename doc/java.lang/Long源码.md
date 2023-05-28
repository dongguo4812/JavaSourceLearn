# **介绍**

```java
public final class Long extends Number implements Comparable<Long>
```

![image-20230529071700042](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305290717310.png)

# **常量&变量**

```java
 /**
     * A constant holding the minimum value a {@code long} can
     * have, -2<sup>63</sup>.
     * 最小值0x8000000000000000L
     */
    @Native public static final long MIN_VALUE = 0x8000000000000000L;

    /**
     * A constant holding the maximum value a {@code long} can
     * have, 2<sup>63</sup>-1.
     * 最大值0x7fffffffffffffffL
     */
    @Native public static final long MAX_VALUE = 0x7fffffffffffffffL;

    /**
     * The {@code Class} instance representing the primitive type
     * {@code long}.
     *
     * @since   JDK1.1
     * long的原始类型
     */
    @SuppressWarnings("unchecked")
    public static final Class<Long>     TYPE = (Class<Long>) Class.getPrimitiveClass("long");
    
    /**
     * The value of the {@code Long}.
     *
     * @serial
     * 存储的long值
     */
    private final long value;
    
    /**
     * The number of bits used to represent a {@code long} value in two's
     * complement binary form.
     *
     * @since 1.5
     * 占用bit位
     */
    @Native public static final int SIZE = 64;

    /**
     * The number of bytes used to represent a {@code long} value in two's
     * complement binary form.
     *
     * @since 1.8
     * 占用字节数
     */
    public static final int BYTES = SIZE / Byte.SIZE;
    
    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    @Native private static final long serialVersionUID = 4290774380558885855L;
```

# **构造方法**

```java
 /**
     * Constructs a newly allocated {@code Long} object that
     * represents the specified {@code long} argument.
     *
     * @param   value   the value to be represented by the
     *          {@code Long} object.
     */
    public Long(long value) {
        this.value = value;
    }

    /**
     * Constructs a newly allocated {@code Long} object that
     * represents the {@code long} value indicated by the
     * {@code String} parameter. The string is converted to a
     * {@code long} value in exactly the manner used by the
     * {@code parseLong} method for radix 10.
     *
     * @param      s   the {@code String} to be converted to a
     *             {@code Long}.
     * @throws     NumberFormatException  if the {@code String} does not
     *             contain a parsable {@code long}.
     * @see        java.lang.Long#parseLong(java.lang.String, int)
     */
    public Long(String s) throws NumberFormatException {
        this.value = parseLong(s, 10);
    }

```

## 内部类

缓存-128至127的Long值

```java
    private static class LongCache {
        private LongCache(){}

        static final Long cache[] = new Long[-(-128) + 127 + 1];

        static {
            for(int i = 0; i < cache.length; i++)
                cache[i] = new Long(i - 128);
        }
    }
```



# **常用方法**

## **toString**

```java
    /**
     * Returns a {@code String} object representing the specified
     * {@code long}.  The argument is converted to signed decimal
     * representation and returned as a string, exactly as if the
     * argument and the radix 10 were given as arguments to the {@link
     * #toString(long, int)} method.
     *
     * @param   i   a {@code long} to be converted.
     * @return  a string representation of the argument in base&nbsp;10.
     */
    public static String toString(long i) {
        //如果发现i等于long的最小值
        if (i == Long.MIN_VALUE)
            return "-9223372036854775808";
        //获取i的字符串长度
        int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
        //初始化字符数组的大小
        char[] buf = new char[size];
        //将i赋值到buf字符数组中
        getChars(i, size, buf);
        return new String(buf, true);
    }
    
    /**
     * Returns a string representation of the first argument in the
     * radix specified by the second argument.
     *
     * <p>If the radix is smaller than {@code Character.MIN_RADIX}
     * or larger than {@code Character.MAX_RADIX}, then the radix
     * {@code 10} is used instead.
     *
     * <p>If the first argument is negative, the first element of the
     * result is the ASCII minus sign {@code '-'}
     * ({@code '\u005Cu002d'}). If the first argument is not
     * negative, no sign character appears in the result.
     *
     * <p>The remaining characters of the result represent the magnitude
     * of the first argument. If the magnitude is zero, it is
     * represented by a single zero character {@code '0'}
     * ({@code '\u005Cu0030'}); otherwise, the first character of
     * the representation of the magnitude will not be the zero
     * character.  The following ASCII characters are used as digits:
     *
     * <blockquote>
     *   {@code 0123456789abcdefghijklmnopqrstuvwxyz}
     * </blockquote>
     *
     * These are {@code '\u005Cu0030'} through
     * {@code '\u005Cu0039'} and {@code '\u005Cu0061'} through
     * {@code '\u005Cu007a'}. If {@code radix} is
     * <var>N</var>, then the first <var>N</var> of these characters
     * are used as radix-<var>N</var> digits in the order shown. Thus,
     * the digits for hexadecimal (radix 16) are
     * {@code 0123456789abcdef}. If uppercase letters are
     * desired, the {@link java.lang.String#toUpperCase()} method may
     * be called on the result:
     *
     * <blockquote>
     *  {@code Long.toString(n, 16).toUpperCase()}
     * </blockquote>
     *
     * @param   i       a {@code long} to be converted to a string.
     * @param   radix   the radix to use in the string representation.
     * @return  a string representation of the argument in the specified radix.
     * @see     java.lang.Character#MAX_RADIX
     * @see     java.lang.Character#MIN_RADIX
     * 将long类型数值转为radix进制的字符串
     */
    public static String toString(long i, int radix) {
        //判断当前进制是否在[2,36]范围内 即[0-9a-z]
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;
        //如果当前进制为10,直接调用toString内部方法
        if (radix == 10)
            return toString(i);
        //long类型占用8个字节,一个字节占用8位
        //8*8 = 64
        //再加上符号位-、+,所以这char的长度为65
        char[] buf = new char[65];
        //定义最后一个索引位置,根据进制原则
        //从右往左,位数依次递增
        int charPos = 64;
        //记录下符号位
        boolean negative = (i < 0);
        //如果i>0,强制转为负数
        if (!negative) {
            i = -i;
        }
        //如果i<=-radix
        //意味着,还可以继续转化进制
        while (i <= -radix) {
            //(int)(-(i % radix)) 将i取余radix得到一个数字
            //Integer.digits[i] 根据得到的数字,从digits进制表中查找对应字符
            //buf[charPos--] = char 将上面查询到字符放到buf[charPos]位置,同时索引位置-1
            buf[charPos--] = Integer.digits[(int)(-(i % radix))];
            //将i / radix取整数
            i = i / radix;
        }
        //最后剩下的i<=-radix
        //将i转为整数,同时从digits进制表中查询对应的字符
        //将查询到的字符放到charPos当前位置
        buf[charPos] = Integer.digits[(int)(-i)];
        //如果确认是负数
        if (negative) {
            //将charPos索引位置继续往前推一位,同时赋值上符号位
            buf[--charPos] = '-';
        }

        //调用String的构造方法,将char[]字符数组转为字符串
        //buf目标字符数组
        //charPos索引开始位置
        //(65-charPos) 截取的元素个数
        return new String(buf, charPos, (65 - charPos));
    }
```

## **toHexString**

```java
    /**
     * Returns a string representation of the {@code long}
     * argument as an unsigned integer in base&nbsp;16.
     *
     * <p>The unsigned {@code long} value is the argument plus
     * 2<sup>64</sup> if the argument is negative; otherwise, it is
     * equal to the argument.  This value is converted to a string of
     * ASCII digits in hexadecimal (base&nbsp;16) with no extra
     * leading {@code 0}s.
     *
     * <p>The value of the argument can be recovered from the returned
     * string {@code s} by calling {@link
     * Long#parseUnsignedLong(String, int) Long.parseUnsignedLong(s,
     * 16)}.
     *
     * <p>If the unsigned magnitude is zero, it is represented by a
     * single zero character {@code '0'} ({@code '\u005Cu0030'});
     * otherwise, the first character of the representation of the
     * unsigned magnitude will not be the zero character. The
     * following characters are used as hexadecimal digits:
     *
     * <blockquote>
     *  {@code 0123456789abcdef}
     * </blockquote>
     *
     * These are the characters {@code '\u005Cu0030'} through
     * {@code '\u005Cu0039'} and  {@code '\u005Cu0061'} through
     * {@code '\u005Cu0066'}.  If uppercase letters are desired,
     * the {@link java.lang.String#toUpperCase()} method may be called
     * on the result:
     *
     * <blockquote>
     *  {@code Long.toHexString(n).toUpperCase()}
     * </blockquote>
     *
     * @param   i   a {@code long} to be converted to a string.
     * @return  the string representation of the unsigned {@code long}
     *          value represented by the argument in hexadecimal
     *          (base&nbsp;16).
     * @see #parseUnsignedLong(String, int)
     * @see #toUnsignedString(long, int)
     * @since   JDK 1.0.2
     * 将long类型数据转换成16进制形式的字符串
     */
    public static String toHexString(long i) {
        return toUnsignedString0(i, 4);
    }
   

   /**
     * Format a long (treated as unsigned) into a String.
     * @param val the value to format
     * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
     */
    static String toUnsignedString0(long val, int shift) {
        // assert shift > 0 && shift <=5 : "Illegal shift value";
        int mag = Long.SIZE - Long.numberOfLeadingZeros(val);
        //确定转化后的字符数组长度
        int chars = Math.max(((mag + (shift - 1)) / shift), 1);
        char[] buf = new char[chars];

        formatUnsignedLong(val, shift, buf, 0, chars);
        return new String(buf, true);
    }

    /**
     * Format a long (treated as unsigned) into a character buffer.
     * @param val the unsigned long to format
     * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
     * @param buf the character buffer to write to
     * @param offset the offset in the destination buffer to start at
     * @param len the number of characters to write
     * @return the lowest character location used
     */
     static int formatUnsignedLong(long val, int shift, char[] buf, int offset, int len) {
        int charPos = len;
        //shift决定进制类型
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            //由于offset=0,所以可以理解成[buf--charPos]
            //索引位置由高位往下递减
            //((int) val) & mask 得到当前数值的的进制处理后的码表索引位置
            //(10 & 15) 等于10,而码表10号位置刚好为a
            buf[offset + --charPos] = Integer.digits[((int) val) & mask];
            //无符号右移,相当于val/2
            val >>>= shift;
        } while (val != 0 && charPos > 0);
         //charPos是先用后减
         //一直到val ==0 或者charPos = 0结束循环
        return charPos;
    }
```

## **parseLong**

```java
    /**
     * Parses the string argument as a signed {@code long} in the
     * radix specified by the second argument. The characters in the
     * string must all be digits of the specified radix (as determined
     * by whether {@link java.lang.Character#digit(char, int)} returns
     * a nonnegative value), except that the first character may be an
     * ASCII minus sign {@code '-'} ({@code '\u005Cu002D'}) to
     * indicate a negative value or an ASCII plus sign {@code '+'}
     * ({@code '\u005Cu002B'}) to indicate a positive value. The
     * resulting {@code long} value is returned.
     *
     * <p>Note that neither the character {@code L}
     * ({@code '\u005Cu004C'}) nor {@code l}
     * ({@code '\u005Cu006C'}) is permitted to appear at the end
     * of the string as a type indicator, as would be permitted in
     * Java programming language source code - except that either
     * {@code L} or {@code l} may appear as a digit for a
     * radix greater than or equal to 22.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     *
     * <li>The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li>The {@code radix} is either smaller than {@link
     * java.lang.Character#MIN_RADIX} or larger than {@link
     * java.lang.Character#MAX_RADIX}.
     *
     * <li>Any character of the string is not a digit of the specified
     * radix, except that the first character may be a minus sign
     * {@code '-'} ({@code '\u005Cu002d'}) or plus sign {@code
     * '+'} ({@code '\u005Cu002B'}) provided that the string is
     * longer than length 1.
     *
     * <li>The value represented by the string is not a value of type
     *      {@code long}.
     * </ul>
     *
     * <p>Examples:
     * <blockquote><pre>
     * parseLong("0", 10) returns 0L
     * parseLong("473", 10) returns 473L
     * parseLong("+42", 10) returns 42L
     * parseLong("-0", 10) returns 0L
     * parseLong("-FF", 16) returns -255L
     * parseLong("1100110", 2) returns 102L
     * parseLong("99", 8) throws a NumberFormatException
     * parseLong("Hazelnut", 10) throws a NumberFormatException
     * parseLong("Hazelnut", 36) returns 1356099454469L
     * </pre></blockquote>
     *
     * @param      s       the {@code String} containing the
     *                     {@code long} representation to be parsed.
     * @param      radix   the radix to be used while parsing {@code s}.
     * @return     the {@code long} represented by the string argument in
     *             the specified radix.
     * @throws     NumberFormatException  if the string does not contain a
     *             parsable {@code long}.
     *             将字符串按照radix进制,转化为long数值
     */
    public static long parseLong(String s, int radix)
              throws NumberFormatException
    {
        //空值检测
        if (s == null) {
            throw new NumberFormatException("null");
        }
        //判断radix转化的进制是否在[2,36]的范围内
        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix +
                                            " less than Character.MIN_RADIX");
        }
        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix +
                                            " greater than Character.MAX_RADIX");
        }
        //用来接收最终结果
        long result = 0;
        //用来标记数据的正负性
        boolean negative = false;
        int i = 0, len = s.length();
        long limit = -Long.MAX_VALUE;
        long multmin;
        //记录索引对应字符的ASCII值
        int digit;

        if (len > 0) {
            //取第一个字符,判断是否为字符
            char firstChar = s.charAt(0);
            //判断是否为数字
            if (firstChar < '0') { // Possible leading "+" or "-"
                //判断是否为负数
                if (firstChar == '-') {
                    //标记符号位
                    negative = true;
                    //如果是负数,设置limit的值为long的最小值
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+')
                    //如果firstChar既不是数字字符
                    //也不是-符号,也不是+符号,则抛出异常
                    throw NumberFormatException.forInputString(s);
                //在进行转换的时候,不能只是+、-符号,所以长度只为1则抛异常
                if (len == 1) // Cannot have lone "+" or "-"
                    throw NumberFormatException.forInputString(s);
                //当满足所有条件,索引后移,读取数据位
                i++;
            }
            //限制对应进制radix的最小值
            multmin = limit / radix;
            //循环遍历字符串中的字符
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                //在指定的基数radix,返回字符ch的数值
                digit = Character.digit(s.charAt(i++),radix);
                //确保得到的基础是有效值
                if (digit < 0) {
                    throw NumberFormatException.forInputString(s);
                }
                //确保最终结果不会超标
                if (result < multmin) {
                    throw NumberFormatException.forInputString(s);
                }
                //遵循原则,根据读取到的位数,实时计算进制本身对应的值
                //最终转化为10进制数
                result *= radix;
                //确保最终结果不会超标
                if (result < limit + digit) {
                    throw NumberFormatException.forInputString(s);
                }
                result -= digit;
            }
        } else {
            throw NumberFormatException.forInputString(s);
        }
        //判断是否需要修改为负数
        return negative ? result : -result;
    }
```

## **decode**

```java
    /**
     * Decodes a {@code String} into a {@code Long}.
     * Accepts decimal, hexadecimal, and octal numbers given by the
     * following grammar:
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
     * Long.parseLong} method with the indicated radix (10, 16, or 8).
     * This sequence of characters must represent a positive value or
     * a {@link NumberFormatException} will be thrown.  The result is
     * negated if first character of the specified {@code String} is
     * the minus sign.  No whitespace characters are permitted in the
     * {@code String}.
     *
     * @param     nm the {@code String} to decode.
     * @return    a {@code Long} object holding the {@code long}
     *            value represented by {@code nm}
     * @throws    NumberFormatException  if the {@code String} does not
     *            contain a parsable {@code long}.
     * @see java.lang.Long#parseLong(String, int)
     * @since 1.2
     * 将字符串转化为Long类型对象
     */
    public static Long decode(String nm) throws NumberFormatException {
        //设置默认的字符串的进制为10进制
        int radix = 10;
        //index记录有效数值的开始索引位置
        //一般需要去除进制位或者符号位
        int index = 0;
        //用来记录正负值类型
        boolean negative = false;
        //用来记录最终的结果
        Long result;
        //如果字符串长度为0,则抛出此异常,此处可能发生空指针异常
        if (nm.isEmpty())
            throw new NumberFormatException("Zero length string");
        //获取第一个索引位置的字符
        char firstChar = nm.charAt(0);
        // Handle sign, if present
        //处理符号位,如果出现-、+,同时索引位置后移一位
        if (firstChar == '-') {
            negative = true;
            index++;
        } else if (firstChar == '+')
            index++;

        // Handle radix specifier, if present
        //判断如果目标字符串以0x或者0X(不区分大小)开头,则说明是16进制数
        //同时索引位置后移两位
        if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        }
        //如果字符串以#开头(注意此时index可能已经后移),
        // 则说明也是16进制数据,索引位置后移一位即可,所以-#16、+#16也是合法的数值
        else if (nm.startsWith("#", index)) {
            index ++;
            radix = 16;
        }
        //如果nm是以0开头,且0后面还有字符,则按照8进制处理
        //Long.decode("0")是按照10进制进行转化的
        //而Long.decode("00")则是按照8进制转化的
        //虽然两者最终结果都是0,但内部处理方式完全不同
        else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
            index ++;
            radix = 8;
        }
        //如果在处理完上面逻辑后,字符串索引位置还出现了-、+等符号
        //则认为字符非法
        if (nm.startsWith("-", index) || nm.startsWith("+", index))
            throw new NumberFormatException("Sign character in wrong position");

        try {
            //截取有效字符串,调用内部valueOf方法,得到Long对象
            result = Long.valueOf(nm.substring(index), radix);
            //最后加上符号位
            result = negative ? Long.valueOf(-result.longValue()) : result;
        } catch (NumberFormatException e) {
            // If number is Long.MIN_VALUE, we'll end up here. The next line
            // handles this case, and causes any genuine format error to be
            // rethrown.
            //异常处理
            String constant = negative ? ("-" + nm.substring(index))
                                       : nm.substring(index);
            result = Long.valueOf(constant, radix);
        }
        return result;
    }
```

## valueOf

```java
    /**
     * Returns a {@code Long} object holding the value
     * extracted from the specified {@code String} when parsed
     * with the radix given by the second argument.  The first
     * argument is interpreted as representing a signed
     * {@code long} in the radix specified by the second
     * argument, exactly as if the arguments were given to the {@link
     * #parseLong(java.lang.String, int)} method. The result is a
     * {@code Long} object that represents the {@code long}
     * value specified by the string.
     *
     * <p>In other words, this method returns a {@code Long} object equal
     * to the value of:
     *
     * <blockquote>
     *  {@code new Long(Long.parseLong(s, radix))}
     * </blockquote>
     *
     * @param      s       the string to be parsed
     * @param      radix   the radix to be used in interpreting {@code s}
     * @return     a {@code Long} object holding the value
     *             represented by the string argument in the specified
     *             radix.
     * @throws     NumberFormatException  If the {@code String} does not
     *             contain a parsable {@code long}.
     */
    public static Long valueOf(String s, int radix) throws NumberFormatException {
        return Long.valueOf(parseLong(s, radix));
    }

    /**
     * Returns a {@code Long} object holding the value
     * of the specified {@code String}. The argument is
     * interpreted as representing a signed decimal {@code long},
     * exactly as if the argument were given to the {@link
     * #parseLong(java.lang.String)} method. The result is a
     * {@code Long} object that represents the integer value
     * specified by the string.
     *
     * <p>In other words, this method returns a {@code Long} object
     * equal to the value of:
     *
     * <blockquote>
     *  {@code new Long(Long.parseLong(s))}
     * </blockquote>
     *
     * @param      s   the string to be parsed.
     * @return     a {@code Long} object holding the value
     *             represented by the string argument.
     * @throws     NumberFormatException  If the string cannot be parsed
     *             as a {@code long}.
     */
    public static Long valueOf(String s) throws NumberFormatException
    {
        return Long.valueOf(parseLong(s, 10));
    }
    
        /**
     * Returns a {@code Long} instance representing the specified
     * {@code long} value.
     * If a new {@code Long} instance is not required, this method
     * should generally be used in preference to the constructor
     * {@link #Long(long)}, as this method is likely to yield
     * significantly better space and time performance by caching
     * frequently requested values.
     *
     * Note that unlike the {@linkplain Integer#valueOf(int)
     * corresponding method} in the {@code Integer} class, this method
     * is <em>not</em> required to cache values within a particular
     * range.
     *
     * @param  l a long value.
     * @return a {@code Long} instance representing {@code l}.
     * @since  1.5
     */
    public static Long valueOf(long l) {
        final int offset = 128;
        //-128 至127 直接返回缓存中已经准备的值
        if (l >= -128 && l <= 127) { // will cache
            return LongCache.cache[(int)l + offset];
        }
        return new Long(l);
    }
```

