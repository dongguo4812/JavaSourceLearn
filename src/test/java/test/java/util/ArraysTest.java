package test.java.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author dongguo
 * @date 2023/6/4
 * @description:
 */
public class ArraysTest {
    @Test
    public void test(){
        System.out.println("当前数组长度为奇数时：");
        int[] sort = new int[]{10,10,30,40,50,60,70};
        int index = Arrays.binarySearch(sort,10);
        System.out.println("10的下标为："+index);
        sort = new int[]{10,20,20,40,50,60,70};
        index = Arrays.binarySearch(sort,20);
        System.out.println("20的下标为："+index);
        sort = new int[]{10,20,30,30,50,60,70};
        index = Arrays.binarySearch(sort,30);
        System.out.println("30的下标为："+index);
        sort = new int[]{10,20,30,40,40,60,70};
        index = Arrays.binarySearch(sort,40);
        System.out.println("40的下标为："+index);
        sort = new int[]{10,20,30,40,50,50,70};
        index = Arrays.binarySearch(sort,50);
        System.out.println("50的下标为："+index);
        sort = new int[]{10,20,30,40,40,60,60};
        index = Arrays.binarySearch(sort,60);
        System.out.println("60的下标为："+index);
        System.out.println("--------------");

        System.out.println("当前数组长度为偶数时：");
        sort = new int[]{10,10,30,40,50,60};
        index = Arrays.binarySearch(sort,10);
        System.out.println("10的下标为："+index);
        sort = new int[]{10,20,20,40,50,60};
        index = Arrays.binarySearch(sort,20);
        System.out.println("20的下标为："+index);
        sort = new int[]{10,20,30,30,50,60};
        index = Arrays.binarySearch(sort,30);
        System.out.println("30的下标为："+index);
        sort = new int[]{10,20,30,40,40,60};
        index = Arrays.binarySearch(sort,40);
        System.out.println("40的下标为："+index);
        sort = new int[]{10,20,30,40,50,50};
        index = Arrays.binarySearch(sort,50);
        System.out.println("50的下标为："+index);
    }
    @Test
    public void test1(){
        int[] sort = new int[]{0,1,2,3,4,5};
        int index = Arrays.binarySearch(sort,0);
        System.out.println("10的下标为："+index);
    }
}
