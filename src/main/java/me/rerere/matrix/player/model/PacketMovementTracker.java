package me.rerere.matrix.player.model;

import java.util.Locale;
import java.util.Set;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.network.IllegalPacketCheck;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;
import me.rerere.matrix.player.teleport.TeleportPosition;
import me.rerere.matrix.util.CollectionUtilities;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/** Tracks client movement packets and pending server teleports for one player. */
public class PacketMovementTracker {
    private final Set<TeleportPosition> pendingTeleports = CollectionUtilities.concurrentSet();
    private final PlayerCheckContext playerState;
    private int confirmedTeleportId = -1;
    private Location lastLocation;
    private boolean onGround;
    private long lastDiagnosticTime;
    private static final long TELEPORT_EXPIRY_MILLIS = 1500L;

    public PacketMovementTracker(PlayerCheckContext playerContext) { playerState = playerContext; }
    public boolean isOnGround() { return onGround; }
    @Nullable public Location getLastLocation() { return lastLocation; }
    public void setLastLocation(Location location) { lastLocation = location; }
    public long getLastDiagnosticTime() { return lastDiagnosticTime; }
    public PlayerCheckContext getPlayerState() { return playerState; }
    public int getConfirmedTeleportId() { return confirmedTeleportId; }
    public Set<TeleportPosition> getPendingTeleports() { return pendingTeleports; }
    public void addTeleport(Location location, int id) { pendingTeleports.add(new TeleportPosition(location.toVector(), id)); }
    public void addUnconfirmedTeleport(Location location) { pendingTeleports.add(new TeleportPosition(location.toVector(), -1)); }

    private void expireTeleports() {
        pendingTeleports.removeIf(teleport -> MillisecondSampler.currentTimeMillis()
                - teleport.teleportTime() >= TELEPORT_EXPIRY_MILLIS);
    }
    public void confirmTeleport(int id) {
        expireTeleports();
        if (pendingTeleports.stream().noneMatch(teleport -> teleport.teleportId() == id)) {
            confirmedTeleportId = 0;
            return;
        }
        confirmedTeleportId = id;
    }
    private boolean positionsMatch(Vector expectedPosition, Vector actualPosition) {
        return expectedPosition.distanceSquared(actualPosition) <= 0x1.5798ee2308c3ap-27;
    }
    public boolean consumeTeleport(Location location) {
        if (pendingTeleports.isEmpty()) { confirmedTeleportId = 0; return false; }
        expireTeleports();
        Vector receivedPosition = location.toVector();
        boolean found = false;
        if (confirmedTeleportId > 0) {
            TeleportPosition confirmed = pendingTeleports.stream()
                    .filter(teleport -> teleport.teleportId() == confirmedTeleportId)
                    .findFirst().orElse(null);
            confirmedTeleportId = 0;
            if (confirmed != null && positionsMatch(confirmed.position(), receivedPosition)) {
                confirmed.setAccepted(true);
                found = true;
            }
        }
        if (!found) {
            for (TeleportPosition teleport : pendingTeleports) {
                if (positionsMatch(teleport.position(), receivedPosition)) {
                    found = true;
                    teleport.setAccepted(true);
                    break;
                }
            }
        }
        pendingTeleports.removeIf(TeleportPosition::accepted);
        return found;
    }

    public void onMovement(PacketReceiveEvent event, Player player, PacketWrapper<?> wrapper) {
        PacketTypeCommon packetType = event.getPacketType();
        if (PacketEventSupport.isPlayerFlyingPacket(packetType)) {
            onGround = ((WrapperPlayClientPlayerPositionAndRotation) wrapper).isOnGround();
            return;
        }
        if (wrapper instanceof WrapperPlayClientPlayerPositionAndRotation) {
            WrapperPlayClientPlayerPositionAndRotation movement =
                    (WrapperPlayClientPlayerPositionAndRotation) wrapper;
            Vector3d position = movement.getPosition();
            if (!IllegalPacketCheck.isFiniteTriple(position.x, position.y, position.z)) event.setCancelled(true);
            float yaw = movement.getYaw();
            float pitch = movement.getPitch();
            if (!IllegalPacketCheck.hasFiniteAngles(yaw, pitch)) event.setCancelled(true);
        } else if (wrapper instanceof WrapperPlayClientVehicleMove) {
            WrapperPlayClientVehicleMove vehicle = (WrapperPlayClientVehicleMove) wrapper;
            Vector3d position = vehicle.getPosition();
            if (!IllegalPacketCheck.isFiniteTriple(position.x, position.y, position.z)) event.setCancelled(true);
        }
        Location destination = lastLocation != null ? lastLocation.clone() : new Location(player.getWorld(), -1, -1, -1, 0, 0);
        if (packetType == PacketType$Play$Client.PLAYER_POSITION || packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION
                || packetType == PacketType$Play$Client.VEHICLE_MOVE) {
            Vector3d position = PacketEventSupport.getMovementPosition(wrapper);
            destination.setX(position.x);
            destination.setY(position.y);
            destination.setZ(position.z);
            if (consumeTeleport(destination)) {
                lastLocation = destination.clone();
                return;
            }
        }
        if (packetType == PacketType$Play$Client.PLAYER_ROTATION || packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation movement = (WrapperPlayClientPlayerPositionAndRotation) wrapper;
            destination.setYaw(movement.getYaw());
            destination.setPitch(movement.getPitch());
        }
        boolean nextOnGround = true;
        if (packetType != PacketType$Play$Client.VEHICLE_MOVE) nextOnGround = ((WrapperPlayClientPlayerPositionAndRotation) wrapper).isOnGround();
        if (lastLocation != null) {
            PlayerMoveEvent movementEvent = new PlayerMoveEvent(player, lastLocation, destination);
            for (Object item : playerState.checks().checks()) {
                CheckRuntimeState check = (CheckRuntimeState) item;
                if (check.checkType().isEnabled()
                        && !playerState.shouldBypass(check.checkType())
                        && check.checkType().isEnabledIn(player.getWorld())) {
                    check.onPlayerMoveWithFlags(movementEvent, lastLocation, destination, onGround, nextOnGround);
                }
            }
        }
        if (DeveloperCommand.flyingTarget != null && player.getName().equals(DeveloperCommand.flyingTarget)) {
            long delay = MillisecondSampler.currentTimeMillis() - lastDiagnosticTime;
            for (Player observer : Bukkit.getOnlinePlayers()) {
                if (observer.isOp()) observer.sendMessage("Moving of " + player.getName() + " | delay: " + delay
                        + "/50 | type: " + packetType.getName().toLowerCase(Locale.ROOT));
            }
            lastDiagnosticTime = MillisecondSampler.currentTimeMillis();
        }
        lastLocation = destination.clone();
        onGround = nextOnGround;
    }
}
