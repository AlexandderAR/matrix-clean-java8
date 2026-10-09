package me.rerere.matrix.checks.packet;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.metrics.RollingStatistics;

/** Tracks rapid START_DIGGING packets and reports sustained block breaking. */
public class BlockNukerCheck extends CheckRuntimeState {
    private long lastDiggingPacketTime;
    private int recentDiggingPackets;

    public BlockNukerCheck(PlayerCheckContext playerData) {
        super(playerData, CheckCategory.BLOCK);
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event,
                                   PacketWrapper wrapper) {
        if (packetType != PacketType$Play$Client.PLAYER_DIGGING) {
            return false;
        }
        WrapperPlayClientPlayerDigging diggingPacket = (WrapperPlayClientPlayerDigging) wrapper;
        if (diggingPacket.getAction() != DiggingAction.START_DIGGING) {
            return false;
        }
        return recordDiggingPacket();
    }

    private boolean recordDiggingPacket() {
        long elapsed = MillisecondSampler.currentTimeMillis() - lastDiggingPacketTime;
        lastDiggingPacketTime = MillisecondSampler.currentTimeMillis();

        if (elapsed < 10L) {
            recentDiggingPackets++;
        } else {
            recentDiggingPackets = Math.max(recentDiggingPackets - 5, 0);
        }

        if (recentDiggingPackets >= 20 && RollingStatistics.metric() > 18.0) {
            super.flag("blk.nuker", violationMessage(recentDiggingPackets), 0);
        }
        return recentDiggingPackets >= 13;
    }

    private String violationMessage(int recentPacketCount) {
        return "send block breaking packets too frequently(vl: " + recentPacketCount + ")";
    }

    public void onTick() {
        if (recentDiggingPackets > 0) {
            recentDiggingPackets--;
        }
    }
}
