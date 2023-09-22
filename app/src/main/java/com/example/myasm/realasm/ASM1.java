package com.example.myasm.realasm;


import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ASM1 {
    public static void main(String[] args) {
        demo2();
    }

    public static void demo2() {

        try {
            String filePath = "com/example/myasm/realasm/";
            // TODO: 2023/9/21 javac 编译不了
            File file = new File(filePath + "SensorsData.class");
            byte[] bytes = FileUtils.readFileToByteArray(file);
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(new ParsingVisitor(),0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void demo1() {
        try {
            // 初始化ClassReader，通过全限定名来加载类
            ClassReader classReader = new ClassReader("java/lang/String");
            // 获取类名信息
            String className = classReader.getClassName();
            // 获取接口信息
            String[] interfaces = classReader.getInterfaces();
            // 获取父类信息
            String superName = classReader.getSuperName();
            // 获取访问限制符
            int access = classReader.getAccess();

            StringBuilder sb = new StringBuilder();
            if ((access & Opcodes.ACC_PUBLIC) != 0) {
                sb.append("public").append(" ");
            }
            sb.append(className)
                    .append(" ")
                    .append("extends")
                    .append(" ")
                    .append(superName);
            if (interfaces != null && interfaces.length != 0) {
                sb.append(" ")
                        .append("implements")
                        .append(" ")
                        .append(Arrays.toString(interfaces)
                                .replace("[", "")
                                .replace("]", ""));
            }

            System.out.println("======" + sb);
            System.out.println("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
