package me.rerere.matrix.util;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.metrics.LatencySweepTask;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.FunctionReferenceImpl;

/** Kotlin function reference bound to a Matrix runnable receiver. */
public final class RunFunctionReference extends FunctionReferenceImpl implements Function0 {
    public RunFunctionReference(Object receiver) {
        super(0, receiver, LatencySweepTask.class,
                "run", "run()V", 0);
    }

    @Override
    public Object invoke() {
        ((LatencySweepTask) this.receiver).run();
        return Unit.INSTANCE;
    }

}
