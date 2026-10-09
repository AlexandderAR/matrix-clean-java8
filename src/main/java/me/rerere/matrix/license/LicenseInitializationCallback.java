package me.rerere.matrix.license;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.listener.RiptideAndPlacementListener;
import me.rerere.matrix.commands.MatrixCommandDispatcher;
import me.rerere.matrix.commands.UpdateCommand;
import me.rerere.matrix.commands.condition.ConditionalCommands;
import me.rerere.matrix.compat.GeyserPluginMessageListener;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.compat.SystemPropertyAccess;
import me.rerere.matrix.crypto.ProviderRefreshCallback;
import me.rerere.matrix.listeners.TickEndEventHandler;
import me.rerere.matrix.listeners.WorldChangeListener;
import me.rerere.matrix.listeners.LoginCooldownListener;
import me.rerere.matrix.listeners.ShearCooldownListener;
import me.rerere.matrix.listeners.TntBehaviorListener;
import me.rerere.matrix.inventory.PlayerInventoryMenuListener;
import me.rerere.matrix.listeners.BlockWorldListener;
import me.rerere.matrix.compat.CompatibilityManager;
import me.rerere.matrix.movement.PlayerMovementHistory;
import me.rerere.matrix.network.GlideMetadataListener;
import me.rerere.matrix.network.PlayerMetadataPacketListener;
import me.rerere.matrix.network.PlayerPacketListener;
import me.rerere.matrix.network.packet.TransactionPacketListener;
import me.rerere.matrix.permissions.PermissionCacheManager;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.plugin.MatrixPluginAccessor;
import me.rerere.matrix.player.SessionRefreshTask;
import me.rerere.matrix.player.PlanState;
import me.rerere.matrix.util.BooleanStateBridge;
import me.rerere.matrix.util.ByteHexFormatter;
import me.rerere.matrix.util.PaperAvailability;
import me.rerere.matrix.util.TypedMatrixMembers;
import me.rerere.matrix.world.BlockLocationCache;
import me.rerere.matrix.world.ChunkActivityCache;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.dev.thomazz.pledge.util.TickEndTask;
import me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;

public final class LicenseInitializationCallback {
    private LicenseInitializationCallback() {}
    public interface NativePreparation { void prepareNative(Object runtime, Object argument) throws Exception; }
    public static final class UnsupportedNativePath extends Error {
        public UnsupportedNativePath(String message) { super(message); }
    }
    public static void initializeLicense(Object runtime, Object argument,
                                         NativePreparation adapter) throws InterruptedException {
        int attempt = 1;
        while (attempt <= 3) {
            try {
                if (runtime == null) throw new NullPointerException("\"INVOKESPECIAL Void npe\" on -1");
                adapter.prepareNative(runtime, argument);
                String fingerprint;
                if (new File("/.dockerenv").exists()) {
                    fingerprint = "docker";
                } else {
                    String mac = firstHardwareMac();
                    int processorCount = Runtime.getRuntime().availableProcessors();
                    String operatingSystem = SystemPropertyAccess.getProperty("os.name");
                    String localAddresses;
                    try { localAddresses = localIpAddress(); }
                    catch (Exception failure) { throw new LicenseResponseException("Failed to get local IP address"); }
                    String fingerprintInput = new StringBuilder(mac).append(processorCount)
                            .append(operatingSystem).append(localAddresses).toString();
                    byte[] digest = MessageDigest.getInstance("MD5")
                            .digest(fingerprintInput.getBytes(Charsets.UTF_8));
                    fingerprint = ArraysKt.joinToString(digest, "", "", "", -1, "...", ByteHexFormatter.INSTANCE);
                }
                MatrixCommandDispatcher.hardwareId = fingerprint;
            } catch (Exception failure) {
                failure.printStackTrace();
                matrixLogger().warning("Failed to load Matrix AntiCheat, retrying in 3000 ms...");
                Thread.sleep(3000L);
                attempt++;
                continue;
            }
            if (attempt < 4) {
                matrixLogger().info("Matrix AntiCheat has been fully initialized.");
                return;
            }
            throw new UnsupportedOperationException("Unexpected attempt value after selected success");
        }
        matrixLogger().warning("Failed to load Matrix AntiCheat, PLS Check Your Network Connection and Token.");
        Thread.sleep(2000L);
        if (!LicenseRuntimeBridge.__ntjGetAngle((LicenseRuntimeBridge) runtime)) {
            LicenseRuntimeBridge.__ntjSetAngle((LicenseRuntimeBridge) runtime, true);
        }
        List<String> features = CollectionsKt.emptyList();
        if (me.rerere.matrix.Matrix.Companion == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        me.rerere.matrix.Matrix plugin = MatrixPluginAccessor.getMatrix();
        PluginCommand command = Bukkit.getPluginCommand("matrix");
        MatrixCommandDispatcher executor = new MatrixCommandDispatcher();
        if (plugin == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
        plugin.setMatrixCommand(executor);
        Intrinsics.checkNotNull(command);
        command.setExecutor(TypedMatrixMembers.getMatrixCommand(plugin));
        command.setTabCompleter(TypedMatrixMembers.getMatrixCommand(plugin));
        PlanState.INSTANCE.setCloudEnabled(false);
        PlanState.INSTANCE.setPlanNames(features);
        if (features.contains("Trial")) {
            UpdateCommand.COMPANION.setEnabled(true);
            BooleanStateBridge.angle.setUpdaterEnabled(true);
        }
        if (!features.isEmpty()) throw new UnsupportedNativePath("Native feature pass 1 is unresolved");
        if (!features.isEmpty()) throw new UnsupportedNativePath("Native feature pass 2 is unresolved");
        if (!features.isEmpty()) throw new UnsupportedNativePath("Native feature pass 3 is unresolved");
        plugin.setPlayerDataManager(new PlayerSessionRegistry());
        plugin.setPermissionManager(new PermissionCacheManager());
        plugin.setConditionalCommands(new ConditionalCommands());
        TypedMatrixMembers.getConditionalCommands(plugin).enable();
        plugin.setPacketListener(new PlayerPacketListener());
        plugin.setPingPongListener(new TransactionPacketListener());
        plugin.setFilters(new PlayerMetadataPacketListener());
        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListenerCommon[]{
                TypedMatrixMembers.getPacketListener(plugin), TypedMatrixMembers.getPingPongListener(plugin),
                TypedMatrixMembers.getFilters(plugin)});
        plugin.setMoveHistory(new PlayerMovementHistory());
        plugin.setChunkManager(new ChunkActivityCache());
        plugin.setClientBlockManager(new BlockLocationCache());
        plugin.setCheckListener(new BlockWorldListener());
        Bukkit.getPluginManager().registerEvents(TypedMatrixMembers.getCheckListener(plugin), plugin);
        if (TypedMatrixMembers.serverVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            Bukkit.getPluginManager().registerEvents(new RiptideAndPlacementListener(), plugin);
        }
        if (TypedMatrixMembers.serverVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            plugin.setElytraListener(new GlideMetadataListener());
            Bukkit.getPluginManager().registerEvents(TypedMatrixMembers.getElytraListener(plugin), plugin);
            PacketEvents.getAPI().getEventManager().registerListeners(new PacketListenerCommon[]{TypedMatrixMembers.getElytraListener(plugin)});
        }
        if (TypedMatrixMembers.serverVersion().isNewerThanOrEquals(ServerVersion.V_1_12)) {
            Bukkit.getPluginManager().registerEvents(new ShearCooldownListener(), plugin);
        }
        Bukkit.getPluginManager().registerEvents(new TntBehaviorListener(), plugin);
        if (FoliaSupport.INSTANCE.isUsingFolia()) {
            Bukkit.getPluginManager().registerEvents(new WorldChangeListener(), plugin);
        }
        Bukkit.getPluginManager().registerEvents(PlayerInventoryMenuListener.INSTANCE, plugin);
        PacketEvents.getAPI().init();
        if (PaperAvailability.INSTANCE.isPaperConfigPresent()
                && PaperAvailability.INSTANCE.hasClass("com.destroystokyo.paper.event.server.ServerTickEndEvent")) {
            PluginManager manager = Bukkit.getPluginManager();
            TickEndEventHandler listener = new TickEndEventHandler();
            manager.registerEvents(listener, plugin);
        } else {
            TickEndTask task = TickEndTask.create(new SessionRefreshTask(plugin));
            plugin.setTickEndTask(task);
        }
        plugin.setRelogManager(new LoginCooldownListener());
        Bukkit.getPluginManager().registerEvents(TypedMatrixMembers.getRelogManager(plugin), plugin);
        FoliaSupport.INSTANCE.runTaskTimerAsync(
                new ProviderRefreshCallback(TypedMatrixMembers.getRelogManager(plugin)), 20L, 20L);
        plugin.setCompatibilityManager(new CompatibilityManager());
        TypedMatrixMembers.getCompatibilityManager(plugin).load();
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "matrix:geyser", new GeyserPluginMessageListener());
        TypedMatrixMembers.startMillisecondSampler();
        TypedMatrixMembers.getInferenceTask(plugin).schedule();
        matrixLogger().info("Matrix AntiCheat has been fully initialized.");
    }

    private static Logger matrixLogger() {
        me.rerere.matrix.Matrix plugin = MatrixPluginAccessor.getMatrix();
        if (plugin == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Logger logger = plugin.getLogger();
        if (logger == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
        return logger;
    }
    private static String localIpAddress() throws SocketException {
        String localAddresses = "";
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address.isSiteLocalAddress()) {
                    localAddresses = new StringBuilder(localAddresses)
                            .append(address.getHostAddress()).toString();
                }
            }
        }
        return localAddresses;
    }
    private static String formatHardware(byte[] address) {
        return HardwareAddressFormatter.format(address);
    }

    private static String firstHardwareMac() throws SocketException {
        NetworkInterface selected = null;
        for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (network.getHardwareAddress() != null) { selected = network; break; }
        }
        return selected == null ? "" : formatHardware(selected.getHardwareAddress());
    }
}
