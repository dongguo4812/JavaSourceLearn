package test.java.util;

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
//        Map<String, List<Order>> orderMap = orderList
//                .stream()
//                .collect(Collectors.groupingBy(Order::getOrderNo));
        Map<String, List<Order>> orderMap = orderList
                .stream()
                .collect(Collectors.groupingBy(Order::getOrderNo, LinkedHashMap::new, Collectors.toList()));
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
}
