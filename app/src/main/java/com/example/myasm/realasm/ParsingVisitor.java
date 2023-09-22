package com.example.myasm.realasm;

import org.objectweb.asm.*;

public class ParsingVisitor extends ClassVisitor {
    public ParsingVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        System.out.println("===== class " + name + " extends " + superName + " {");
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        System.out.println(" " + name + ", " + descriptor);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        System.out.println(" " + name + " , " + descriptor);
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        System.out.println(" } end");
    }
}
