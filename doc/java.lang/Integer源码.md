# **介绍**

Integer是int类型的包装类，继承自Number抽象类，实现了Comparable接口。提供了一些处理int类型的方法，比如int到String类型的转换方法或String类型到int类型的转换方法，当然也包含与其他类型之间的转换方法。

- Comparable提供了比较大小的功能
- Number抽象类主要抽象出了对数值类型的转换方法。

```java
public final class Integer extends Number implements Comparable<Integer> 
```

![image-20230528081440284](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305280814288.png)

# **常量&变量**

```java
  /**
     * A constant holding the minimum value an {@code int} can
     * have, -2<sup>31</sup>.
     *最小值0x80000000
     */
    @Native public static final int   MIN_VALUE = 0x80000000;

    /**
     * A constant holding the maximum value an {@code int} can
     * have, 2<sup>31</sup>-1.
     * 最大值0x7fffffff
     */
    @Native public static final int   MAX_VALUE = 0x7fffffff;

    /**
     * The {@code Class} instance representing the primitive type
     * {@code int}.
     *
     * @since   JDK1.1
     * int的原始类型
     */
    @SuppressWarnings("unchecked")
    public static final Class<Integer>  TYPE = (Class<Integer>) Class.getPrimitiveClass("int");

    /**
     * All possible chars for representing a number as a String
     * 数字表示为字符串的所有可能字符
     */
    final static char[] digits = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };

    //使用DigitOnes和DigitTens来确定一个两位数int对应的char
    //十位数
    final static char [] DigitTens = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        } ;
    //个位数
    final static char [] DigitOnes = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        } ;
        
    //位数上限的数组
    final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999,
                                      99999999, 999999999, Integer.MAX_VALUE };
                                      
   /**
     * The value of the {@code Integer}.
     *
     * @serial
     * 存储的int值
     */
    private final int value;
    
    /**
     * The number of bits used to represent an {@code int} value in two's
     * complement binary form.
     *
     * @since 1.5
     * 占用bit位
     */
    @Native public static final int SIZE = 32;

    /**
     * The number of bytes used to represent a {@code int} value in two's
     * complement binary form.
     *
     * @since 1.8
     * 占用字节数
     */
    public static final int BYTES = SIZE / Byte.SIZE;
    
    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    @Native private static final long serialVersionUID = 1360826667806852920L;
```

- value：表示Integer对应的int值。

- MIN_VALUE：定义一个常量，表示int类型的最小值，-2^31，@Native 表示被注解的内容是原生，不影响java代码的本身逻辑。

- MAX_VALUE：定义一个常量，表示int类型的最大值，2^31-1。

- TYPE：表示这个包装类包装的是基本类型int。

- Size：定义常量，用于以二进制补码形式表示int值的位数。

- BYTES：定义常量，用于以二进制补码形式表示int值的字节数。

- digits：表示所有可能用来表示数字的字符，因为int是支持2-36进制，所以需要36个字符在表示不同的数字。

- DigitOnes和DigitTens：使用DigitOnes和DigitTens来确定一个两位数int对应的char。如65，DigitOnes[65]=5，DigitTens[65]=6。

- sizeTable：主要用来计算一个int类型对应字符串的长度。如下的stringSize方法



# **构造方法**

```java
  /**
     * Constructs a newly allocated {@code Integer} object that
     * represents the specified {@code int} value.
     *
     * @param   value   the value to be represented by the
     *                  {@code Integer} object.
     */
    public Integer(int value) {
        this.value = value;
    }

    /**
     * Constructs a newly allocated {@code Integer} object that
     * represents the {@code int} value indicated by the
     * {@code String} parameter. The string is converted to an
     * {@code int} value in exactly the manner used by the
     * {@code parseInt} method for radix 10.
     *
     * @param      s   the {@code String} to be converted to an
     *                 {@code Integer}.
     * @exception  NumberFormatException  if the {@code String} does not
     *               contain a parsable integer.
     * @see        java.lang.Integer#parseInt(java.lang.String, int)
     */
    public Integer(String s) throws NumberFormatException {
        this.value = parseInt(s, 10);
    }
```

# **常用方法**

## **toString**

将int转成字符串

```java
    /**
     * Returns a {@code String} object representing this
     * {@code Integer}'s value. The value is converted to signed
     * decimal representation and returned as a string, exactly as if
     * the integer value were given as an argument to the {@link
     * java.lang.Integer#toString(int)} method.
     *
     * @return  a string representation of the value of this object in
     *          base&nbsp;10.
     */
    public String toString() {
        return toString(value);
    }
```



```java
    /**
     * Returns a {@code String} object representing the
     * specified integer. The argument is converted to signed decimal
     * representation and returned as a string, exactly as if the
     * argument and radix 10 were given as arguments to the {@link
     * #toString(int, int)} method.
     *
     * @param   i   an integer to be converted.
     * @return  a string representation of the argument in base&nbsp;10.
     */
    public static String toString(int i) {
        //int的最小值
        if (i == Integer.MIN_VALUE)
            return "-2147483648";
        //调用stringSize计算int值对应字符串的长度，负数有一个符号位所以要+1
        int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
        //新建一个临时数组，用来存放int值每一位转成char后的值
        char[] buf = new char[size];
        //将int值每一位转成char放到buf中
        getChars(i, size, buf);
        return new String(buf, true);
    }
```



```java
    /**
     * Returns a string representation of the first argument in the
     * radix specified by the second argument.
     *
     * <p>If the radix is smaller than {@code Character.MIN_RADIX}
     * or larger than {@code Character.MAX_RADIX}, then the radix
     * {@code 10} is used instead.
     *
     * <p>If the first argument is negative, the first element of the
     * result is the ASCII minus character {@code '-'}
     * ({@code '\u005Cu002D'}). If the first argument is not
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
     * {@code '\u005Cu007A'}. If {@code radix} is
     * <var>N</var>, then the first <var>N</var> of these characters
     * are used as radix-<var>N</var> digits in the order shown. Thus,
     * the digits for hexadecimal (radix 16) are
     * {@code 0123456789abcdef}. If uppercase letters are
     * desired, the {@link java.lang.String#toUpperCase()} method may
     * be called on the result:
     *
     * <blockquote>
     *  {@code Integer.toString(n, 16).toUpperCase()}
     * </blockquote>
     *
     * @param   i       an integer to be converted to a string.
     * @param   radix   the radix to use in the string representation.
     * @return  a string representation of the argument in the specified radix.
     * @see     java.lang.Character#MAX_RADIX
     * @see     java.lang.Character#MIN_RADIX
     */
    public static String toString(int i, int radix) {
        //当基数小于2或大于36，radix默认为10进制
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            radix = 10;

        /* Use the faster version */
        //当基数为10时，直接调用toString方法后返回
        if (radix == 10) {
            return toString(i);
        }
        //因为int最大为32位(2进制占的位数)，所以只需要33位就可以存储int+符号位
        char buf[] = new char[33];
        boolean negative = (i < 0);
        int charPos = 32;
        //将正数转换为负数
        if (!negative) {
            i = -i;
        }
        //循环 当负值i 依然小于 负值radix
        while (i <= -radix) {
            //buf[32] = digits[]
            buf[charPos--] = digits[-(i % radix)];
            i = i / radix;
        }
        buf[charPos] = digits[-i];
        //i小于0，符号标志位为'-'
        if (negative) {
            buf[--charPos] = '-';
        }

        return new String(buf, charPos, (33 - charPos));
    }
```

## **stringSize**

获取一个int值对应字符串的长度

```java
    // Requires positive x
    //返回位数 利用sizeTable属性，可以高效的获取一个int值对应字符串你的长度，不用过多的除法或取模运算
    static int stringSize(int x) {
        for (int i=0; ; i++)
            if (x <= sizeTable[i])
                return i+1;
    }
```

## **getChars**

在toString方法中调用，主要作用是，将int值的每一位转成char后放到buf中。

```java
    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Integer.MIN_VALUE
     */
    static void getChars(int i, int index, char[] buf) {
        int q, r;
        //buf数组的长度
        int charPos = index;
        //符号标志位
        char sign = 0;
        //当i小于0时，
        if (i < 0) {
            //定义符号位‘-’
            sign = '-';
            //将负值i取反
            i = -i;
        }

        // Generate two digits per iteration
        //①如果i大于65536（两个字节的长度）那么就去除i的后两位
        while (i >= 65536) {
            //去除i的后两位赋值给q  比如i为65536，那么q为655
            q = i / 100;
        // really: r = i - (q * 100);
            //②计算后两位的值，如果i为65537，那么r为37，公式 r = 65537 - (655 * 100)
            r = i - ((q << 6) + (q << 5) + (q << 2));
            //去除后两位重新赋值i
            i = q;
            //通过DigitOnes和DigitTens获取r的个位和十位对应的char。
            buf [--charPos] = DigitOnes[r];
            buf [--charPos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        //经过上面循环，i小于等于65536
        for (;;) {
            //③就是q = i/10，如果i=655，那么q=65
            q = (i * 52429) >>> (16+3);
            //取i的最后一位  r= 655 - (65 * 10) = 5
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            //通过digits数组获取对应的char
            buf [--charPos] = digits [r];
            // q=65，并赋值给i，进入下一个循环
            i = q;
            if (i == 0) break;
        }
        //符号标志为不为0 即为‘-’
        if (sign != 0) {
            //数组下标为0的char为‘-’
            buf [--charPos] = sign;
        }
    }
```

## **parseInt**

```java
    /**
     * Parses the string argument as a signed integer in the radix
     * specified by the second argument. The characters in the string
     * must all be digits of the specified radix (as determined by
     * whether {@link java.lang.Character#digit(char, int)} returns a
     * nonnegative value), except that the first character may be an
     * ASCII minus sign {@code '-'} ({@code '\u005Cu002D'}) to
     * indicate a negative value or an ASCII plus sign {@code '+'}
     * ({@code '\u005Cu002B'}) to indicate a positive value. The
     * resulting integer value is returned.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     * <li>The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li>The radix is either smaller than
     * {@link java.lang.Character#MIN_RADIX} or
     * larger than {@link java.lang.Character#MAX_RADIX}.
     *
     * <li>Any character of the string is not a digit of the specified
     * radix, except that the first character may be a minus sign
     * {@code '-'} ({@code '\u005Cu002D'}) or plus sign
     * {@code '+'} ({@code '\u005Cu002B'}) provided that the
     * string is longer than length 1.
     *
     * <li>The value represented by the string is not a value of type
     * {@code int}.
     * </ul>
     *
     * <p>Examples:
     * <blockquote><pre>
     * parseInt("0", 10) returns 0
     * parseInt("473", 10) returns 473
     * parseInt("+42", 10) returns 42
     * parseInt("-0", 10) returns 0
     * parseInt("-FF", 16) returns -255
     * parseInt("1100110", 2) returns 102
     * parseInt("2147483647", 10) returns 2147483647
     * parseInt("-2147483648", 10) returns -2147483648
     * parseInt("2147483648", 10) throws a NumberFormatException
     * parseInt("99", 8) throws a NumberFormatException
     * parseInt("Kona", 10) throws a NumberFormatException
     * parseInt("Kona", 27) returns 411787
     * </pre></blockquote>
     *
     * @param      s   the {@code String} containing the integer
     *                  representation to be parsed
     * @param      radix   the radix to be used while parsing {@code s}.
     * @return     the integer represented by the string argument in the
     *             specified radix.
     * @exception  NumberFormatException if the {@code String}
     *             does not contain a parsable {@code int}.
     *             将radix进制的String类型整数转换为int类型。
     */
    public static int parseInt(String s, int radix)
                throws NumberFormatException
    {
        /*
         * WARNING: This method may be invoked early during VM initialization
         * before IntegerCache is initialized. Care must be taken to not use
         * the valueOf method.
         */

        if (s == null) {
            throw new NumberFormatException("null");
        }

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix +
                                            " less than Character.MIN_RADIX");
        }

        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix +
                                            " greater than Character.MAX_RADIX");
        }

        int result = 0;
        boolean negative = false;
        int i = 0, len = s.length();
        int limit = -Integer.MAX_VALUE;
        int multmin;
        int digit;

        if (len > 0) {
            char firstChar = s.charAt(0);
            // 若firstChar < '0' 说明第一个字符是+或—。
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+')
                    throw NumberFormatException.forInputString(s);

                if (len == 1) // Cannot have lone "+" or "-"
                    throw NumberFormatException.forInputString(s);
                i++;
            }
            // 这个变量是为了防止超过最大整数
            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                // 获取进制为radix的字符i的整数int类型
                digit = Character.digit(s.charAt(i++),radix);
                if (digit < 0) {
                    throw NumberFormatException.forInputString(s);
                }
                // 乘以radix之前先判断是否越界
                if (result < multmin) {
                    throw NumberFormatException.forInputString(s);
                }
                result *= radix;
                if (result < limit + digit) {
                    throw NumberFormatException.forInputString(s);
                }
                // 这里使用负数进行计算，因为最小负数比最大正数多一个，不然可能出现溢出
                result -= digit;
            }
        } else {
            throw NumberFormatException.forInputString(s);
        }
        return negative ? result : -result;
    }

    /**
     * Parses the string argument as a signed decimal integer. The
     * characters in the string must all be decimal digits, except
     * that the first character may be an ASCII minus sign {@code '-'}
     * ({@code '\u005Cu002D'}) to indicate a negative value or an
     * ASCII plus sign {@code '+'} ({@code '\u005Cu002B'}) to
     * indicate a positive value. The resulting integer value is
     * returned, exactly as if the argument and the radix 10 were
     * given as arguments to the {@link #parseInt(java.lang.String,
     * int)} method.
     *
     * @param s    a {@code String} containing the {@code int}
     *             representation to be parsed
     * @return     the integer value represented by the argument in decimal.
     * @exception  NumberFormatException  if the string does not contain a
     *               parsable integer.
     *               默认十进制
     */
    public static int parseInt(String s) throws NumberFormatException {
        return parseInt(s,10);
    }
```

## **parseUnsignedInt**

```java
    /**
     * Parses the string argument as an unsigned integer in the radix
     * specified by the second argument.  An unsigned integer maps the
     * values usually associated with negative numbers to positive
     * numbers larger than {@code MAX_VALUE}.
     *
     * The characters in the string must all be digits of the
     * specified radix (as determined by whether {@link
     * java.lang.Character#digit(char, int)} returns a nonnegative
     * value), except that the first character may be an ASCII plus
     * sign {@code '+'} ({@code '\u005Cu002B'}). The resulting
     * integer value is returned.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     * <li>The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li>The radix is either smaller than
     * {@link java.lang.Character#MIN_RADIX} or
     * larger than {@link java.lang.Character#MAX_RADIX}.
     *
     * <li>Any character of the string is not a digit of the specified
     * radix, except that the first character may be a plus sign
     * {@code '+'} ({@code '\u005Cu002B'}) provided that the
     * string is longer than length 1.
     *
     * <li>The value represented by the string is larger than the
     * largest unsigned {@code int}, 2<sup>32</sup>-1.
     *
     * </ul>
     *
     *
     * @param      s   the {@code String} containing the unsigned integer
     *                  representation to be parsed
     * @param      radix   the radix to be used while parsing {@code s}.
     * @return     the integer represented by the string argument in the
     *             specified radix.
     * @throws     NumberFormatException if the {@code String}
     *             does not contain a parsable {@code int}.
     * @since 1.8
     * 将String类型的无符号数转换为int类型。
     */
    public static int parseUnsignedInt(String s, int radix)
                throws NumberFormatException {
        if (s == null)  {
            throw new NumberFormatException("null");
        }

        int len = s.length();
        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar == '-') {
                throw new
                    NumberFormatException(String.format("Illegal leading minus sign " +
                                                       "on unsigned string %s.", s));
            } else {
                // 这里先判断String长度是否小于等于5，这是因为最大整数用36进制表示为6位，越界了
                if (len <= 5 || // Integer.MAX_VALUE in Character.MAX_RADIX is 6 digits
                    // 因为10进制比较常用，所以这里它专门判断是不是10进制
                    (radix == 10 && len <= 9) ) { // Integer.MAX_VALUE in base 10 is 10 digits
                    return parseInt(s, radix);
                } else {
                    // 如果无法用parseInt来转换就需要使用长整型long
                    long ell = Long.parseLong(s, radix);
                    // 若转换后的long高32位有数字说明越界了
                    if ((ell & 0xffff_ffff_0000_0000L) == 0) {
                        return (int) ell;
                    } else {
                        throw new
                            NumberFormatException(String.format("String value %s exceeds " +
                                                                "range of unsigned int.", s));
                    }
                }
            }
        } else {
            throw NumberFormatException.forInputString(s);
        }
    }

    /**
     * Parses the string argument as an unsigned decimal integer. The
     * characters in the string must all be decimal digits, except
     * that the first character may be an an ASCII plus sign {@code
     * '+'} ({@code '\u005Cu002B'}). The resulting integer value
     * is returned, exactly as if the argument and the radix 10 were
     * given as arguments to the {@link
     * #parseUnsignedInt(java.lang.String, int)} method.
     *
     * @param s   a {@code String} containing the unsigned {@code int}
     *            representation to be parsed
     * @return    the unsigned integer value represented by the argument in decimal.
     * @throws    NumberFormatException  if the string does not contain a
     *            parsable unsigned integer.
     * @since 1.8
     */
    public static int parseUnsignedInt(String s) throws NumberFormatException {
        return parseUnsignedInt(s, 10);
    }
```

## **valueOf**

```java
    /**
     * Returns an {@code Integer} object holding the value
     * extracted from the specified {@code String} when parsed
     * with the radix given by the second argument. The first argument
     * is interpreted as representing a signed integer in the radix
     * specified by the second argument, exactly as if the arguments
     * were given to the {@link #parseInt(java.lang.String, int)}
     * method. The result is an {@code Integer} object that
     * represents the integer value specified by the string.
     *
     * <p>In other words, this method returns an {@code Integer}
     * object equal to the value of:
     *
     * <blockquote>
     *  {@code new Integer(Integer.parseInt(s, radix))}
     * </blockquote>
     *
     * @param      s   the string to be parsed.
     * @param      radix the radix to be used in interpreting {@code s}
     * @return     an {@code Integer} object holding the value
     *             represented by the string argument in the specified
     *             radix.
     * @exception NumberFormatException if the {@code String}
     *            does not contain a parsable {@code int}.
     *            调用ParseInt方法将String转换为Integer。
     */
    public static Integer valueOf(String s, int radix) throws NumberFormatException {
        return Integer.valueOf(parseInt(s,radix));
    }

    /**
     * Returns an {@code Integer} object holding the
     * value of the specified {@code String}. The argument is
     * interpreted as representing a signed decimal integer, exactly
     * as if the argument were given to the {@link
     * #parseInt(java.lang.String)} method. The result is an
     * {@code Integer} object that represents the integer value
     * specified by the string.
     *
     * <p>In other words, this method returns an {@code Integer}
     * object equal to the value of:
     *
     * <blockquote>
     *  {@code new Integer(Integer.parseInt(s))}
     * </blockquote>
     *
     * @param      s   the string to be parsed.
     * @return     an {@code Integer} object holding the value
     *             represented by the string argument.
     * @exception  NumberFormatException  if the string cannot be parsed
     *             as an integer.
     */
    public static Integer valueOf(String s) throws NumberFormatException {
        return Integer.valueOf(parseInt(s, 10));
    }
    
       /**
     * Returns an {@code Integer} instance representing the specified
     * {@code int} value.  If a new {@code Integer} instance is not
     * required, this method should generally be used in preference to
     * the constructor {@link #Integer(int)}, as this method is likely
     * to yield significantly better space and time performance by
     * caching frequently requested values.
     *
     * This method will always cache values in the range -128 to 127,
     * inclusive, and may cache other values outside of this range.
     *
     * @param  i an {@code int} value.
     * @return an {@code Integer} instance representing {@code i}.
     * @since  1.5
     * 首先判断缓存里有没有，如果有就从缓存里面拿，没有就创建一个。
     */
    public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
    }

```

## **IntegerCache**

```java
    /**
     * Cache to support the object identity semantics of autoboxing for values between
     * -128 and 127 (inclusive) as required by JLS.
     *
     * The cache is initialized on first usage.  The size of the cache
     * may be controlled by the {@code -XX:AutoBoxCacheMax=<size>} option.
     * During VM initialization, java.lang.Integer.IntegerCache.high property
     * may be set and saved in the private system properties in the
     * sun.misc.VM class.
     * 缓存静态内部类 -128 ，127
     */

    private static class IntegerCache {
        static final int low = -128;
        static final int high;
        static final Integer cache[];

        static {
            // high value may be configured by property
            int h = 127;
            // 这个是启动虚拟机的时候带的参数，可以自行设置表示缓存的最大整数
            String integerCacheHighPropValue =
                sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
            if (integerCacheHighPropValue != null) {
                try {
                    int i = parseInt(integerCacheHighPropValue);
                    i = Math.max(i, 127);
                    // Maximum array size is Integer.MAX_VALUE
                    // 缓存的最大整数
                    h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
                } catch( NumberFormatException nfe) {
                    // If the property cannot be parsed into an int, ignore it.
                }
            }
            high = h;

            cache = new Integer[(high - low) + 1];
            int j = low;
            for(int k = 0; k < cache.length; k++)
                cache[k] = new Integer(j++);

            // range [-128, 127] must be interned (JLS7 5.1.7)
            assert IntegerCache.high >= 127;
        }

        private IntegerCache() {}
    }
```

## **decode**

```java
    /**
     * Decodes a {@code String} into an {@code Integer}.
     * Accepts decimal, hexadecimal, and octal numbers given
     * by the following grammar:
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
     * Integer.parseInt} method with the indicated radix (10, 16, or
     * 8).  This sequence of characters must represent a positive
     * value or a {@link NumberFormatException} will be thrown.  The
     * result is negated if first character of the specified {@code
     * String} is the minus sign.  No whitespace characters are
     * permitted in the {@code String}.
     *
     * @param     nm the {@code String} to decode.
     * @return    an {@code Integer} object holding the {@code int}
     *             value represented by {@code nm}
     * @exception NumberFormatException  if the {@code String} does not
     *            contain a parsable integer.
     * @see java.lang.Integer#parseInt(java.lang.String, int)
     * 将String类型的nm解码为Integer类型
     */
    public static Integer decode(String nm) throws NumberFormatException {
        int radix = 10;
        int index = 0;
        boolean negative = false;
        Integer result;

        if (nm.isEmpty())
            throw new NumberFormatException("Zero length string");
        char firstChar = nm.charAt(0);
        // Handle sign, if present
        // 首先判断是否有符号
        if (firstChar == '-') {
            negative = true;
            index++;
        } else if (firstChar == '+')
            index++;

        // Handle radix specifier, if present
        // 查看字符串表示的整数的进制
        // 是否是16进制
        if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        }
        // 是否是16进制
        else if (nm.startsWith("#", index)) {
            index ++;
            radix = 16;
        }
        // 是否是8进制
        else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
            index ++;
            radix = 8;
        }

        // 判断符号是否写错地方了
        if (nm.startsWith("-", index) || nm.startsWith("+", index))
            throw new NumberFormatException("Sign character in wrong position");

        try {
            // 将相应进制的字符串转换为对应的Integer类型
            // 这里如果是最小负数会出错进入到下面的catch语句中处理
            // 这里有点操作麻烦了，如果是我就会在这里将nm的符号一起传入
            result = Integer.valueOf(nm.substring(index), radix);
            // 将符号赋值给result
            result = negative ? Integer.valueOf(-result.intValue()) : result;
        } catch (NumberFormatException e) {
            // If number is Integer.MIN_VALUE, we'll end up here. The next line
            // handles this case, and causes any genuine format error to be
            // rethrown.
            String constant = negative ? ("-" + nm.substring(index))
                                       : nm.substring(index);
            result = Integer.valueOf(constant, radix);
        }
        return result;
    }
```

