package me.rerere.matrix.checks.movement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.license.BlockPlacePostLicenseState;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.reflect.KClass;

/** Tracks position displacement and packet ordering around movement updates. */
public final class MovementPacketSequenceCheck extends CheckRuntimeState {
    private double previousX;
    private double previousY;
    private double previousZ;
    private boolean movementSequenceOpen;
    private static boolean enabled;
    private int consecutiveMovementTicks;
    private final ArrayList<PacketTypeCommon> pendingPacketTypes;
    public static final BlockPlacePostLicenseState weight;
    private final ArrayList<PacketTypeCommon> bufferedPacketTypes;
    static {
        weight = new BlockPlacePostLicenseState(
                (DefaultConstructorMarker) null);
    }

    public MovementPacketSequenceCheck(PlayerCheckContext user) {
        super(user, CheckCategory.BADPACKETS);
        pendingPacketTypes = new ArrayList<>();
        bufferedPacketTypes = new ArrayList<>();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    private void updatePosition(WrapperPlayClientPlayerPositionAndRotation packet) {

        double currentX = packet.hasPositionChanged()
                ? packet.getPosition().x : previousX;
        double currentY = packet.hasPositionChanged()
                ? packet.getPosition().y : previousY;
        double currentZ = packet.hasPositionChanged()
                ? packet.getPosition().z : previousZ;
        double deltaX = Math.abs(currentX - previousX);
        double deltaY = Math.abs(currentY - previousY);
        double deltaZ = Math.abs(currentZ - previousZ);
        previousX = currentX;
        previousY = currentY;
        previousZ = currentZ;

        double squaredDistance = Math.pow(deltaX, 2.0D)
                + Math.pow(deltaY, 2.0D) + Math.pow(deltaZ, 2.0D);
        boolean countsAsMovement = squaredDistance > 0.03D
                || playerState().clientVersion().isOlderThanOrEquals(ClientVersion.V_1_8);
        if (countsAsMovement) {
            consecutiveMovementTicks++;
        } else {
            consecutiveMovementTicks = 0;
        }

        if (!bufferedPacketTypes.isEmpty() && consecutiveMovementTicks >= 2) {
            String packetNames = bufferedPacketTypes.stream()
                    .map(MovementPacketSequenceCheck::packetTypeName)
                    .collect(Collectors.joining(", "));
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("t", packetNames);
            metadata.put("r", String.valueOf(consecutiveMovementTicks));
            flag("bp.post", "post", metadata,
                    bufferedPacketTypes.size() * 10);
        }
        bufferedPacketTypes.clear();
    }

    private static CharSequence packetTypeName(PacketTypeCommon packetType) {
        return (CharSequence) packetType.getName();
    }

    private static String castFailureMessage(Class<?> actualClass, Object expectedType) {
        KClass<?> expectedClass = (KClass<?>) expectedType;
        return "Cannot cast " + actualClass + " to " + expectedClass;
    }

    @Override
    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event,
                     PacketWrapper wrapper) {
        if (!enabled) return false;
        if (PacketEventSupport.isPlayerFlying(packetType)) {
            movementSequenceOpen = true;
            pendingPacketTypes.clear();
            if (wrapper == null) return false;
            if (!(wrapper instanceof WrapperPlayClientPlayerPositionAndRotation)) {
                throw new ClassCastException(castFailureMessage(
                        wrapper.getClass(), WrapperPlayClientPlayerPositionAndRotation.class));
            }
            updatePosition((WrapperPlayClientPlayerPositionAndRotation) wrapper);
            return false;
        }

        if (PacketEventSupport.isWindowConfirmationOrPongPacket(packetType)) {
            bufferedPacketTypes.addAll(pendingPacketTypes);
            pendingPacketTypes.clear();
            movementSequenceOpen = false;
            return false;
        }

        if (!movementSequenceOpen || wrapper == null) return false;
        PacketTypeCommon receivedType = event.getPacketType();
        if (receivedType == PacketType$Play$Client.CLICK_WINDOW) {
            ClientVersion version = playerState().clientVersion();
            if (version.isOlderThan(ClientVersion.V_1_13)) {
                pendingPacketTypes.add(PacketType$Play$Client.CLICK_WINDOW);
            }
            return false;
        }

        if (receivedType == PacketType$Play$Client.ENTITY_ACTION) {
            if (playerState().isInVehicle()) {
                return false;
            }
            if (!(wrapper instanceof WrapperPlayClientEntityAction)) {
                throw new ClassCastException(castFailureMessage(
                        wrapper.getClass(), WrapperPlayClientEntityAction.class));
            }
            WrapperPlayClientEntityAction actionPacket =
                    (WrapperPlayClientEntityAction) wrapper;
            if (actionPacket.getAction()
                    != WrapperPlayClientEntityAction$Action.START_FLYING_WITH_ELYTRA) {
                pendingPacketTypes.add(PacketType$Play$Client.ENTITY_ACTION);
            }
            return false;
        }

        if (receivedType == PacketType$Play$Client.PLAYER_ABILITIES
                || receivedType == PacketType$Play$Client.INTERACT_ENTITY
                || receivedType == PacketType$Play$Client.PLAYER_BLOCK_PLACEMENT
                || receivedType == PacketType$Play$Client.USE_ITEM
                || receivedType == PacketType$Play$Client.PLAYER_DIGGING) {
            pendingPacketTypes.add(event.getPacketType());
        }
        return false;
    }
}
