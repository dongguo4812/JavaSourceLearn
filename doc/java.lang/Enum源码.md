# **介绍**

枚举是一个被命名的整型常数的集合，用于声明一组带标识符的常数。枚举在曰常生活中很常见，例如一个人的性别只能是“男”或者“女”，一周的星期只能是 7 天中的一个等。类似这种当一个变量有几种固定可能的取值时，就可以将它定义为枚举类型。

在 JDK 1.5 之前没有枚举类型，那时候一般用接口常量来替代。而使用 Java枚举类型 enum 可以更贴近地表示这种常量。

```java
public abstract class Enum<E extends Enum<E>>
        implements Comparable<E>, Serializable
```

![image-20230531211609891](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305312116013.png)

# **常量&变量**

```java
    /**
     * The name of this enum constant, as declared in the enum declaration.
     * Most programmers should use the {@link #toString} method rather than
     * accessing this field.
     * 枚举常量的名称
     */
    private final String name;

    /**
     * Returns the name of this enum constant, exactly as declared in its
     * enum declaration.
     *
     * <b>Most programmers should use the {@link #toString} method in
     * preference to this one, as the toString method may return
     * a more user-friendly name.</b>  This method is designed primarily for
     * use in specialized situations where correctness depends on getting the
     * exact name, which will not vary from release to release.
     *
     * @return the name of this enum constant
     */
    public final String name() {
        return name;
    }

    /**
     * The ordinal of this enumeration constant (its position
     * in the enum declaration, where the initial constant is assigned
     * an ordinal of zero).
     *
     * Most programmers will have no use for this field.  It is designed
     * for use by sophisticated enum-based data structures, such as
     * {@link java.util.EnumSet} and {@link java.util.EnumMap}.
     * 此枚举常量的序数（它在枚举声明中的位置，其中初始常量的序数为零）。 
     * 大多数程序员都不会使用这个领域。 它设计用于复杂的基于枚举的数据结构，、
     * 例如java.util.EnumSet和java.util.EnumMap。
     */
    private final int ordinal;

    /**
     * Returns the ordinal of this enumeration constant (its position
     * in its enum declaration, where the initial constant is assigned
     * an ordinal of zero).
     *
     * Most programmers will have no use for this method.  It is
     * designed for use by sophisticated enum-based data structures, such
     * as {@link java.util.EnumSet} and {@link java.util.EnumMap}.
     *
     * @return the ordinal of this enumeration constant
     * 
     */
    public final int ordinal() {
        return ordinal;
    }
```

# **构造方法**

```java
    /**
     * Sole constructor.  Programmers cannot invoke this constructor.
     * It is for use by code emitted by the compiler in response to
     * enum type declarations.
     *
     * @param name - The name of this enum constant, which is the identifier
     *               used to declare it.
     * @param ordinal - The ordinal of this enumeration constant (its position
     *         in the enum declaration, where the initial constant is assigned
     *         an ordinal of zero).
     *         无法调用此构造函数  由编译器发出的代码用于响应枚举类型声明。
     */
    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }
```

# **答疑**

## **枚举是如何保证线程安全的**

当一个Java类第一次被真正使用到的时候静态资源被初始化，Java类的加载和初始化过程都是线程安全的。所以，创建一个enum类型是线程安全的。

## **为什么用枚举实现的单例是最好的方式**

- 枚举写法简单
- 枚举自己处理序列化
- 枚举实例创建是thread-safe(线程安全的)