package test;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Dongguo
 * @date 2021/8/20 0020-9:47
 * @description:
 */
class TestTest {
    @Test
    public void test1() {

        String array[]= {"hello","world","java"};
//        List<String> list1 = Arrays.asList(array);
//        list1.add("zhangsan");
//        System.out.println(list1);
//        list1.remove("zhangsan");
//        System.out.println(list1);
        List<String> list = new ArrayList<String>(Arrays.asList(array));
        list.add("zhangsan");
        System.out.println(list);
        list.remove("zhangsan");
        System.out.println(list);
    }
}