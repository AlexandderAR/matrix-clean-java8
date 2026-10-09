package me.rerere.matrix.license;

import java.util.List;
import java.util.function.Consumer;
import me.rerere.matrix.network.response.SuccessfulResponseActions;
import me.rerere.matrix.compat.FoliaSchedulerBridge;
import me.rerere.matrix.network.SuccessfulResponsePipelineV2Actions;

/** Production action adapter for the selected-response license pipeline. */
public final class SuccessfulLicenseResponseActions implements SuccessfulResponsePipelineV2Actions {
    private final SuccessfulResponseActions previousActions;

    public SuccessfulLicenseResponseActions(Object inputs) {
        this.previousActions = new SuccessfulResponseActions(inputs);
    }

    public Object getAsyncScheduler() {
        return FoliaSchedulerBridge.getAsyncScheduler();
    }

    public Object matrixFromHeight() {
        return FoliaSchedulerBridge.matrixFromHeight();
    }

    public Object runAtFixedRate(Object scheduler, Object plugin, Consumer<Object> task,
            long initialDelay, long period) {
        return FoliaSchedulerBridge.runAtFixedRate(scheduler, plugin, task, initialDelay, period);
    }

    public boolean isAngleCheckEnabled() { return this.previousActions.isAngleCheckEnabled(); }

    public void setAngleCheckEnabled(boolean value) { this.previousActions.setAngleCheckEnabled(value); }

    public Object resolveMatrix() { return this.previousActions.resolveMatrix(); }

    public Object pluginCommand(String name) { return this.previousActions.pluginCommand(name); }

    public Object createVerifier() { return this.previousActions.createVerifier(); }

    public void installVerifier(Object matrix, Object verifier) {
        this.previousActions.installVerifier(matrix, verifier);
    }

    public void requireNonNull(Object value) { this.previousActions.checkNotNull(value); }

    public Object verifierForMatrix(Object matrix) { return this.previousActions.verifierForMatrix(matrix); }

    public void setExecutor(Object command, Object executor) {
        this.previousActions.setExecutor(command, executor);
    }

    public void setTabCompleter(Object command, Object completer) {
        this.previousActions.setTabCompleter(command, completer);
    }

    public void setSkBoolean(boolean value) { this.previousActions.setSkBoolean(value); }

    public void setSkList(List<?> value) { this.previousActions.setSkList(value); }

    public void setHiBoolean(boolean value) { this.previousActions.setHiBoolean(value); }

    public void setJeBoolean(boolean value) { this.previousActions.setJeBoolean(value); }

    @SuppressWarnings("unchecked")
    public void firstPass(List<?> values) {
        this.previousActions.firstPass((List<String>) values);
    }

    @SuppressWarnings("unchecked")
    public void secondPass(List<?> values) {
        this.previousActions.secondPass((List<String>) values);
    }

    public void startInference() { this.previousActions.startInference(); }

    public void requestCloudMetadata() { this.previousActions.requestCloudMetadata(); }
}
