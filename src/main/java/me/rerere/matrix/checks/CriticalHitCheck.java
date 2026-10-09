package me.rerere.matrix.checks;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.player.PlayerUtilities;

public final class CriticalHitCheck extends CheckRuntimeState {
    private static int penalty;
    private static boolean enabled;
    private static boolean cancelAttack;
    private double lastJumpRise;
    private double previousAttackY;
    private long lastGlidingTime;
    private long lastVelocityTime;
    private long lastTeleportTime;

    public CriticalHitCheck(PlayerCheckContext player) {
        super(player, CheckCategory.KILLAURA);
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        Player player = event.getPlayer();
        if (PlayerUtilities.hasPlayerDataOrEntityFlag(player) && to.getY() > from.getY()
                && System.currentTimeMillis() - lastVelocityTime > 2000L) {
            lastJumpRise = to.getY() - from.getY();
        }
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8) && player.isGliding()) {
            lastGlidingTime = MillisecondSampler.currentTimeMillis();
        }
    }

    public void onVelocity(PlayerVelocityEvent event) { lastVelocityTime = System.currentTimeMillis(); }
    public void onPlayerTeleport(PlayerTeleportEvent event) { lastTeleportTime = MillisecondSampler.currentTimeMillis(); }

    public void onAttackWithContext(EntityDamageByEntityEvent event) {
        Player attacker = (Player) event.getDamager();
        if (attacker.getGameMode() != GameMode.SURVIVAL && attacker.getGameMode() != GameMode.ADVENTURE) return;
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && (attacker.isGliding() || MillisecondSampler.currentTimeMillis() - lastGlidingTime <= 1000L)) return;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13) && attacker.isSwimming()) return;
        if (MillisecondSampler.lastSampleMillis() - lastTeleportTime <= 150L) return;
        if (attacker.getFallDistance() > 0.0F && !PlayerUtilities.hasPlayerEntityFlag(attacker)
                && !attacker.isInsideVehicle() && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS)
                && !BlockEnvironment.isPlayerOnLadderOrVine(attacker)
                && !BlockEnvironment.isSpecialPredicateAtOrAbove(attacker)
                && playerState().movementContext().integerValue == 0) {
            evaluateCritical(event, attacker);
        }
        lastJumpRise = 0.0;
    }

    private void evaluateCritical(EntityDamageEvent event, Player attacker) {
        Location location = attacker.getLocation();
        if (MaterialPolicy.cobwebBridge(location.getBlock().getType())) return;
        if (location.getY() == previousAttackY) {
            double fraction = location.getY() - location.getBlock().getY();
            if (enabled && fraction < 0x1.6872b020c49bap-7
                    && Math.abs(fraction - 0x1.89374bc6a7efap-11) > 0x1.a36e2eb1c432dp-14) {
                flag("ka.critical.modify", formatModifyMessage(
                        playerState().clientVersion().getReleaseName(), fraction), penalty);
                if (cancelAttack) event.setCancelled(true);
            }
        }
        if (location.getY() % 1.0 == 0.0 && MaterialPolicy.isAirMaterial(location.getBlock().getType())
                && enabled && location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
            flag("ka.critical.ground", "critical hit without correct jumping // ground spoof", penalty);
            if (cancelAttack) event.setCancelled(true);
        }
        if (lastJumpRise > 0.0 && lastJumpRise < 0x1.a3d70a3d70a3dp-2
                && !BlockEnvironment.hasOaWeightWithinUnitHeadVolume(attacker)
                && BlockEnvironment.isPredicateMaterialAtAndAbove(attacker) && enabled) {
            boolean exempt = false;
            if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8) && attacker.isGliding()) exempt = true;
            if (BlockEnvironment.hasSlimeBlockUnderPlayer(attacker)) exempt = true;
            if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_15)
                    && BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.HONEY_BLOCK)) exempt = true;
            if (!exempt) flag("ka.critical.hop", formatMiniJumpMessage(lastJumpRise), penalty);
        }
        previousAttackY = location.getY();
    }

    public void reloadConfiguration() {
        penalty = configuration().getInt("modules.critical.vl_weight", 5);
        enabled = configuration().getBoolean("modules.critical.enable", true);
        cancelAttack = configuration().getBoolean("modules.critical.cancel_attack", true);
    }

    private static String formatModifyMessage(String version, double fraction) {
        return "critical hit without correct jumping // modify (ver: " + version + " d: " + fraction + ")";
    }

    private static String formatMiniJumpMessage(double jumpRise) {
        return "critical hit without correct jumping // mini-jump (d:" + jumpRise + ")";
    }
}
