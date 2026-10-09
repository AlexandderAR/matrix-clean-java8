package me.rerere.matrix.compat;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.GeyserPlayerSupport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;

/** Detects Bedrock players through Geyser's Bukkit bridge or Floodgate. */
public class BedrockPlayerSupport {
    public static boolean geyserEnabled;
    public static boolean floodgateEnabled;

    public BedrockPlayerSupport() { }

    public static void detectPlugins() {
        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser != null && geyser.isEnabled()) {
            geyserEnabled = true;
            Matrix.logMessage("(Compatibility) Hooked with GeyserMC");
        }

        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        if (floodgate != null && floodgate.isEnabled()) {
            floodgateEnabled = true;
            geyserEnabled = true;
            Matrix.logMessage("(Compatibility) Hooked with Floodgate");
        }
    }

    public static boolean isBedrockPlayer(Player player) {
        if (floodgateEnabled) {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        }
        return GeyserPlayerSupport.isBedrockPlayer(player);
    }
}
