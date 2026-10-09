package me.rerere.matrix.checks.block;

import java.util.List;
import java.util.Map;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.network.packet.PacketRegistration;
import me.rerere.matrix.network.packet.PacketWrapperSnapshot;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.CollectionsSupport;
import org.bukkit.event.block.BlockBreakEvent;

public final class FastBreakCheck extends CheckRuntimeState {
    private static boolean enabled;
    private boolean suspiciousDelay;
    private final PacketRegistration recentPackets;
    private static final long minimumPacketIntervalMillis;

    static {
        minimumPacketIntervalMillis = 10L;
    }

    public FastBreakCheck(PlayerCheckContext context) {
        super(context);
        setCheckType(CheckCategory.BLOCK);
        recentPackets = new PacketRegistration(4);
    }

    public void reloadConfiguration() {
        enabled = configuration().getBoolean("modules.fastbreak.enable", true);
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (PacketEventSupport.isPlayerFlying(type) || type == PacketType$Play$Client.PLAYER_DIGGING) {
            if (wrapper == null) throw new IllegalStateException("Required value was null.");
            if (wrapper instanceof WrapperPlayClientPlayerDigging
                    && ((WrapperPlayClientPlayerDigging) wrapper).getAction() == DiggingAction.CHANGE_DESTROY_DIRECTION) {
                return false;
            }
            PacketRegistration packetHistory = recentPackets;
            if (enabled) {
                packetHistory.registry().add(new PacketWrapperSnapshot(wrapper, System.currentTimeMillis()));
                if (packetHistory.registry().isAtCapacity()) {
                    List<?> recentPacketSnapshots = CollectionsKt.toList((Iterable<?>) packetHistory.registry());
                    int lastIndex = recentPacketSnapshots.size() - 1;
                    int packetIndex = 1;
                    // A rapid START_DIGGING followed by a finish can be canceled at block-break time.
                    if (packetIndex <= lastIndex) {
                        while (true) {
                            if (((PacketWrapperSnapshot) recentPacketSnapshots.get(packetIndex)).getTimestamp()
                                    - ((PacketWrapperSnapshot) recentPacketSnapshots.get(packetIndex - 1)).getTimestamp() < minimumPacketIntervalMillis
                                    && ((PacketWrapperSnapshot) recentPacketSnapshots.get(packetIndex)).getPacketWrapper()
                                            instanceof WrapperPlayClientPlayerDigging) {
                                PacketWrapper currentPacket = ((PacketWrapperSnapshot) recentPacketSnapshots.get(packetIndex)).getPacketWrapper();
                                DiggingAction currentAction = currentPacket instanceof WrapperPlayClientPlayerDigging
                                        ? ((WrapperPlayClientPlayerDigging) currentPacket).getAction() : null;
                                boolean startsOrFinishes = currentAction == DiggingAction.START_DIGGING;
                                if (!startsOrFinishes) {
                                    currentPacket = ((PacketWrapperSnapshot) recentPacketSnapshots.get(packetIndex)).getPacketWrapper();
                                    DiggingAction currentDiggingAction = currentPacket instanceof WrapperPlayClientPlayerDigging
                                            ? ((WrapperPlayClientPlayerDigging) currentPacket).getAction() : null;
                                    startsOrFinishes = currentDiggingAction == DiggingAction.FINISHED_DIGGING;
                                }
                                if (startsOrFinishes
                                        && ((PacketWrapperSnapshot) recentPacketSnapshots.get(packetIndex - 1)).getPacketWrapper()
                                                instanceof WrapperPlayClientPlayerDigging) {
                                    PacketWrapper previousPacket = ((PacketWrapperSnapshot) recentPacketSnapshots.get(packetIndex - 1)).getPacketWrapper();
                                    DiggingAction previousAction = previousPacket instanceof WrapperPlayClientPlayerDigging
                                            ? ((WrapperPlayClientPlayerDigging) previousPacket).getAction() : null;
                                    if (previousAction == DiggingAction.START_DIGGING) suspiciousDelay = true;
                                }
                            }
                            if (packetIndex == lastIndex) break;
                            packetIndex++;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void onBlockBreak(BlockBreakEvent event) {
        if (suspiciousDelay) {
            suspiciousDelay = false;
            if (event.getBlock().getType().isSolid()
                    && flag("blk.delay", "break block with incorrect delay (type A)",
                            (Map<String, Object>) (Map<?, ?>) CollectionsSupport.linkedMapOf(), 0)) {
                event.setCancelled(true);
            }
        }
    }
}
