package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.util.ClassPredicateSet;
import me.rerere.matrix.license.LicenseResponseException;

public final class MethodParameterTypeMatcher extends ReflectiveMemberMatcher<Method> {
    private final int parameterIndex;
    private final boolean matchesWhenMissing;
    private final Function1 callback;

    public MethodParameterTypeMatcher(int parameterIndex, boolean matchesWhenMissing, Function1 callback) {
        super();
        this.parameterIndex = parameterIndex;
        this.matchesWhenMissing = matchesWhenMissing;
        this.callback = callback;
    }

    @Override
    public boolean test(Method method) {
        return matchesMethod(method);
    }

    public boolean matchesMethod(Method method) {
        Class<?> parameterType;
        try {
            parameterType = (Class<?>) ArraysKt.getOrNull(method.getParameterTypes(), parameterIndex);
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        if (parameterType == null) {
            return matchesWhenMissing;
        }
        ClassPredicateSet predicates = new ClassPredicateSet();
        callback.invoke(predicates);
        return predicates.test(parameterType);
    }

}
