package me.rerere.matrix.util;

import java.lang.reflect.Field;
import org.bukkit.entity.Player;
import me.rerere.matrix.server.ServerNames;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;

/** Lazy reflective access to the player connection's last position and look fields. */
public final class PlayerConnectionFields {
    private PlayerConnectionFields() { }

    private static final Lazy<Class<?>> CONNECTION_CLASS = LazyKt.lazy(
            (Function0<Class<?>>) PlayerConnectionFields::resolveConnectionClass);
    private static final Lazy<Field> LAST_POS_X = LazyKt.lazy(
            (Function0<Field>) () -> resolveField("lastPosX"));
    private static final Lazy<Field> LAST_POS_Y = LazyKt.lazy(
            (Function0<Field>) () -> resolveField("lastPosY"));
    private static final Lazy<Field> LAST_POS_Z = LazyKt.lazy(
            (Function0<Field>) () -> resolveField("lastPosZ"));
    private static final Lazy<Field> LAST_YAW = LazyKt.lazy(
            (Function0<Field>) () -> resolveField("lastYaw"));
    private static final Lazy<Field> LAST_PITCH = LazyKt.lazy(
            (Function0<Field>) () -> resolveField("lastPitch"));

    private static Class<?> resolveConnectionClass() {
        String className = ServerNames.versioned(
                "PlayerConnection", "server.network.PlayerConnection");
        String resolvedName = ReflectionNameResolver.mapDescriptorName(className);
        try {
            return Class.forName(resolvedName);
        } catch (ClassNotFoundException exception) {
            return PlayerConnectionFields.<RuntimeException, Class<?>>rethrow(exception);
        }
    }

    private static Field resolveField(String requestedFieldName) {
        Class<?> connectionClass = CONNECTION_CLASS.getValue();
        String fieldName = ReflectionNameResolver.mapFieldName(connectionClass, requestedFieldName);
        try {
            Field field = connectionClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            return PlayerConnectionFields.<RuntimeException, Field>rethrow(exception);
        }
    }

    public static void updateLastPositionAndLook(Player player, double x, double y, double z,
                                                 float yaw, float pitch) {
        Object connection = SpigotReflectionUtil.getPlayerConnection(player);
        set(LAST_POS_X, connection, x);
        set(LAST_POS_Y, connection, y);
        set(LAST_POS_Z, connection, z);
        set(LAST_YAW, connection, yaw);
        set(LAST_PITCH, connection, pitch);
    }

    private static void set(Lazy<Field> field, Object receiver, Object fieldValue) {
        try {
            field.getValue().set(receiver, fieldValue);
        } catch (IllegalAccessException exception) {
            PlayerConnectionFields.<RuntimeException, Void>rethrow(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R rethrow(Throwable throwable) throws E {
        throw (E) throwable;
    }
}

