package me.rerere.matrix.compat;

import me.rerere.matrix.compat.ScheduledTaskProxy;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Kotlin callback adapter for an asynchronous scheduled task. */
public final class AsyncTaskCallback implements Function1<Object, Object> {
    private final Function0<?> task;

    public AsyncTaskCallback(Function0<?> task) {
        this.task = task;
    }

    @Override
    public Object invoke(Object boxedScheduledTask) {
        invokeScheduledTask(((ScheduledTaskProxy) boxedScheduledTask).unboxImpl());
        return Unit.INSTANCE;
    }

    public final void invokeScheduledTask(Object unboxedScheduledTask) {
        // The wrapper is unboxed by invoke; this adapter runs its stored action.
        task.invoke();
    }
}
