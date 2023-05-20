# *本次针对jdk8u版本的搭建*

## **1.新建项目**

新建java项目JavaSourceLearn  ，这里我创建的是maven  

![image-20230520090429984](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954597.png)

## **2.获取JDK源码**

打开Project Structure

![image-20230520085654043](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954986.png)

找到本地JDK安装位置将src.zip解压到项目java包中

![image-20230520085713356](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954694.png)

整理下项目结构，删除用不到的目录

![image-20230520090526428](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954462.png)

**提示**: 添加源码到项目之后首次运行需要较长时间进行编译,建议先设置好**进程堆**,防止编译失败

## **3.构建进程堆大小**

打开File -> Settings -> Build, Execution, Deployment -> Compiler

设置Build process heap size (Mbytes)

![image-20230520090849390](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954862.png)

## **4.调试设置**

打开File -> Settings -> Build, Execution, Deployment -> Debugger -> Stepping

取消Do not step into the classes

![image-20230520090923817](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954046.png)

**说明**: 该设置勾选后会在Debug时不进入到相关类路径下的方法内

## **5. 设置源码路径**

为了能够注释以及避免修改到其他项目所使用的内容，

1.添加一个名为“JavaSourceLearn1.8”的SDK

2.并将Sourcepath修改为当前项目src下的源码内容（新增）

3.移除Sourcepath中关联的src.zip（删除）

![image-20230520091811390](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954487.png)

修改项目使用的JDK为新建的JavaSourceLearn1.8

![image-20230520092337761](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954094.png)

**说明**: 原先JDK源码包为锁定状态,Debug进入源码后无法修改/添加注释 ，这样就可以添加注释了

## **6. 解决报错问题**

###  找不到UNIXToolkit和FontConfigManager

这是因为在Windows平台下缺少了这2个java类文件所导致的。

这2个类文件可在OpenJDK网站上找到，地址：http://openjdk.java.net/，打开后点Mercurial，点jdk8u，点jdk，点browse，点src，点solaris，点classes，点sun，可以找到2个文件夹，awt和font，缺少的2个文件分别在这2个文件夹下



![image-20230520092407165](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200954928.png)

![image-20230520092412349](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955638.png)

按照下图所示目录结构，新建同样名字的目录和空内容的文件

打开openJDK上同样名字的java文件，把对应的内容复制到IDEA

### 程序包com.sun.tools.javac.*不存在

这是缺少tools的jar包所导致的。

点击**File–>Project Structure–>SDKs**，手动把jdk8的tools.jar包添加到JavaSourceLearn1.8，

![image-20230520092648828](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955007.png)

这些都解决了，重新build发现还是有错误

### GTKLookAndFeel类依然报红

![image-20230520092803462](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955167.png)

百度了一下也没有找到原因，觉得这是和Unit平台相关的判断，应该问题不大，注释掉启动果然OK



## **7.代码换行注释后， debug 错行的问题**

之后在这里的源码中换行注释，打断点 debug 会出现错行的问题，并且无法显示局部变量，提示 source code does not match bytecode。

![image-20230520093237785](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955238.png)

临时解决方案：E:\workspace\JavaSourceLearn\target\classes为当前项目编译后classes的路径

让BootStrapClassLoader优先加载我们的类

```
-Xbootclasspath/p:"E:\workspace\JavaSourceLearn\target\classes"
```

在测试类中配置VM options

![image-20230520094137158](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955063.png)

![image-20230520093349336](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955814.png)

这样就不受注释的影响了，这样每次都要配置，可以配置一个模板，每次创建都自动指定



![image-20230520094217813](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955651.png)

![image-20230520094253673](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955851.png)

## **新建测试**

```java
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dongguo
 * @date 2021/8/19 0019-19:05
 * @description:
 */
public class Test {
    public static void main(String[] args) {
        Map<Integer,String> hashMap = new HashMap<>();
        hashMap.put(1,"张三");
        hashMap.put(2,"李四");
        hashMap.put(3,"王五");
    }
}
```

启动调试，F7调试进入源码内部

![image-20230520094349950](https://gitee.com/dongguo4812_admin/image/raw/master/image/202305200955811.png)

此时进入到我们自己搭建的源码阅读环境中。并且可以在源码中添加自己的注释。