package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import me.rerere.matrix.util.reflect.MethodVisitorFilter;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.util.bytecode.PackedMethodAdapter;

/** Registry that applies method predicates in insertion order. */
public final class ReflectionMethodRegistry extends ReflectiveMemberMatcher<Method> {

    public ReflectionMethodRegistry() {
        super();
    }

    /** Adds a parameter-count rule. */
    public void addParameterCounts(int[] counts) {
        predicates().add(new MethodVisitorFilter(counts));
    }

    /** Adds the companion rule kind. */
    public void addModifierFlags(int[] flags) {
        predicates().add(new PackedMethodAdapter(flags));
    }

    @Override
    public boolean test(Method method) {
        return test(method, 0);
    }

    /** Tests every registered rule; negative mode retries a failed rule. */
    public boolean test(Method method, int mode) {
        ArrayList<ReflectiveMemberMatcher<Method>> rules = predicates();
        for (ReflectiveMemberMatcher<Method> rule : rules) {
            while (!rule.test(method)) {
                if (mode >= 0) return false;
            }
        }
        return true;
    }

}
