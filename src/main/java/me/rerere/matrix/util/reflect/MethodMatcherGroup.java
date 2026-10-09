package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import me.rerere.matrix.util.reflect.MethodParameterCountPredicate;
import me.rerere.matrix.util.MethodReturnTypePredicate;
import me.rerere.matrix.util.reflect.MethodParameterTypeMatcher;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.reflect.ReflectionMethodRegistry;

/** Composite matcher that applies each registered predicate to a reflected method. */
public final class MethodMatcherGroup extends ReflectiveMemberMatcher<Method> {
    public void addModifierConstraint(int modifierMask) {
        predicateList().add(new ModifierMaskMatcher(modifierMask));
    }

    @Override
    public boolean test(Method method) {
        return test(method, 0);
    }

    public void addParameterCountConstraint(int expectedCount) {
        predicateList().add(new MethodParameterCountPredicate(expectedCount));
    }

    public void addCallbackPredicate(Function1 callback) {
        ReflectionMethodRegistry predicate = new ReflectionMethodRegistry();
        callback.invoke(predicate);
        predicateList().add(predicate);
    }

    public void addPredicate(Function1 predicateFactory) {
        predicateList().add(new MethodReturnTypePredicate(predicateFactory));
    }

    public boolean test(Method method, int mode) {
        return testPredicates(method, mode);
    }

    private ArrayList<ReflectiveMemberMatcher<Method>> predicateList() {
        return predicates();
    }

    private boolean testPredicates(Method method, int mode) {
        ArrayList<ReflectiveMemberMatcher<Method>> predicates = predicateList();
        for (ReflectiveMemberMatcher<Method> predicate : predicates) {
            boolean matches = predicate.test(method);
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    public void addMethodPropertyConstraint(int property, boolean enabled, Function1 predicate) {
        predicateList().add(new MethodParameterTypeMatcher(property, enabled, predicate));
    }

    public void addMethodNamePredicates(Function1[] predicates) {
        this.predicateList().add(new AnyMethodPredicate(predicates));
    }

    public MethodMatcherGroup() {
        super();
    }

    public void addMethodNamePredicate(Function1 namePredicate) {
        predicateList().add(new MethodNameMatcher(namePredicate));
    }
}
