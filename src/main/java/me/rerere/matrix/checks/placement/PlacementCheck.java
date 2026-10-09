package me.rerere.matrix.checks.placement;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/** Placement and jump movement checks recovered from event-handler bytecode. */
public class PlacementCheck extends me.rerere.matrix.checks.CheckRuntimeState {
    private static boolean placementEnabled;
    private static boolean jumpCheckEnabled;
    public static boolean scale;
    private static int violationWeight;
    private static int windowFlag;

    static { windowFlag = 0; }
    private long recentJumpAt;
    private long recentPlacementAt;
    private int cancellationTicks;

    public PlacementCheck(me.rerere.matrix.player.PlayerCheckContext playerData) {
        super(playerData);
        this.cancellationTicks = 0;
        this.setCheckType(me.rerere.matrix.checks.CheckCategory.SCAFFOLD);
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (to.getY() > from.getY()) {
            double deltaY = to.getY() - from.getY();
            if (Math.abs(deltaY - 0.42D) <= 0.001D
                    && me.rerere.matrix.checks.movement.MovementMetrics.locationDistance(from, to) > 0.25D) {
                this.recentJumpAt = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
            }
        }
    }

    public void onBlockPlace(BlockPlaceEvent event) {

        if (cancellationTicks > 0) {
            cancellationTicks--;
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        if (placementEnabled && jumpCheckEnabled
                && event.getBlockPlaced().getY() == event.getBlockAgainst().getY()
                && event.getBlockAgainst().getRelative(BlockFace.DOWN).getType() == Material.AIR
                && event.getBlockPlaced().getRelative(BlockFace.DOWN).getType() == Material.AIR
                && player.getLocation().getY() > event.getBlockPlaced().getY() + 1.9D
                && player.getLocation().getY() >= 60.0D
                && event.getBlockPlaced().getType().isSolid()
                && me.rerere.matrix.world.BlockCollisionGeometry.isUnitCube(
                        me.rerere.matrix.world.BlockCollisionGeometry.getWorldShapes(
                                event.getBlockAgainst(), me.rerere.matrix.geometry.BlockShapeMode.INTERACT))) {
            if (scale) {
                long now = me.rerere.matrix.util.time.MillisecondSampler.currentTimeMillis();
                if (now - recentPlacementAt <= 550L
                        && now - recentJumpAt <= 350L
                        && me.rerere.matrix.checks.movement.MovementMetrics.locationDistance(
                                player.getLocation(), event.getBlockPlaced().getLocation().clone()
                                        .add(0.5D, 0.5D, 0.5D)) <= 0.6D) {
                    this.flag("sfd.place.u",
                            "scaffold-like placement | type U", violationWeight / 2);
                    event.setCancelled(true);
                    Location adjusted = player.getLocation().clone();
                    adjusted.setPitch(0.0F);
                    adjusted.setYaw(adjusted.getYaw() - 180.0F);
                    me.rerere.matrix.player.PlayerUtilities.teleportViaPlatform(player, adjusted);
                    cancellationTicks = 2;
                }
                recentPlacementAt = now;
            }
        }
    }

    public void reloadConfiguration() {
        placementEnabled = this.configuration().getBoolean("modules.place.enable", true);
        violationWeight = this.configuration().getInt("modules.place.vl_weight", 5);
        jumpCheckEnabled = this.configuration().getBoolean("modules.place.check_jump", true);
    }

    public static void setWindowFlag(int value) { windowFlag = value; }
    public static int windowFlag() { return windowFlag; }
    public static int computedWindow() { return windowFlag == 0 ? 40 : 0; }
}
