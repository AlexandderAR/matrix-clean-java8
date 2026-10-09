package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$BooleanRef;
import me.rerere.matrix.thirdparty.org.objectweb.asm.MethodVisitor;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.util.bytecode.JumpOpcodeFilter;
import me.rerere.matrix.util.reflect.MethodProbeHarness;
import me.rerere.matrix.util.Exceptions;

/** Filters reflected methods through configured bytecode visitor checks. */
public final class MethodVisitorFilter extends ReflectiveMemberMatcher<Method> {
    public final int[] selectedCodes;

    public MethodVisitorFilter(int[] selectedCodes) {
        super();
        this.selectedCodes = selectedCodes;
    }

    @Override
    public boolean test(Method method) {
        return matches(method);
    }

    public boolean matches(Method method) {
        Ref$BooleanRef matched = new Ref$BooleanRef();
        MethodVisitor visitor = (MethodVisitor) new JumpOpcodeFilter(selectedCodes, matched);
        try {
            MethodProbeHarness.inspectMethod(method, visitor);
        } catch (Exception failure) {
            throw Exceptions.propagate(failure);
        }
        return matched.element;
    }
}
