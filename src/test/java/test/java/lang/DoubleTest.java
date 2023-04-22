package test.java.lang;

import org.junit.jupiter.api.Test;

/**
 * @author dongguo
 * @date 2023/4/22
 * @description:
 */
public class DoubleTest {
    @Test
    public void test() {
        Double d = new Double(0.01);
        String string = d.toString();
        System.out.println(string);
    }
}
