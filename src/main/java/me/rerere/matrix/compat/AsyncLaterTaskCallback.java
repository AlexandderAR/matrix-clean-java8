package me.rerere.matrix.compat;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.compat.ScheduledTaskProxy;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Kotlin callback adapter for a delayed asynchronous scheduled task. */
public final class AsyncLaterTaskCallback implements Function1<Object, Object> {
    private final Function0<?> task;

    public AsyncLaterTaskCallback(Function0<?> task) {
        this.task = task;
    }

    @Override
    public Object invoke(Object boxedTask) {
        ((ScheduledTaskProxy) boxedTask).unboxImpl();
        runScheduledTask();
        return Unit.INSTANCE;
    }

    public void runScheduledTask() {
        task.invoke();
    }
}
