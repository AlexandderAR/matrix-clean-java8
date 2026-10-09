package me.rerere.matrix.permissions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.sequences.SequencesKt;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.runtime.ModuleRegistry;
import me.rerere.matrix.util.RunActionReference;

public final class PermissionCacheManager implements Listener {
    private final Map<UUID, PermissionCache> playerPermissions = new HashMap<>();
    public static final ModuleRegistry COMPANION = new ModuleRegistry();
    private static final List<String> declaredPermissions = SequencesKt.toList(SequencesKt.map(
            CollectionsKt.asSequence(Matrix.Companion.matrixInstance().getDescription().getPermissions()), Permission::getName));

    public PermissionCacheManager() {
        Bukkit.getPluginManager().registerEvents(this, Matrix.Companion.matrixInstance());
        FoliaSupport.INSTANCE.runTaskTimerAsync(new RunActionReference(this),
                20L, (long) MatrixConfiguration.permissionRefreshInterval * 20L);
    }

    public boolean hasCachedPermission(Player player, String permission) {
        PermissionCache cached = playerPermissions.get(player.getUniqueId());
        Intrinsics.checkNotNull(cached);
        return cached.hasPermission(7467131, permission);
    }
    public static boolean hasPermission(Player player, String permission) { return COMPANION.isAllowed(player, permission); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) { playerPermissions.remove(event.getPlayer().getUniqueId()); }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) { refreshPlayer(event.getPlayer()); }
    public void refreshOnlinePlayers() {
        Iterable<? extends Player> online = Bukkit.getOnlinePlayers();
        for (Player player : online) refreshPlayer(player);
    }
    public void refreshPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerPermissions.containsKey(playerId)) {
            playerPermissions.put(playerId, new PermissionCache(playerId, null, 2));
        }
        PermissionCache cached = playerPermissions.get(playerId);
        for (String permission : declaredPermissions) {
            Intrinsics.checkNotNull(cached);
            cached.cachePermission(permission, player.hasPermission(permission));
            boolean globalBypass = StringsKt.equals(permission, "matrix.bypass", true);
            if (globalBypass) {
                PlayerCheckContext state = Matrix.Companion.matrixInstance()
                        .getPlayerDataManager().find(playerId);
                if (state != null) state.setPermissionBypass(cached.hasPermission(7467131, permission));
            }
        }
        for (CheckCategory category : CheckCategory.values()) {
            String lower = category.name().toLowerCase(Locale.ROOT);
            Intrinsics.checkNotNullExpressionValue(lower, "toLowerCase(...)");
            boolean bypass = player.hasPermission("matrix.bypass." + lower);
            PlayerCheckContext state = Matrix.Companion.matrixInstance()
                    .getPlayerDataManager().find(playerId);
            if (state != null) {
                ConcurrentHashMap<CheckCategory, Boolean> perCategory = state.categoryBypasses();
                if (perCategory != null) perCategory.put(category, bypass);
            }
            Intrinsics.checkNotNull(cached);
            cached.cachePermission("matrix.bypass." + lower, bypass);
        }
    }
}
