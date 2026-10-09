package me.rerere.matrix.crypto;

import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.license.PrefaceUnresolvedNativePath;
import me.rerere.matrix.support.KotlinResultBridge;
import me.rerere.matrix.plugin.MatrixPluginAccessor;

/** Decodes the three authenticated values carried in the request preface. */
public final class AuthenticatedPrefaceDecoder {
    private static final byte[] KEY1 = new byte[] {52, 97, 54, 99, 109, 109, 55, 83, 75, 82, 108, 51, 83, 106, 81, 118, 69, 54, 51, 48};
    private static final byte[] KEY2 = new byte[] {121, 48, 119, 76, 116, 55, 48, 101, 50, 77, 88, 53, 72, 86, 50, 89, 90, 51, 83, 77};

    private AuthenticatedPrefaceDecoder() {}

    public static Fields decode(PrefaceRuntimeContext encodedFields) {
        String encodedUserKey = encodedFields.getUserKey();
        if (encodedUserKey == null) {
            try {
                me.rerere.matrix.thirdparty.kotlin.text.StringsKt.chunked((CharSequence) null, 2);
            } catch (Exception ignored) {
                rejectNullEncodedAfterCaughtChunked();
            }
            throw new PrefaceUnresolvedNativePath("unexpected return from shaded chunked(null, 2)");
        }
        return new Fields(
                decodeNth(encodedUserKey, 0),
                decodeNth(encodedFields.getUserKey(), 1),
                decodeNth(encodedFields.getUserKey(), 2));
    }

    private static void rejectNullEncodedAfterCaughtChunked() {
        me.rerere.matrix.Matrix matrix = MatrixPluginAccessor.getMatrix();
        matrix.getLogger().severe("Your key is invalid. Please re-download the jar file from localhost.");
        Object ignoredResultCompanion = me.rerere.matrix.thirdparty.kotlin.Result.Companion;
        Object result;
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object reflected = f.get(null);
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(reflected, "null cannot be cast to non-null type sun.misc.Unsafe");
            if (!(reflected instanceof sun.misc.Unsafe)) throw new PrefaceUnresolvedNativePath("reflected value is not sun.misc.Unsafe");
            result = KotlinResultBridge.construct(reflected);
        } catch (PrefaceUnresolvedNativePath e) {
            throw e;
        } catch (Throwable failure) {
            result = KotlinResultBridge.construct(
                    me.rerere.matrix.thirdparty.kotlin.ResultKt.createFailure(failure));
        }
        if (KotlinResultBridge.exceptionOrNull(result) != null) {
            java.lang.Runtime.getRuntime().halt(1);
        }
        me.rerere.matrix.thirdparty.kotlin.ResultKt.throwOnFailure(result);
        if (!(result instanceof sun.misc.Unsafe)) throw new PrefaceUnresolvedNativePath("Result success is not sun.misc.Unsafe");
        ((sun.misc.Unsafe) result).getAndAddLong(Long.valueOf(Long.MAX_VALUE), 0L, 0L);
        java.lang.Runtime.getRuntime().halt(10);
        throw new PrefaceUnresolvedNativePath("Runtime.halt(10) returned");
    }

    private static String decodeNth(String encoded, int index) {
        if (encoded == null) throw new PrefaceUnresolvedNativePath("null encoded input");
        String decodedPayload = me.rerere.matrix.thirdparty.kotlin.text.StringsKt.decodeToString(
                EmbeddedAuthDecoder.decode(encoded, KEY1, KEY2, 5556565728L, 0));
        java.util.ArrayList<String> nonEmptyFields = new java.util.ArrayList<>();
        for (String field : me.rerere.matrix.thirdparty.kotlin.text.StringsKt.split(
                decodedPayload, new String[] {"#"}, false, 0)) {
            if (field.length() > 0) {
                nonEmptyFields.add(field);
            }
        }
        return nonEmptyFields.size() > index ? nonEmptyFields.get(index) : "";
    }

    public static final class Fields {
        public final String index0;
        public final String index1;
        public final String index2;

        private Fields(String index0, String index1, String index2) {
            this.index0 = index0;
            this.index1 = index1;
            this.index2 = index2;
        }

    }
}
