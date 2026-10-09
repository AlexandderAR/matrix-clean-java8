package me.rerere.matrix.util;

import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Predicate that delegates a class simple name to a supplied function. */
public final class ClassNamePredicate extends ReflectiveMemberMatcher<Class<?>> {
    public final Function1 namePredicate;

    public ClassNamePredicate(Function1 namePredicate) {
        super();
        this.namePredicate = namePredicate;
    }

    public boolean matchesArguments(Class<?> candidateClass) {
        return matchesClass(candidateClass);
    }

    @Override
    public final boolean test(Class<?> candidateClass) {
        return matchesArguments(candidateClass);
    }

    private boolean matchesClass(Class<?> candidateClass) {
        return ((Boolean) namePredicate.invoke(candidateClass.getSimpleName())).booleanValue();
    }

}
