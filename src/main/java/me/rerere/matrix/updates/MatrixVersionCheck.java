package me.rerere.matrix.updates;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.config.ProductVersion;

/** Checks the remote product version and reports available updates. */
public final class MatrixVersionCheck {
    private MatrixVersionCheck() { }
    public MatrixVersionCheck(DefaultConstructorMarker ignored) { this(); }

    private static String latestVersion(String version) {
        return "§aYou are using the latest version of Matrix! (v" + version + ")";
    }
    public String remoteVersionMessage() {
        return me.rerere.matrix.util.BooleanStateBridge.getString();
    }
    public boolean isUpdateAvailable() {
        return me.rerere.matrix.util.BooleanStateBridge.isEnabled();
    }
    public void setUpdaterEnabled(boolean enabled) {
        me.rerere.matrix.util.BooleanStateBridge.setEnabled(enabled);
    }
    private static String prefixLine(String line) {
        return "§8| §f" + line;
    }
    private static String newVersion(String version) {
        return "§8| §3There is a new version of Matrix available! (" + version + ")";
    }

    public void checkForUpdates() {
        boolean enabled = me.rerere.matrix.config.MatrixConfiguration.checkUpdate;
        if (enabled || this.isUpdateAvailable()) {
            try {
                Runnable updateTask = MatrixVersionCheck::performUpdateCheck;
                Function0 updateFunction = (Function0) new me.rerere.matrix.util.concurrent.RunnableFunctionReference(updateTask);
                me.rerere.matrix.compat.FoliaSupport.INSTANCE.runTaskAsync(updateFunction);
            } catch (Exception failure) {
                me.rerere.matrix.Matrix.Companion.logMessage("Failed to check update!");
            }
        }
    }
    private static void dispatchUpdateCommand() {
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getServer().getConsoleSender(), "matrix update");
    }
    private static void performUpdateCheck() {
        me.rerere.matrix.config.MatrixConfigBridge height = me.rerere.matrix.Matrix.Companion;
        String checking = me.rerere.matrix.i18n.LocaleMessages.getLocaleValue(
            "misc.update.start-checking");
        height.logMessage(checking);

        me.rerere.matrix.Matrix matrix = height.matrixInstance();
        me.rerere.matrix.cloud.CloudServiceClient updateClient = matrix.getRuntimeServices();
        Object result = updateClient.fetchProductInfo(1);
        if (resultSucceeded(result)) {
            me.rerere.matrix.api.ApiProductInfo response =
                (me.rerere.matrix.api.ApiProductInfo) result;
            String installedVersion = me.rerere.matrix.version.VersionText.identityVersionText(
                matrix.getDescription().getVersion());
            String remoteVersion;
            try {
                ProductVersion rawVersion = response.version();
                me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(rawVersion);
                remoteVersion = me.rerere.matrix.version.VersionText.identityVersionText(
                    rawVersion.version());
            } catch (Exception failure) {
                System.out.println(response);
                MatrixVersionCheck.<RuntimeException, Void>throwUnchecked(failure);
                return;
            }

            ProductVersion rawVersion = response.version();
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(rawVersion);
            me.rerere.matrix.util.BooleanStateBridge.setString(rawVersion.version());

            int comparison = me.rerere.matrix.version.VersionText.compareVersionTexts(
                remoteVersion, installedVersion);
            if (comparison > 0) {
                try {
                    String displayVersion = me.rerere.matrix.version.VersionText.firstVersionText(
                        remoteVersion);
                    String message = newVersion(displayVersion);
                    height.logMessage(message);
                } catch (Exception failure) {
                    System.out.println(me.rerere.matrix.version.VersionText.firstVersionText(remoteVersion));
                    MatrixVersionCheck.<RuntimeException, Void>throwUnchecked(failure);
                    return;
                }

                String releaseNotes = response.version().description();
                java.util.List<?> lines = me.rerere.matrix.thirdparty.kotlin.text.StringsKt__StringsKt.split(
                    (CharSequence) releaseNotes, new String[] { "\n" }, false, 0);
                for (Object lineValue : lines) {
                    String line = (String) lineValue;
                    String displayLine = prefixLine(line);
                    height.logMessage(displayLine);
                }
                height.logMessage("§8| §3You can download it by using §b/matrix update");
                if (me.rerere.matrix.util.BooleanStateBridge.angle.isUpdateAvailable()) {
                    me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.GlobalRegionScheduler scheduler =
                        me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler
                            .getGlobalRegionScheduler();
                    org.bukkit.plugin.Plugin plugin = (org.bukkit.plugin.Plugin) matrix;
                    scheduler.run(plugin, ignored -> dispatchUpdateCommand());
                }
            } else {
                try {
                    String message = latestVersion(me.rerere.matrix.version.VersionText.firstVersionText(installedVersion));
                    height.logMessage(message);
                } catch (Exception failure) {
                    MatrixVersionCheck.<RuntimeException, Void>throwUnchecked(failure);
                    return;
                }
            }
        }

        Throwable updateFailure = resultException(result);
        if (updateFailure != null)
            height.logMessage("Failed to check update!");
    }

    private static boolean resultSucceeded(Object value) {
        try {
            Class<?> resultClass = Class.forName("me.rerere.matrix.thirdparty.kotlin.Result");
            java.lang.invoke.MethodHandle method = java.lang.invoke.MethodHandles.lookup().findStatic(
                resultClass, "isSuccess-impl",
                java.lang.invoke.MethodType.methodType(boolean.class, Object.class));
            return (boolean) method.invokeExact(value);
        } catch (Throwable failure) {
            return MatrixVersionCheck.<RuntimeException, Boolean>throwUnchecked(failure);
        }
    }

    private static Throwable resultException(Object value) {
        try {
            Class<?> resultClass = Class.forName("me.rerere.matrix.thirdparty.kotlin.Result");
            java.lang.invoke.MethodHandle method = java.lang.invoke.MethodHandles.lookup().findStatic(
                resultClass, "exceptionOrNull-impl",
                java.lang.invoke.MethodType.methodType(Throwable.class, Object.class));
            return (Throwable) method.invokeExact(value);
        } catch (Throwable failure) {
            return MatrixVersionCheck.<RuntimeException, Throwable>throwUnchecked(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }
}
