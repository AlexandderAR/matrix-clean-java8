package me.rerere.matrix.license;

import java.net.ProxySelector;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.crypto.EncodedTokenSelector;
import me.rerere.matrix.crypto.PreparedNormalEntry;
import me.rerere.matrix.crypto.PingRsaCipher;
import me.rerere.matrix.license.identity.HardwareIdentityRegion;
import me.rerere.matrix.network.NetworkInput;
import me.rerere.matrix.network.PingPlaintextGenerator;
import me.rerere.matrix.network.ProxySelectorWindow;
import me.rerere.matrix.network.NetworkSecurityInputs;
import me.rerere.matrix.network.SiteLocalAddressesSelected;
import me.rerere.matrix.compat.SystemPropertyAccess;
import me.rerere.matrix.util.Exceptions;

public final class LicenseRequestPreparer {
    private LicenseRequestPreparer() {}

    public static PreparedNormalEntry.Prepared prepare(String token, ProxySelector proxySelector,
            Object nativeReceiver) throws Throwable {
        if (nativeReceiver == null) {
            throw new UnsupportedNativePath("missing native sw receiver");
        }
        String publicKeyBase64 = NetworkSecurityInputs.earlyPingPublicKeyBase64();
        String randomPlaintext = PingPlaintextGenerator.randomPlaintext();
        String selectedMac = NetworkInput.selectedMacString();
        int processorCount = Runtime.getRuntime().availableProcessors();
        String operatingSystem = SystemPropertyAccess.getProperty("os.name");
        String siteLocalAddress = SiteLocalAddressesSelected.selected();
        String hardwareId = HardwareIdentityRegion.fromParts(selectedMac, processorCount, operatingSystem, siteLocalAddress);
        String encryptedPing;
        try {
            encryptedPing = PingRsaCipher.encrypt(randomPlaintext, publicKeyBase64);
        } catch (Exception failure) {
            throw Exceptions.propagate(failure);
        }
        java.util.HashMap<String, Object> requestFields = LicenseRequestProperties.build(encryptedPing, token, hardwareId);
        String requestUrl = NetworkSecurityInputs.laterRequestUrl();
        return new PreparedNormalEntry.Prepared(requestUrl, requestFields, randomPlaintext, proxySelector, nativeReceiver);
    }

    public static PreparedNormalEntry.Prepared prepareFromNativeInput(
            PrefaceRuntimeContext input, Object nativeReceiver, PostTokenChecks postTokenChecks) throws Throwable {
        String token = EncodedTokenSelector.fromInput(input);
        if (postTokenChecks == null) {
            throw new UnsupportedNativePath("native post-token checks/effects unavailable");
        }
        postTokenChecks.run(input, token);
        ProxySelector proxySelector = ProxySelectorWindow.captureAndClear();
        return prepare(token, proxySelector, nativeReceiver);
    }

    public interface PostTokenChecks {
        void run(PrefaceRuntimeContext input, String token) throws Throwable;
    }

    public static final class UnsupportedNativePath extends Error {
        public UnsupportedNativePath(String message) { super(message); }
    }
}
