# **介绍**

基本数据类型boolean的包装类

```java
public final class Boolean implements java.io.Serializable,
                                      Comparable<Boolean>
```

![image-20230525070532478](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305250708523.png)

# **常量&变量**

```java
    /**
     * The {@code Boolean} object corresponding to the primitive
     * value {@code true}.
     * TRUE与FALSE是Boolean提供的两个静态常量，
     * 在用到true或者false时，可直接用这两个常量，无需再耗费资源来创建类似new Boolean(true)这样的新实例；
     */
    public static final Boolean TRUE = new Boolean(true);

    /**
     * The {@code Boolean} object corresponding to the primitive
     * value {@code false}.
     */
    public static final Boolean FALSE = new Boolean(false);

    /**
     * The Class object representing the primitive type boolean.
     *
     * @since   JDK1.1
     * 基本类型 boolean 的 Class 对象，可用于类反射
     */
    @SuppressWarnings("unchecked")
    public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean");

    /**
     * The value of the Boolean.
     *
     * @serial
     * final 定义的私有变量 true/false
     */
    private final boolean value;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    //序列化版本号
    private static final long serialVersionUID = -3665804199014368530L;
```

# **构造方法**

```java
    /**
     * Allocates a {@code Boolean} object representing the
     * {@code value} argument.
     *
     * <p><b>Note: It is rarely appropriate to use this constructor.
     * Unless a <i>new</i> instance is required, the static factory
     * {@link #valueOf(boolean)} is generally a better choice. It is
     * likely to yield significantly better space and time performance.</b>
     *
     * @param   value   the value of the {@code Boolean}.
     *                  
     */
    public Boolean(boolean value) {
        this.value = value;
    }

    /**
     * Allocates a {@code Boolean} object representing the value
     * {@code true} if the string argument is not {@code null}
     * and is equal, ignoring case, to the string {@code "true"}.
     * Otherwise, allocate a {@code Boolean} object representing the
     * value {@code false}. Examples:<p>
     * {@code new Boolean("True")} produces a {@code Boolean} object
     * that represents {@code true}.<br>
     * {@code new Boolean("yes")} produces a {@code Boolean} object
     * that represents {@code false}.
     *
     * @param   s   the string to be converted to a {@code Boolean}.
     *   若传入字符串"true" 返回true 否则返回false
     */
    public Boolean(String s) {
        this(parseBoolean(s));
    }


```

## **常用方法**

## **parseBoolean**

```java
/**
     * Parses the string argument as a boolean.  The {@code boolean}
     * returned represents the value {@code true} if the string argument
     * is not {@code null} and is equal, ignoring case, to the string
     * {@code "true"}. <p>
     * Example: {@code Boolean.parseBoolean("True")} returns {@code true}.<br>
     * Example: {@code Boolean.parseBoolean("yes")} returns {@code false}.
     *
     * @param      s   the {@code String} containing the boolean
     *                 representation to be parsed
     * @return     the boolean represented by the string argument
     * @since 1.5
     * 将字符串解析为布尔值
     */
    public static boolean parseBoolean(String s) {
        return ((s != null) && s.equalsIgnoreCase("true"));
    }
```

## **hashCode**

```java
    /**
     * Returns a hash code for this {@code Boolean} object.
     *
     * @return  the integer {@code 1231} if this object represents
     * {@code true}; returns the integer {@code 1237} if this
     * object represents {@code false}.
     */
    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    /**
     * Returns a hash code for a {@code boolean} value; compatible with
     * {@code Boolean.hashCode()}.
     *
     * @param value the value to hash
     * @return a hash code value for a {@code boolean} value.
     * @since 1.8
     * true的hashCode为1231，false的hashCode为1237
     */
    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }
```

## **思考**

[你可能不清楚的Java细节(1)--为什么Boolean的hashCode()方法返回值是1231或1237](https://blog.csdn.net/qq_21251983/article/details/52164403?locationNum=1&fps=1)