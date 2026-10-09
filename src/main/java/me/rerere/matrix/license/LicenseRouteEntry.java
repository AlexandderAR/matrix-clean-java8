package me.rerere.matrix.license;

import java.util.Arrays;
import java.util.List;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.bootstrap.MatrixServiceRegistrar;
import me.rerere.matrix.compat.PluginSetupBridge;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.license.LicenseClassLoaderPrologue;
import me.rerere.matrix.util.Exceptions;

public final class LicenseRouteEntry {
    private LicenseRouteEntry() {}
    public static void continueSelected(LicenseRuntimeBridge runtime, PrefaceRuntimeContext result) {
        try {
            LicenseClassLoaderPrologue.selectedBeforeJmx(runtime);
        } catch (Throwable failure) {
            throw Exceptions.propagate(failure);
        }
        setupOfflineEnterprise(runtime);
    }

    private static void setupOfflineEnterprise(LicenseRuntimeBridge runtime) {
        LicenseRuntimeBridge.__ntjSetAngle(runtime, true);

        Matrix plugin = (Matrix) PluginSetupBridge.matrixFromHeight();
        Object command = PluginSetupBridge.pluginCommand();
        Object executor = PluginSetupBridge.newCommandExecutor();
        PluginSetupBridge.installCommandExecutor(plugin, executor);
        PluginSetupBridge.checkNotNull(command);
        Object handler = PluginSetupBridge.getCommandExecutor(plugin);
        PluginSetupBridge.setExecutor(command, handler);
        PluginSetupBridge.setTabCompleter(command, handler);

        List<String> plans = Arrays.asList("Enterprise");
        PluginSetupBridge.configureSkBoolean(false);
        PluginSetupBridge.configureSkList(plans);

        LicenseFeatureEnabler.enablePremiumOrEnterprise(plans);
        LicenseFeatureEnabler.enableEnterprise(plans);

        MatrixServiceRegistrar.registerComponentsAndListeners(plugin);
    }
}
