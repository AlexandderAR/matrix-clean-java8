package me.rerere.matrix.compat;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.compat.ScheduledTaskProxy;

/** Kotlin callback adapter for a repeating asynchronous scheduled task. */
public final class AsyncTimerTaskCallback implements Function1<Object, Object> {
    private final Function0<?> task;

    public AsyncTimerTaskCallback(Function0<?> task) {
        this.task = task;
    }

    public void invokeScheduledTask() {
        task.invoke();
    }

    @Override
    public Object invoke(Object scheduledTaskArgument) {
        ScheduledTaskProxy scheduledTask = (ScheduledTaskProxy) scheduledTaskArgument;
        scheduledTask.unboxImpl();
        invokeScheduledTask();
        return Unit.INSTANCE;
    }
}
