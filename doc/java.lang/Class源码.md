# **介绍**

如果想要在程序运行阶段访问某个类的所有信息，并支持修改类的状态或者行为的话，肯定会用到反射，而反射靠的就是Class类。

**通过Class类可以获取类的实例，构造方法，字段，成员方法，接口等信息。获取之后可以通过API进行相应的操作。**

```java
public final class Class<T> implements java.io.Serializable,
                              GenericDeclaration,
                              Type,
                              AnnotatedElement
```
![image](https://github.com/dongguo4812/JavaSourceLearn/assets/87865453/3db947e9-096d-4afb-9494-655902b8450a)



# **常量&变量**

```java
    //标识注解类型
    private static final int ANNOTATION= 0x00002000;
    //标识枚举类型
    private static final int ENUM      = 0x00004000;
    //表示该class文件并非由Java源代码所生成
    private static final int SYNTHETIC = 0x00001000;
    //注册本地方法
    private static native void registerNatives();
    static {
        registerNatives();
    }

    //缓存构造器
    private volatile transient Constructor<T> cachedConstructor;
    //调用者的缓存构造器
    private volatile transient Class<?>       newInstanceCallerCache;
    
    // cache the name to reduce the number of calls into the VM
    //实体的名称
    private transient String name;
    private native String getName0();
    
    // Package-private to allow ClassLoader access
    ClassLoader getClassLoader0() { return classLoader; }

    // Initialized in JVM not by private constructor
    // This field is filtered from reflection access, i.e. getDeclaredField
    // will throw NoSuchFieldException
    //类加载器
    private final ClassLoader classLoader;
    
    /** protection domain returned when the internal domain is null */
    //null值的安全返回
    private static java.security.ProtectionDomain allPermDomain;
    
    /**
     * Reflection support.
     */

    // Caches for certain reflective results
    //缓存反射结果
    private static boolean useCaches = true;
    
    //反射信息
    private volatile transient SoftReference<ReflectionData<T>> reflectionData;

    // Incremented by the VM on each call to JVM TI RedefineClasses()
    // that redefines this class or a superclass.
    //每次调用JVM时由VM递增
    private volatile transient int classRedefinedCount = 0;
    

    // Generic signature handling
    private native String getGenericSignature0();

    // Generic info repository; lazily initialized
    //通用信息
    private volatile transient ClassRepository genericInfo;
    
    
    /** use serialVersionUID from JDK 1.1 for interoperability */
    //序列化版本号
    private static final long serialVersionUID = 3206093459760846163L;


    /**
     * Class Class is special cased within the Serialization Stream Protocol.
     *
     * A Class instance is written initially into an ObjectOutputStream in the
     * following format:
     * <pre>
     *      {@code TC_CLASS} ClassDescriptor
     *      A ClassDescriptor is a special cased serialization of
     *      a {@code java.io.ObjectStreamClass} instance.
     * </pre>
     * A new handle is generated for the initial time the class descriptor
     * is written into the stream. Future references to the class descriptor
     * are written as references to the initial class descriptor instance.
     *
     * @see java.io.ObjectStreamClass
     */
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];
        

    //反射工厂
    private static ReflectionFactory reflectionFactory;

    // To be able to query system properties as soon as they're available
    //是否初始化
    private static boolean initted = false;
    private static void checkInitted() {
        if (initted) return;
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    // Tests to ensure the system properties table is fully
                    // initialized. This is needed because reflection code is
                    // called very early in the initialization process (before
                    // command-line arguments have been parsed and therefore
                    // these user-settable properties installed.) We assume that
                    // if System.out is non-null then the System class has been
                    // fully initialized and that the bulk of the startup code
                    // has been run.

                    if (System.out == null) {
                        // java.lang.System not yet fully initialized
                        return null;
                    }

                    // Doesn't use Boolean.getBoolean to avoid class init.
                    String val =
                        System.getProperty("sun.reflect.noCaches");
                    if (val != null && val.equals("true")) {
                        useCaches = false;
                    }

                    initted = true;
                    return null;
                }
            });
    }

    //枚举类常量 null
    private volatile transient T[] enumConstants = null;
    
    //枚举类常量目录
    private volatile transient Map<String, T> enumConstantDirectory = null;
    
    // Annotations cache
    @SuppressWarnings("UnusedDeclaration")
    //注解数据
    private volatile transient AnnotationData annotationData;
    
    @SuppressWarnings("UnusedDeclaration")
    //注解类型
    private volatile transient AnnotationType annotationType;
    
    /* Backing store of user-defined values pertaining to this class.
     * Maintained by the ClassValue class.
     */
    transient ClassValue.ClassValueMap classValueMap;
```

# **构造方法**

```java
    /*
     * Private constructor. Only the Java Virtual Machine creates Class objects.
     * This constructor is not used and prevents the default constructor being
     * generated.
     *只能由java虚拟机创建对象
     */
    private Class(ClassLoader loader) {
        // Initialize final field for classLoader.  The initialization value of non-null
        // prevents future JIT optimizations from assuming this final field is null.
        classLoader = loader;
    }
```

Class并不能直接通过new Class()获取实例。那么应该如何获取呢？

```java
//直接通过类的静态变量来获取
Class<Integer> intClass = Integer.class;

//通过实例变量的getClass方法
Integer integer = new Integer(0);
Class<? extends Integer> aClass = integer.getClass();

//通过Class.forName("类的全限定名")
Class<?> aClass1 = Class.forName("java.lang.Integer");
```





# **常用方法**

## **forName**

```java
    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given string name.  Invoking this method is
     * equivalent to:
     *
     * <blockquote>
     *  {@code Class.forName(className, true, currentLoader)}
     * </blockquote>
     *
     * where {@code currentLoader} denotes the defining class loader of
     * the current class.
     *
     * <p> For example, the following code fragment returns the
     * runtime {@code Class} descriptor for the class named
     * {@code java.lang.Thread}:
     *
     * <blockquote>
     *   {@code Class t = Class.forName("java.lang.Thread")}
     * </blockquote>
     * <p>
     * A call to {@code forName("X")} causes the class named
     * {@code X} to be initialized.
     *
     * @param      className   the fully qualified name of the desired class.
     * @return     the {@code Class} object for the class with the
     *             specified name.
     * @exception LinkageError if the linkage fails
     * @exception ExceptionInInitializerError if the initialization provoked
     *            by this method fails
     * @exception ClassNotFoundException if the class cannot be located
     * 手动加载一个类，
     */
    @CallerSensitive
    public static Class<?> forName(String className)
                throws ClassNotFoundException {
        Class<?> caller = Reflection.getCallerClass();
        return forName0(className, true, ClassLoader.getClassLoader(caller), caller);
    }


    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given string name, using the given class loader.
     * Given the fully qualified name for a class or interface (in the same
     * format returned by {@code getName}) this method attempts to
     * locate, load, and link the class or interface.  The specified class
     * loader is used to load the class or interface.  If the parameter
     * {@code loader} is null, the class is loaded through the bootstrap
     * class loader.  The class is initialized only if the
     * {@code initialize} parameter is {@code true} and if it has
     * not been initialized earlier.
     *
     * <p> If {@code name} denotes a primitive type or void, an attempt
     * will be made to locate a user-defined class in the unnamed package whose
     * name is {@code name}. Therefore, this method cannot be used to
     * obtain any of the {@code Class} objects representing primitive
     * types or void.
     *
     * <p> If {@code name} denotes an array class, the component type of
     * the array class is loaded but not initialized.
     *
     * <p> For example, in an instance method the expression:
     *
     * <blockquote>
     *  {@code Class.forName("Foo")}
     * </blockquote>
     *
     * is equivalent to:
     *
     * <blockquote>
     *  {@code Class.forName("Foo", true, this.getClass().getClassLoader())}
     * </blockquote>
     *
     * Note that this method throws errors related to loading, linking or
     * initializing as specified in Sections 12.2, 12.3 and 12.4 of <em>The
     * Java Language Specification</em>.
     * Note that this method does not check whether the requested class
     * is accessible to its caller.
     *
     * <p> If the {@code loader} is {@code null}, and a security
     * manager is present, and the caller's class loader is not null, then this
     * method calls the security manager's {@code checkPermission} method
     * with a {@code RuntimePermission("getClassLoader")} permission to
     * ensure it's ok to access the bootstrap class loader.
     *
     * @param name       fully qualified name of the desired class
     * @param initialize if {@code true} the class will be initialized.
     *                   See Section 12.4 of <em>The Java Language Specification</em>.
     * @param loader     class loader from which the class must be loaded
     * @return           class object representing the desired class
     *
     * @exception LinkageError if the linkage fails
     * @exception ExceptionInInitializerError if the initialization provoked
     *            by this method fails
     * @exception ClassNotFoundException if the class cannot be located by
     *            the specified class loader
     *
     * @see       java.lang.Class#forName(String)
     * @see       java.lang.ClassLoader
     * @since     1.2
     * boolean initialize 是否对类进行初始化
     */
    @CallerSensitive
    public static Class<?> forName(String name, boolean initialize,
                                   ClassLoader loader)
        throws ClassNotFoundException
    {
        Class<?> caller = null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // Reflective call to get caller class is only needed if a security manager
            // is present.  Avoid the overhead of making this call otherwise.
            caller = Reflection.getCallerClass();
            if (sun.misc.VM.isSystemDomainLoader(loader)) {
                ClassLoader ccl = ClassLoader.getClassLoader(caller);
                if (!sun.misc.VM.isSystemDomainLoader(ccl)) {
                    sm.checkPermission(
                        SecurityConstants.GET_CLASSLOADER_PERMISSION);
                }
            }
        }
        return forName0(name, initialize, loader, caller);
    }

    /** Called after security check for system loader access checks have been made. */
    private static native Class<?> forName0(String name, boolean initialize,
                                            ClassLoader loader,
                                            Class<?> caller)
        throws ClassNotFoundException;
```

在使用反射的时候，Class.forName是最常用的一种方式。而Class.forName底层会指向forName0这个本地方法

参数：

（1）name：类的全限定名

（2）initialize：是否初始化这个类

（3）loader：类加载器

（4）caller：调用Class.forName所在类的Class，比如A类代码块里有Class.forName，那么caller就是A的class实例。



## **getConstructors**

```java
    /**
     * Returns an array containing {@code Constructor} objects reflecting
     * all the public constructors of the class represented by this
     * {@code Class} object.  An array of length 0 is returned if the
     * class has no public constructors, or if the class is an array class, or
     * if the class reflects a primitive type or void.
     *
     * Note that while this method returns an array of {@code
     * Constructor<T>} objects (that is an array of constructors from
     * this class), the return type of this method is {@code
     * Constructor<?>[]} and <em>not</em> {@code Constructor<T>[]} as
     * might be expected.  This less informative return type is
     * necessary since after being returned from this method, the
     * array could be modified to hold {@code Constructor} objects for
     * different classes, which would violate the type guarantees of
     * {@code Constructor<T>[]}.
     *
     * @return the array of {@code Constructor} objects representing the
     *         public constructors of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since JDK1.1
     * 返回构造函数组成的数据
     */
    @CallerSensitive
    public Constructor<?>[] getConstructors() throws SecurityException {
        //判断是否有访问权限
        checkMemberAccess(Member.PUBLIC, Reflection.getCallerClass(), true);
        //获取到声明的构造方法，并拷贝一份构造方法
        return copyConstructors(privateGetDeclaredConstructors(true));
    }
```

### privateGetDeclaredConstructors()

```java
    // Returns an array of "root" constructors. These Constructor
    // objects must NOT be propagated to the outside world, but must
    // instead be copied via ReflectionFactory.copyConstructor.
    private Constructor<T>[] privateGetDeclaredConstructors(boolean publicOnly) {
        //判断是否配置了useCached属性
        checkInitted();
        Constructor<T>[] res;
        //获取缓存当中的数据
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            //返回缓存当中的构造方法 根据publicOnly决定是共有还是私有的构造方法
            res = publicOnly ? rd.publicConstructors : rd.declaredConstructors;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        if (isInterface()) {
            //直接生成一个数组长度为0的Constructor数组  因为接口没有构造方法
            @SuppressWarnings("unchecked")
            Constructor<T>[] temporaryRes = (Constructor<T>[]) new Constructor<?>[0];
            res = temporaryRes;
        } else {
            //从JVM当中读取数据
            res = getDeclaredConstructors0(publicOnly);
        }
        if (rd != null) {
            //更新缓存reflectionData当中的构造方法
            if (publicOnly) {
                rd.publicConstructors = res;
            } else {
                rd.declaredConstructors = res;
            }
        }
        return res;
    }
```

##### reflectionData

```java
    // Lazily create and cache ReflectionData
    //创建和缓存反射获得的数据
    private ReflectionData<T> reflectionData() {
        //获取当前reflectionData
        SoftReference<ReflectionData<T>> reflectionData = this.reflectionData;
        int classRedefinedCount = this.classRedefinedCount;
        ReflectionData<T> rd;
        //如果可以使用缓存，并且缓存当中的数据不为null，而且缓存没有失效，（缓存当中redefinedCount等于classRedefinedCount的值）
        if (useCaches &&
            reflectionData != null &&
            (rd = reflectionData.get()) != null &&
            rd.redefinedCount == classRedefinedCount) {
            //直接返回缓存当中的reflectionData
            return rd;
        }
        // else no SoftReference or cleared SoftReference or stale ReflectionData
        // -> create and replace new instance
        //创建新的reflectionData，并保存到缓存当中去
        return newReflectionData(reflectionData, classRedefinedCount);
    }
```

###### newReflectionData

```java
    private ReflectionData<T> newReflectionData(SoftReference<ReflectionData<T>> oldReflectionData,
                                                int classRedefinedCount) {
        //不使用缓存，直接返回
        if (!useCaches) return null;
        //while + cas
        while (true) {
            //创建新的ReflectionData
            ReflectionData<T> rd = new ReflectionData<>(classRedefinedCount);
            // try to CAS it...
            //更新成功，返回
            if (Atomic.casReflectionData(this, oldReflectionData, new SoftReference<>(rd))) {
                return rd;
            }
            // else retry
            //否则获得旧的reflectionData和classRedefinedCount的值
            oldReflectionData = this.reflectionData;
            classRedefinedCount = this.classRedefinedCount;
            //如果旧的值不为null, 并且缓存未失效，说明其他线程更新成功了
            if (oldReflectionData != null &&
                (rd = oldReflectionData.get()) != null &&
                rd.redefinedCount == classRedefinedCount) {
                //直接返回
                return rd;
            }
        }
    }
```



## newInstance

`Constructor.java`

获取到Constructor之后，可以通过newInstance的方式获取到实例方法

```java
Constructor.java
   /**
     * Uses the constructor represented by this {@code Constructor} object to
     * create and initialize a new instance of the constructor's
     * declaring class, with the specified initialization parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as necessary.
     *
     * <p>If the number of formal parameters required by the underlying constructor
     * is 0, the supplied {@code initargs} array may be of length 0 or null.
     *
     * <p>If the constructor's declaring class is an inner class in a
     * non-static context, the first argument to the constructor needs
     * to be the enclosing instance; see section 15.9.3 of
     * <cite>The Java&trade; Language Specification</cite>.
     *
     * <p>If the required access and argument checks succeed and the
     * instantiation will proceed, the constructor's declaring class
     * is initialized if it has not already been initialized.
     *
     * <p>If the constructor completes normally, returns the newly
     * created and initialized instance.
     *
     * @param initargs array of objects to be passed as arguments to
     * the constructor call; values of primitive types are wrapped in
     * a wrapper object of the appropriate type (e.g. a {@code float}
     * in a {@link java.lang.Float Float})
     *
     * @return a new object created by calling the constructor
     * this object represents
     *
     * @exception IllegalAccessException    if this {@code Constructor} object
     *              is enforcing Java language access control and the underlying
     *              constructor is inaccessible.
     * @exception IllegalArgumentException  if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion; if
     *              this constructor pertains to an enum type.
     * @exception InstantiationException    if the class that declares the
     *              underlying constructor represents an abstract class.
     * @exception InvocationTargetException if the underlying constructor
     *              throws an exception.
     * @exception ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *  获取到实例方法
     */
    @CallerSensitive
    public T newInstance(Object ... initargs)
        throws InstantiationException, IllegalAccessException,
               IllegalArgumentException, InvocationTargetException
    {
        //判断语言级别的访问权限是否被覆盖
        if (!override) {
            //没有被覆盖需要检查是否有访问权限
            if (!Reflection.quickCheckMemberAccess(clazz, modifiers)) {
                Class<?> caller = Reflection.getCallerClass();
                checkAccess(caller, clazz, null, modifiers);
            }
        }
        //判断是否为枚举类型
        if ((clazz.getModifiers() & Modifier.ENUM) != 0)
            //如果是枚举类型不能通过反射创建
            throw new IllegalArgumentException("Cannot reflectively create enum objects");
        //获取constructorAccessor
        ConstructorAccessor ca = constructorAccessor;   // read volatile
        if (ca == null) {
            //如果为null, 通过acquireConstructorAccessor获取
            ca = acquireConstructorAccessor();
        }
        //通过ConstructorAccessor创建实例对象
        @SuppressWarnings("unchecked")
        T inst = (T) ca.newInstance(initargs);
        return inst;
    }
```

### acquireConstructorAccessor

构建ConstructorAccessor

```java
Constructor.java 
     // NOTE that there is no synchronization used here. It is correct
    // (though not efficient) to generate more than one
    // ConstructorAccessor for a given Constructor. However, avoiding
    // synchronization will probably make the implementation more
    // scalable.
    private ConstructorAccessor acquireConstructorAccessor() {
        // First check to see if one has been created yet, and take it
        // if so.
        ConstructorAccessor tmp = null;
        if (root != null) tmp = root.getConstructorAccessor();
        if (tmp != null) {
            //先判断是否已经创建了ConstructorAccessor 如果已经创建了，直接返回获取就可以
            constructorAccessor = tmp;
        } else {
            // Otherwise fabricate one and propagate it up to the root
            //通过reflectionFactory来创建一个新的ConstructorAccessor
            tmp = reflectionFactory.newConstructorAccessor(this);
            //设置当前Class对象当中的ConstructorAccessor对象
            setConstructorAccessor(tmp);
        }

        return tmp;
    }
```

## getDeclaredMethod

获取指定的Method对象方法

```java
    /**
     * Returns a {@code Method} object that reflects the specified
     * declared method of the class or interface represented by this
     * {@code Class} object. The {@code name} parameter is a
     * {@code String} that specifies the simple name of the desired
     * method, and the {@code parameterTypes} parameter is an array of
     * {@code Class} objects that identify the method's formal parameter
     * types, in declared order.  If more than one method with the same
     * parameter types is declared in a class, and one of these methods has a
     * return type that is more specific than any of the others, that method is
     * returned; otherwise one of the methods is chosen arbitrarily.  If the
     * name is "&lt;init&gt;"or "&lt;clinit&gt;" a {@code NoSuchMethodException}
     * is raised.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code clone()} method.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter array
     * @return  the {@code Method} object for the method of this class
     *          matching the specified name and parameters
     * @throws  NoSuchMethodException if a matching method is not found.
     * @throws  NullPointerException if {@code name} is {@code null}
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared method
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since JDK1.1
     * 获取指定的Method对象方法
     */
    @CallerSensitive
    public Method getDeclaredMethod(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        //判断当前method是否有访问权限
        checkMemberAccess(Member.DECLARED, Reflection.getCallerClass(), true);
        //privateGetDeclaredMethods获取到当前所声明的所有方法
        //根据当前传递的方法名和参数类型从声明的方法中找到匹配的方法
        Method method = searchMethods(privateGetDeclaredMethods(false), name, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(getName() + "." + name + argumentTypesToString(parameterTypes));
        }
        //返回
        return method;
    }
```

### privateGetDeclaredMethods

```java
    // Returns an array of "root" methods. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    //获取类中声明方法具体的实现
    //publicOnly 是否只获取public方法
    private Method[] privateGetDeclaredMethods(boolean publicOnly) {
        //等待系统内部类初始化完成，系统属性(sun.reflect.noCaches)被解析完成
        checkInitted();
        Method[] res;
        //判断当前是否有配置信息
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            //从缓存中获取数据
            res = publicOnly ? rd.declaredPublicMethods : rd.declaredMethods;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        //从JVM当中读取数据  getDeclaredMethods0(publicOnly) 使用native方式读取methods数组对象
        res = Reflection.filterMethods(this, getDeclaredMethods0(publicOnly));
        if (rd != null) {
            //将缓存当中的数据更新
            if (publicOnly) {
                rd.declaredPublicMethods = res;
            } else {
                rd.declaredMethods = res;
            }
        }
        return res;
    }
```

##### 
