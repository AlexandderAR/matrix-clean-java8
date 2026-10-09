package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;

public final class MethodParameterCountPredicate extends ReflectiveMemberMatcher<Method> {
    public final int requiredParameterCount;
    public MethodParameterCountPredicate(int requiredParameterCount) {
        super();
        this.requiredParameterCount = requiredParameterCount;
    }

    public boolean matchesArguments(Method method) {
        return method.getParameterCount() == requiredParameterCount;
    }

    @Override
    public boolean test(Method method) {
        return matchesArguments(method);
    }
}
