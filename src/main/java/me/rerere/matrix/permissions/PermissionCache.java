package me.rerere.matrix.permissions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Stores cached permissions for a player and resolves cache misses from Bukkit. */
public final class PermissionCache {
    private final Map<String, Boolean> cacheMap;
    private final UUID uuid;
    private static final long HASH_MULTIPLIER = 8096984006978961439L;

    public PermissionCache(UUID uuid, Map<String, Boolean> cacheMap) {
        this.uuid = uuid;
        this.cacheMap = cacheMap;
    }

    public void cachePermission(String permission, boolean granted) {
        cacheMap.put(permission, Boolean.valueOf(granted));
    }

    public PermissionCache copy(UUID uuid, Map<String, Boolean> cacheMap) {
        return new PermissionCache(uuid, cacheMap);
    }

    public static PermissionCache copyWithDefaults(PermissionCache original, UUID uuid,
                                                    Map<String, Boolean> cacheMap, int defaultMask) {
        UUID selectedUuid = uuid;
        Map<String, Boolean> selectedCache = cacheMap;
        if ((defaultMask & 1) != 0) {
            selectedUuid = original.uuid;
        }
        if ((defaultMask & 2) != 0) {
            selectedCache = original.cacheMap;
        }
        return original.copy(selectedUuid, selectedCache);
    }

    private static String format(UUID playerId, Map<?, ?> permissions) {
        return new StringBuilder("PermissionCache(uuid=").append(playerId)
                .append(", cacheMap=").append(permissions).append(")").toString();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PermissionCache)) return false;
        PermissionCache that = (PermissionCache) other;
        return Intrinsics.areEqual(uuid, that.uuid)
                && Intrinsics.areEqual(cacheMap, that.cacheMap);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode() * (int) HASH_MULTIPLIER + cacheMap.hashCode();
    }

    @Override
    public String toString() {
        return format(uuid, cacheMap);
    }

    public boolean hasPermission(int refreshMode, String permission) {
        Boolean cachedResult = cacheMap.get(permission);
        if (cachedResult != null && refreshMode >= 0) {
            return cachedResult.booleanValue();
        }
        Player player = Bukkit.getPlayer(uuid);
        Boolean liveResult = player == null
                ? null : Boolean.valueOf(player.hasPermission(permission));
        return liveResult != null ? liveResult.booleanValue() : false;
    }

    public PermissionCache(UUID uuid, Map<String, Boolean> cacheMap, int defaultMask) {
        this(uuid, (defaultMask & 2) != 0 ? new LinkedHashMap<String, Boolean>() : cacheMap);
    }
}
