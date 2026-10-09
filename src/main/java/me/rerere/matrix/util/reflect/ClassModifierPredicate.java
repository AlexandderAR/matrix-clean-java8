package me.rerere.matrix.util.reflect;

/** Predicate that checks requested Java class modifier bits. */
public final class ClassModifierPredicate extends ReflectiveMemberMatcher<Class<?>> {
    public final int requiredModifierBits;

    public ClassModifierPredicate(int requiredModifierBits) {
        super();
        this.requiredModifierBits = requiredModifierBits;
    }

    public boolean matchesArguments(Class<?> candidate) {
        return matchesRequiredModifiers(candidate);
    }

    @Override
    public final boolean test(Class<?> candidate) {
        return matchesArguments(candidate);
    }

    private boolean matchesRequiredModifiers(Class<?> candidate) {
        return (candidate.getModifiers() & requiredModifierBits) == requiredModifierBits;
    }
}
