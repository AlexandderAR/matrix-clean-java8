package me.rerere.matrix.crypto;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.FunctionReferenceImpl;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.listeners.LoginCooldownListener;

/** Kotlin Function0 adapter that forwards to the registered provider refresh callback. */
public final class ProviderRefreshCallback extends FunctionReferenceImpl implements Function0 {
    public ProviderRefreshCallback(Object receiver) {
        super(0, receiver, LoginCooldownListener.class, "run", "run()V", 0);
    }

    public final void invokeBridge() {
        ((LoginCooldownListener) this.receiver).run();
    }

    @Override public Object invoke() {
        invokeBridge();
        return Unit.INSTANCE;
    }
}
