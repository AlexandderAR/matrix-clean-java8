package me.rerere.matrix.bootstrap;

import java.io.Closeable;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.condition.ConditionalCommands;
import me.rerere.matrix.compat.CompatibilityManager;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.dev.thomazz.pledge.util.TickEndTask;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;

public final class MatrixServiceRegistrar {
    private MatrixServiceRegistrar() {}

    public static void registerComponentsAndListeners(Matrix matrix) {
        matrix.setPlayerDataManager(new me.rerere.matrix.player.PlayerSessionRegistry());
        matrix.setPermissionManager(new me.rerere.matrix.permissions.PermissionCacheManager());

        ConditionalCommands conditionalCommands = new ConditionalCommands();
        matrix.setConditionalCommands(conditionalCommands);
        me.rerere.matrix.util.TypedMatrixMembers.getConditionalCommands(matrix).enable();

        matrix.setPacketListener(new me.rerere.matrix.network.PlayerPacketListener());
        matrix.setPingPongListener(new me.rerere.matrix.network.packet.TransactionPacketListener());
        matrix.setFilters(new me.rerere.matrix.network.PlayerMetadataPacketListener());

        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListenerCommon[] {
                me.rerere.matrix.util.TypedMatrixMembers.getPacketListener(matrix),
                me.rerere.matrix.util.TypedMatrixMembers.getPingPongListener(matrix),
                me.rerere.matrix.util.TypedMatrixMembers.getFilters(matrix)
        });

        matrix.setMoveHistory(new me.rerere.matrix.movement.PlayerMovementHistory());
        matrix.setChunkManager(new me.rerere.matrix.world.ChunkActivityCache());
        matrix.setClientBlockManager(new me.rerere.matrix.world.BlockLocationCache());
        matrix.setCheckListener(new me.rerere.matrix.listeners.BlockWorldListener());
        Bukkit.getPluginManager().registerEvents(me.rerere.matrix.util.TypedMatrixMembers.getCheckListener(matrix), matrix);

        ServerVersion serverVersion = me.rerere.matrix.util.TypedMatrixMembers.serverVersion();
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_13)) {
            Bukkit.getPluginManager().registerEvents(new me.rerere.matrix.checks.listener.RiptideAndPlacementListener(), matrix);
        }
        if (me.rerere.matrix.util.TypedMatrixMembers.serverVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            matrix.setElytraListener(new me.rerere.matrix.network.GlideMetadataListener());
            Bukkit.getPluginManager().registerEvents(me.rerere.matrix.util.TypedMatrixMembers.getElytraListener(matrix), matrix);
            PacketEvents.getAPI().getEventManager().registerListeners(new PacketListenerCommon[] {
                    me.rerere.matrix.util.TypedMatrixMembers.getElytraListener(matrix)
            });
        }
        if (me.rerere.matrix.util.TypedMatrixMembers.serverVersion().isNewerThanOrEquals(ServerVersion.V_1_12)) {
            Bukkit.getPluginManager().registerEvents(new me.rerere.matrix.listeners.ShearCooldownListener(), matrix);
        }
        Bukkit.getPluginManager().registerEvents(new me.rerere.matrix.listeners.TntBehaviorListener(), matrix);

        if (FoliaSupport.INSTANCE.isUsingFolia()) {
            Bukkit.getPluginManager().registerEvents(new me.rerere.matrix.listeners.WorldChangeListener(), matrix);
        }
        Bukkit.getPluginManager().registerEvents(me.rerere.matrix.inventory.PlayerInventoryMenuListener.INSTANCE, matrix);
        PacketEvents.getAPI().init();

        if (me.rerere.matrix.util.PaperAvailability.INSTANCE.isPaperConfigPresent()
                && me.rerere.matrix.util.PaperAvailability.INSTANCE.hasClass(
                        "com.destroystokyo.paper.event.server.ServerTickEndEvent")) {
            PluginManager pluginManager = Bukkit.getPluginManager();
            pluginManager.registerEvents(new me.rerere.matrix.listeners.TickEndEventHandler(), matrix);
        } else {
            TickEndTask task = TickEndTask.create(new me.rerere.matrix.player.SessionRefreshTask(matrix));
            matrix.setTickEndTask(task);
        }

        matrix.setRelogManager(new me.rerere.matrix.listeners.LoginCooldownListener());
        Bukkit.getPluginManager().registerEvents(me.rerere.matrix.util.TypedMatrixMembers.getRelogManager(matrix), matrix);
        FoliaSupport.INSTANCE.runTaskTimerAsync(new me.rerere.matrix.crypto.ProviderRefreshCallback(me.rerere.matrix.util.TypedMatrixMembers.getRelogManager(matrix)), 20L, 20L);

        CompatibilityManager compatibilityManager = new CompatibilityManager();
        matrix.setCompatibilityManager(compatibilityManager);
        me.rerere.matrix.util.TypedMatrixMembers.getCompatibilityManager(matrix).load();

        matrix.getServer().getMessenger().registerIncomingPluginChannel(
                matrix, "matrix:geyser", new me.rerere.matrix.compat.GeyserPluginMessageListener());
        me.rerere.matrix.util.TypedMatrixMembers.startMillisecondSampler();
        me.rerere.matrix.util.TypedMatrixMembers.getInferenceTask(matrix).schedule();
    }

    public static void closeSelectedNormal(Closeable closeable) {
        CloseableKt.closeFinally(closeable, null);
    }
}
