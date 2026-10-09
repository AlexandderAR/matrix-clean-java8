package me.rerere.matrix.player;

import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.text.Charsets;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Shared plan/cloud state and its reflection lookup helpers. */
public final class PlanState {
    private static List<String> planNames;
    private static boolean cloudEnabled;
    public static final PlanState INSTANCE;
    private PlanState() { }

    public final String display() {
        return "Plans[" + joinPlanNames(planNames) + "], Cloud[" + cloudEnabled + "]";
    }

    public final boolean isCloudEnabled() { return cloudEnabled; }

    public final boolean hasTrialOrNoPlans() {
        return planNames.isEmpty() || planNames.contains("Trial");
    }

    public final void setCloudEnabled(boolean enabled) { cloudEnabled = enabled; }
    public final void setPlanNames(List<String> names) {
        Iterable<String> planNameValues = names;
        int capacity = planNameValues instanceof Collection<?> ? ((Collection<?>) planNameValues).size() : 10;
        Collection<String> copiedNames = new ArrayList<>(capacity);
        for (String planName : planNameValues) {
            byte[] utf8 = planName.getBytes(Charsets.UTF_8);
            Intrinsics.checkNotNullExpressionValue(utf8, "getBytes(...)");
            copiedNames.add(new String(utf8, Charsets.UTF_8));
        }
        PlanState.planNames = (List<String>) copiedNames;
    }

    private static String joinPlanNames(Iterable<String> values) {
        StringBuilder joined = new StringBuilder();
        boolean first = true;
        for (String planName : values) {
            if (!first) joined.append(',');
            joined.append(String.valueOf(planName));
            first = false;
        }
        return joined.toString();
    }

    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?> returnType,
                                             int parameterCount, Class<?>[] parameterTypes) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getReturnType() != returnType) continue;
            Class<?>[] actual = method.getParameterTypes();
            if (actual.length != parameterCount) continue;
            boolean matches = true;
            for (int i = 0; i < parameterCount; i++) {
                if (actual[i] != parameterTypes[i]) { matches = false; break; }
            }
            if (matches) return method;
        }
        return null;
    }

    private static Method findInterfaceMethod(Class<?> owner, String name, Class<?> returnType,
                                              int parameterCount, Class<?>[] parameterTypes) {
        Method method = findDeclaredMethod(owner, name, returnType, parameterCount, parameterTypes);
        if (method != null) return method;
        for (Class<?> iface : owner.getInterfaces()) {
            method = findInterfaceMethod(iface, name, returnType, parameterCount, parameterTypes);
            if (method != null) return method;
        }
        return null;
    }

    static {
        INSTANCE = new PlanState();
        planNames = CollectionsKt.emptyList();
    }
}




