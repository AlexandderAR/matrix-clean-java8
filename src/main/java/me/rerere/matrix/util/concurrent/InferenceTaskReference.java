package me.rerere.matrix.util.concurrent;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.FunctionReferenceImpl;
import me.rerere.matrix.inference.InferenceTask;

public final class InferenceTaskReference extends FunctionReferenceImpl implements Function0 {
    public InferenceTaskReference(Object task) {
        super(0, task, InferenceTask.class, "run", "run()V", 0);
    }

    @Override
    public Unit invoke() {
        ((InferenceTask) receiver).run();
        return Unit.INSTANCE;
    }
}
