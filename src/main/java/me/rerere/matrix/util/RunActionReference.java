package me.rerere.matrix.util;

import me.rerere.matrix.permissions.PermissionCacheManager;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.FunctionReferenceImpl;

/** Kotlin function reference that invokes the receiver's zero-argument run method. */
public final class RunActionReference extends FunctionReferenceImpl implements Function0 {
    public RunActionReference(Object receiver) {
        super(0, receiver, PermissionCacheManager.class,
                "refreshOnlinePlayers", "refreshOnlinePlayers()V", 0);
    }

    public final void invokeTarget() {
        ((PermissionCacheManager) receiver).refreshOnlinePlayers();
    }

    @Override
    public Object invoke() {
        invokeTarget();
        return Unit.INSTANCE;
    }
}
