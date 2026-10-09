package me.rerere.matrix.proxy.bungee;

import java.util.UUID;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.geysermc.api.Geyser;

/** Access point for determining whether a Bungee player is a Bedrock client. */
public final class GeyserPlayerAccess {
    public static final GeyserPlayerAccess INSTANCE = new GeyserPlayerAccess();

    private GeyserPlayerAccess() { }

    public static boolean isBedrockPlayer(ProxiedPlayer player) {
        UUID playerId = player.getUniqueId();
        if (playerId.toString().startsWith("00000000-0000-0000-")) return true;
        return Geyser.api().isBedrockPlayer(player.getUniqueId());
    }
}
