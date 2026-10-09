package me.rerere.matrix.util.reflect;

import java.lang.reflect.Field;
import me.rerere.matrix.util.reflect.ReflectiveMemberMatcher;

public final class FieldModifierPredicate extends ReflectiveMemberMatcher<Field> {
    public final int requiredModifierBits;
    public FieldModifierPredicate(int requiredModifierBits) {
        super();
        this.requiredModifierBits = requiredModifierBits;
    }

    public boolean matchesArguments(Field field) {
        return (field.getModifiers() & requiredModifierBits) == requiredModifierBits;
    }

    @Override
    public boolean test(Field field) {
        return matchesArguments(field);
    }

}
