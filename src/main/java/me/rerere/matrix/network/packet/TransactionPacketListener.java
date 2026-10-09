package me.rerere.matrix.network.packet;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerAbstract;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.player.PlayerCheckContext;

public final class TransactionPacketListener extends PacketListenerAbstract {
    @Override public void onPacketReceive(PacketReceiveEvent event) {
        try {
            PacketTypeCommon packetType = event.getPacketType();
            if (packetType == PacketType$Play$Client.WINDOW_CONFIRMATION) {
                WrapperPlayClientWindowConfirmation packet =
                        new WrapperPlayClientWindowConfirmation(event);
                PlayerCheckContext player = Matrix.Companion.matrixInstance()
                        .getPlayerDataManager().find(event.getUser().getUUID());
                if (player == null) return;
                int transactionId = packet.getActionId();
                if (transactionId < 0) {
                    player.transactionQueue().receiveTransaction(transactionId);
                }
            } else if (packetType == PacketType$Play$Client.PONG) {
                WrapperPlayClientPong packet = new WrapperPlayClientPong(event);
                PlayerCheckContext player = Matrix.Companion.matrixInstance()
                        .getPlayerDataManager().find(event.getUser().getUUID());
                if (player == null) return;
                int transactionId = packet.getId();
                if (transactionId < 0) {
                    player.transactionQueue().receiveTransaction(transactionId);
                }
            }
        } catch (LicenseResponseException failure) {
            throw failure;
        }
    }

    @Override public void onPacketSend(PacketSendEvent event) {
        try {
            PacketTypeCommon packetType = event.getPacketType();
            if (packetType == PacketType$Play$Server.WINDOW_CONFIRMATION) {
                WrapperPlayServerWindowConfirmation packet =
                        new WrapperPlayServerWindowConfirmation(event);
                if (packet.getActionId() < 0) {
                    PlayerCheckContext player = Matrix.Companion.matrixInstance()
                            .getPlayerDataManager().find(event.getUser().getUUID());
                    if (player == null) return;
                    player.transactionQueue().onQueuedPing((int) packet.getActionId());
                }
            } else if (packetType == PacketType$Play$Server.PING) {
                WrapperPlayServerPing packet = new WrapperPlayServerPing(event);
                if (packet.getId() < 0 && isShortId(packet.getId())) {
                    PlayerCheckContext player = Matrix.Companion.matrixInstance()
                            .getPlayerDataManager().find(event.getUser().getUUID());
                    if (player == null) return;
                    player.transactionQueue().onQueuedPing(packet.getId());
                }
            }
        } catch (LicenseResponseException failure) {
            throw failure;
        }
    }
    private static boolean isShortId(int id) {
        try {
            return (short) id == id;
        } catch (LicenseResponseException failure) {
            throw failure;
        }
    }
}
