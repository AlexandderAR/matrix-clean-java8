package me.rerere.matrix.network;

import java.io.InputStream;
import java.net.ProxySelector;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.Matrix;

public final class HttpSuccessPipeline {
    private HttpSuccessPipeline() {}

    public static void continueAfterSavedStream(
            InputStream input, String expectedPing, ProxySelector proxySelector,
            me.rerere.matrix.network.SuccessfulResponsePipelineV2Actions actions,
            me.rerere.matrix.cloud.SavedResponseReader readSaved,
            me.rerere.matrix.commands.setup.TrialSchedule trialSchedule,
            me.rerere.matrix.license.BeforeCloseAction beforeClose,
            me.rerere.matrix.network.ResponseCleanup.Outer outer) throws Throwable {
        if (actions == null || readSaved == null || beforeClose == null || outer == null) {
            throw new me.rerere.matrix.commands.setup.UnsupportedSetupPathError("missing 200 adapter");
        }
        me.rerere.matrix.network.ResponseCleanup.run(input, proxySelector,
                stream -> {
                    String savedResponse = readSaved.read(stream);
                    me.rerere.matrix.license.LicenseResponseParser.ParsedLicenseResponse parsed =
                            me.rerere.matrix.license.LicenseResponseParser.parseSelected200(
                                    savedResponse, expectedPing);
                    me.rerere.matrix.network.response.ResponseFeatureComposer.StoppedAtPostCloudBoundary stopped =
                            me.rerere.matrix.network.response.ResponseFeatureComposer.continueAfterSelectedParse(
                                    parsed, actions, actions, actions, trialSchedule);
                    beforeClose.run(stopped.setup.matrix);
                },
                new Recovery(), outer);
    }

    public static void continueAfterPreInputGate(
            me.rerere.matrix.license.network.ResponseStreamOpener openStream,
            String expectedPing, ProxySelector proxySelector,
            me.rerere.matrix.network.SuccessfulResponsePipelineV2Actions actions,
            me.rerere.matrix.cloud.SavedResponseReader readSaved,
            me.rerere.matrix.commands.setup.TrialSchedule trialSchedule,
            me.rerere.matrix.license.BeforeCloseAction beforeClose,
            me.rerere.matrix.network.ResponseCleanup.Outer outer) throws Throwable {
        if (openStream == null || outer == null) {
            throw new me.rerere.matrix.commands.setup.UnsupportedSetupPathError("missing input adapter");
        }
        InputStream input;
        try {
            input = openStream.open();
        } catch (Throwable failure) {
            Recovery recovery = new Recovery();
            if (recovery.isUnresolved(failure)) {
                throw failure;
            }
            outer.failure(failure, proxySelector);
            throw new me.rerere.matrix.network.OuterFailureHandlerReturned();
        }
        continueAfterSavedStream(input, expectedPing, proxySelector, actions, readSaved,
                trialSchedule, beforeClose, outer);
    }

    public static void continueAfterObserved200(
            HttpsURLConnection connection, long preConnectMillis, String expectedPing,
            ProxySelector proxySelector, me.rerere.matrix.network.SuccessfulResponsePipelineV2Actions actions,
            me.rerere.matrix.commands.setup.TrialSchedule trialSchedule,
            me.rerere.matrix.license.BeforeCloseAction beforeClose,
            me.rerere.matrix.network.ResponseCleanup.Outer outer) throws Throwable {
        me.rerere.matrix.network.TlsResponseGuard.check(connection, preConnectMillis);
        Objects.requireNonNull(connection);
        continueAfterPreInputGate(connection::getInputStream, expectedPing, proxySelector,
                actions, me.rerere.matrix.license.EarlyPingResponseReader::readSavedStream, trialSchedule, beforeClose, outer);
    }

    public static void continueProductionAfterObserved200(
            HttpsURLConnection connection, long preConnectMillis, String expectedPing,
            ProxySelector proxySelector, Object productionActions) throws Throwable {
        me.rerere.matrix.network.SuccessfulResponsePipelineV2Actions actions = new me.rerere.matrix.license.SuccessfulLicenseResponseActions(productionActions);
        continueAfterObserved200(connection, preConnectMillis, expectedPing, proxySelector, actions,
                me.rerere.matrix.compat.TrialLinkage::scheduleSelected,
                value -> me.rerere.matrix.bootstrap.MatrixServiceRegistrar
                        .registerComponentsAndListeners((Matrix) value),
                new me.rerere.matrix.network.ResponseCleanup.ProductionOuter());
    }

    public static final class Recovery implements me.rerere.matrix.network.ResponseCleanup.RecoveryBoundary {
        private final me.rerere.matrix.license.LicenseUnresolvedPathClassifier earlier =
                new me.rerere.matrix.license.LicenseUnresolvedPathClassifier();

        @Override
        public boolean isUnresolved(Throwable failure) {
            return earlier.isUnresolved(failure)
                    || failure instanceof me.rerere.matrix.commands.setup.UnsupportedSetupPathError
                    || failure instanceof me.rerere.matrix.compat.TrialLinkage.UnresolvedLinkerFailure
                    || failure instanceof me.rerere.matrix.network.UnresolvedLinkerFailure
                    || failure instanceof me.rerere.matrix.network.TlsResponseGuard.UnresolvedGuard;
        }
    }
}
