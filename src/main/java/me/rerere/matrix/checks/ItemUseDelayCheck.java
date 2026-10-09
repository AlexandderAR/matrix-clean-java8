package me.rerere.matrix.checks;

import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.CheckSetting;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.StringLinkageSlot;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.time.Duration;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class ItemUseDelayCheck extends CheckRuntimeState {
    private int rapidMovementPackets;
    private static int usingItemsPenalty;
    private static boolean cancelAction;
    public static final StringLinkageSlot COMPANION = new StringLinkageSlot(null);
    private long lastMovementPacketTime;
    private long lastRapidMovementTime;
    private long lastItemInteractionTime;

    public ItemUseDelayCheck(PlayerCheckContext playerContext) {
        super(playerContext, CheckCategory.DELAY);
    }
    public void onItemConsume(PlayerItemConsumeEvent event) {
        int expectedTicks = Matrix.Companion.matrixInstance().getNmsManager().visitor()
                .getItemUseDuration(event.getItem(), event.getPlayer());
        double elapsedFraction = (double) (MillisecondSampler.currentTimeMillis() - lastItemInteractionTime)
                / ((double) expectedTicks * 50.0);
        int fasterPercent = (int) ((1.0 - elapsedFraction) * 100.0);
        if (usingItemsPenalty > 0 && elapsedFraction <= 0.82 && rapidMovementPackets >= 20
                && MillisecondSampler.currentTimeMillis() - lastRapidMovementTime <= 75L) {
            long playerDelay = playerState().smoothedLatency();
            if (KotlinTimeAccess.compareDurations(playerDelay, DurationKt.toDuration(160, DurationUnit.MILLISECONDS)) < 0) {
                flag("delay.use", fasterMessage(fasterPercent), usingItemsPenalty);
                if (cancelAction) {
                    event.setCancelled(true);
                    PlayerUtilities.resyncInventory(event.getPlayer());
                }
            }
        }
    }
    @Override
    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper<?> wrapper) {
        return onPacket(type, event, wrapper);
    }

    public boolean onPacket(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (PacketEventSupport.isPlayerFlying(type)) {
            long now = MillisecondSampler.currentTimeMillis();
            long elapsed = now - lastMovementPacketTime;
            lastMovementPacketTime = now;
            if (elapsed <= 40L) {
                rapidMovementPackets++;
                lastRapidMovementTime = now;
            } else rapidMovementPackets = 0;
        }
        if (PacketEventSupport.isBlockPlacementPacket(type)) {
            lastItemInteractionTime = MillisecondSampler.currentTimeMillis();
        }
        return false;
    }
    public void reloadConfiguration() {
        CheckSetting settings = checkType().getConfiguration();
        Intrinsics.checkNotNull(settings);
        cancelAction = settings.getConfigurationSection().getBoolean("cancel_action", true);
        usingItemsPenalty = configuration().getInt("check_using_items", 1);
    }

    private static String fasterMessage(int fasterPercent) {
        return "perform actions faster than expected // using_item ("
                + fasterPercent + "% faster than expected)";
    }
}
