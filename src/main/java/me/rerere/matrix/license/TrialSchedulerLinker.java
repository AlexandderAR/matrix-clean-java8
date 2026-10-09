package me.rerere.matrix.license;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;
import me.rerere.matrix.compat.scheduler.TrialSchedulerActions;
import me.rerere.matrix.network.UnresolvedLinkerFailure;

/** Links trial scheduler callbacks through {@link LambdaMetafactory}. */
public final class TrialSchedulerLinker {
    private TrialSchedulerLinker() {}

    public static MethodHandle linkConsumerFactory(MethodHandles.Lookup lookup,
                                                   Class<?> targetClass,
                                                   String targetName) {
        try {
            MethodType erasedConsumer = MethodType.methodType(void.class, Object.class);
            MethodHandle target = lookup.findStatic(targetClass, targetName, erasedConsumer);
            CallSite consumerFactory = LambdaMetafactory.metafactory(
                    lookup,
                    "accept",
                    MethodType.methodType(Consumer.class),
                    erasedConsumer,
                    target,
                    erasedConsumer);
            return consumerFactory.getTarget();
        } catch (Throwable failure) {
            throw new UnresolvedLinkerFailure(failure);
        }
    }

    @SuppressWarnings("unchecked")
    public static Consumer<Object> invokeConsumerFactory(MethodHandle factory) throws Throwable {
        return (Consumer<Object>) factory.invokeExact();
    }

    public static TrialLinker productionLinker() {
        return TrialSchedulerLinker::productionLink;
    }

    private static MethodHandle productionLink(String name) {
        String targetName = "i".equals(name) ? "warnTrialLicense"
                : "w".equals(name) ? "enforce24HourLimit" : name;
        return linkConsumerFactory(LicenseRuntimeBridge.__ntjTrialLookup(),
                LicenseRuntimeBridge.class, targetName);
    }

    public static void scheduleSelected(TrialSchedulerActions actions,
                                        TrialLinker linker) throws Throwable {
        if (actions == null || linker == null) {
            throw new IllegalArgumentException("selected Trial adapters");
        }

        Object asyncScheduler = actions.getAsyncScheduler();
        Object matrixFromHeight = actions.matrixFromHeight();
        Consumer<Object> firstConsumer = invokeConsumerFactory(
                linker.linkFactory("i"));
        actions.runAtFixedRate(asyncScheduler, matrixFromHeight, firstConsumer, 20L, 24000L);

        Object secondAsyncScheduler = actions.getAsyncScheduler();
        Object secondMatrixFromHeight = actions.matrixFromHeight();
        Consumer<Object> secondConsumer = invokeConsumerFactory(
                linker.linkFactory("w"));
        actions.runAtFixedRate(secondAsyncScheduler, secondMatrixFromHeight,
                secondConsumer, 1728000L, 1728000L);
    }
}
