package test.java.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author dongguo
 * @date 2023/4/10
 * @description:
 */
public class ArrayListTest {
    //    Arrays.toString()
    @Test
    public void testSubList(){
        List<String> names = new ArrayList<String>() {{
            add("zhangsan");
            add("lisi");
            add("wangwu");
        }};
        names = names.subList(0,1);
        System.out.println(names);
        names.add("zhaoliu");
        System.out.println(names);
    }
}
