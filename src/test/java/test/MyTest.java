package test;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * @author Dongguo
 * @date 2021/8/20 0020-9:47
 * @description:
 */
class MyTest {
    @Test
    public void test1() {
        String array[] = {"hello", "world", "java"};
        List<String> list = new ArrayList<String>(Arrays.asList(array));
        list.add("zhangsan");
        System.out.println(list);
        list.remove("zhangsan");
        System.out.println(list);
    }
    @Test
    public void testAdd(){
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
    }


    @Test
    public void test2() {
        HashMap hashMap = new HashMap();
        hashMap.put(1, "1");
        hashMap.put(1, "1");
        hashMap.put(11, "11");
        System.out.println(hashMap);
    }
}
