package me.rerere.matrix.callbacks;

/** Kotlin function-reference adapter that invokes a captured Runnable. */
public final class RunnableFunctionReference extends me.rerere.matrix.thirdparty.kotlin.jvm.internal.FunctionReferenceImpl implements me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0 {

    public RunnableFunctionReference(Object receiver) {
        super(0, receiver, Runnable.class, "run", "run()V", 0);
    }

    private void runCaptured() {
        ((Runnable) this.receiver).run();
    }

    @Override public Object invoke() {
        runCaptured();
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }


}
