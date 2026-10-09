package me.rerere.matrix.util.concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.Matrix;

/** Periodically drains queued asynchronous tasks and reports task failures. */
public class AsyncTaskPool implements Function0<Object> {
    private final Queue<Runnable> tasks;
    private static final String errorPrefix;

    public AsyncTaskPool() {
        this.tasks = new LinkedBlockingQueue<>();
        FoliaSupport.INSTANCE.runTaskTimerAsync(this, 1L, 1L);
    }

    @Override
    public Object invoke() {
        while (tasks.size() > 0) {
            try {
                Runnable task = tasks.poll();
                if (task != null) {
                    task.run();
                }
            } catch (Exception taskFailure) {
                String exceptionClassName = taskFailure.getClass().getName();
                String message = formatFailure(exceptionClassName);
                Matrix.logMessage(message);
                taskFailure.printStackTrace();
            }
        }
        return Unit.INSTANCE;
    }

    public final void enqueue(Runnable queuedTask) {
        tasks.add(queuedTask);
    }

    /** Compatibility entry point used by MatrixAPIBack; shares the canonical queue path. */
    public final void execute(Runnable queuedTask) {
        enqueue(queuedTask);
    }

    public final int queuedTaskCount() {
        return tasks.size();
    }

    private static String formatFailure(String taskClassName) {
        return new StringBuilder(errorPrefix).append(taskClassName).toString();
    }


    static {
        errorPrefix = "[AsyncPool] Find a error when run a async task, exception type: ".intern();
    }
}
