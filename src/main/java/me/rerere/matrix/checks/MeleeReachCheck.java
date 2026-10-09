package me.rerere.matrix.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.world.BukkitAttributes;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.combat.HitboxViolationIncrease;
import me.rerere.matrix.checks.hitbox.HitBoxCancelMode;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.entity.MovementContextSnapshot;
import me.rerere.matrix.entity.MovementLocationSample;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.Ray;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.runtime.EntityCategories;
import me.rerere.matrix.util.CollectionsSupport;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.ObjectSampleBuffer;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.EntityLocationRecorder;
import me.rerere.matrix.world.TimedLocation;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;

/** Checks attack rays against recorded target positions and applies reach violations. */
public class MeleeReachCheck extends CheckRuntimeState {
    private static int missedHitboxViolationWeight;
    private final ObjectSampleBuffer standingReachHistory;
    private static int minimumDynamicViolation;
    private static int standingReachViolationWeight;
    private MovementContextSnapshot currentReachEvent;
    private static int movingReachViolationWeight;
    private static int maximumDynamicViolation;
    private int reachEventAgeTicks;
    private static HitBoxCancelMode cancellationMode;
    private final ObjectSampleBuffer recentAttacks;
    private static int dynamicViolationExpander;
    private static double configuredMaxReach;
    private static int traceBackLength;
    private static int maxBurstViolation;
    private static final double[] collisionHeights;
    private static boolean dynamicViolationsEnabled;
    private static int cancelViolationLimit;
    private final EntityLocationRecorder entityLocationRecorder;
    private int pendingVehicleGraceTicks;
    private boolean cancelOnNextDamage;

    static {
        collisionHeights = new double[] {1.62D, 1.3D, 1.54D};
    }

    public MeleeReachCheck(PlayerCheckContext context) {
        super(context);
        standingReachHistory = new ObjectSampleBuffer(Double.class, 10);
        this.reachEventAgeTicks = 0;
        this.entityLocationRecorder = new EntityLocationRecorder();
        this.recentAttacks = new ObjectSampleBuffer(HitboxViolationIncrease.class, 50);
        this.cancelOnNextDamage = false;
        this.setCheckType(CheckCategory.HITBOX);
    }

    private static String formatIntegerPair(int first, int second) {
        return new StringBuilder().append(first).append("/").append(second).toString();
    }

    private static Vector directionFromYawPitch(float yaw, float pitch) {
        Vector direction = new Vector();
        direction.setY(-Math.sin(Math.toRadians(pitch)));
        double pitchCosine = Math.cos(Math.toRadians(pitch));
        direction.setX(-pitchCosine * Math.sin(Math.toRadians(yaw)));
        direction.setZ(pitchCosine * Math.cos(Math.toRadians(yaw)));
        return direction;
    }

    private static boolean isRecentAttack(HitboxViolationIncrease attack) {
        return attack != null && MillisecondSampler.currentTimeMillis()
                - attack.getIncreaseTime() <= 5000L;
    }

    private boolean isSwimming() {
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            return this.player().isSwimming();
        }
        boolean clientSupportsSwimming = this.playerState().clientVersion().isNewerThanOrEquals(ClientVersion.V_1_13);
        if (!clientSupportsSwimming) return false;
        return BlockEnvironment.isSpecialPredicateAtOrAbove(this.player());
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        MovementContextSnapshot trace = this.currentReachEvent;
        if (trace != null) {
            Location adjusted = from.clone();
            adjusted.setYaw(to.getYaw());
            adjusted.setPitch(to.getPitch());
            this.traceTarget(trace, adjusted, true);
            this.currentReachEvent = null;
            this.reachEventAgeTicks = 0;
        }
    }

    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    private List<Location> recentLocations(List<?> locationSnapshots, long cutoffMillis) {
        if (locationSnapshots == null) return Collections.emptyList();
        List<Location> recentLocations = new ArrayList<>();
        for (Object value : locationSnapshots) {
            TimedLocation timedLocation = (TimedLocation) value;
            if (timedLocation != null && !(timedLocation.timestamp() < cutoffMillis)) {
                recentLocations.add(timedLocation.location());
            }
        }
        return recentLocations;
    }

    private int clampDynamicViolation(int amount) {
        return Math.min(maximumDynamicViolation, Math.max(minimumDynamicViolation, amount));
    }

    private void handleAttack(Entity target, List<?> positions) {
        Player player = this.player();
        this.pendingVehicleGraceTicks = 20;
        if (!(target instanceof LivingEntity) || player.isInsideVehicle() || this.isSwimming()) return;
        if (target instanceof Player) {
            Player attackedPlayer = (Player) target;
            if (attackedPlayer.isOnline()) {
                this.captureReachTrace(player, attackedPlayer, positions);
            }
            return;
        }
        LivingEntity livingTarget = (LivingEntity) target;
        EntityType type = livingTarget.getType();
        if (type == EntityType.SLIME || type == EntityType.ARMOR_STAND
                || type == EntityType.ENDER_DRAGON || type == EntityType.GIANT) return;
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && livingTarget.getType() == EntityType.SHULKER) return;
        this.entityLocationRecorder.updateTrackedEntity(livingTarget);
        if (this.entityLocationRecorder.getRecordedLocationCount() > 10 || !positions.isEmpty()) {
            this.captureReachTrace(player, livingTarget, positions);
        }
    }

    private static String crystalTooFarMessage(String entity, double distance) {
        return new StringBuilder("attack crystal too far way (entity: ")
                .append(entity).append(", distance: ").append(distance).append(")").toString();
    }

    private static AxisAlignedBox makeTargetBox(double width, double height, boolean expand, Location at) {
        AxisAlignedBox box = new AxisAlignedBox(at.toVector(), width, height);
        if (expand) box.grow(0.1D, 0.1D, 0.1D);
        return box;
    }

    private double targetHeight(LivingEntity target) {
        if (target == null || target instanceof Player) return 2.0D;
        return BlockCollisionGeometry.getEntityBounds(target).height() + 0.25D;
    }

    private double targetWidth(LivingEntity target) {
        if (target == null || target instanceof Player) {
            ClientVersion version = this.playerState().clientVersion();
            return version.isOlderThan(ClientVersion.V_1_13) ? (double) 0.41F : (double) 0.31F;
        }
        return BlockCollisionGeometry.getEntityBounds(target).width() / 2.0D + 0.1D;
    }

    private double playerMaxReach(Player player) {
        double reach = configuredMaxReach;
        double base = configuredMaxReach - 3.0D;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5)) {
            AttributeInstance attribute = player.getAttribute(BukkitAttributes.INDEX.interactionRangeValue());
            reach = attribute.getValue() + base;
        }
        return reach + (player.getGameMode() == GameMode.CREATIVE ? 3.0D : 0.0D);
    }

    public void onEntityDamageWithContext(EntityDamageByEntityEvent event) {
        if (!Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) return;
        if (event.getEntityType() != EntityCategories.legacyEndCrystal()) return;
        Player attacker = (Player) event.getDamager();
        Entity target = event.getEntity();
        double distance = attacker.getLocation().distance(target.getLocation());
        double maximum = 5.0D + (attacker.getGameMode() == GameMode.CREATIVE ? 1.0D : 0.0D);
        if (!(distance > maximum)) return;
        String entityName = target.getType().name().toLowerCase();
        this.flag("hb.crys", crystalTooFarMessage(
                entityName, NumberUtils.round3(distance)), 5);
        event.setCancelled(true);
    }

    public void onAttackWithContext(EntityDamageByEntityEvent event) {
        if (!this.cancelOnNextDamage || this.violationCount() < cancelViolationLimit) return;
        this.cancelOnNextDamage = false;
        if (cancellationMode == HitBoxCancelMode.SILENT) event.setDamage(0.0D);
        if (cancellationMode == HitBoxCancelMode.CANCEL) event.setCancelled(true);
    }

    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    private List<MovementLocationSample> calculateRayDistances(
            List<Ray> rays, List<Location> targetLocations,
            LivingEntity target, boolean useAggregateBox) {
        List<MovementLocationSample> distances = new ArrayList<>();
        boolean legacyClient = this.playerState().clientVersion()
                .isOlderThanOrEquals(ClientVersion.V_1_8);
        if (!useAggregateBox) {
            for (Ray ray : rays) {
                for (Location location : targetLocations) {
                    AxisAlignedBox box = new AxisAlignedBox(location.toVector(),
                            this.targetWidth(target), this.targetHeight(target));
                    box.grow(0.001D, 0.001D, 0.001D);
                    if (legacyClient) box.grow(0.1D, 0.1D, 0.1D);
                    Vector intersection = box.clipSegmentAgainstBox(ray, 0.0F, 10.0F);
                    boolean hit = intersection != null;
                    double distance = hit ? intersection.distance(ray.getOrigin()) : 0.0D;
                    distances.add(new MovementLocationSample(hit, distance, location));
                }
            }
        } else {
            double width = this.targetWidth(target);
            double height = this.targetHeight(target);
            AxisAlignedBox aggregate = targetLocations.stream()
                    .map(location -> makeTargetBox(width, height, legacyClient, location))
                    .reduce((left, right) -> left.union(right)).get();
            for (Ray ray : rays) {
                Vector intersection = aggregate.clipSegmentAgainstBox(ray, 0.0F, 10.0F);
                boolean hit = intersection != null;
                double distance = hit ? intersection.distance(ray.getOrigin()) : 0.0D;
                Location firstWorldLocation = aggregate.center()
                        .toLocation(targetLocations.get(0).getWorld());
                distances.add(new MovementLocationSample(hit, distance, firstWorldLocation));
            }
        }
        return distances;
    }

    public ObjectSampleBuffer getStandingReachHistory() {
        return standingReachHistory;
    }

    private static String standingReachMessage(int weight) {
        return new StringBuilder("standing/").append(weight).toString();
    }

    public void onTick() {
        if (this.pendingVehicleGraceTicks > 0 && --this.pendingVehicleGraceTicks <= 0) {
            this.entityLocationRecorder.clear();
        }
        this.entityLocationRecorder.recordCurrentLocation();
        if (this.currentReachEvent == null) {
            this.reachEventAgeTicks = 0;
            return;
        }
        this.reachEventAgeTicks++;
        if (this.reachEventAgeTicks >= 2) {
            try {
                this.traceTarget(this.currentReachEvent,
                        this.currentReachEvent.getOriginLocation(), false);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            this.currentReachEvent = null;
            this.reachEventAgeTicks = 0;
        }
    }

    public void onEntityAction(Entity target, List<?> positions) {
        this.handleAttack(target, positions);
    }

    private void captureReachTrace(Player attacker, LivingEntity target, List<?> extraPositions) {
        List<?> positionHistory;
        List<Location> targetLocations;
        int traceWindowTicks;
        if (target instanceof Player) {
            Queue<?> queue = Matrix.getInstance().getMoveHistory().getHistory((Player) target);
            positionHistory = new ArrayList<>(queue);
            traceWindowTicks = this.extendTraceWindow();
            targetLocations = this.recentLocations(positionHistory,
                    System.currentTimeMillis() - traceWindowTicks * 50L);
            targetLocations.add(target.getLocation());
        } else {
            Queue<?> queue = this.entityLocationRecorder.getLocations();
            positionHistory = new ArrayList<>(queue);
            traceWindowTicks = this.extendTraceWindow();
            targetLocations = this.recentLocations(positionHistory,
                    System.currentTimeMillis() - traceWindowTicks * 50L);
            targetLocations.add(target.getLocation());
        }
        if (!extraPositions.isEmpty()) {
            targetLocations.clear();
            for (Object position : extraPositions) {
                targetLocations.add(((Vector) position).toLocation(target.getWorld()));
            }
        }
        int allSamples = positionHistory.size() + 1;
        String entityName = target instanceof Player ? "player" : target.getType().name().toLowerCase(Locale.ROOT);
        this.currentReachEvent = new MovementContextSnapshot(attacker, target,
                positionHistory, targetLocations, traceWindowTicks, allSamples,
                attacker.getLocation().clone(), entityName,
                !extraPositions.isEmpty());
    }

    private static String movingReachMessage(double reach) {
        return new StringBuilder("\u00a77[\u00a76HitBox\u00a77] Reach: ")
                .append(reach).append(" \u00a78[Standing]").toString();
    }

    private static String movingReachMessageWithTarget(String target, double reach) {
        return new StringBuilder("\u00a77[\u00a76HitBox\u00a77] Reach: ")
                .append(target).append(reach).append(" \u00a78[Move]").toString();
    }

    private int dynamicViolationForCap(int requested) {
        if (maxBurstViolation <= 0) return requested;
        Object[] records = this.recentAttacks.getSamples();
        int recent = 0;
        for (Object record : records) {
            HitboxViolationIncrease attack = (HitboxViolationIncrease) record;
            if (isRecentAttack(attack)) recent += attack.getAmount();
        }
        int remaining = Math.max(0, maxBurstViolation - recent);
        return Math.min(requested, remaining);
    }

    @Override
    public void reloadConfiguration() {
        configuredMaxReach = this.configuration().getDouble("max_reach", 3.1D);
        traceBackLength = this.configuration().getInt("trace_back_length", 3);
        missedHitboxViolationWeight = this.configuration().getInt("vl_weights.miss_player_hitbox", 2);
        standingReachViolationWeight = this.configuration().getInt("vl_weights.reach_target_standing", 10);
        movingReachViolationWeight = this.configuration().getInt("vl_weights.reach_target_moving", 3);
        dynamicViolationsEnabled = this.configuration().getBoolean("dynamic_vl.enable", true);
        dynamicViolationExpander = this.configuration().getInt("dynamic_vl.expander", 10);
        minimumDynamicViolation = this.configuration().getInt("dynamic_vl.min_vl", 3);
        maximumDynamicViolation = this.configuration().getInt("dynamic_vl.max_vl", 15);
        maxBurstViolation = this.configuration().getInt("max_burst_vl", 15);
        String policyName = this.configuration().getString("cancel_way", "silent");
        cancellationMode = HitBoxCancelMode.resolve(policyName);
        cancelViolationLimit = this.configuration().getInt("cancel_vl", 5);
    }

    private int extendTraceWindow() {
        return traceBackLength + 7;
    }

    private void traceTarget(MovementContextSnapshot trace, Location from, boolean immediate) {
        Player attacker = trace.getPlayer();
        Vector look = from.getDirection();
        List<Ray> rays = new ArrayList<>();
        for (double height : collisionHeights) {
            Vector base = from.toVector().add(new Vector(0.0D, height, 0.0D));
            rays.add(new Ray(base, look));
            rays.add(new Ray(base, trace.getOriginLocation().getDirection()));
            Location targetLocation = trace.getOriginLocation();
            rays.add(new Ray(base, directionFromYawPitch(
                    targetLocation.getYaw(), from.getPitch())));
        }
        rays.add(new Ray(from.toVector().add(new Vector(0.0D, attacker.getEyeHeight(), 0.0D)), look));
        Location targetLocation = trace.getOriginLocation();
        rays.add(new Ray(from.toVector().add(new Vector(0.0D, attacker.getEyeHeight(), 0.0D)),
                targetLocation.getDirection()));
        rays.add(new Ray(from.toVector().add(new Vector(0.0D, attacker.getEyeHeight(), 0.0D)), directionFromYawPitch(
                targetLocation.getYaw(), from.getPitch())));

        List<?> historical = trace.movementPositions();
        int traceCount = trace.primaryValue();
        @SuppressWarnings("unchecked")
        List<Location> targetLocations = (List<Location>) (List<?>) trace.recentMoves();
        List<MovementLocationSample> distances = this.calculateRayDistances(rays, targetLocations,
                trace.target(), trace.isStatusFlagSet());
        if (distances.isEmpty()) return;
        int sampleCount = historical.size() + 1;
        int positionCount = targetLocations.size();
        boolean noRayHit = distances.stream().noneMatch(MovementLocationSample::hasStateFlag);
        if (noRayHit) {
            if (DeveloperCommand.movementOption) {
                this.player().sendMessage("\u00a77[\u00a76HitBox\u00a77] Not Aiming at the target Hitbox");
            }
            if (PlayerUtilities.isGliding(attacker)) return;
            LivingEntity target = trace.target();
            if (!target.isInsideVehicle()) {
                int weight = this.dynamicViolationForCap(missedHitboxViolationWeight);
                this.recentAttacks.add(HitboxViolationIncrease.create(weight));
                Map<String, Object> details = (Map) CollectionsSupport.linkedMapOf(
                        "target", trace.getTargetType(),
                        "positions", String.valueOf(positionCount),
                        "all", String.valueOf(sampleCount),
                        "tk", String.valueOf(traceCount),
                        "prep", String.valueOf(immediate));
                this.flag("hb.mis", "missed hitbox", details, weight);
                this.cancelOnNextDamage = true;
            }
            standingReachHistory.add(3.5D);
            return;
        }

        double reach;
        int violationWeight;
        if (distances.size() == 1) {
            reach = distances.get(0).getDistance();
            standingReachHistory.add(reach);
            if (DeveloperCommand.movementOption) {
                this.player().sendMessage(movingReachMessage(reach));
            }
            if (!(reach >= this.playerMaxReach(attacker))) return;
            int dynamic = this.clampDynamicViolation(
                    (int) ((reach - this.playerMaxReach(attacker)) * dynamicViolationExpander));
            violationWeight = this.dynamicViolationForCap(
                    dynamicViolationsEnabled ? dynamic : standingReachViolationWeight);
            this.recentAttacks.add(HitboxViolationIncrease.create(violationWeight));
            this.flag("hb.reach", "attack too far away",
                    (Map) CollectionsSupport.linkedMapOf(
                            "target", trace.getTargetType(),
                            "reach", String.valueOf(NumberUtils.round3(reach)),
                            "max", String.valueOf(this.playerMaxReach(attacker)),
                            "positions", standingReachMessage(sampleCount),
                            "tk", String.valueOf(traceCount), "prep", String.valueOf(immediate)), violationWeight);
            this.cancelOnNextDamage = true;
            return;
        }
        reach = distances.stream().filter(MovementLocationSample::hasStateFlag)
                .mapToDouble(MovementLocationSample::getDistance).min().orElse(0.0D);
        if (DeveloperCommand.movementOption) {
            ChatColor color = reach >= this.playerMaxReach(attacker) ? ChatColor.RED : ChatColor.GREEN;
            this.player().sendMessage(movingReachMessageWithTarget(
                    String.valueOf(color), reach));
        }
        LivingEntity target = trace.target();
        if (!target.isDead() && target.isInsideVehicle()) reach -= 0.3D;
        standingReachHistory.add(reach);
        if (!(reach >= this.playerMaxReach(attacker))) return;
        int dynamic = this.clampDynamicViolation(
                (int) ((reach - this.playerMaxReach(attacker)) * dynamicViolationExpander));
        violationWeight = this.dynamicViolationForCap(
                dynamicViolationsEnabled ? dynamic : movingReachViolationWeight);
        this.recentAttacks.add(HitboxViolationIncrease.create(violationWeight));
        this.flag("hb.reach", "attack too far away",
                (Map) CollectionsSupport.linkedMapOf(
                        "target", trace.getTargetType(),
                        "reach", String.valueOf(NumberUtils.round3(reach)),
                        "max", String.valueOf(this.playerMaxReach(attacker)),
                        "positions", formatIntegerPair(positionCount, sampleCount),
                        "tk", String.valueOf(traceCount), "prep", String.valueOf(immediate)), violationWeight);
        this.cancelOnNextDamage = true;
    }
}
