package me.rerere.matrix.util.bytecode;

import java.lang.reflect.Method;
import me.rerere.matrix.thirdparty.org.objectweb.asm.MethodVisitor;
import me.rerere.matrix.util.Exceptions;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.util.reflect.MethodProbeHarness;
import me.rerere.matrix.util.OpcodeFilterMethodVisitor;

/** Applies configured opcode filters while visiting a reflected method. */
public final class PackedMethodAdapter extends ReflectiveMemberMatcher<Method> {
    public final int[] salts;

    public PackedMethodAdapter(int[] salts) {
        super();
        this.salts = salts;
    }

    public boolean emitMethod(Method method) {
        return inspectMethod(method);
    }

    @Override
    public boolean test(Method method) {
        return inspectMethod(method);
    }

    private boolean inspectMethod(Method method) {
        MethodVisitor adapter = (MethodVisitor) new OpcodeFilterMethodVisitor(salts);
        try {
            MethodProbeHarness.inspectMethod(method, adapter);
        } catch (Exception failure) {
            throw Exceptions.propagate(failure);
        }
        return false;
    }
}
