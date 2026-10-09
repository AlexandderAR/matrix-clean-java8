package me.rerere.matrix.util.reflect;

import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.ClassPredicateSet;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.license.LicenseResponseException;

public final class ConfiguredClassPredicate extends ReflectiveMemberMatcher<Class<?>> {
    public final Function1<? super ClassPredicateSet, ?>[] configurations;

    public ConfiguredClassPredicate(Function1<? super ClassPredicateSet, ?>[] configurations) {
        super();
        this.configurations = configurations;
    }

    @Override
    public boolean test(Class<?> candidate) {
        Function1<? super ClassPredicateSet, ?>[] configurationFunctions = configurations;
        int count = configurationFunctions.length;
        int index = 0;
        while (index < count) {
            Function1<? super ClassPredicateSet, ?> configure = configurationFunctions[index];
            ClassPredicateSet candidateCriteria = new ClassPredicateSet();
            try {
                configure.invoke(candidateCriteria);
                if (candidateCriteria.test(candidate)) {
                    return true;
                }
            } catch (LicenseResponseException failure) {
                throw failure;
            }
            index++;
        }
        return false;
    }

}
