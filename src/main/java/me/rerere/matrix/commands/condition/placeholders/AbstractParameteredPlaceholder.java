package me.rerere.matrix.commands.condition.placeholders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public abstract class AbstractParameteredPlaceholder implements Placeholder {
    private final Pattern pattern;

    public AbstractParameteredPlaceholder(String placeholderName) {
        pattern = Pattern.compile(buildPatternSource(placeholderName));
    }

    private static String buildPatternSource(String placeholderName) {
        return "-" + placeholderName + ":([A-Za-z0-9%.]*)-";
    }

    @Override
    public String doSubstitution(String text, Player player) {
        Matcher placeholderMatcher = pattern.matcher(text);
        while (placeholderMatcher.find()) {
            String placeholder = placeholderMatcher.group();
            String parameter = placeholderMatcher.group(1);
            text = text.replaceAll(Pattern.quote(placeholder), getSub(player, parameter));
        }
        return text;
    }

    public boolean shouldApply(String text) {
        return pattern.matcher(text).find();
    }

    public abstract String getSub(Player player, String parameter);

    public void init(Plugin plugin) { }
}
