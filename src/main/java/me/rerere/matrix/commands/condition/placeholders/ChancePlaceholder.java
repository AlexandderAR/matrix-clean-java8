package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

public final class ChancePlaceholder extends AbstractParameteredPlaceholder {
    public ChancePlaceholder() { super("chance"); }

    private static String invalidNumberMessage(String value) {
        return "Invalid number format: " + value;
    }

    @Override
    public String getSub(Player player, String parameter) {
        if (!parameter.endsWith("%"))
            throw new RuntimeException("Invalid chance parameter format, expecting <num>%");
        final double percent;
        try {
            percent = Double.parseDouble(parameter.substring(0, parameter.length() - 1));
        } catch (NumberFormatException ex) {
            throw new RuntimeException(invalidNumberMessage(parameter), ex);
        }
        if (percent > 100.0 || percent < 0.0)
            throw new RuntimeException("Expecting percentage between 0 and 100");
        return ThreadLocalRandom.current().nextDouble() * 100.0 < percent ? "1.0" : "0.0";
    }
}
