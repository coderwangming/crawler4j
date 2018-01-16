package me.crawler4j.test;

/**
 * @author ¶ÅôÞ¿ý
 * @date 2018/1/12
 */
public class Test implements Runnable{
    public static Object lock=new Object();
    int method=0;
    public Test(int method) {
        this.method=method;
    }

    public synchronized void func1(){
        while(true) {
            System.out.println("aaa");
        }
    }

    public void func2(){
       synchronized(this){
           while(true){
                System.out.println("bbb");
            }
        }
    }

    public void func3(){
        while(true){
            synchronized(this){
                System.out.println("ccc");
            }
        }
    }

    @Override
    public void run() {
        Test test=new Test(method);
        if(method==1)
            test.func1();
        if(method==2)
              test.func2();
        if(method==3)
             test.func3();
    }

    public static void main(String[] args) {
        Thread t=new Thread(new Test(1));
        Thread t2=new Thread(new Test(2));
        Thread t3=new Thread(new Test(3));
        t.start();
        t2.start();
        t3.start();
    }
}
