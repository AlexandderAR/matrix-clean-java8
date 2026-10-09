package me.rerere.matrix.commands.condition;

import me.rerere.matrix.commands.condition.placeholders.Placeholder;
import me.rerere.matrix.commands.condition.placeholders.PlaceholderPing;
import me.rerere.matrix.commands.condition.placeholders.DeprecatedLagPointPlaceholder;
import me.rerere.matrix.commands.condition.placeholders.PlaceholderTPS;
import me.rerere.matrix.commands.condition.placeholders.PlaceholderTimeOnline;
import me.rerere.matrix.commands.condition.placeholders.PlaceholderPlayerCount;
import me.rerere.matrix.commands.condition.placeholders.PlaceholderUptime;
import me.rerere.matrix.commands.condition.placeholders.PermissionPlaceholder;
import me.rerere.matrix.commands.condition.placeholders.ViolationLevelPlaceholder;
import me.rerere.matrix.commands.condition.placeholders.ChancePlaceholder;

public enum Placeholders {
    PING(new PlaceholderPing()),
    LAG_POINT(new DeprecatedLagPointPlaceholder()),
    TPS(new PlaceholderTPS()),
    TIME_ONLINE(new PlaceholderTimeOnline()),
    PLAYER_COUNT(new PlaceholderPlayerCount()),
    UPTIME(new PlaceholderUptime()),
    PERM(new PermissionPlaceholder()),
    VL(new ViolationLevelPlaceholder()),
    CHANCE(new ChancePlaceholder());

    private final Placeholder placeholder;

    Placeholders(Placeholder placeholder) {
        this.placeholder = placeholder;
    }

    public Placeholder getPlaceholder() {
        return placeholder;
    }
}
