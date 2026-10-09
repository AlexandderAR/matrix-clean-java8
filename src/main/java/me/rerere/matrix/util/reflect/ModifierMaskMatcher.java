package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;

/** Matches methods whose modifier bits equal the configured mask. */
public final class ModifierMaskMatcher extends ReflectiveMemberMatcher<Method> {
    public final int requiredModifiers;

    public ModifierMaskMatcher(int requiredModifiers) {
        super();
        this.requiredModifiers = requiredModifiers;
    }

    @Override
    public boolean test(Method method) {
        return (method.getModifiers() & requiredModifiers) == requiredModifiers;
    }

    public boolean testMethod(Method method) {
        return test(method);
    }

}
