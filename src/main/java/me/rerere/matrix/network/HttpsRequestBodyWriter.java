package me.rerere.matrix.network;

import java.io.OutputStream;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.thirdparty.kotlin.text.Charsets;

/** Encodes a request body as UTF-8 and writes it to an HTTPS connection. */
public final class HttpsRequestBodyWriter {
    private HttpsRequestBodyWriter() {
    }

    public static void writeBody(HttpsURLConnection connection, String body) throws Exception {
        writeAndClose(connection, () -> encodeUtf8(body));
    }

    public static void writeAndClose(HttpsURLConnection connection, Encoder encoder) throws Exception {
        OutputStream output = connection.getOutputStream();
        Throwable failure = null;
        try {
            byte[] encodedBody = encoder.encode();
            if (output == null) {
                throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
            }
            output.write(encodedBody);
        } catch (Throwable caught) {
            failure = caught;
            throw rethrow(caught);
        } finally {
            CloseableKt.closeFinally(output, failure);
        }
    }

    private static byte[] encodeUtf8(String body) throws Exception {
        byte[] encoded = body.getBytes(Charsets.UTF_8);
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(encoded, "getBytes(...)");
        return encoded;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException rethrow(Throwable failure) throws E {
        throw (E) failure;
    }

    public interface Encoder {
        byte[] encode() throws Exception;
    }
}


