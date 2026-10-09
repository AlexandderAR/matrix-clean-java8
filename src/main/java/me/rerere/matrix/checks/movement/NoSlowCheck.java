package me.rerere.matrix.checks.movement;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

/** Detects unusually rapid sneak and blocking-use packet sequences. */
public final class NoSlowCheck extends CheckRuntimeState {
    private static boolean enabled;
    private static boolean status = true;

    private boolean correctNextMove;
    private long lastSneakPacketAt;
    private int sneakPacketCount;
    private int blockUsePacketCount;
    private long lastReportAt;
    private int consecutiveBlockUseCount;
    private long lastReleaseUseItemAt;

    public NoSlowCheck(PlayerCheckContext playerData) {
        super(playerData);
        sneakPacketCount = 0;
        setCheckType(CheckCategory.MOVE);
    }

    private static String frequentSneakMessage(long elapsed) {
        return new StringBuilder("(sneak) send sneak packet frequently: ")
                .append(elapsed).toString();
    }

    public void onTick() {
        if (System.currentTimeMillis() - lastReportAt < 1_000L) {
            return;
        }
        if (blockUsePacketCount > 10 && consecutiveBlockUseCount > 8) {
            flag("move.blk.packet",
                    "(blocking) illegal using items when moving", 10);
        }
        lastReportAt = System.currentTimeMillis();
        consecutiveBlockUseCount = 0;
        blockUsePacketCount = 0;
    }

    public void loadConfiguration() {
        ConfigurationSection config = configuration();
        enabled = config.getBoolean("check_noslow", true);
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event,
                                   PacketWrapper wrapper) {
        boolean detectedSneakBurst = false;
        if (PacketEventSupport.isStartSneakingActionPacket(wrapper)) {
            long elapsedSneakTime = System.currentTimeMillis() - lastSneakPacketAt;
            if (enabled && elapsedSneakTime < 45L) {
                sneakPacketCount++;
                if (sneakPacketCount >= 3) {
                    flag("move.snk.pkt",
                            frequentSneakMessage(elapsedSneakTime), 30);
                    detectedSneakBurst = true;
                }
            } else {
                sneakPacketCount = 0;
            }
            lastSneakPacketAt = System.currentTimeMillis();
        }

        if (packetType == PacketType$Play$Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging =
                    (WrapperPlayClientPlayerDigging) wrapper;
            if (digging.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                long elapsedReleaseTime = System.currentTimeMillis() - lastReleaseUseItemAt;
                if (enabled && elapsedReleaseTime < 50L) {
                    ItemStack heldItem = player().getItemInHand();
                    if (heldItem.getType().name().endsWith("SWORD")
                            && playerState().clientVersion().isOlderThan(ClientVersion.V_1_21)) {
                        flag("move.blk.pkt", blockingPacketMessage(elapsedReleaseTime), 2);
                        correctNextMove = true;
                    }
                }
                lastReleaseUseItemAt = System.currentTimeMillis();
                consecutiveBlockUseCount++;
            }
        }
        return detectedSneakBurst;
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (correctNextMove) {
            correctNextMove = false;
            if (PlayerUtilities.hasPlayerDataOrEntityFlag(event.getPlayer())) {
                event.setTo(from);
            }
        }
    }

    private static String blockingPacketMessage(long elapsed) {
        return new StringBuilder("(blocking) send release use item packet frequently: ")
                .append(elapsed).toString();
    }

    public static void setStatus(boolean value) {
        status = value;
    }

    public static boolean status() {
        return status;
    }

    public static boolean isStatusDisabled() {
        return !status;
    }
}
