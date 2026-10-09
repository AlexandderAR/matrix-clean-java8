package me.rerere.matrix.checks.movement;

import java.util.List;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.checks.combat.AttackReplaySnapshot;
import me.rerere.matrix.replay.PacketStateSnapshot;

/** Default packet and movement hook methods. */
public interface PacketMovementHooks {
    default boolean onPacketSend(PacketTypeCommon type, PacketSendEvent event, PacketWrapper<?> wrapper) {
        return false;
    }

    default void applyStateUpdate(int value, AttackReplaySnapshot state) {
        PacketStateSnapshot update = state.getPlayerState();
        recordEntityState(value, update);
    }

    default boolean checkBlockInteraction(int entityId, PacketStateSnapshot update) {
        return decodeFlag(entityId);
    }

    default void onCheckPulse(int entityId) { }

    default void onPacketRotation(PacketTypeCommon type, float yaw, float pitch) { }

    default boolean canHandleEntityState(int entityId, AttackReplaySnapshot state) {
        return checkBlockInteraction(entityId, state.getPlayerState());
    }

    default boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper<?> wrapper) {
        return false;
    }

    default void recordEntityState(int entityId, PacketStateSnapshot update) {
        onCheckPulse(entityId);
    }

    default void onEntityAction(Entity entity, List<?> values) { }

    default boolean checkTrackedEntity(int value, AttackReplaySnapshot state) { return true; }

    default void onPlayerMoveWithFlags(PlayerMoveEvent event, Location from, Location to, boolean first, boolean second) { }

    default boolean decodeFlag(int entityId) {
        return true;
    }
}
