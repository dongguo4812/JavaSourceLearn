# **介绍**

```java
public final class Short extends Number implements Comparable<Short>
```

![image-20230530072501390](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305300725927.png)

# **常量&变量**

```java
 /**
     * A constant holding the minimum value a {@code short} can
     * have, -2<sup>15</sup>.
     * 最小值-32768
     */
    public static final short   MIN_VALUE = -32768;

    /**
     * A constant holding the maximum value a {@code short} can
     * have, 2<sup>15</sup>-1.
     * 最大值32767
     */
    public static final short   MAX_VALUE = 32767;

    /**
     * The {@code Class} instance representing the primitive type
     * {@code short}
     * short的原始类型
     */
    @SuppressWarnings("unchecked")
    public static final Class<Short>    TYPE = (Class<Short>) Class.getPrimitiveClass("short");
    
    /**
     * The value of the {@code Short}.
     *
     * @serial
     * 存储的short值
     */
    private final short value;
    
  /**
     * The number of bits used to represent a {@code short} value in two's
     * complement binary form.
     * @since 1.5
     * 占用bit位
     */
    public static final int SIZE = 16;

    /**
     * The number of bytes used to represent a {@code short} value in two's
     * complement binary form.
     *
     * @since 1.8
     * 占用字节数
     */
    public static final int BYTES = SIZE / Byte.SIZE;
    
    /** use serialVersionUID from JDK 1.1. for interoperability */
    //序列化版本号
    private static final long serialVersionUID = 7515723908773894738L;
```

# **构造方法**

```java
    /**
     * Constructs a newly allocated {@code Short} object that
     * represents the specified {@code short} value.
     *
     * @param value     the value to be represented by the
     *                  {@code Short}.
     */
    public Short(short value) {
        this.value = value;
    }

    /**
     * Constructs a newly allocated {@code Short} object that
     * represents the {@code short} value indicated by the
     * {@code String} parameter. The string is converted to a
     * {@code short} value in exactly the manner used by the
     * {@code parseShort} method for radix 10.
     *
     * @param s the {@code String} to be converted to a
     *          {@code Short}
     * @throws  NumberFormatException If the {@code String}
     *          does not contain a parsable {@code short}.
     * @see     java.lang.Short#parseShort(java.lang.String, int)
     */
    public Short(String s) throws NumberFormatException {
        this.value = parseShort(s, 10);
    }
```

## 内部类

```java
    //缓存-128 到127的short值
    private static class ShortCache {
        private ShortCache(){}

        static final Short cache[] = new Short[-(-128) + 127 + 1];

        static {
            for(int i = 0; i < cache.length; i++)
                cache[i] = new Short((short)(i - 128));
        }
    }
```

# **常用方法**

## **valueOf**

```java
    /**
     * Returns a {@code Short} instance representing the specified
     * {@code short} value.
     * If a new {@code Short} instance is not required, this method
     * should generally be used in preference to the constructor
     * {@link #Short(short)}, as this method is likely to yield
     * significantly better space and time performance by caching
     * frequently requested values.
     *
     * This method will always cache values in the range -128 to 127,
     * inclusive, and may cache other values outside of this range.
     *
     * @param  s a short value.
     * @return a {@code Short} instance representing {@code s}.
     * @since  1.5
     * 若s在缓存区间内就从缓存中取，否则将新创建一个对象。
     */
    public static Short valueOf(short s) {
        final int offset = 128;
        int sAsInt = s;
        if (sAsInt >= -128 && sAsInt <= 127) { // must cache
            return ShortCache.cache[sAsInt + offset];
        }
        return new Short(s);
    }
```

