package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Applies a Kotlin predicate to a reflected method's name. */
public final class MethodNameMatcher extends ReflectiveMemberMatcher<Method> {
    private final Function1<String, Boolean> namePredicate;

    public MethodNameMatcher(Function1<String, Boolean> namePredicate) {
        this.namePredicate = namePredicate;
    }

    @Override
    public boolean test(Method method) {
        return matchesMethod(method);
    }

    public final boolean matchesMethod(Method method) {
        return ((Boolean) namePredicate.invoke(method.getName())).booleanValue();
    }
}
