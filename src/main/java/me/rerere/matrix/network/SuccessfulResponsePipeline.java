package me.rerere.matrix.network;

import me.rerere.matrix.commands.setup.TrialSchedule;
import me.rerere.matrix.license.BeforeCloseAction;
import me.rerere.matrix.license.LicenseResponsePreconditions;
import me.rerere.matrix.license.EarlyPingResponseReader;
import me.rerere.matrix.license.SuccessfulLicenseResponseActions;
import me.rerere.matrix.bootstrap.MatrixServiceRegistrar;
import me.rerere.matrix.compat.TrialLinkage;
import me.rerere.matrix.network.ResponseCleanup.Outer;
import me.rerere.matrix.network.ResponseCleanup.ProductionOuter;
import me.rerere.matrix.network.SuccessfulResponsePipelineV2Actions;
import me.rerere.matrix.util.Exceptions;

import java.net.ProxySelector;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;

/** Continues the selected response path after the connection reports HTTP 200. */
public final class SuccessfulResponsePipeline {
    private SuccessfulResponsePipeline() { }

    public static void continueAfterObserved200(
            HttpsURLConnection connection,
            long requestStartedAt,
            String requestUrl,
            ProxySelector proxySelector,
            SuccessfulResponsePipelineV2Actions actions,
            TrialSchedule trialSchedule,
            BeforeCloseAction beforeClose,
            Outer outer) {
        try {
            LicenseResponsePreconditions.check(
                    connection, requestStartedAt, proxySelector, outer);
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
        Objects.requireNonNull(connection);
        try {
            HttpSuccessPipeline.continueAfterPreInputGate(
                    connection::getInputStream,
                    requestUrl,
                    proxySelector,
                    actions,
                    EarlyPingResponseReader::readSavedStream,
                    trialSchedule,
                    beforeClose,
                    outer);
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
    }

    public static void continueProductionAfterObserved200(
            HttpsURLConnection connection,
            long requestStartedAt,
            String requestUrl,
            ProxySelector proxySelector,
            Object registrationContext) {
        try {
            continueAfterObserved200(
                    connection,
                    requestStartedAt,
                    requestUrl,
                    proxySelector,
                    new SuccessfulLicenseResponseActions(registrationContext),
                    TrialLinkage::scheduleSelected,
                    SuccessfulResponsePipeline::beforeResponseClose,
                    new ProductionOuter());
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
    }

    private static void beforeResponseClose(Object registrationContext) {
        MatrixServiceRegistrar.registerComponentsAndListeners(
                (me.rerere.matrix.Matrix) registrationContext);
    }
}
