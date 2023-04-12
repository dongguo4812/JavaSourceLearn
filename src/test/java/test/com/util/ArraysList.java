package test.com.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author dongguo
 * @date 2023/4/11
 * @description:
 */
public class ArraysList {
    @Test
    public void testAsList(){
        List<Integer> list = Arrays.asList(1,2,3);
        System.out.println(list);
        list.add(4);
        System.out.println(list);
    }
}
