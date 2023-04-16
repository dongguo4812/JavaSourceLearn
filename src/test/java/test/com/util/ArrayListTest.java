package test.com.util;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dongguo
 * @date 2023/4/10
 * @description:
 */
public class ArrayListTest {
    //    Arrays.toString()
    @Test
    public void testSubList() {
        List<String> names = new ArrayList<>();
        names.add("zhangsan");
        names.add("lisi");
        names.add("wangwu");
        names = names.subList(0, 1);
        names.toString();
        System.out.println(names);
        names.add("zhaoliu");
        System.out.println(names);
    }
}
