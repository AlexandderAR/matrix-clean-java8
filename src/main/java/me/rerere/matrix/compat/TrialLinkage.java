package me.rerere.matrix.compat;

import me.rerere.matrix.compat.scheduler.TrialSchedulerActions;
import me.rerere.matrix.license.LicenseRuntimeBridge;
import me.rerere.matrix.license.TrialLinker;
import me.rerere.matrix.license.TrialSchedulerLinker;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

/** Selected invokedynamic linker bridge recovered from the generated ASM. */
public final class TrialLinkage {
    private TrialLinkage() {}

    public interface Bootstrap {
        CallSite invoke(MethodHandles.Lookup caller, MethodHandle implementation, MethodType targetType) throws Throwable;
    }

    public static final class UnresolvedLinkerFailure extends Error {
        public UnresolvedLinkerFailure(String message) { super(message); }
    }

    public static MethodHandle linkSelected(MethodHandles.Lookup lookup, Class<?> owner,
                                            String name, Bootstrap bootstrap) {
        try {
            MethodType implementationType = MethodType.methodType(void.class, Object.class);
            MethodHandle implementation = lookup.findStatic(owner, name, implementationType);
            CallSite site;
            try {
                site = bootstrap.invoke(lookup, implementation, implementationType);
            } catch (Error failure) {
                throw failure;
            } catch (Throwable failure) {
                throw new BootstrapMethodError("bootstrap method initialization exception", failure);
            }
            if (site == null || !site.getTarget().type().equals(MethodType.methodType(Consumer.class)))
                throw new UnresolvedLinkerFailure("unexpected CallSite target type");
            return site.getTarget();
        } catch (UnresolvedLinkerFailure failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new BootstrapMethodError(failure);
        }
    }

    public static MethodHandle linkSelectedNormal(MethodHandles.Lookup lookup, Class<?> owner, String name) {
        return linkSelected(lookup, owner, name, TrialLinkage::selectedConsumer);
    }


    public static TrialLinker productionLinker() {
        return TrialLinkage::productionHandle;
    }

    public static void scheduleSelected(TrialSchedulerActions actions) {
        try {
            TrialSchedulerLinker.scheduleSelected(actions, productionLinker());
        } catch (Throwable failure) {
            sneakyThrow(failure);
        }
    }

    private static MethodHandle productionHandle(String name) {
        try {
            MethodHandles.Lookup lookup = LicenseRuntimeBridge.__ntjTrialLookup();
            String targetName = "i".equals(name) ? "warnTrialLicense"
                    : "w".equals(name) ? "enforce24HourLimit" : name;
            return linkSelectedNormal(lookup, LicenseRuntimeBridge.class, targetName);
        } catch (Throwable failure) {
            return sneakyThrow(failure);
        }
    }

    private static CallSite selectedConsumer(MethodHandles.Lookup lookup, MethodHandle implementation,
                                             MethodType targetType) {
        try {
            return LambdaMetafactory.metafactory(lookup, "accept", MethodType.methodType(Consumer.class),
                    targetType, implementation, targetType);
        } catch (Throwable failure) {
            return sneakyThrow(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable failure) throws E { throw (E) failure; }
}
