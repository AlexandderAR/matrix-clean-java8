package me.rerere.matrix.util;

import java.util.ArrayList;
import java.util.Iterator;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.util.reflect.ConfiguredClassPredicate;
import me.rerere.matrix.util.reflect.ClassModifierPredicate;
import me.rerere.matrix.util.reflect.MethodPredicate;
import me.rerere.matrix.util.reflect.ExactClassPredicate;

public final class ClassPredicateSet extends ReflectiveMemberMatcher<Class<?>> {

    public ClassPredicateSet() {
        super();
    }

    public void addPredicate(Function1 predicate) {
        this.predicates().add(new ClassNamePredicate(predicate));
    }

    @Override
    public boolean test(Class<?> candidate) {
        ArrayList<ReflectiveMemberMatcher<Class<?>>> matchers = this.predicates();
        if (matchers.isEmpty()) return true;
        Iterator<ReflectiveMemberMatcher<Class<?>>> iterator = matchers.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().test(candidate)) return false;
        }
        return true;
    }

    public void addIndex(int index) {
        this.predicates().add(new ClassModifierPredicate(index));
    }

    public void addCompositePredicates(Function1[] predicates) {
        this.predicates().add(new ConfiguredClassPredicate(predicates));
    }

    public void addAlternativePredicate(Function1 predicate) {
        this.predicates().add(new MethodPredicate(predicate));
    }

    public void addClassPredicate(Class<?>[] classes) {
        this.predicates().add(new ExactClassPredicate(classes));
    }

}
