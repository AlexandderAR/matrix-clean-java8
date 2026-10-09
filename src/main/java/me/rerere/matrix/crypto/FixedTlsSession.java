package me.rerere.matrix.crypto;

import java.security.Principal;
import java.security.cert.Certificate;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;

/** SSL session values exposed to the licensing TLS configuration. */
public final class FixedTlsSession implements SSLSession {
    public FixedTlsSession() {
    }

    @Override public String getPeerHost() { return "localhost"; }
    @Override public Principal getLocalPrincipal() { return null; }
    @Override public int getPacketBufferSize() { return 16384; }
    @Override public long getCreationTime() { return System.currentTimeMillis(); }
    @Override public long getLastAccessedTime() { return System.currentTimeMillis(); }
    @Override public void invalidate() { }
    @Override public boolean isValid() { return true; }
    @Override public Object getValue(String name) { return null; }
    @Override public String[] getValueNames() { return new String[0]; }
    @Override public int getPeerPort() { return 443; }
    @Override public javax.security.cert.X509Certificate[] getPeerCertificateChain() { return null; }
    @Override public byte[] getId() { return new byte[0]; }
    @Override public String getProtocol() { return "TLSv1.3"; }
    @Override public Principal getPeerPrincipal() { return null; }
    @Override public String getCipherSuite() { return "TLS_AES_256_GCM_SHA384"; }
    @Override public SSLSessionContext getSessionContext() { return null; }
    @Override public int getApplicationBufferSize() { return 16384; }
    @Override public void putValue(String name, Object value) { }
    @Override public Certificate[] getLocalCertificates() { return new Certificate[0]; }
    @Override public void removeValue(String name) { }
    @Override public Certificate[] getPeerCertificates() { return new Certificate[0]; }

}
