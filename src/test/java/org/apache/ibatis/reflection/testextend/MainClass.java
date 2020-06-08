package org.apache.ibatis.reflection.testextend;

public class MainClass {

    TestA<String, Integer> testA = new TestA<String, Integer>();

    TestAGrandSon<Long> testAGrandSon = new TestAGrandSon<>();

    TestASon<Long, Long> testASon = new TestAGrandSon<>();


}
