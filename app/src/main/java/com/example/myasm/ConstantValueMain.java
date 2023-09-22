package com.example.myasm;

import java.lang.reflect.Field;

public class ConstantValueMain {
    private static final int A = 10;
    private static int B = 11;
    private int C = 12;

    public ConstantValueMain() {


    }

    final void foo() {
        synchronized (this) {
            int a = 10;
        }
    }

    public void foo2() {
        int b = 11;
        foo();
    }

    public static int getReturn() {
        int a = 0;

        try {
            a = 1;
            throw new NullPointerException();

        } catch (Exception e) {
            a = 2;
        } finally {
//            return 3;
//            a = 3;
        }
        return a;
    }

    public static void main(String[] args) {
        int i = getReturn();
        System.out.println("=========" + i);
    }

}
