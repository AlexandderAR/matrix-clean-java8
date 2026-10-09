package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;
import me.rerere.matrix.player.PlayerPingAccessor;

/** Placeholder whose value is the player's current ping, in milliseconds. */
public class PlaceholderPing extends AbstractStandardPlaceholder {
    public PlaceholderPing() { super("ping"); }

    @Override
    public double getStat(Player player) {
        return PlayerPingAccessor.getPing(player);
    }
}
