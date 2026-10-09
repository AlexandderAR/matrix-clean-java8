package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;

public class DeprecatedLagPointPlaceholder extends me.rerere.matrix.commands.condition.placeholders.AbstractStandardPlaceholder {
    public DeprecatedLagPointPlaceholder() { super("lp"); }

    @Override
    public double getStat(Player player) {
        me.rerere.matrix.Matrix.getInstance().getLogger().warning("%lp% was deprecated, please don't use it!");
        return 0.0;
    }
}
