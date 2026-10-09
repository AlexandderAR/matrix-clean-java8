package me.rerere.matrix.compat;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.compat.ScheduledTaskProxy;

/** Kotlin callback adapter used by the entity scheduler for a fixed-rate task. */
public final class FixedRateTaskCallback implements Function1<Object, Object> {
    private final Function0<?> task;

    public FixedRateTaskCallback(Function0<?> task) {
        this.task = task;
    }

    @Override
    public Object invoke(Object scheduledTask) {
        invokeScheduledTask(((ScheduledTaskProxy) scheduledTask).unboxImpl());
        return Unit.INSTANCE;
    }

    public void invokeScheduledTask(Object ignoredScheduledTask) {
        task.invoke();
    }
}
