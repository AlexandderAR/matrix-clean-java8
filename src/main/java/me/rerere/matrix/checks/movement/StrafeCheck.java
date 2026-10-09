package me.rerere.matrix.checks.movement;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.BlockGeometry;
import me.rerere.matrix.world.MaterialPolicy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/** Detects repeated horizontal acceleration while the player is in combat grace range. */
public final class StrafeCheck extends CheckRuntimeState {
    private static boolean enabled;
    private static boolean cancelMovement;
    private Vector previousHorizontalMotion;
    private static int violationWeight;
    private int graceTicks;
    private int repeatedStrafeTicks;

    public StrafeCheck(PlayerCheckContext manager) {
        super(manager);
        graceTicks = 0;
        repeatedStrafeTicks = 0;
        setCheckType(CheckCategory.KILLAURA);
    }

    public void reloadConfiguration() {
        ConfigurationSection config = configuration();
        enabled = config.getBoolean("modules.strafe.enable", true);
        violationWeight = config.getInt("modules.strafe.vl_weight", 3);
        cancelMovement = config.getBoolean("modules.strafe.cancel_move", true);
    }

    public void onAttackWithContext(EntityDamageByEntityEvent event) {
        if (!enabled) return;
        if (graceTicks == 0) previousHorizontalMotion = null;
        graceTicks = 15;
    }

    private boolean hasOtherEntity(Player player, Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1.25, 2.0, 1.25)) {
            if (entity.getEntityId() != player.getEntityId()) return true;
        }
        return false;
    }

    public void onTick() {
        if (graceTicks > 0) {
            graceTicks--;
            if (graceTicks == 0) previousHorizontalMotion = null;
        }
    }

    private static String formatStrafeViolation(String speed) {
        return "bad strafe movement during combat // strafe (" + speed + ")";
    }

    private boolean isCollisionFree(Location location, double verticalOffset) {
        AxisAlignedBox playerBox = new AxisAlignedBox(
                location.toVector(), verticalOffset + 0x1.99999ap-2, 0x1.cf5c28p0);
        World world = location.getWorld();
        int blockX = (int) playerBox.minX() - 2;
        while ((double) blockX <= playerBox.maxX() + 1.0d) {
            int blockY = (int) playerBox.minY() - 2;
            while ((double) blockY <= playerBox.maxY() + 1.0d) {
                int blockZ = (int) playerBox.minZ() - 2;
                while ((double) blockZ <= playerBox.maxZ() + 1.0d) {
                    Block block = world.getBlockAt(blockX, blockY, blockZ);
                    if (!MaterialPolicy.isAirMaterial(block.getType())) {
                        AxisAlignedBox[] shapes = BlockCollisionGeometry.getWorldShapes(
                                block, BlockShapeMode.COLLISION);
                        for (AxisAlignedBox shape : shapes) {
                            if (shape.intersects(playerBox)) return false;
                        }
                    }
                    blockZ++;
                }
                blockY++;
            }
            blockX++;
        }
        return true;
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (graceTicks <= 0) return;
        Player player = event.getPlayer();
        if (player.isFlying()) return;
        if (Matrix.getServerVersion()
                .isNewerThan(ServerVersion.V_1_8_8) && player.isGliding()) return;

        World world = from.getWorld();
        float friction = Matrix.getInstance().getNmsManager()
                .visitor()
                .getBlockFriction(world.getBlockAt(
                        MovementMetrics.toInt(from.getX()),
                        MovementMetrics.toInt(from.getY()) - 1,
                        MovementMetrics.toInt(from.getZ())));
        float motionScale = 0x1.d1eb86p-1f;
        if (PlayerUtilities.hasPlayerDataOrEntityFlag(player)) motionScale = friction * 0x1.d1eb86p-1f;

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        if (previousHorizontalMotion == null) {
            previousHorizontalMotion = new Vector(deltaX * motionScale, 0.0d, deltaZ * motionScale);
            return;
        }

        Vector acceleration = new Vector(
                deltaX - previousHorizontalMotion.getX(),
                0.0d,
                deltaZ - previousHorizontalMotion.getZ());
        if (PlayerUtilities.hasPlayerDataOrEntityFlag(player)
                && to.getY() == from.getY()
                && acceleration.length() >= 0.1d
                && acceleration.dot(previousHorizontalMotion) > 0.0d
                && to.getY() % 1.0d == 0.0d) {
            double turn = Math.toDegrees(acceleration.angle(to.getDirection().setY(0))) % 45.0d;
            if (turn > 5.0d && Math.abs(turn - 45.0d) > 5.0d
                    && BlockEnvironment.isPredicateMaterialAtAndAbove(from)
                    && !hasOtherEntity(player, from)
                    && isCollisionFree(from, 0.1d)) {
                repeatedStrafeTicks++;
                if (repeatedStrafeTicks >= 10) {
                    String details = "ka.strafe";
                    String violation = formatStrafeViolation(
                            DiagnosticFormat.formatDouble(acceleration.length()));
                    flag(details, violation, violationWeight);
                    repeatedStrafeTicks = 10;
                    if (cancelMovement && BlockGeometry.collides(from)) {
                        event.setTo(from);
                    }
                }
            } else {
                repeatedStrafeTicks = Math.max(0, repeatedStrafeTicks - 2);
            }
        }
        previousHorizontalMotion = new Vector(deltaX * motionScale, 0.0d, deltaZ * motionScale);
    }
}
