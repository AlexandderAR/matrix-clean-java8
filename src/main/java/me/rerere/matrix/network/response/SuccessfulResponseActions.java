package me.rerere.matrix.network.response;

import java.util.List;
import me.rerere.matrix.compat.SuccessfulResponsePipelineV1Actions;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.PluginSetupBridge;
import me.rerere.matrix.license.CloudRegistrationActions;
import me.rerere.matrix.license.UnsupportedLicenseSetupPath;
import me.rerere.matrix.player.PlayerRotationAccess;
import me.rerere.matrix.license.LicenseFeatureEnabler;

/** Connects successful license responses to Matrix setup and cloud actions. */
public final class SuccessfulResponseActions implements SuccessfulResponsePipelineV1Actions {
    private final Object receiver;
    private Matrix matrix;
    private CloudRegistrationActions cloud;

    public SuccessfulResponseActions(Object receiver) { this.receiver = receiver; }
    @Override public boolean isAngleCheckEnabled() { return PlayerRotationAccess.get(receiver); }
    @Override public void setAngleCheckEnabled(boolean value) { PlayerRotationAccess.set(receiver, value); }
    @Override public Object resolveMatrix() {
        matrix = (Matrix) PluginSetupBridge.matrixFromHeight();
        return matrix;
    }
    @Override public Object pluginCommand(String name) {
        if (!"matrix".equals(name)) throw new UnsupportedLicenseSetupPath("unexpected command name");
        return PluginSetupBridge.pluginCommand();
    }
    @Override public Object createVerifier() { return PluginSetupBridge.newCommandExecutor(); }
    @Override public void installVerifier(Object command, Object value) {
        PluginSetupBridge.installCommandExecutor(command, value);
    }
    @Override public void checkNotNull(Object value) { PluginSetupBridge.checkNotNull(value); }
    @Override public Object verifierForMatrix(Object value) {
        return PluginSetupBridge.getCommandExecutor(value);
    }
    @Override public void setExecutor(Object command, Object executor) {
        PluginSetupBridge.setExecutor(command, executor);
    }
    @Override public void setTabCompleter(Object command, Object completer) {
        PluginSetupBridge.setTabCompleter(command, completer);
    }
    @Override public void setSkBoolean(boolean value) { PluginSetupBridge.configureSkBoolean(value); }
    @Override @SuppressWarnings("unchecked")
    public void setSkList(List<?> value) {
        PluginSetupBridge.configureSkList((List<String>) value);
    }
    @Override public void setHiBoolean(boolean value) { PluginSetupBridge.configureHiBoolean(value); }
    @Override public void setJeBoolean(boolean value) { PluginSetupBridge.configureJeBoolean(value); }
    @Override public void firstPass(List<String> values) { LicenseFeatureEnabler.enablePremiumOrEnterprise(values); }
    @Override public void secondPass(List<String> values) { LicenseFeatureEnabler.enableEnterprise(values); }
    @Override public void startInference() {
        if (cloud == null) cloud = new CloudRegistrationActions(matrix);
        cloud.startInference();
    }
    @Override public void requestCloudMetadata() {
        if (cloud == null) cloud = new CloudRegistrationActions(matrix);
        cloud.requestCloudMetadata();
    }
}
