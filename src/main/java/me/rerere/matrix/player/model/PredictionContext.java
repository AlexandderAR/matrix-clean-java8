package me.rerere.matrix.player.model;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class PredictionContext {
    private final PlayerCheckContext player;
    private final Vector movement;
    private final Vector velocity;
    private final Location from;
    private final Location to;
    private final float yaw;

    public PredictionContext(PlayerCheckContext player, Vector movement, Vector velocity,
                              Location from, Location to, float yaw) {
        this.player = player;
        this.movement = movement;
        this.velocity = velocity;
        this.from = from;
        this.to = to;
        this.yaw = yaw;
    }

    public PlayerCheckContext getPlayer() { return player; }
    public Vector getMovement() { return movement; }
    public Vector getVelocity() { return velocity; }
    public Location getFrom() { return from; }
    public Location getTo() { return to; }
    public float getYaw() { return yaw; }

    public PlayerCheckContext component1() { return player; }
    public Vector component2() { return movement; }
    public Vector component3() { return velocity; }
    public Location component4() { return from; }
    public Location component5() { return to; }
    public float component6() { return yaw; }

    public PredictionContext copy(PlayerCheckContext player, Vector movement, Vector velocity,
                                   Location from, Location to, float yaw) {
        return new PredictionContext(player, movement, velocity, from, to, yaw);
    }

    @Override
    public String toString() {
        return new StringBuilder("PredictionContext(player=").append(player).append(", movement=")
                .append(movement).append(", velocity=").append(velocity).append(", from=")
                .append(from).append(", to=").append(to).append(", yaw=").append(yaw).append(")").toString();
    }

    @Override
    public int hashCode() {
        int hash = player.hashCode();
        hash = hash * 31 + movement.hashCode();
        hash = hash * 31 + velocity.hashCode();
        hash = hash * 31 + from.hashCode();
        hash = hash * 31 + to.hashCode();
        return hash * 31 + Float.hashCode(yaw);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) { return true; }
        if (!(object instanceof PredictionContext)) { return false; }
        PredictionContext other = (PredictionContext) object;
        if (!Intrinsics.areEqual(player, other.player)) { return false; }
        if (!Intrinsics.areEqual(movement, other.movement)) { return false; }
        if (!Intrinsics.areEqual(velocity, other.velocity)) { return false; }
        if (!Intrinsics.areEqual(from, other.from)) { return false; }
        if (!Intrinsics.areEqual(to, other.to)) { return false; }
        return Float.compare(yaw, other.yaw) == 0;
    }
}
