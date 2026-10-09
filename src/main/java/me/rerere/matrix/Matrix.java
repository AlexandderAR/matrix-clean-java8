package me.rerere.matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import me.rerere.matrix.api.MatrixAPIBack;
import me.rerere.matrix.api.MatrixAPIProvider;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.cloud.CloudServiceClient;
import me.rerere.matrix.commands.condition.ConditionalCommands;
import me.rerere.matrix.commands.MatrixCommandDispatcher;
import me.rerere.matrix.compat.ProtocolTranslationSupport;
import me.rerere.matrix.compat.SystemPropertyAccess;
import me.rerere.matrix.compat.BedrockPlayerSupport;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.compat.NotepadPlusPlusNotice;
import me.rerere.matrix.config.ConfigurationManager;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.inference.InferenceTask;
import me.rerere.matrix.inventory.PlayerInventoryMenuListener;
import me.rerere.matrix.license.LicenseRuntimeBridge;
import me.rerere.matrix.listeners.LoginCooldownListener;
import me.rerere.matrix.listeners.BlockWorldListener;
import me.rerere.matrix.i18n.LocaleConfiguration;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.license.LicenseRouteEntry;
import me.rerere.matrix.logging.LogFileWriter;
import me.rerere.matrix.compat.CompatibilityManager;
import me.rerere.matrix.movement.PlayerMovementHistory;
import me.rerere.matrix.network.GlideMetadataListener;
import me.rerere.matrix.network.KickReportService;
import me.rerere.matrix.network.PlayerMetadataPacketListener;
import me.rerere.matrix.network.PlayerPacketListener;
import me.rerere.matrix.network.packet.TransactionPacketListener;
import me.rerere.matrix.permissions.PermissionCacheManager;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.server.ServerImplementationFactory;
import me.rerere.matrix.util.BooleanStateBridge;
import me.rerere.matrix.util.BuildMetadata;
import me.rerere.matrix.util.concurrent.AsyncTaskPool;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockLocationCache;
import me.rerere.matrix.world.ChunkActivityCache;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.thirdparty.kotlin.io.TextStreamsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.dev.thomazz.pledge.util.TickEndTask;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

public final class Matrix extends JavaPlugin {
    public PlayerMovementHistory moveHistory;
    public PlayerPacketListener packetListener;
    private static Matrix instance;
    public GlideMetadataListener elytraListener;
    public ChunkActivityCache chunkManager;
    public LogFileWriter matrixLogger;
    public PlayerSessionRegistry playerDataManager;
    private static boolean placeholderApiHooked;
    public AsyncTaskPool asyncPool;
    public TransactionPacketListener pingPongListener;
    public ConditionalCommands conditionalCommands;
    public ConfigurationManager configManager;
    private final InferenceTask inferenceTask;
    private TickEndTask tickEndTask;
    public BlockLocationCache clientBlockManager;
    public BlockWorldListener checkListener;
    private static ServerVersion serverVersion;
    public PermissionCacheManager permissionManager;
    public ServerImplementationFactory nmsManager;
    public MatrixCommandDispatcher matrixCommand;
    private static boolean runtimeEnabled;
    private final CloudServiceClient runtimeServices;
    public static ClassLoader pluginClassLoader;
    private final KickReportService kickReportService;
    public PlayerMetadataPacketListener filters;
    public LoginCooldownListener relogManager;
    public CompatibilityManager compatibilityManager;
    public static final MatrixConfigBridge Companion = new MatrixConfigBridge(null);

    public Matrix() {
        runtimeServices = new CloudServiceClient();
        inferenceTask = new InferenceTask();
        kickReportService = new KickReportService();
        instance = this;
    }

    private static <T> T initialized(T value, String property) {
        if (value != null) return value;
        Intrinsics.throwUninitializedPropertyAccessException(property);
        return null;
    }
    public final PermissionCacheManager getPermissionManager() { return initialized(permissionManager, "permissionManager"); }
    public final void setPermissionManager(PermissionCacheManager value) { permissionManager = value; }
    public final CompatibilityManager getCompatibilityManager() { return initialized(compatibilityManager, "compatibilityManager"); }
    public final void setCompatibilityManager(CompatibilityManager value) { compatibilityManager = value; }
    public final ChunkActivityCache getChunkManager() { return initialized(chunkManager, "chunkManager"); }
    public final void setChunkManager(ChunkActivityCache value) { chunkManager = value; }
    public final PlayerPacketListener getPacketListener() { return initialized(packetListener, "packetListener"); }
    public final void setPacketListener(PlayerPacketListener value) { packetListener = value; }
    public final LogFileWriter getMatrixLogger() { return initialized(matrixLogger, "matrixLogger"); }
    public final void setMatrixLogger(LogFileWriter value) { matrixLogger = value; }
    public final MatrixCommandDispatcher getMatrixCommand() { return initialized(matrixCommand, "matrixCommand"); }
    public final void setMatrixCommand(MatrixCommandDispatcher value) { matrixCommand = value; }
    public final PlayerMetadataPacketListener getFilters() { return initialized(filters, "filters"); }
    public final void setFilters(PlayerMetadataPacketListener value) { filters = value; }
    public final ConfigurationManager getConfigManager() { return initialized(configManager, "configManager"); }
    public final void setConfigManager(ConfigurationManager value) { configManager = value; }
    public final AsyncTaskPool getAsyncPool() { return initialized(asyncPool, "asyncPool"); }
    public final void setAsyncPool(AsyncTaskPool value) { asyncPool = value; }
    public final LoginCooldownListener getRelogManager() { return initialized(relogManager, "relogManager"); }
    public final void setRelogManager(LoginCooldownListener value) { relogManager = value; }
    public final BlockLocationCache getClientBlockManager() { return initialized(clientBlockManager, "clientBlockManager"); }
    public final void setClientBlockManager(BlockLocationCache value) { clientBlockManager = value; }
    public final ConditionalCommands getConditionalCommands() { return initialized(conditionalCommands, "conditionalCommands"); }
    public final void setConditionalCommands(ConditionalCommands value) { conditionalCommands = value; }
    public final ServerImplementationFactory getNmsManager() { return initialized(nmsManager, "nmsManager"); }
    public final void setNmsManager(ServerImplementationFactory value) { nmsManager = value; }
    public final GlideMetadataListener getElytraListener() { return initialized(elytraListener, "elytraListener"); }
    public final void setElytraListener(GlideMetadataListener value) { elytraListener = value; }
    public final PlayerSessionRegistry getPlayerDataManager() { return initialized(playerDataManager, "playerDataManager"); }
    public final void setPlayerDataManager(PlayerSessionRegistry value) { playerDataManager = value; }
    public final TransactionPacketListener getPingPongListener() { return initialized(pingPongListener, "pingPongListener"); }
    public final void setPingPongListener(TransactionPacketListener value) { pingPongListener = value; }
    public final BlockWorldListener getCheckListener() { return initialized(checkListener, "checkListener"); }
    public final void setCheckListener(BlockWorldListener value) { checkListener = value; }
    public final PlayerMovementHistory getMoveHistory() {
        return initialized(moveHistory, "moveHistory");
    }
    public final void setMoveHistory(PlayerMovementHistory value) { moveHistory = value; }
    public final KickReportService getKickReportService() { return kickReportService; }
    public final InferenceTask getInferenceTask() { return inferenceTask; }
    public final CloudServiceClient getRuntimeServices() { return runtimeServices; }
    public final TickEndTask getTickEndTask() { return tickEndTask; }
    public final void setTickEndTask(TickEndTask value) { tickEndTask = value; }
    public final File getPluginFile() { return getFile(); }
    public static Matrix getInstance() { return Companion.matrixInstance(); }
    public static Matrix getStoredInstance() { return instance; }
    public static ServerVersion getServerVersion() { return Companion.serverVersion(); }
    public static ServerVersion getStoredServerVersion() { return serverVersion; }
    public static boolean isRuntimeEnabled() { return Companion.isRunning(); }
    public static boolean getRuntimeFlag() { return runtimeEnabled; }
    public static void setRuntimeFlag(boolean enabled) { runtimeEnabled = enabled; }
    public static void setRuntimeEnabled(boolean enabled) { Companion.setEnabled(enabled); }
    public static boolean isPlaceholderApiHooked() { return placeholderApiHooked; }
    public static void setPlaceholderApiHooked(boolean hooked) { placeholderApiHooked = hooked; }
    public static void logMessage(String message) { Companion.logMessage(message); }
    public static void waitForServer(long delay) {
        Companion.sleepAndLog(delay);
    }

    @Override public void onLoad() {
        try {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            PacketEvents.getAPI().getSettings().fullStackTrace(true).kickOnPacketException(true)
                    .checkForUpdates(false).reEncodeByDefault(false).debug(false);
            PacketEvents.getAPI().load();
            serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
            checkEnvironment();
        } catch (Throwable failure) { Matrix.<RuntimeException>propagate(failure); }
    }

    @Override public void onEnable() {
        try {
            instance = this;
            pluginClassLoader = getClassLoader();
            InputStream banner = getResource("banner.txt");
            if (banner != null) {
                Throwable closeFailure = null;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(banner, StandardCharsets.UTF_8), 8192);
                    for (String line : TextStreamsKt.readLines(reader)) Bukkit.getConsoleSender().sendMessage(formatBannerLine(line));
                } catch (Throwable failure) { closeFailure = failure; throw failure; }
                finally { CloseableKt.closeFinally(banner, closeFailure); }
            }
            Bukkit.getConsoleSender().sendMessage(formatStartupBanner(getDescription().getVersion(), getServer().getVersion()));
            getLogger().info(formatServerVersion(Companion.serverVersion().name()));
            Companion.sleepAndLog(500L);
            hookPlaceholderApi();
            setNmsManager(new ServerImplementationFactory());
            getNmsManager().initialize();
            setConfigManager(new ConfigurationManager());
            getConfigManager().load();
            ProtocolTranslationSupport.initialize();
            BedrockPlayerSupport.detectPlugins();
            warnConflictingPlugins();
            setAsyncPool(new AsyncTaskPool());
            setMatrixLogger(new LogFileWriter());
            getMatrixLogger().appendLog(formatEnabledVersion(Companion.matrixInstance().getDescription().getVersion()));
            getMatrixLogger().deleteOutdatedLogs(MatrixConfiguration.deleteTime);
            new LicenseRuntimeBridge().scheduleSelectedCallback(new PrefaceRuntimeContext(
                    BuildMetadata.LICENSE_METADATA.getEmbeddedToken(), MatrixConfiguration.accessToken,
                    MatrixConfiguration.authLine, BuildMetadata.LICENSE_METADATA.getDisplayLabel(),
                    BuildMetadata.LICENSE_METADATA.getRevisionMarker(), BuildMetadata.LICENSE_METADATA.getBuildNumber(),
                    BuildMetadata.LICENSE_METADATA.getEnabledFlag()));
            MatrixAPIProvider.register(new MatrixAPIBack());
            Companion.logMessage("Welcome to Matrix!");
            BooleanStateBridge.angle.checkForUpdates();
            showRegionalNotice();
        } catch (Throwable failure) { Matrix.<RuntimeException>propagate(failure); }
    }

    @Override public void onDisable() {
        try {
            TickEndTask pendingTickEndTask = tickEndTask;
            if (pendingTickEndTask != null) pendingTickEndTask.cancel();
            PacketEvents.getAPI().getEventManager().unregisterListeners(getPacketListener(), getPingPongListener());
            if (Companion.serverVersion().isNewerThanOrEquals(ServerVersion.V_1_12))
                PacketEvents.getAPI().getEventManager().unregisterListeners(getElytraListener());
            PacketEvents.getAPI().terminate();
            PlayerInventoryMenuListener.INSTANCE.closeTrackedInventories();
            HandlerList.unregisterAll(this);
            getServer().getMessenger().unregisterIncomingPluginChannel(this);
            Bukkit.getScheduler().cancelTasks(this);
            MillisecondSampler.INSTANCE.stop();
        } catch (Exception failure) {
            Companion.logMessage(formatDisableError(failure.getClass().getName()));
        }
        Companion.logMessage("Disabled Matrix AntiCheat.");
    }

    private void hookPlaceholderApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiHooked = true;
            Companion.logMessage("Hooked with PlaceholderAPI");
        }
    }
    private void warnConflictingPlugins() {
        for (String name : new String[]{"Vulcan", "GrimAC", "SpiterLoader", "Intave", "Polar", "PolarLoader", "Medusa", "Falcon", "AAC5", "Verus", "Diona", "CookieAntiCheat"}) {
            if (Bukkit.getPluginManager().getPlugin(name) != null)
                getLogger().warning(formatConflictWarning(name));
        }
    }

    private void checkEnvironment() throws Throwable {
        Companion.logMessage("Checking Environment...");
        if (!Intrinsics.areEqual(getDescription().getName(), "Matrix")) {
            Companion.logMessage("Matrix didn't load correctly, please check your server environment!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Throwable chatColorLookupFailure = null;
        try { Class.forName("net.md_5.bungee.api.ChatColor"); }
        catch (Throwable failure) { chatColorLookupFailure = failure; }
        if (chatColorLookupFailure != null) {
            Companion.logMessage("** Error: You should use Spigot/Paper to run Matrix!");
            Companion.logMessage("** Don't use CraftBukkit, it's outdated!");
            Companion.logMessage("** Download Paper at: https://papermc.io/");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Plugin viaBackwards = Bukkit.getPluginManager().getPlugin("ViaBackwards");
        if (viaBackwards != null) {
            viaBackwards = Bukkit.getPluginManager().getPlugin("ViaBackwards");
            Intrinsics.checkNotNull(viaBackwards);
            File configurationFile = new File(viaBackwards.getDataFolder(), "config.yml");
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile);
            boolean handlesPingsAsInventoryAcknowledgements =
                    configuration.getBoolean("handle-pings-as-inv-acknowledgements", false);
            if (!handlesPingsAsInventoryAcknowledgements) {
                getLogger().warning(Matrix::viaBackwardsWarning);
                configuration.set("handle-pings-as-inv-acknowledgements", true);
                configuration.save(configurationFile);
                com.viaversion.viabackwards.ViaBackwards.getConfig().reload();
                Companion.sleepAndLog(3000L);
            }
        }
        boolean folia = FoliaSupport.INSTANCE.isUsingFolia();
        if (folia) getLogger().warning("Folia is detected, note that Folia is not a primary support target of Matrix, I cannot guarantee stability when using Folia.");
        boolean unsupportedJvm = Intrinsics.areEqual(SystemPropertyAccess.getProperty("sun.arch.data.model"), "32");
        if (unsupportedJvm) {
            String javaVersion = SystemPropertyAccess.getProperty("java.version");
            String javaVendor = SystemPropertyAccess.getProperty("java.vendor");
            String bits = SystemPropertyAccess.getProperty("sun.arch.data.model");
            Companion.logMessage(formatJavaEnvironment(javaVersion, javaVendor, bits));
            String osName = SystemPropertyAccess.getProperty("os.name");
            String osArchitecture = SystemPropertyAccess.getProperty("os.arch");
            String osVersion = SystemPropertyAccess.getProperty("os.version");
            Companion.logMessage(formatSystemEnvironment(osName, osArchitecture, osVersion));
            Companion.logMessage("Matrix doesn't support 32-bit JVM, please use 64-bit JVM to run Matrix.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Companion.logMessage("Passed all environment checks");
    }

    private void showRegionalNotice() {
        boolean customNotice = ((LocaleConfiguration)getConfigManager().getMainConfiguration())
                .configuration().getBoolean("I-love-RE-forever", false);
        if (customNotice) {
            Companion.logMessage("Good, good, good, good");
            return;
        }
        if (Intrinsics.areEqual(Locale.getDefault().getCountry(), "CN"))
            NotepadPlusPlusNotice.inspectAndNotify();
    }

    public final void reloadConfiguration() {
        Companion.logMessage(LocaleMessages.getLocaleValue("misc.reload.start-reload"));
        CheckRegistry.loaded = false;
        getConfigManager().load();
        getMatrixCommand().registerCommands();
        getPlayerDataManager().reconcile();
        Companion.logMessage(LocaleMessages.getLocaleValue("misc.reload.reloaded"));
        getMatrixLogger().appendLog("Reloaded Matrix.");
    }

    private static String formatConflictWarning(String pluginName) {
        return new StringBuilder(pluginName)
                .append(" is detected, Matrix might not work properly with it.").toString();
    }

    private static String formatDisableError(String className) {
        return "Error when disabling Matrix: " + className;
    }

    private static String formatJavaEnvironment(String version, String vendor, String bitness) {
        return "Java: " + version + "(" + vendor + "/" + bitness + "bit)";
    }

    private static String formatSystemEnvironment(String name, String architecture, String version) {
        return "System: " + name + "(" + architecture + "/" + version + ")";
    }

    private static String formatServerVersion(String version) {
        return "Server Version: " + version;
    }

    private static String formatEnabledVersion(String version) {
        return "Matrix Enabled! Version: " + version;
    }

    private static String formatBannerLine(String line) {
        return "§6* §b" + line;
    }

    private static String formatStartupBanner(String version, String serverVersion) {
        return "§6* §2Matrix AntiCheat §av" + version + " §8| §2" + serverVersion;
    }

    private static String viaBackwardsWarning() {
        return "[!] You are using `ViaBackwards`, but you didn't enable \"handle-pings-as-inv-acknowledgements\" in ViaBackwards config.yml, this may cause some issues.";
    }

    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?> returnType,
                                             int parameterCount, Class<?>[] parameterTypes) {
        Method[] methods = owner.getDeclaredMethods();
        for (Method method : methods) {
            if (!method.getName().equals(name) || method.getReturnType() != returnType) continue;
            Class<?>[] actualTypes = method.getParameterTypes();
            if (actualTypes.length != parameterCount) continue;
            boolean matches = true;
            for (int index = 0; index < parameterCount; index++) {
                if (actualTypes[index] != parameterTypes[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return method;
        }
        return null;
    }

    private static Method findMethodInInterfaces(Class<?> owner, String name, Class<?> returnType,
                                                  int parameterCount, Class<?>[] parameterTypes) {
        Method declared = findDeclaredMethod(owner, name, returnType, parameterCount, parameterTypes);
        if (declared != null) return declared;
        Class<?>[] interfaces = owner.getInterfaces();
        if (interfaces == null) return null;
        for (Class<?> implemented : interfaces) {
            Method inherited = findMethodInInterfaces(implemented, name, returnType,
                    parameterCount, parameterTypes);
            if (inherited != null) return inherited;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void propagate(Throwable failure) throws E { throw (E) failure; }
}
