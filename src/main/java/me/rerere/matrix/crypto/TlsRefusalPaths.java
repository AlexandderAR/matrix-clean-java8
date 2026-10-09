package me.rerere.matrix.crypto;

import me.rerere.matrix.thirdparty.kotlin.Result;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.support.KotlinResultBridge;
public final class TlsRefusalPaths {
    public TlsRefusalPaths() {}
    public static Object resultOfReflection(UnsafeAccessActions actions) throws Throwable {
        Object ignored = Result.Companion;
        try {
            Object unsafeField = actions.declaredField();
            actions.accessible(unsafeField);
            Object reflectedValue = actions.value(unsafeField);
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(reflectedValue, "null cannot be cast to non-null type sun.misc.Unsafe");
            if (!actions.isUnsafe(reflectedValue)) throw new UnresolvedNativePath("first IsInstanceOf(Unsafe) false");
            return KotlinResultBridge.construct(reflectedValue);
        } catch (UnresolvedNativePath unresolvedPath) {
            throw unresolvedPath;
        } catch (Throwable failure) {
            return KotlinResultBridge.construct(ResultKt.createFailure(failure));
        }
    }
    public static void hostMismatchSelected(UnsafeAccessActions actions) throws Throwable {
        Object unsafeResult = resultOfReflection(actions);
        if (KotlinResultBridge.exceptionOrNull(unsafeResult) != null) actions.halt(1);
        ResultKt.throwOnFailure(unsafeResult);
        if (!actions.isUnsafe(unsafeResult)) throw new UnresolvedNativePath("second host IsInstanceOf(Unsafe) false @0x216f56");
        actions.putAddress(unsafeResult, 0L, 0L);
        throw new UnresolvedNativePath("host post-put continuation unresolved");
    }
    public static void factoryPositiveSelected(UnsafeAccessActions actions) throws Throwable {
        Object unsafeResult = resultOfReflection(actions);
        if (KotlinResultBridge.exceptionOrNull(unsafeResult) != null) actions.halt(1);
        ResultKt.throwOnFailure(unsafeResult);
        if (!actions.isUnsafe(unsafeResult)) throw new UnresolvedNativePath("second factory IsInstanceOf(Unsafe) false");
        actions.getAndAddLong(unsafeResult, "", 0L, 0L);
        actions.putAddress(unsafeResult, 0L, 0L);
        actions.halt(0);
        throw new UnresolvedNativePath("factory halt-return continuation unresolved");
    }
    public static void rejectHost() {
        try { hostMismatchSelected(new UnsafeAccessProduction()); }
        catch (Throwable failure) { unchecked(failure); }
    }
    public static void rejectFactory() {
        try { factoryPositiveSelected(new UnsafeAccessProduction()); }
        catch (Throwable failure) { unchecked(failure); }
    }
    public static void unchecked(Throwable failure) {
        TlsRefusalPaths.<RuntimeException>throwUnchecked(failure);
    }

    @SuppressWarnings("unchecked")
    private static <Failure extends Throwable> void throwUnchecked(Throwable failure) throws Failure {
        throw (Failure) failure;
    }
}
