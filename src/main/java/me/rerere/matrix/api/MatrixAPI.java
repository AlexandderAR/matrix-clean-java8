package me.rerere.matrix.api;

import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** Public Matrix API for bypass, verbose notifications, violations, and client state. */
public interface MatrixAPI {
    boolean hasVerbose(Player player);

    boolean isBypass(Player player);

    boolean isEnable(HackType hackType);

    double getTPS();

    void setEnable(HackType hackType, boolean enabled);

    void setVerbose(Player target, boolean verbose, Player actor);

    void setVerbose(Player target, boolean verbose);

    void flag(Player player, HackType hackType, String message, String detail, int threshold);

    void setViolations(Player player, HackType hackType, int violations);

    void reloadPermissionCache(Player player);

    void registerClientBlock(Block block);

    void tempBypass(Player player, HackType hackType, Long expiry);

    int getViolations(Player player, HackType hackType);

    int getLatency(Player player);

    void markAsBedrockPlayer(Player player);

    boolean isBypass(@NotNull Player player, @NotNull HackType hackType);
}
