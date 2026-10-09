package me.rerere.matrix.proxy.velocity;

import com.velocitypowered.api.proxy.Player;
import org.geysermc.api.Geyser;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;

/** Adapter for querying Geyser's Bedrock-player registry. */
public final class GeyserAccessor {
    @NotNull
    public static final GeyserAccessor INSTANCE = new GeyserAccessor();
    private GeyserAccessor() { }
    public static boolean isBedrockPlayer(@NotNull Player player) {
        return Geyser.api().isBedrockPlayer(player.getUniqueId());
    }
}
