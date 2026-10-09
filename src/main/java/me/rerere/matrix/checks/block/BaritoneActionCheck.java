package me.rerere.matrix.checks.block;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.MaterialPolicy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Pose;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BaritoneActionCheck extends CheckRuntimeState {
    private static final Material FARMLAND;
    public static boolean enabled;
    private static boolean compatibilityFlag;
    private long lastTeleportMillis;
    private long lastMineReportTime;
    private int rotationChanges;
    private long lastTinyPitchMovementTime;
    private float lastPlacementYaw;
    private float lastPlacementPitch;

    static {
        setCompatibilityFlag(true);
        FARMLAND = MaterialPolicy.materialByVersionNames("SOIL", "FARMLAND");
    }

    public BaritoneActionCheck(PlayerCheckContext context) {
        super(context);
        setCheckType(CheckCategory.fromSettingName("AUTOBOT"));
    }

    public static void setCompatibilityFlag(boolean value) {
        compatibilityFlag = value;
    }

    public static boolean compatibilityFlag() {
        return compatibilityFlag;
    }

    public static boolean isCompatibilityDisabled() {
        try {
            return !compatibilityFlag();
        } catch (LicenseResponseException failure) {
            throw failure;
        }
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (to.getPitch() != from.getPitch()) {
            float pitchDelta = Math.abs(to.getPitch() - from.getPitch());
            if (pitchDelta > 0.0f && (double) pitchDelta < 0.001) {
                double displacement = MovementMetrics.locationDistance(from, to);
                if (displacement > 0.0 && displacement < 0.1
                        && MillisecondSampler.currentTimeMillis() - lastTeleportMillis > 1000L) {
                    float yawDelta = Math.abs(to.getYaw() - from.getYaw());
                    if ((double) yawDelta <= 0.01)
                        lastTinyPitchMovementTime = MillisecondSampler.currentTimeMillis();
                }
            }
        }
        if (to.getYaw() != from.getYaw()
                && (double) GeometryMath.angularDistance(from.getYaw(), to.getYaw()) > 0.01) {
            rotationChanges++;
            if (rotationChanges > 10) rotationChanges = 10;
        }
    }

    public void onBlockBreak(BlockBreakEvent event) {
        if (System.currentTimeMillis() - lastTinyPitchMovementTime < 1000L
                && MillisecondSampler.currentTimeMillis() - lastMineReportTime > 1000L) {
            boolean nonStandingPose = false;
            if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                    && event.getPlayer().getPose() != Pose.STANDING) nonStandingPose = true;
            if (!nonStandingPose && enabled) {
                flag("bot.baritone", "bot like actions (#mine)", 4);
                lastMineReportTime = MillisecondSampler.currentTimeMillis();
            }
        }
    }

    public void onPlayerTeleport(PlayerTeleportEvent event) {
        lastTeleportMillis = System.currentTimeMillis();
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockAgainst().getType() == FARMLAND
                && event.getBlockPlaced().getY() == event.getBlockAgainst().getY() + 1) {
            if (rotationChanges <= 2 && !event.getPlayer().isInsideVehicle()) {
                float yawDelta = GeometryMath.angularDistance(lastPlacementYaw, event.getPlayer().getLocation().getYaw());
                float pitchDelta = Math.abs(lastPlacementPitch - event.getPlayer().getLocation().getPitch());
                if (yawDelta > 30.0f || pitchDelta > 30.0f) {
                    String message = new StringBuilder("bot like actions (#farm, a: ")
                            .append(yawDelta).append(", p: ").append(pitchDelta).append(")").toString();
                    flag("bot.baritone", message, 4);
                }
            }
            rotationChanges = 0;
            lastPlacementYaw = event.getPlayer().getLocation().getYaw();
            lastPlacementPitch = event.getPlayer().getLocation().getPitch();
        }
    }

}
