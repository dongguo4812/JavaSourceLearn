# **jdk8低版本中 elementData = c.toArray();存在bug**

```java
public ArrayList(Collection<? extends E> c) {
    elementData = c.toArray();
    if ((size = elementData.length) != 0) {
        //这里有个bug，c.toArray()可能不会返回Object[] ,官方bug库编号 6260652，详情请参看https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6260652
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(elementData, size, Object[].class);
    } else {
        // replace with empty array.
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
```

官方bug库编号 6260652，详情请参看https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6260652



能够看到这个bug提交时间是2005-04-25，受到影响的版本追溯到jdk5，好像在jdk8后期的版本中修复了，子任务中看到jdk9受到了影响，所以显示修复了jdk9的bug





大意是Arrays.asList()返回的是List的实例，List的实例调用toArray()方法应该总是返回Object[]，

但是在以前的版本中，它可能会返回某种子类型的数组，

Collection.toArray()声明的返回值是Object[]，它允许返回子类型数组的实例，但是在明确要求返回Object[]后，可能会导致某些成功运行的代码出现ClassCastException





# **jdk8高版本**

```java
    public ArrayList(Collection<? extends E> c) {
        Object[] a = c.toArray();
        if ((size = a.length) != 0) {
            if (c.getClass() == ArrayList.class) {
                elementData = a;
            } else {
                elementData = Arrays.copyOf(a, size, Object[].class);
            }
        } else {
            // replace with empty array.
            elementData = EMPTY_ELEMENTDATA;
        }
    }
```

