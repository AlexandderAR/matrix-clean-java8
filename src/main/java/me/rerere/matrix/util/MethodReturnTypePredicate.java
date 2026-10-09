package me.rerere.matrix.util;

import java.lang.reflect.Method;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;

/** Applies a caller-supplied predicate, then checks the reflected method return type. */
public final class MethodReturnTypePredicate extends ReflectiveMemberMatcher<Method> {
    private final Function1<Object, ?> predicate;

    public MethodReturnTypePredicate(Function1<Object, ?> predicate) {
        super();
        this.predicate = predicate;
    }

    @Override public boolean test(Method method) {
        return matchesReturnType(method);
    }

    private boolean matchesReturnType(Method method) {
        ClassPredicateSet probe = new ClassPredicateSet();
        predicate.invoke(probe);
        return probe.test(method.getReturnType());
    }
}
