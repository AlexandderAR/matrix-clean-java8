package me.rerere.matrix.util.reflect;

import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;

public final class ExactClassPredicate extends ReflectiveMemberMatcher<Class<?>> {
    public final Class<?>[] acceptedClasses;

    public ExactClassPredicate(Class<?>[] acceptedClasses) {
        super();
        this.acceptedClasses = acceptedClasses;
    }

    private boolean matchesClass(Class<?> candidate) {
        Class<?>[] accepted = acceptedClasses;
        int count = accepted.length;
        int index = 0;
        while (index < count) {
            Class<?> acceptedClass = accepted[index];
            if (java.util.Objects.equals(acceptedClass, candidate)) {
                return true;
            }
            index++;
        }
        return false;
    }

    @Override
    public boolean test(Class<?> candidate) {
        return matchesClass(candidate);
    }
}
