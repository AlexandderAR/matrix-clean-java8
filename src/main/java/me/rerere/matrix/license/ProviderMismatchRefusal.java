package me.rerere.matrix.license;

import java.lang.reflect.Field;
import sun.misc.Unsafe;
import me.rerere.matrix.thirdparty.kotlin.Result;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.support.KotlinResultBridge;

public final class ProviderMismatchRefusal {
    private ProviderMismatchRefusal() {}
    public interface Actions {
        Object declaredField() throws Throwable;
        void accessible(Object field) throws Throwable;
        Object value(Object field) throws Throwable;
        boolean isUnsafe(Object value) throws Throwable;
        void halt(int status) throws Throwable;
        void putAddress(Object unsafe, long address, long value) throws Throwable;
    }
    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) { super(message); }
    }
    static final class Production implements Actions {
        private Production() {}
        public Object declaredField() throws NoSuchFieldException { return Unsafe.class.getDeclaredField("theUnsafe"); }
        public void accessible(Object field) { ((Field) field).setAccessible(true); }
        public Object value(Object field) throws IllegalAccessException { return ((Field) field).get(null); }
        public boolean isUnsafe(Object value) { return value instanceof Unsafe; }
        public void halt(int status) { Runtime.getRuntime().halt(status); }
        public void putAddress(Object unsafe, long address, long value) { ((Unsafe) unsafe).putAddress(address, value); }
    }
    public static void run(Actions actions) throws Throwable {
        Object companion = Result.Companion;
        Object kotlinResult;
        try {
            Object field = actions.declaredField();
            actions.accessible(field);
            Object unsafeInstance = actions.value(field);
            Intrinsics.checkNotNull(unsafeInstance, "null cannot be cast to non-null type sun.misc.Unsafe");
            if (!actions.isUnsafe(unsafeInstance)) {
                throw new UnresolvedNativePath("IsInstanceOf false exits to 0xcd13a");
            }
            kotlinResult = KotlinResultBridge.construct(unsafeInstance);
        } catch (UnresolvedNativePath unresolved) {
            throw unresolved;
        } catch (Throwable failure) {
            kotlinResult = KotlinResultBridge.construct(ResultKt.createFailure(failure));
        }
        if (KotlinResultBridge.exceptionOrNull(kotlinResult) != null) actions.halt(1);
        ResultKt.throwOnFailure(kotlinResult);
        actions.putAddress(kotlinResult, 0L, 0L);
    }
    public static void reject() {
        try { run(new Production()); }
        catch (Throwable failure) { ProviderMismatchRefusal.<RuntimeException>unchecked(failure); }
    }
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void unchecked(Throwable failure) throws E { throw (E) failure; }
}
