package me.rerere.matrix.checks.movement;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.runtime.EntityCategories;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.checks.movement.MovementMetrics;

/** Boat movement handler reconstructed from its own bytecode and parent callsites. */
public class BoatMovementCheck extends CheckRuntimeState {
    private long lastSuffocationMillis;
    private static final long SUFFOCATION_GRACE_MILLIS = 3000L;

    public BoatMovementCheck(PlayerCheckContext playerState) {
        super(playerState);
        setCheckType(CheckCategory.VEHICLE);
    }

    private static String boatPhaseText(double phase) {
        return new StringBuilder("boat phase: ").append(phase).toString();
    }

    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            lastSuffocationMillis = System.currentTimeMillis();
        }
    }

    private boolean isClearWaterLocation(Location location) {
        if (BlockEnvironment.isPredicateMaterialAtAndAbove(location)) return false;
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        if (feet.isLiquid() || feet.getRelative(BlockFace.DOWN).isLiquid() || head.isLiquid()) return false;
        for (BlockFace side : BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES) {
            if (MaterialPolicy.isAirMaterial(feet.getRelative(side).getType())) return false;
            if (MaterialPolicy.isAirMaterial(head.getRelative(side).getType())) return false;
            if (feet.getRelative(side).isLiquid() || head.getRelative(side).isLiquid()) return false;
        }
        return true;
    }

    public void onPlayerMove(PlayerMoveEvent event, Location ignoredFrom, Location ignoredTo) {
        Player player = event.getPlayer();
        if (player.isInsideVehicle() && player.getVehicle() != null
                && EntityCategories.isBoat(player.getVehicle().getType())) {
            // Keep the final getVehicle() evaluation from the original helper call.
            player.getVehicle();
            handleBoatMovement(player, event);
        }
    }

    private void handleBoatMovement(Player player, PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (System.currentTimeMillis() - lastSuffocationMillis < SUFFOCATION_GRACE_MILLIS
                && isClearWaterLocation(from) && isClearWaterLocation(to)) {
            Block fromBlock = from.getBlock();
            Block toBlock = to.getBlock();
            if (!fromBlock.equals(toBlock) && MovementMetrics.locationDistance(from, to) > 0.01d) {
                String phase = boatPhaseText(
                        MovementMetrics.locationDistance(from, to));
                this.flag("veh.bth", phase, 2);
                event.setTo(from);
                player.leaveVehicle();
            }
        }
    }
}
