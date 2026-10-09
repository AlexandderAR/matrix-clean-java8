package me.rerere.matrix.checks.packet;

import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.util.BooleanDispatchBridge;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.BlockFace;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

public final class BadPacketCheck extends CheckRuntimeState {
    public static final BooleanDispatchBridge COMPANION = new BooleanDispatchBridge((DefaultConstructorMarker) null);
    private static boolean enabled;
    private static String storedMessage;
    private int previousSlot = -1;
    private boolean rollbackPending;

    public BadPacketCheck(PlayerCheckContext context) {
        super(context, CheckCategory.BADPACKETS);
    }

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public final boolean isRollbackPending() { return rollbackPending; }
    public final void setRollbackPending(boolean value) { rollbackPending = value; }
    public final int getPreviousSlot() { return previousSlot; }
    public final void setPreviousSlot(int value) { previousSlot = value; }
    public static void setStoredMessage(String value) { storedMessage = value; }
    public static String storedMessage() { return storedMessage; }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper ignoredWrapper) {
        if (!enabled) return false;
        if (type == PacketType$Play$Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);
            if (digging.getAction() == DiggingAction.RELEASE_USE_ITEM
                    && (digging.getBlockFace() != BlockFace.DOWN
                        || digging.getBlockPosition().getX() != 0
                        || digging.getBlockPosition().getY() != 0
                        || digging.getBlockPosition().getZ() != 0)) {
                flag("bp.badpacket.a", "send suspicious packets | type A", 2);
                event.setCancelled(true);
            }
        }
        if (type == PacketType$Play$Client.HELD_ITEM_CHANGE) {
            int selectedSlot = new WrapperPlayClientHeldItemChange(event).getSlot();
            if (selectedSlot == previousSlot) {
                flag("bp.badpacket.b", "send suspicious packets | type B", 2);
                event.setCancelled(true);
            }
            previousSlot = selectedSlot;
        }
        if (me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(type)) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (flying.hasRotationChanged()) {
                float pitch = flying.getLocation().getPitch();
                if (Math.abs(pitch) > 90.0f) {
                    flag("bp.badpacket.c", "send suspicious packets | type C", 10);
                    flying.getLocation().setPitch(90.0f);
                }
            }
        }
        return false;
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location ignoredTo) {
        if (rollbackPending) {
            rollbackPending = false;
            if (PlayerUtilities.hasPlayerDataOrEntityFlag(event.getPlayer())) event.setTo(from);
        }
    }
}
