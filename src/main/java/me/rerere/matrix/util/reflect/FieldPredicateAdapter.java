package me.rerere.matrix.util.reflect;

import java.lang.reflect.Field;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;
import me.rerere.matrix.util.ClassPredicateSet;

/** Predicate adapter for reflected field metadata. */
public final class FieldPredicateAdapter extends ReflectiveMemberMatcher<Field> {
    public final Function1<Object, Object> predicate;

    public FieldPredicateAdapter(Function1<Object, Object> predicate) {
        super();
        this.predicate = predicate;
    }

    @Override
    public boolean test(Field field) {
        return matchesType(field);
    }

    private boolean matchesType(Field field) {
        ClassPredicateSet predicates = new ClassPredicateSet();
        predicate.invoke(predicates);
        return predicates.test(field.getType());
    }

}
