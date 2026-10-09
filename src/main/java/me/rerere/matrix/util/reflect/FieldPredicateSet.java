package me.rerere.matrix.util.reflect;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import me.rerere.matrix.util.reflect.FieldModifierPredicate;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.util.reflect.FieldPredicateAdapter;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

public final class FieldPredicateSet extends ReflectiveMemberMatcher<Field> {

    public FieldPredicateSet() {
        super();
    }

    public boolean matchesField(Field field) {
        Iterable<ReflectiveMemberMatcher<Field>> matchers = this.predicates();
        if (matchers instanceof Collection<?> && ((Collection<?>) matchers).isEmpty()) return true;
        Iterator<ReflectiveMemberMatcher<Field>> iterator = matchers.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().test(field)) return false;
        }
        return true;
    }

    public void addPredicate(Function1<?, ?> predicate) {
        this.predicates().add(new FieldPredicateAdapter((Function1) predicate));
    }

    public void addIndex(int index) {
        this.predicates().add(new FieldModifierPredicate(index));
    }

    public void addAlternativePredicate(Function1<?, ?> predicate) {
        this.predicates().add(new FieldNameMatcherAdapter((Function1) predicate));
    }

    @Override
    public boolean test(Field field) {
        return matchesField(field);
    }
}
