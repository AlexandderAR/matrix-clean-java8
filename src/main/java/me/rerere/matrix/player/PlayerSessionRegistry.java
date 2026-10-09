package me.rerere.matrix.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;
import me.rerere.matrix.Matrix;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import me.rerere.matrix.callbacks.CancelableAction;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.combat.CombatTargetTracker;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.network.packet.TransactionPingQueue;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.User;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.metrics.LatencySweepTask;
import me.rerere.matrix.util.RunFunctionReference;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;

/** Bukkit player-session index and its periodic reconciliation callbacks. */
public final class PlayerSessionRegistry implements Listener {
    private final LatencySweepTask latencySweepTask;
    private final Map<UUID, PlayerCheckContext> sessionsByPlayerId;

    public PlayerSessionRegistry() {
        this.sessionsByPlayerId = (Map<UUID, PlayerCheckContext>) (Map<?, ?>)
                com.google.common.collect.Maps.newConcurrentMap();
        this.latencySweepTask = new LatencySweepTask(this);
        Function0<Unit> observeTask = (Function0<Unit>) (Object)
                new RunFunctionReference(latencySweepTask);
        FoliaSupport.INSTANCE.runTaskTimerAsync(observeTask, 20L, 20L);
        Function0<Unit> maintenanceTask = () -> {
            tickSessions(this);
            return Unit.INSTANCE;
        };
        FoliaSupport.INSTANCE.runTaskTimerAsync(maintenanceTask, 1L, 1L);
        registerAndRefresh();
    }

    private static String offlinePlayerMessage(Player player) {
        return offlinePlayerMessage(player.getName(), player.getUniqueId());
    }

    private static String offlinePlayerMessage(String name, UUID id) {
        return new StringBuilder("Player ").append(name).append('(').append(id)
                .append(") is not online, but in player list. ").toString();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sessionsByPlayerId.remove(player.getUniqueId());
    }

    @Nullable
    public PlayerCheckContext find(UUID playerId) {
        return sessionsByPlayerId.get(playerId);
    }

    private static Unit tickSessions(PlayerSessionRegistry registry) {
        for (PlayerCheckContext session : registry.sessionsByPlayerId.values()) {
            try {
                TransactionPingQueue queue = session.transactionQueue();
                queue.periodicMaintenance();
            } catch (LicenseResponseException failure) {
                throw failure;
            }
        }
        return Unit.INSTANCE;
    }

    public Map<UUID, PlayerCheckContext> sessions() {
        return sessionsByPlayerId;
    }

    public void remove(UUID playerId) {
        sessionsByPlayerId.remove(playerId);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            if (!player.isOnline()) return;
            if (Bukkit.getPlayer(player.getUniqueId()) == null) return;
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        addOnlinePlayer(player);
    }

    /** Periodic reconciliation of sessions and their check state. */
    public void reconcile() {
        for (PlayerCheckContext session : sessionsByPlayerId.values()) {
            Object raw = session.checks().firstCheckOfType(CombatTargetTracker.class);
            if (raw != null) {
                ((CombatTargetTracker) raw).clearTarget();
            }
            CancelableAction pingManager = session.pingManager();
            pingManager.cancel();
        }

        List<UUID> previouslyTracked = new ArrayList<>();
        for (PlayerCheckContext session : sessionsByPlayerId.values()) {
            if (session.isBedrockClient()) {
                previouslyTracked.add(session.playerId());
            }
        }

        sessionsByPlayerId.clear();
        CheckRegistry.constructThrowaway();
        for (Player player : Bukkit.getOnlinePlayers()) addOnlinePlayer(player);
        for (UUID id : previouslyTracked) {
            PlayerCheckContext session = sessionsByPlayerId.get(id);
            if (session != null) session.setBedrockClient(true);
        }
    }

    public void addOnlinePlayer(Player player) {
        Player current;
        try {
            current = Bukkit.getPlayer(player.getUniqueId());
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        if (current == null) {
            Logger logger = Matrix.Companion.matrixInstance().getLogger();
            logger.warning((Supplier<String>) () -> offlinePlayerMessage(player));
            return;
        }
        UUID id = player.getUniqueId();
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;
        PlayerCheckContext session;
        try {
            session = new PlayerCheckContext(id, player.getName(), user);
        } catch (Throwable failure) {
            session = PlayerSessionRegistry.<PlayerCheckContext, RuntimeException>throwUnchecked(failure);
        }
            sessionsByPlayerId.put(id, session);
    }

    private void registerAndRefresh() {
        Collection<? extends Player> onlinePlayers;
        boolean hasOnlinePlayers;
        try {
            PluginManager manager = Bukkit.getPluginManager();
            Plugin plugin = Matrix.Companion.matrixInstance();
            manager.registerEvents(this, plugin);
            onlinePlayers = Bukkit.getOnlinePlayers();
            hasOnlinePlayers = !onlinePlayers.isEmpty();
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        if (hasOnlinePlayers) {
            List<Player> notified = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Permissible permissible = player;
                if (permissible.isOp() || permissible.hasPermission("matrix.notify")) notified.add(player);
            }
            for (Player player : notified) player.sendMessage("\u00a78[Matrix] Hot Reloaded.");
        }
        for (Player player : Bukkit.getOnlinePlayers()) addOnlinePlayer(player);
        CheckRegistry.constructThrowaway();
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }

}
