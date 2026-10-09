package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import me.rerere.matrix.util.reflect.MethodMatcherGroup;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Applies a supplied initializer and modifier predicate to each declared method. */
public final class MethodPredicate extends ReflectiveMemberMatcher<Class<?>> {
    public final Function1 predicateFactory;

    public MethodPredicate(Function1 predicateFactory) {
        super();
        this.predicateFactory = predicateFactory;
    }

    private boolean hasDeclaredMethod(Class<?> targetClass) {
        Method[] methods = targetClass.getDeclaredMethods();
        for (Method method : methods) {
            MethodMatcherGroup matcher = new MethodMatcherGroup();
            predicateFactory.invoke(matcher);
            if (matcher.test(method)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean test(Class<?> targetClass) {
        return hasDeclaredMethod(targetClass);
    }
}
