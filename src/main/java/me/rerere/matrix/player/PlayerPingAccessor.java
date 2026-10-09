package me.rerere.matrix.player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;

/** Resolves the server handle ping field on older Bukkit versions. */
public final class PlayerPingAccessor {
    private static Method handleMethod;
    private static Field pingField;

    private PlayerPingAccessor() { }

    public static int readHandlePing(Player player) {
        try {
            if (handleMethod == null) {
                Class<?> playerClass = player.getClass();
                String methodName = ReflectionNameResolver.mapMethodName(
                        "getHandle", playerClass, new Class<?>[0]);
                handleMethod = playerClass.getDeclaredMethod(methodName, new Class<?>[0]);
                handleMethod.setAccessible(true);
            }

            Object handle = handleMethod.invoke(player, new Object[0]);
            if (pingField == null) {
                ServerVersion version = Matrix.getServerVersion();
                if (version.isOlderThan(ServerVersion.V_1_17)) {
                    Class<?> handleClass = handle.getClass();
                    String fieldName = ReflectionNameResolver.mapFieldName(
                            handleClass, "ping");
                    pingField = handleClass.getDeclaredField(fieldName);
                    pingField.setAccessible(true);
                }
            }

            boolean atLeast117 = Matrix.getServerVersion()
                    .isNewerThanOrEquals(ServerVersion.V_1_17);
            if (atLeast117) return player.getPing();
            return Math.max(pingField.getInt(handle), 0);
        } catch (Exception failure) {
            failure.printStackTrace();
            return 0;
        }
    }

    public static int getPing(Player player) {
        if (player == null || !player.isOnline()) return 0;

        if (CheckCategory.BADPACKETS.isEnabled()) {
            PlayerCheckContext session = Matrix.getInstance()
                    .getPlayerDataManager().find(player.getUniqueId());
            if (session != null) return (int) session.latencyMilliseconds();
        }
        return readHandlePing(player);
    }

}
