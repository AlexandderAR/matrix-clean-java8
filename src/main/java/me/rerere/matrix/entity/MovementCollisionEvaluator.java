package me.rerere.matrix.entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.world.RecentBlockTracker;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.model.PreparedMovementData;
import me.rerere.matrix.checks.MutableGeometryState;
import me.rerere.matrix.world.CollisionGeometry;
import me.rerere.matrix.world.CollisionRegion;
import me.rerere.matrix.world.MaterialPolicy;

/** Evaluates collision and timeout checks for a movement transition. */
public final class MovementCollisionEvaluator {
    public static final MovementCollisionEvaluator INSTANCE = new MovementCollisionEvaluator();
    private MovementCollisionEvaluator() {
    }

    public static PreparedMovementData evaluate(
            Location from,
            Location to,
            Player player,
            PlayerCheckContext playerData) {
        World world = from.getWorld();
        Intrinsics.checkNotNull(world);
        CollisionRegion collision = CollisionGeometry.buildMovementEnvelope(
                player, world, from, to, playerData.clientVersion());
        boolean touchesSlime = collision.intersectsLargerMaterials(Material.SLIME_BLOCK);
        boolean matchesCollisionSet = collision.containsAnyLargerSetElement(MaterialPolicy.bedMaterials);

        MutableGeometryState movementState = playerData.movementContext();
        RecentBlockTracker timeout = movementState.timeout;
        boolean accepted = timeout.hasRecentBlockAt(from);
        playerData.movementContext().timeout.removeExpired();
        return new PreparedMovementData(accepted, touchesSlime, matchesCollisionSet, collision);
    }
}
