package test.java.util;

import org.junit.jupiter.api.Test;

import java.util.Hashtable;

/**
 * @author dongguo
 * @date 2023/4/16
 * @description:
 */
public class HashTableTest {
    @Test
    public void test(){
        Hashtable<Object, Object> hashtable = new Hashtable<>();
        hashtable.put(1,"1");
        hashtable.hashCode();
    }
}
