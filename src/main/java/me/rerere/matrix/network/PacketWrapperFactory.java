package me.rerere.matrix.network;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPunch;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;

/** Selects packet wrappers for the packet types consumed by Matrix. */
public final class PacketWrapperFactory {
    public static final PacketWrapperFactory INSTANCE = new PacketWrapperFactory();

    private PacketWrapperFactory() {
    }

    public static PacketWrapper wrapClientReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType$Play$Client.PLAYER_FLYING
                || packetType == PacketType$Play$Client.PLAYER_POSITION
                || packetType == PacketType$Play$Client.PLAYER_ROTATION
                || packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION) {
            return new WrapperPlayClientPlayerPositionAndRotation(event);
        }
        if (packetType == PacketType$Play$Client.VEHICLE_MOVE) {
            return new WrapperPlayClientVehicleMove(event);
        }
        if (packetType == PacketType$Play$Client.ANIMATION) {
            return new WrapperPlayClientAnimation(event);
        }
        if (packetType == PacketType$Play$Client.PUNCH) {
            return new WrapperPlayClientPunch(event);
        }
        if (packetType == PacketType$Play$Client.INTERACT_ENTITY) {
            return new WrapperPlayClientInteractEntity(event);
        }
        if (packetType == PacketType$Play$Client.ATTACK) {
            return new WrapperPlayClientAttack(event);
        }
        if (packetType == PacketType$Play$Client.HELD_ITEM_CHANGE) {
            return new WrapperPlayClientHeldItemChange(event);
        }
        if (packetType == PacketType$Play$Client.ENTITY_ACTION) {
            return new WrapperPlayClientEntityAction(event);
        }
        if (packetType == PacketType$Play$Client.WINDOW_CONFIRMATION) {
            return new WrapperPlayClientWindowConfirmation(event);
        }
        if (packetType == PacketType$Play$Client.CLICK_WINDOW) {
            return new WrapperPlayClientClickWindow(event);
        }
        if (packetType == PacketType$Play$Client.PONG) {
            return new WrapperPlayClientPong(event);
        }
        if (packetType == PacketType$Play$Client.PLAYER_BLOCK_PLACEMENT) {
            return new WrapperPlayClientPlayerBlockPlacement(event);
        }
        if (packetType == PacketType$Play$Client.PLAYER_DIGGING) {
            return new WrapperPlayClientPlayerDigging(event);
        }
        if (packetType == PacketType$Play$Client.USE_ITEM) {
            return new WrapperPlayClientUseItem(event);
        }
        if (packetType == PacketType$Play$Client.TELEPORT_CONFIRM) {
            return new WrapperPlayClientTeleportConfirm(event);
        }
        if (packetType == PacketType$Play$Client.PLAYER_INPUT) {
            return new WrapperPlayClientPlayerInput(event);
        }
        return null;
    }

    public static PacketWrapper wrapServerSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType$Play$Server.ENTITY_VELOCITY) {
            return new WrapperPlayServerEntityVelocity(event);
        }
        if (packetType == PacketType$Play$Server.EXPLOSION) {
            return new WrapperPlayServerExplosion(event);
        }
        if (packetType == PacketType$Play$Server.PLAYER_POSITION_AND_LOOK) {
            return new WrapperPlayServerPlayerPositionAndLook(event);
        }
        return null;
    }
}
