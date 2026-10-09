package me.rerere.matrix.checks.network;

import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

/** Detects rapid changes to the visible client skin-section mask. */
public class ClientSkinSettingsCheck extends CheckRuntimeState {
    private static final long SAMPLE_WINDOW_MILLIS = 100L;
    private int previousMask;
    private long previousPacketAt;

    public ClientSkinSettingsCheck(PlayerCheckContext configuration) {
        super(configuration);
        setCheckType(CheckCategory.BADPACKETS);
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event, PacketWrapper ignored) {
        if (packetType != PacketType$Play$Client.CLIENT_SETTINGS) return false;
        byte mask = new WrapperPlayClientSettings(event).getVisibleSkinSectionMask();
        long elapsed = System.currentTimeMillis() - previousPacketAt;
        boolean reported = false;
        if (elapsed <= SAMPLE_WINDOW_MILLIS && mask != previousMask) {
            flag("packet.skin", "skin derp", 2);
            reported = true;
        }
        previousPacketAt = System.currentTimeMillis();
        previousMask = mask;
        return reported;
    }
}
