package me.rerere.matrix.util;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

/** Builds Consumer call sites through LambdaMetafactory. */
public final class CallbackMetafactoryBootstrap {
    private CallbackMetafactoryBootstrap() {
    }

    public static MethodHandle link(MethodHandles.Lookup lookup, ClassLoader classLoader) {
        try {
            MethodType bootstrapType = MethodType.fromMethodDescriptorString(
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                            + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                            + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                    classLoader);
            MethodHandle bootstrap = lookup.findStatic(
                    LambdaMetafactory.class, "metafactory", bootstrapType);

            MethodType factoryType = MethodType.fromMethodDescriptorString(
                    "(Lme/rerere/matrix/license/LicenseRuntimeBridge;Lme/rerere/matrix/config/PrefaceRuntimeContext;)Ljava/util/function/Consumer;",
                    classLoader);
            MethodType samType = MethodType.fromMethodDescriptorString("(Ljava/lang/Object;)V", classLoader);
            MethodType implementationType = MethodType.fromMethodDescriptorString(
                    "(Lme/rerere/matrix/license/LicenseRuntimeBridge;Lme/rerere/matrix/config/PrefaceRuntimeContext;Ljava/lang/Object;)V", classLoader);
            MethodHandle implementation = lookup.findStatic(
                    lookup.lookupClass(), "runCallback", implementationType);
            MethodType instantiatedType = MethodType.fromMethodDescriptorString("(Ljava/lang/Object;)V", classLoader);

            CallSite callSite;
            try {
                callSite = (CallSite) bootstrap.invoke(
                        lookup, "accept", factoryType, samType, implementation, instantiatedType);
            } catch (Error error) {
                throw error;
            } catch (Throwable failure) {
                throw new BootstrapMethodError("bootstrap method initialization exception", failure);
            }
            return callSite.getTarget();
        } catch (Throwable failure) {
            throw new BootstrapMethodError(failure);
        }
    }

    @SuppressWarnings("unchecked")
    public static Consumer<Object> invokeFactory(MethodHandle factory, Object runtimeBridge, Object prefaceContext) {
        try {
            return (Consumer<Object>) factory.invoke(runtimeBridge, prefaceContext);
        } catch (Throwable failure) {
            throw rethrow(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException rethrow(Throwable failure) throws E {
        throw (E) failure;
    }
}
