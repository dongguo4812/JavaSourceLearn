# Collectors.groupingBy() 输出乱序

## 场景

比如说将有序的订单列表（按照创建时间降序），以订单编号进行分组，返回订单列表信息

使用Collectors.groupingBy最终返回给前端的数据和分组前有序的订单列表顺序不一致，产生了乱序输出。

```java
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dongguo
 * @date 2023/6/17
 * @description:
 */
public class CollectorsTest {
    @Test
    public void testGroupingBy() {
        List<Order> list = Arrays.asList(
                new Order("339242501193350531", "苹果", DateUtil.parse("2021-10-10 10:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501193350531", "橘子", DateUtil.parse("2021-10-10 10:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501183340238", "香蕉", DateUtil.parse("2021-10-10 8:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501180357433", "手机壳", DateUtil.parse("2021-10-10 6:10:10", DatePattern.NORM_DATETIME_PATTERN)));
        List<Order> orderList = new ArrayList<>(list);
        orderList.forEach(System.out::println);
        Map<String, List<Order>> orderMap = orderList
                .stream()
                .collect(Collectors.groupingBy(Order::getOrderNo));
        System.out.println(orderMap);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Order {
        private String orderNo;
        private String orderName;
        private Date createDate;
    }
}

```

输出

```json
CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10)
CollectorsTest.Order(orderNo=339242501193350531, orderName=橘子, createDate=2021-10-10 10:10:10)
CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10)
CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10)
{339242501180357433=[CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10)], 339242501193350531=[CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10), CollectorsTest.Order(orderNo=339242501193350531, orderName=橘子, createDate=2021-10-10 10:10:10)], 339242501183340238=[CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10)]}
```

查看Collectors.toMap()源码发现其默认输出的Map是HashMap，而HashMap不是按顺序存的。

```java
    /**
     * Returns a {@code Collector} implementing a "group by" operation on
     * input elements of type {@code T}, grouping elements according to a
     * classification function, and returning the results in a {@code Map}.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The collector produces a {@code Map<K, List<T>>} whose keys are the
     * values resulting from applying the classification function to the input
     * elements, and whose corresponding values are {@code List}s containing the
     * input elements which map to the associated key under the classification
     * function.
     *
     * <p>There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} or {@code List} objects returned.
     * @implSpec
     * This produces a result similar to:
     * <pre>{@code
     *     groupingBy(classifier, toList());
     * }</pre>
     *
     * @implNote
     * The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If
     * preservation of the order in which elements appear in the resulting {@code Map}
     * collector is not required, using {@link #groupingByConcurrent(Function)}
     * may offer better parallel performance.
     *
     * @param <T> the type of the input elements
     * @param <K> the type of the keys
     * @param classifier the classifier function mapping input elements to keys
     * @return a {@code Collector} implementing the group-by operation
     *
     * @see #groupingBy(Function, Collector)
     * @see #groupingBy(Function, Supplier, Collector)
     * @see #groupingByConcurrent(Function)
     */
    public static <T, K> Collector<T, ?, Map<K, List<T>>>
    groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, toList());
    }

    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, grouping elements according to a
     * classification function, and then performing a reduction operation on
     * the values associated with a given key using the specified downstream
     * {@code Collector}.
     *
     * <p>The classification function maps elements to some key type {@code K}.
     * The downstream collector operates on elements of type {@code T} and
     * produces a result of type {@code D}. The resulting collector produces a
     * {@code Map<K, D>}.
     *
     * <p>There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code Map} returned.
     *
     * <p>For example, to compute the set of last names of people in each city:
     * <pre>{@code
     *     Map<City, Set<String>> namesByCity
     *         = people.stream().collect(groupingBy(Person::getCity,
     *                                              mapping(Person::getLastName, toSet())));
     * }</pre>
     *
     * @implNote
     * The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If
     * preservation of the order in which elements are presented to the downstream
     * collector is not required, using {@link #groupingByConcurrent(Function, Collector)}
     * may offer better parallel performance.
     *
     * @param <T> the type of the input elements
     * @param <K> the type of the keys
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Collector} implementing the cascaded group-by operation
     * @see #groupingBy(Function)
     *
     * @see #groupingBy(Function, Supplier, Collector)
     * @see #groupingByConcurrent(Function, Collector)
     */
    public static <T, K, A, D>
    Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier,
                                          Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, HashMap::new, downstream);
    }
```

HashMap输出乱序，和它存数据的方式有关。

HashMap使用哈希表来存储，为了解决Hash冲突使用的是链地址法。在每个数组元素上都一个链表结构，当数据被Hash后，得到被扰乱的hash值，然后通过位与运算获取数组下标，最后把数据放在该数组下标链表上。

遍历输出时，是遍历的tab[]数组，而数组经过计算hash值，存进的数据和最初的List顺序可能不一致了。



如果想要输出有序，推荐使用LinkedHashMap。

LinkedHashMap除实现HashMap，还维护了一个双向链表。LinkedHashMap为每个Entry添加了前驱和后继，每次向linkedHashMap插入键值对，除了将其插入到哈希表的对应位置之外，还要将其插入到双向循环链表的尾部。



## 解决方案

为保证输出有序，选择LinkedHashMap，具体修改方案如下：

```java
    @Test
    public void testGroupingBy() {
        List<Order> list = Arrays.asList(
                new Order("339242501193350531", "苹果", DateUtil.parse("2021-10-10 10:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501193350531", "橘子", DateUtil.parse("2021-10-10 10:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501183340238", "香蕉", DateUtil.parse("2021-10-10 8:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501180357433", "手机壳", DateUtil.parse("2021-10-10 6:10:10", DatePattern.NORM_DATETIME_PATTERN)));
        List<Order> orderList = new ArrayList<>(list);
        orderList.forEach(System.out::println);
//        Map<String, List<Order>> orderMap = orderList
//                .stream()
//                .collect(Collectors.groupingBy(Order::getOrderNo));
        Map<String, List<Order>> orderMap = orderList
                .stream()
                .collect(Collectors.groupingBy(Order::getOrderNo, LinkedHashMap::new, Collectors.toList()));
        System.out.println(orderMap);
    }
```

输出

```json
CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10)
CollectorsTest.Order(orderNo=339242501193350531, orderName=橘子, createDate=2021-10-10 10:10:10)
CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10)
CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10)
{339242501193350531=[CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10), CollectorsTest.Order(orderNo=339242501193350531, orderName=橘子, createDate=2021-10-10 10:10:10)], 339242501183340238=[CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10)], 339242501180357433=[CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10)]}
```



# Collectors.toMap() 输出乱序

## 场景

同Collectors.groupingBy()，使用Collectors.toMap()默认也是使用HashMap接收，也会出现乱序问题

```java
    @Test
    public void testToMap() {
        List<Order> list = Arrays.asList(
                new Order("339242501193350531", "苹果", DateUtil.parse("2021-10-10 10:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                 new Order("339242501183340238", "香蕉", DateUtil.parse("2021-10-10 8:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501180357433", "手机壳", DateUtil.parse("2021-10-10 6:10:10", DatePattern.NORM_DATETIME_PATTERN)));
        List<Order> orderList = new ArrayList<>(list);
        orderList.forEach(System.out::println);
        Map<String, Order> orderMap = orderList
                .stream()
                .collect(Collectors.toMap(Order::getOrderNo, order -> order, (k1, k2) -> k1));
        System.out.println(orderMap);
    }
```

输出

```java
CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10)
CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10)
CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10)
{339242501180357433=CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10), 339242501193350531=CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10), 339242501183340238=CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10)}
```

以Collectors.toMap(Order::getOrderNo, order -> order, (k1, k2) -> k1)为例，Collectors.toMap()有三个参数，第一个参数Order::getOrderNo为Map的key，第二个参数order -> order为value，第三个参数(k1, k2) -> k1表示出现相同的key时，取旧key值对应的value值。

```java
    /**
     * Returns a {@code Collector} that accumulates elements into a
     * {@code Map} whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped
     * keys contains duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the
     * results are merged using the provided merging function.
     *
     * @apiNote
     * There are multiple ways to deal with collisions between multiple elements
     * mapping to the same key.  The other forms of {@code toMap} simply use
     * a merge function that throws unconditionally, but you can easily write
     * more flexible merge policies.  For example, if you have a stream
     * of {@code Person}, and you want to produce a "phone book" mapping name to
     * address, but it is possible that two persons have the same name, you can
     * do as follows to gracefully deals with these collisions, and produce a
     * {@code Map} mapping names to a concatenated list of addresses:
     * <pre>{@code
     *     Map<String, String> phoneBook
     *         people.stream().collect(toMap(Person::getName,
     *                                       Person::getAddress,
     *                                       (s, a) -> s + ", " + a));
     * }</pre>
     *
     * @implNote
     * The returned {@code Collector} is not concurrent.  For parallel stream
     * pipelines, the {@code combiner} function operates by merging the keys
     * from one map into another, which can be an expensive operation.  If it is
     * not required that results are merged into the {@code Map} in encounter
     * order, using {@link #toConcurrentMap(Function, Function, BinaryOperator)}
     * may offer better parallel performance.
     *
     * @param <T> the type of the input elements
     * @param <K> the output type of the key mapping function
     * @param <U> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied
     *                      to {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code Collector} which collects elements into a {@code Map}
     * whose keys are the result of applying a key mapping function to the input
     * elements, and whose values are the result of applying a value mapping
     * function to all input elements equal to the key and combining them
     * using the merge function
     *
     * @see #toMap(Function, Function)
     * @see #toMap(Function, Function, BinaryOperator, Supplier)
     * @see #toConcurrentMap(Function, Function, BinaryOperator)
     */
    public static <T, K, U>
    Collector<T, ?, Map<K,U>> toMap(Function<? super T, ? extends K> keyMapper,
                                    Function<? super T, ? extends U> valueMapper,
                                    BinaryOperator<U> mergeFunction) {
        //默认使用HashMap
        return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
    }
```

## 解决方案

```java
    @Test
    public void testToMap() {
        List<Order> list = Arrays.asList(
                new Order("339242501193350531", "苹果", DateUtil.parse("2021-10-10 10:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                 new Order("339242501183340238", "香蕉", DateUtil.parse("2021-10-10 8:10:10", DatePattern.NORM_DATETIME_PATTERN)),
                new Order("339242501180357433", "手机壳", DateUtil.parse("2021-10-10 6:10:10", DatePattern.NORM_DATETIME_PATTERN)));
        List<Order> orderList = new ArrayList<>(list);
        orderList.forEach(System.out::println);
//        Map<String, Order> orderMap = orderList
//                .stream()
//                .collect(Collectors.toMap(Order::getOrderNo, order -> order, (k1, k2) -> k1));
        Map<String, Order> orderMap = orderList
                .stream()
                .collect(Collectors.toMap(Order::getOrderNo, order -> order, (k1, k2) -> k1, LinkedHashMap::new));
        System.out.println(orderMap);
    }
```

输出

```java
CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10)
CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10)
CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10)
{339242501193350531=CollectorsTest.Order(orderNo=339242501193350531, orderName=苹果, createDate=2021-10-10 10:10:10), 339242501183340238=CollectorsTest.Order(orderNo=339242501183340238, orderName=香蕉, createDate=2021-10-10 08:10:10), 339242501180357433=CollectorsTest.Order(orderNo=339242501180357433, orderName=手机壳, createDate=2021-10-10 06:10:10)}
```

