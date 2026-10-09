package me.rerere.matrix.crypto;

/** Immutable data passed from normal request preparation to its consumer. */
public class PreparedNormalEntry {
    private PreparedNormalEntry() { }

    public static final class Prepared {
        public final String decodedUrl;
        public final java.util.Map requestValues;
        public final String pingPlaintext;
        public final java.net.ProxySelector previousSelector;
        public final Object swReceiver;

        public Prepared(String decodedUrl, java.util.Map requestValues, String pingPlaintext,
                        java.net.ProxySelector previousSelector, Object swReceiver) {
            this.decodedUrl = decodedUrl;
            this.requestValues = requestValues;
            this.pingPlaintext = pingPlaintext;
            this.previousSelector = previousSelector;
            this.swReceiver = swReceiver;
        }
    }
}
