package me.rerere.matrix.player;

import org.bukkit.entity.Player;
import org.geysermc.api.Geyser;

/** Provides the Geyser bedrock-player query. */
public final class GeyserPlayerSupport {
    public static final GeyserPlayerSupport INSTANCE;

    static {
        INSTANCE = new GeyserPlayerSupport();
    }

    private GeyserPlayerSupport() {}

    public static boolean isBedrockPlayer(Player player) {
        return Geyser.api().isBedrockPlayer(player.getUniqueId());
    }
}
