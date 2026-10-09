package me.rerere.matrix.util.text;

import java.util.Map;
import java.util.StringJoiner;
import org.bukkit.Location;
import org.bukkit.block.Block;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.world.BlockGeometry;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/** Formatting helpers for coordinates, booleans, enum values and map entries. */
public final class DiagnosticFormat {
    private DiagnosticFormat() { }

    private static String concatenate(String prefix, String value) {
        return prefix + value;
    }

    public static String formatBooleanArray(boolean[] values) {
        Boolean[] boxed = new Boolean[values.length];
        for (int i = 0; i < values.length; i++) boxed[i] = values[i];
        return joinBooleans(boxed);
    }

    public static String formatFloat(float value) {
        return String.format("%.3f", value);
    }

    public static String movementSigns(PlayerMoveEvent event) {
        String from = BlockGeometry.collides(event.getFrom()) ? "+" : "-";
        String to = BlockGeometry.collides(java.util.Objects.requireNonNull(event.getTo())) ? "+" : "-";
        return from + to;
    }

    private static CharSequence mapEntry(Map.Entry<?, ?> entry) {
        Object key = entry.getKey();
        Object value = entry.getValue();
        return entryText(key, value);
    }

    public static String formatBoolean(boolean value) {
        return value ? "+" : "-";
    }

    public static String formatDouble(double value) {
        return String.format("%.3f", value);
    }

    public static String formatLocation(Location location) {
        if (MatrixConfiguration.hideLocation) {
            return "?,?,?";
        }
        return coordinates(formatDouble(location.getX()), formatDouble(location.getY()), formatDouble(location.getZ()));
    }

    private static CharSequence enumName(Enum<?> value) {
        return value.name();
    }

    private static String coordinates(String x, String y, String z) {
        return x + "," + y + "," + z;
    }

    public static String joinMap(Map<?, ?> values) {
        StringJoiner joined = new StringJoiner(", ");
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            joined.add(mapEntry(entry).toString());
        }
        return joined.toString();
    }

    private static CharSequence booleanCharSequence(boolean value) {
        return formatBoolean(value);
    }

    public static String joinBooleans(Boolean[] values) {
        StringJoiner joined = new StringJoiner(",");
        for (Boolean value : values) {
            joined.add(formatBoolean(value.booleanValue()));
        }
        return joined.toString();
    }

    public static String joinEnums(Iterable<?> values) {
        StringJoiner joined = new StringJoiner(",");
        for (Object value : values) {
            joined.add(enumName((Enum<?>) value).toString());
        }
        return joined.toString();
    }

    private static String entryText(Object key, Object value) {
        return key + ": " + value;
    }

    private static String parenthesized(String name, String value) {
        return name + "(" + value + ")";
    }

    public static String blockDescription(Block block) {
        return block.getType().name() + "(" + formatLocation(block.getLocation()) + ")";
    }

    public static String joinIterable(Iterable<?> values) {
        StringJoiner joined = new StringJoiner(",");
        for (Object value : values) joined.add(String.valueOf(value));
        return joined.toString();
    }

    private static CharSequence booleanTransform(boolean value) {
        return formatBoolean(value);
    }

    public static String vector(Vector vector) {
        return coordinates(formatDouble(vector.getX()), formatDouble(vector.getY()), formatDouble(vector.getZ()));
    }

}
