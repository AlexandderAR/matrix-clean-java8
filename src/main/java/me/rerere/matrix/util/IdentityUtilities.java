package me.rerere.matrix.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.server.ServerNames;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.SpigotReflectionUtil;

/** Player selection and identity generation helpers. */
public final class IdentityUtilities {
    private static final String[] ALPHABET = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };
    private static Field reflectedCounter;

    static {
        try {
            String entityName = Matrix.getServerVersion().isOlderThan(ServerVersion.V_1_17)
                    ? "Entity" : "world.entity.Entity";
            Class<?> entityClass = Class.forName(ReflectionNameResolver.mapDescriptorName(
                    ServerNames.versionedSince117(entityName)));
            if (Matrix.getServerVersion().isOlderThan(ServerVersion.V_1_17)) {
                Field counter = entityClass.getDeclaredField(
                        ReflectionNameResolver.mapFieldName(entityClass, "entityCount"));
                counter.setAccessible(true);
                reflectedCounter = counter;
            } else {
                for (Field field : entityClass.getDeclaredFields()) {
                    if (field.getType().equals(AtomicInteger.class)) {
                        field.setAccessible(true);
                        reflectedCounter = field;
                        break;
                    }
                }
            }
        } catch (NoSuchFieldException | ClassNotFoundException failure) {
            failure.printStackTrace();
        }
    }

    public IdentityUtilities() { }

    public static int randomEntityId(boolean syntheticEntity) {
        boolean primaryThread = Bukkit.isPrimaryThread();
        if (!primaryThread || syntheticEntity) {
            return 100000 + new Random().nextInt(10000);
        }
        return SpigotReflectionUtil.generateEntityId();
    }

    public static String randomToken() {
        int length = 5 + new Random().nextInt(6);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String selectedLetter = ALPHABET[new Random().nextInt(ALPHABET.length)];
            result.append(selectedLetter);
        }
        return result.toString();
    }

    public static String chooseOtherPlayer(Set<Player> players, Player excluded) {
        List<Player> candidates = new ArrayList<>(players);
        candidates.remove(excluded);
        return candidates.get(new Random().nextInt(candidates.size())).getName();
    }

}
