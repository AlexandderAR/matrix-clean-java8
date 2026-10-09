package me.rerere.matrix.license.network;

import java.net.ProxySelector;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.crypto.PreparedNormalEntry.Prepared;
import me.rerere.matrix.license.CertificateChainVerifier;
import me.rerere.matrix.license.LicenseHostGuard;
import me.rerere.matrix.license.LicenseRequestResponseScope.Actions;
import me.rerere.matrix.license.LicenseRequestTransport;
import me.rerere.matrix.license.LicenseRequestTransport.Connected;
import me.rerere.matrix.license.LicenseRequestTransport.TlsSetup;
import me.rerere.matrix.license.LicenseTlsConfiguration;
import me.rerere.matrix.network.NonSuccessResponseHandler.ProductionBoundaries;
import me.rerere.matrix.network.ResponseStatusRouter;
import me.rerere.matrix.network.ResponseStatusRouter.SuccessfulResponseHandler;
import me.rerere.matrix.network.ResponseCleanup.ProductionOuter;
import me.rerere.matrix.network.SuccessfulResponsePipeline;
import me.rerere.matrix.util.Exceptions;

/** Actions bound to one prepared request. */
final class LicenseRequestActions implements Actions {
    private final Prepared prepared;

    LicenseRequestActions(Prepared prepared) {
        this.prepared = prepared;
    }

    @Override public String body() {
        return LicenseRequestTransport.bodyForVerifiedUrl(prepared.decodedUrl,
                prepared.requestValues);
    }

    @Override public Connected send(String body) {
        TlsSetup setup = LicenseRequestActions::configureConnection;
        try {
            return LicenseRequestTransport.sendPreparedBody(
                    prepared.decodedUrl, body, setup);
        } catch (Exception failure) {
            throw Exceptions.propagate(failure);
        }
    }

    @Override public void certificates(HttpsURLConnection connection) {
        try {
            CertificateChainVerifier.verifyAfterConnect(connection);
        } catch (Exception failure) {
            throw Exceptions.propagate(failure);
        }
    }

    @Override public void response(HttpsURLConnection connection, long startedMillis) {
        SuccessfulResponseHandler responseHandler = responseConnection -> handleSuccessfulResponse(
                startedMillis, prepared, responseConnection);
        ProxySelector previousSelector = prepared.previousSelector;
        try {
            ResponseStatusRouter.routeResponse(connection, previousSelector, responseHandler,
                    new ProductionBoundaries(),
                    new ProductionOuter());
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
    }

    private static void handleSuccessfulResponse(long startedMillis, Prepared prepared,
                                                 HttpsURLConnection connection) {
        SuccessfulResponsePipeline.continueProductionAfterObserved200(connection, startedMillis,
                prepared.pingPlaintext, prepared.previousSelector,
                prepared.swReceiver);
    }

    private static void configureConnection(HttpsURLConnection connection, URL url) {
        LicenseHostGuard.checkBeforeDefaultTls(connection);
        try {
            LicenseTlsConfiguration.configureAfterHostCheck(connection);
        } catch (Exception failure) {
            throw Exceptions.propagate(failure);
        }
    }
}
