package me.rerere.matrix.util.concurrent;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.FunctionReferenceImpl;

/** Kotlin Function0 adapter around Runnable.run. */
public final class RunnableFunctionReference extends FunctionReferenceImpl implements Function0 {

    public RunnableFunctionReference(Object runnable) {
        super(0, runnable, Runnable.class, "run", "run()V", 0);
    }

    @Override public Object invoke() {
        ((Runnable) this.receiver).run();
        return Unit.INSTANCE;
    }
}
