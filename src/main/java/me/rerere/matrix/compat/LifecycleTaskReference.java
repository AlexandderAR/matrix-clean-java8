package me.rerere.matrix.compat;

import me.rerere.matrix.world.BlockLocationCache;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.FunctionReferenceImpl;

/** Kotlin function reference for a receiver's run method. */
public final class LifecycleTaskReference extends FunctionReferenceImpl implements Function0 {
    public LifecycleTaskReference(Object receiver) {
        super(0, receiver, BlockLocationCache.class, "run", "run()V", 0);
    }

    public void invokeReferencedTask() {
        ((BlockLocationCache) receiver).run();
    }

    @Override
    public Object invoke() {
        invokeReferencedTask();
        return Unit.INSTANCE;
    }
}
