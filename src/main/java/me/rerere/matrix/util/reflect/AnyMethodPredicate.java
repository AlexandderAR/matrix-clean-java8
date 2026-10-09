package me.rerere.matrix.util.reflect;

import java.lang.reflect.Method;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Tests whether a reflected method matches at least one configured predicate. */
public final class AnyMethodPredicate extends ReflectiveMemberMatcher<Method> {
    private final Function1<MethodMatcherGroup, Unit>[] alternatives;

    public AnyMethodPredicate(Function1<MethodMatcherGroup, Unit>[] alternatives) {
        super();
        this.alternatives = alternatives;
    }

    @Override
    public boolean test(Method method) {
        for (Function1<MethodMatcherGroup, Unit> alternative : alternatives) {
            MethodMatcherGroup query = new MethodMatcherGroup();
            alternative.invoke(query);
            if (query.test(method)) return true;
        }
        return false;
    }
}
