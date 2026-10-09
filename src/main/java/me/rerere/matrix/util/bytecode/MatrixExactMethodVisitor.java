package me.rerere.matrix.util.bytecode;

import java.lang.reflect.Method;
import java.util.Objects;
import me.rerere.matrix.thirdparty.org.objectweb.asm.ClassVisitor;
import me.rerere.matrix.thirdparty.org.objectweb.asm.MethodVisitor;
import me.rerere.matrix.thirdparty.org.objectweb.asm.Type;

/** Returns the delegate only for the reflected method's exact class-file signature. */
public final class MatrixExactMethodVisitor extends ClassVisitor {
    public final Method reflectedMethod;
    public final MethodVisitor delegate;

    public MatrixExactMethodVisitor(Method reflectedMethod, MethodVisitor delegate) {
        super(589824); // ASM9, the low 32 bits passed by the original constructor.
        this.reflectedMethod = reflectedMethod;
        this.delegate = delegate;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        try {
            if (!Objects.equals(name, reflectedMethod.getName())) return null;
            if (!Objects.equals(descriptor, Type.getMethodDescriptor(reflectedMethod))) return null;
            if (access != reflectedMethod.getModifiers()) return null;
            return delegate;
        } catch (me.rerere.matrix.license.LicenseResponseException failure) {
            throw failure;
        }
    }
}
