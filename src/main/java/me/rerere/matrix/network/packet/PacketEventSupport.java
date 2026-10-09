package me.rerere.matrix.network.packet;

import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus$Action;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity$InteractAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import me.rerere.matrix.Matrix;

/** PacketEvents packet predicates and wrapper accessors recovered from direct ASM. */
public final class PacketEventSupport {
    @NotNull
    public static final PacketEventSupport INSTANCE = new PacketEventSupport();

    private PacketEventSupport() {}

    public static boolean isLegacyAchievementOpen(PacketReceiveEvent event) {
        return Matrix.Companion.serverVersion() == ServerVersion.V_1_8_8
                && event.getPacketType() == PacketType$Play$Client.CLIENT_STATUS
                && new WrapperPlayClientClientStatus(event).getAction()
                    == WrapperPlayClientClientStatus$Action.OPEN_INVENTORY_ACHIEVEMENT;
    }

    public static boolean isPlayerFlyingPacket(PacketTypeCommon packetType) {
        if (Matrix.Companion.serverVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            return packetType == PacketType$Play$Client.PLAYER_FLYING;
        }
        return packetType == PacketType$Play$Client.PLAYER_FLYING;
    }

    public static boolean isAttackEvent(PacketReceiveEvent event) {
        if (Matrix.Companion.serverVersion().isNewerThanOrEquals(ServerVersion.V_26_1)) {
            return event.getPacketType() == PacketType$Play$Client.ATTACK;
        }
        return event.getPacketType() == PacketType$Play$Client.INTERACT_ENTITY
                && new WrapperPlayClientInteractEntity(event).getAction()
                    == WrapperPlayClientInteractEntity$InteractAction.ATTACK;
    }

    public static int getAttackEntityId(PacketWrapper packet) {
        if (packet instanceof WrapperPlayClientAttack) {
            return ((WrapperPlayClientAttack) packet).getEntityId();
        }
        if (packet instanceof WrapperPlayClientInteractEntity) {
            return ((WrapperPlayClientInteractEntity) packet).getEntityId();
        }
        throw new IllegalStateException("Not an attack packet: " + packet.getClass().getSimpleName());
    }

    public static boolean isStartSneakingActionPacket(PacketWrapper packet) {
        ServerVersion version = Matrix.Companion.serverVersion();
        if (version.compareTo(ServerVersion.V_1_21_6) >= 0) {
            if (packet == null) return false;
            if (!(packet instanceof WrapperPlayClientPlayerInput)) return false;
            return ((WrapperPlayClientPlayerInput) packet).isShift();
        }
        if (packet == null) return false;
        if (!(packet instanceof WrapperPlayClientEntityAction)) return false;
        WrapperPlayClientEntityAction$Action action = ((WrapperPlayClientEntityAction) packet).getAction();
        return action == WrapperPlayClientEntityAction$Action.START_SNEAKING;
    }

    public static boolean isBlockPlacementPacket(PacketTypeCommon packetType) {
        return packetType == PacketType$Play$Client.PLAYER_BLOCK_PLACEMENT;
    }

    public static boolean isWindowConfirmationOrPongPacket(PacketTypeCommon packetType) {
        ServerVersion version = Matrix.Companion.serverVersion();
        return version.isOlderThan(ServerVersion.V_1_17)
                ? packetType == PacketType$Play$Client.WINDOW_CONFIRMATION
                : packetType == PacketType$Play$Client.PONG;
    }

    public static WrapperPlayClientEntityAction$Action getEntityAction(PacketWrapper packet) {
        if (!(packet instanceof WrapperPlayClientEntityAction)) {
            throw new IllegalStateException(unknownPacketMessage(packet.getClass().getSimpleName()));
        }
        return ((WrapperPlayClientEntityAction) packet).getAction();
    }

    private static String unknownPacketMessage(String packetName) {
        return "Unknown packet type: " + packetName;
    }

    public static void receivePacket(Player player, PacketWrapper packet) {
        try {
            PacketEvents.getAPI().getPlayerManager().receivePacket(player, packet);
        } catch (IllegalArgumentException closedChannel) {
            System.out.println((Object) "[Matrix] Trying to send data to the closed channel");
        }
    }

    public static boolean isPlayerRotationPacket(PacketTypeCommon packetType) {
        return packetType == PacketType$Play$Client.PLAYER_ROTATION || packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION;
    }

    public static Vector3d getMovementPosition(PacketWrapper packet) {
        if (packet instanceof WrapperPlayClientPlayerPositionAndRotation) {
            return ((WrapperPlayClientPlayerPositionAndRotation) packet).getPosition();
        }
        if (packet instanceof WrapperPlayClientVehicleMove) {
            return ((WrapperPlayClientVehicleMove) packet).getPosition();
        }
        throw new IllegalStateException(unknownPacketMessage(packet.getClass().getSimpleName()));
    }

    public static void writePacketIfPresent(Player player, PacketWrapper packet) {
        if (player == null) return;
        try {
            PacketEvents.getAPI().getPlayerManager().writePacket(player, packet);
        } catch (IllegalArgumentException closedChannel) {
            System.out.println((Object) "[Matrix] Trying to send data to the closed channel");
        }
    }

    public static int getHeldItemSlot(PacketWrapper packet) {
        if (packet instanceof WrapperPlayClientHeldItemChange) {
            return ((WrapperPlayClientHeldItemChange) packet).getSlot();
        }
        throw new IllegalStateException(unknownPacketMessage(packet.getClass().getSimpleName()));
    }

    public static boolean isPlayerFlying(PacketTypeCommon packetType) {
        return WrapperPlayClientPlayerFlying.isFlying(packetType);
    }

    public static boolean isAttackWrapper(PacketWrapper packet) {
        if (packet instanceof WrapperPlayClientAttack) return true;
        return packet instanceof WrapperPlayClientInteractEntity
                && ((WrapperPlayClientInteractEntity) packet).getAction()
                    == WrapperPlayClientInteractEntity$InteractAction.ATTACK;
    }
}
