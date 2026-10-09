package me.rerere.matrix.checks.packet;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;

import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;

/** Detects repeated legacy signing channels and selected oversized chat commands. */
public final class PacketPayloadGuard extends CheckRuntimeState {
    private long lastLegacySigningPacketTime;
    private static final int PACKET_MESSAGE_CODE = 10;
    private static final long LEGACY_PACKET_WINDOW_MILLIS = 100L;

    public PacketPayloadGuard(PlayerCheckContext configuration) {
        super(configuration);
        setCheckType(CheckCategory.BADPACKETS);
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event,
                                   PacketWrapper wrapper) {
        boolean violationDetected = false;
        if (packetType == PacketType$Play$Client.PLUGIN_MESSAGE
                && Matrix.getServerVersion()
                        .isOlderThan(ServerVersion.V_1_13)) {
            String channelName = new WrapperPlayClientPluginMessage(event).getChannelName();
            if (channelName.equalsIgnoreCase("MC|BSign") || channelName.equalsIgnoreCase("MC|BEdit")) {
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - lastLegacySigningPacketTime < LEGACY_PACKET_WINDOW_MILLIS) {
                    violationDetected = true;
                }
                lastLegacySigningPacketTime = System.currentTimeMillis();
            }
        }
        if (packetType == PacketType$Play$Client.CHAT_MESSAGE) {
            String chatMessage = new WrapperPlayClientChatMessage(event).getMessage();
            if ((chatMessage.startsWith("//calc ") || chatMessage.startsWith("/to "))
                    && chatMessage.contains("for(")) {
                violationDetected = true;
                flag("packet.crash", "world edit crash", PACKET_MESSAGE_CODE);
            }
        }
        return violationDetected;
    }

}
