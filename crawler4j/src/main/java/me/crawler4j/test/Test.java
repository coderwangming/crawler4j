package me.crawler4j.test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ¶ÅôŞ¿ı
 * @date 2018/1/12
 */
public class Test{

    public <T> List<T> fun(T... ts){
        List<T> list=new ArrayList<>();

        for (T t:ts) {
            list.add(t);
        }

        return list;
    }

    public static void main(String[] args) {
        Test t=new Test();
        t.fun("dugenkui".split("")).forEach(x-> System.out.println(x));
    }
}
