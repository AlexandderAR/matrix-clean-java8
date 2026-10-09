package me.rerere.matrix.util.reflect;

import java.lang.reflect.Field;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Adapts a field-name predicate to the reflective member matcher base class. */
public final class FieldNameMatcherAdapter
        extends me.rerere.matrix.util.reflect.ReflectiveMemberMatcher<Field> {
    public final Function1<String, Boolean> predicate;

    @SuppressWarnings("unchecked")
    public FieldNameMatcherAdapter(Function1<? super String, Boolean> predicate) {
        this.predicate = (Function1<String, Boolean>) predicate;
    }

    @Override
    public boolean test(Field field) {
        return matchesFieldName(field);
    }

    public boolean matchesFieldName(Field field) {
        return predicate.invoke(field.getName());
    }
}
