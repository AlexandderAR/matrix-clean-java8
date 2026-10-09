package me.rerere.matrix.checks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.listeners.VelocityExplosionType;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BoundingBoxExpansion;
import me.rerere.matrix.geometry.DuplicatePosition;
import me.rerere.matrix.logging.DebugLogger;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.movement.MotionVectorState;
import me.rerere.matrix.movement.MoveData;
import me.rerere.matrix.movement.MovementPredictionState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.player.VelocityHistory;
import me.rerere.matrix.player.model.PreparedMovementData;
import me.rerere.matrix.util.CollectionsSupport;
import me.rerere.matrix.util.EnumSelection;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.ObjectSampleBuffer;
import me.rerere.matrix.util.SafeBuffer;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.BlockGeometry;
import me.rerere.matrix.world.CollisionGeometry;
import me.rerere.matrix.world.CollisionRegion;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.world.WorldAccess;
import me.rerere.matrix.world.BukkitAttributes;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionTypes;

/** Validates player movement against server movement physics. */
public class MovementPhysicsCheck extends CheckRuntimeState {
    private long damageWindowStartedAt;
    private long initialMovementTimestamp;
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable
    private Vector pendingTeleportVector;
    private long lastTeleportAt;
    private long lastCollisionUpdate;
    private long lastTickTimestamp;
    private long lastPearlTeleportAt;
    private long lastUseAt;
    private Vector pendingChorusVector;
    private final SafeBuffer recentCorrections;
    private int firstMoveBudget;
    private int waterExitGraceTicks;
    private int minAirborneTicks;
    private final ObjectSampleBuffer recentVelocityTimes;
    private int collisionGraceTicks;
    private boolean wasSprinting;
    private int moveSuppressionTicks;
    private List<BoundingBoxExpansion> recentCollisionBoxes;
    private final MovementPredictionState velocityTolerance;
    private boolean ignoreVehicleUntilExit;
    private long lastInventoryClickAt;
    private int locationFlagCleanupTicks;
    private int waterCorrectionCount;
    private boolean cancelMovement;
    private boolean requestSetback;
    private int noSlowViolationBuffer;
    private int fallCorrectionTicks;
    private long recentLevitationAt;
    private long movementCancellationEndedAt;
    private long swimmingStoppedAt;
    private long speedAttributeChangedAt;
    private long groundedInventoryMoveAt;
    private int repeatedHorizontalMoveCount;
    private double levitationErrorBuffer;
    private double movementViolationBuffer;
    private boolean hasRecentTeleport;
    private static boolean allowDamage;
    private static boolean checkNoSlow;
    private static boolean allowVoidSetback;
    private static boolean strictKnockbackHandling;
    private static boolean allowBridge;
    private static boolean checkInventoryMovement;
    private static final Set<String> elytraEffects;
    private static final Set<String> riptideEffects;
    private static int cancelViolationLimit;

    public MovementPhysicsCheck(PlayerCheckContext playerData) {
        super(playerData, CheckCategory.MOVE);
        this.recentVelocityTimes = new ObjectSampleBuffer(Long.class, 5);
        this.waterExitGraceTicks = 0;
        this.waterCorrectionCount = 0;
        this.minAirborneTicks = 0;
        this.movementViolationBuffer = 0.0D;
        this.pendingChorusVector = null;
        this.cancelMovement = false;
        this.requestSetback = false;
        this.firstMoveBudget = 0;
        this.initialMovementTimestamp = 0L;
        this.lastTickTimestamp = 0L;
        this.swimmingStoppedAt = 0L;
        this.recentCollisionBoxes = Collections.emptyList();
        this.velocityTolerance = new MovementPredictionState();
        this.recentCorrections = new SafeBuffer(25);
        this.hasRecentTeleport = false;
        this.movementCancellationEndedAt = 0L;
        this.wasSprinting = false;
        this.moveSuppressionTicks = 20;
        this.ignoreVehicleUntilExit = false;
        if (this.playerState() != null) {
            Player player = this.player();
            MutableGeometryState movementState = this.playerState().movementContext();
            if (player != null && movementState != null) {
                movementState.capacity = player.getLocation().clone();
            }
        }
    }

    static {
        elytraEffects = CollectionsSupport.linkedSetOf("elytra");
        riptideEffects = CollectionsSupport.linkedSetOf("riptide");
        cancelViolationLimit = 30;
    }

    /** True when server swimming state should be treated as movement state. */
    private boolean isSwimming() {
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
            if (this.player().isSwimming()) return true;
            return wasSprinting;
        }
        return this.playerState().clientVersion()
                .isNewerThanOrEquals(ClientVersion.V_1_13);
    }

    public void onEntityDamage(EntityDamageByEntityEvent event) {
        damageWindowStartedAt = MillisecondSampler.currentTimeMillis();
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        Player movedPlayer = event.getPlayer();
        boolean playerExempt = PlayerUtilities.hasPlayerDataOrEntityFlag(movedPlayer);
        boolean destinationInSpecialRegion = this.playerState()
                .locationFlags().getOrCompute(to);
        processPlayerMove(event, from, to, playerExempt, destinationInSpecialRegion);
    }

    public void processPlayerMove(PlayerMoveEvent event, Location from, Location to,
            boolean playerExempt, boolean destinationInSpecialRegion) {
        Player player = event.getPlayer();
        EnumSelection movementHistory = this.playerState().moveHistory();
    
        if (firstMoveBudget > 0 && playerExempt == destinationInSpecialRegion
                && from.distanceSquared(to) == 0.0D) {
            DuplicatePosition correctionKey = new DuplicatePosition(to.getX(), to.getY(), to.getZ());
            if (recentCorrections.contains(correctionKey)) {
                recentCorrections.remove(correctionKey);
                firstMoveBudget--;
                return;
            }
        }
        if (from.equals(to)) {
            return;
        }
    
        if (moveSuppressionTicks < 20) {
            moveSuppressionTicks++;
        }
    
        MutableGeometryState movementState = this.playerState().movementContext();
        PreparedMovementData inputState = movementState.index;
        GameMode gameMode = player.getGameMode();
        CollisionRegion environment = inputState.getCollisionCache();
        WorldAccess physicsAttributes = Matrix.getInstance().getNmsManager().visitor();
    
        if (DeveloperCommand.debugCollision) {
            org.bukkit.Bukkit.broadcastMessage(formatCollision(environment.toString()));
        }
    
        boolean suppressed = player.isFlying()
                || minAirborneTicks > 0
                || player.isDead()
                || player.isSleeping()
                || collisionGraceTicks > 0
                || (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE)
                || PlayerUtilities.isInVehicleOrPetVehicle(player)
                || environment.intersectsCollisionMaterials(MaterialPolicy.cobwebMaterial())
                || (ignoreVehicleUntilExit && moveSuppressionTicks <= 1);
    
        if (collisionGraceTicks > 0) {
            collisionGraceTicks--;
        }
        if (minAirborneTicks > 0) {
            minAirborneTicks--;
        }
        if (environment.primaryFlag()) {
            waterExitGraceTicks = 20;
            waterCorrectionCount = 0;
        } else if (waterExitGraceTicks > 0) {
            waterExitGraceTicks--;
        }
    
        MoveData current = MoveData.fromEvent(
                event, playerExempt, destinationInSpecialRegion, suppressed,
                physicsAttributes.getMovementSpeed(player), player.isSneaking(), player.isSprinting(),
                this.playerState().potionEffects().keySet());
        movementState.height = current.getHorizontalDistance();
    
        if (suppressed && PlayerUtilities.isInVehicleOrPetVehicle(player)) {
            current.getTags().add("vehicle");
        }
        if (environment.primaryFlag()) {
            current.getTags().add("water");
        }
        if (suppressed && environment.intersectsCollisionMaterials(MaterialPolicy.cobwebMaterial())) {
            current.getTags().add("web_ignore");
        }
        if (current.getSpeedAttribute() > 1.0F) {
            suppressed = true;
        }
    
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            if (!suppressed && this.playerState().isGliding()) {
                suppressed = true;
                current.setTags(elytraEffects);
            }
            if (!player.hasGravity()) {
                suppressed = true;
            }
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13) && player.isRiptiding()) {
            suppressed = true;
            current.setTags(riptideEffects);
            MoveData previousMovement = movementHistory.selected();
            if (previousMovement != null) {
                double travelled = to.distance(from);
                double priorTravelled = previousMovement.getFrom()
                        .distance(previousMovement.getTo());
                double excess = travelled - priorTravelled;
                long elapsed = System.currentTimeMillis() - lastTickTimestamp;
                double expected = 1.95D + PlayerUtilities.maxArmorEnchantment(
                        player, org.bukkit.enchantments.Enchantment.RIPTIDE) * 0.75D;
                if (excess > expected && elapsed < 65L) {
                    event.setTo(from);
                    this.flag("move.riptide",
                            formatRiptide(NumberUtils.round3(excess),
                                    NumberUtils.round3(expected), elapsed), 20);
                }
            }
        }
    
        current.setIgnoreMoveCheck(suppressed);
        if (!suppressed && evaluateMovement(event, from, to, current)) {
            current.setInvalid(true);
        }
    
        MoveData previous = movementHistory.selected();
        if (requestSetback) {
            current.getTags().add("force_setback");
        }
        if (movementState.opacity) {
            cancelMovement = true;
            if (!BlockGeometry.hasNonOpenAdjacentMaterial(to)) {
                if (!BlockEnvironment.hasFa5ySolidBlockNearOrBelow(to)) {
                    requestSetback = true;
                }
                AxisAlignedBox expandedBox = BlockCollisionGeometry.getEntityBounds(player)
                        .symmetricallyExpandedCopy(0.3D, 0.345D, 0.3D);
                if (!BlockEnvironment.findOaMatchingBlock(to.getWorld(), expandedBox, to).isPresent()) {
                    requestSetback = true;
                }
            }
            if (movementState.margin) {
                requestSetback = true;
            }
        }
        if (movementState.angle) {
            movementState.angle = false;
            cancelMovement = true;
            requestSetback = true;
        }
    
        double teleportThreshold = Math.max(8.0D * (double) current.getSpeedAttribute() * 10.0D, 8.0D);
        if (current.getHorizontalDistance() >= teleportThreshold
                && gameMode != GameMode.SPECTATOR
                && !PlayerUtilities.isInVehicleOrPetVehicle(player)
                && moveSuppressionTicks > 1) {
            this.flag("move.tp",
                    formatTeleportMove(NumberUtils.round3(current.getHorizontalDistance())), 35);
            event.setTo(from);
            current.setInvalid(true);
            cancelMovement = false;
            requestSetback = false;
            movementState.opacity = false;
            return;
        }
    
        if (Math.abs(to.getPitch()) > 90.0F) {
            this.flag("move.derp", "pitch derp",
                    diagnosticDetails("val", String.valueOf(to.getPitch())), 10);
            Location corrected = event.getTo().clone();
            corrected.setPitch(0.0F);
            event.setTo(corrected);
        }
    
        if (previous != null) {
            boolean suppressedWeb = suppressed
                    && environment.intersectsCollisionMaterials(MaterialPolicy.cobwebMaterial())
                    && !this.playerState().velocityTracker().hasRecentData();
            boolean previousWebIgnore = !suppressed
                    && previous.isIgnoreMoveCheck()
                    && previous.getTags().contains("web_ignore")
                    && !this.playerState().velocityTracker().hasRecentData();
            if (!PlayerUtilities.isGliding(player)
                    && (suppressedWeb || previousWebIgnore)) {
                double webExcess = GeometryMath.distance(from, to)
                        - previous.getFrom().distance(previous.getTo());
                if (webExcess > 0.6D) {
                    this.flag("move.webacel", formatIllegalWebMovement(webExcess), 20);
                    event.setTo(from);
                    current.setInvalid(true);
                    cancelMovement = false;
                    requestSetback = false;
                    movementState.opacity = false;
                    return;
                }
            }
        }
    
        if (cancelMovement && cancelViolationLimit >= 0) {
            if (this.violationCount() >= cancelViolationLimit || requestSetback) {
                if (allowVoidSetback
                        && PlayerUtilities.hasOnlyPassableBlocksBelow(from)
                        && !BlockGeometry.hasNonOpenAdjacentMaterial(from)
                        && !BlockEnvironment.findOaMatchingBlock(from).isPresent()) {
                    if (allowDamage) {
                        player.damage(5.0D);
                    }
                    if (to.getY() >= 0.0D) {
                        PlayerUtilities.correctFallLocation(player);
                    } else {
                        event.setTo(from);
                    }
                } else if (movementState.capacity != null) {
                    Location capacity = movementState.capacity;
                    if (CollisionGeometry.isLocationCollisionFree(capacity, this.playerState())
                            && GeometryMath.horizontalManhattanDistance(capacity, from) <= 32.0D) {
                        event.setTo(capacity.clone());
                    } else {
                        event.setTo(from);
                    }
                } else {
                    PlayerUtilities.moveToGround(player, allowDamage);
                }
            }
        } else {
            if (!current.isInvalid()) {
                if (current.isToGround() && !movementState.opacity
                        && BlockGeometry.hasSpaceOrCollision(to, 0.30000001192092896D)) {
                    movementState.capacity = to.clone();
                }
                if (suppressed) {
                    movementState.capacity = to.clone();
                }
            }
        }
    
        cancelMovement = false;
        requestSetback = false;
        movementState.opacity = false;
        if (!current.getTo().equals(event.getTo().toVector())) {
            current.setTo(to.toVector());
            current.setPredictedHorizontalMove(GeometryMath.horizontalDistance(
                    current.getFrom(), event.getTo().toVector()));
        }
        movementHistory.select(current);
    }
    
    private boolean evaluateMovement(PlayerMoveEvent event, Location from, Location to,
            MoveData current) {
        boolean accepted = false;
        Player player = event.getPlayer();
        EnumSelection movementHistory = this
                .playerState().moveHistory();
        MoveData previous = movementHistory.selected();
        long now = MillisecondSampler.lastSampleMillis();

        if (previous == null) {
            if (Math.abs(current.getVerticalDistance()) >= 0.6D) {
                event.setTo(from);
            this.flag("move.first", "first move teleport", 10);
            }
            return false;
        }
        if (current.getPotionEffectCount() != previous.getPotionEffectCount()
                && Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && !current.hasPotionEffect(PotionTypes.LEVITATION)
                && previous.hasPotionEffect(PotionTypes.LEVITATION)) {
            minAirborneTicks = 5;
        }

        Set<String> tags = com.google.common.collect.Sets.newHashSet();
        double previousVertical = previous.getVerticalDistance();
        double horizontalDistance = current.getHorizontalDistance();
        double verticalDelta = current.getVerticalDistance();
        velocityTolerance.predictedVerticalMove = (previousVertical - 0.08D) * 0.98D;

        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                && this.playerState()
                        .hasPotionEffect(PotionTypes.SLOW_FALLING)
                && verticalDelta <= 0.0D) {
            velocityTolerance.predictedVerticalMove = (previousVertical - 0.01D) * 0.98D;
            if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                    && Math.abs(velocityTolerance.predictedVerticalMove - verticalDelta) <= 0.0786D) {
                velocityTolerance.predictedVerticalMove = verticalDelta;
            }
            tags.add("slow_falling");
        }

        MutableGeometryState movementState = this.playerState().movementContext();
        PreparedMovementData inputState = movementState.index;
        CollisionRegion environment = inputState.getCollisionCache();
        World world = player.getWorld();
        boolean recentTeleportRegion = Matrix.getInstance().getChunkManager()
                .wasRecordedRecently(from,
                    RollingStatistics.metric() < 18.0D
                        ? 15000L : 10000L);
        WorldAccess attributes = Matrix.getInstance().getNmsManager().visitor();
        VelocityHistory physics = this
                .playerState().velocityTracker();

        if (current.isFromGround() || verticalDelta > 0.0D) fallCorrectionTicks = 0;
        if (previous.isIgnoreMoveCheck()
                && !previous.getTags().contains("vehicle")
                && !previous.getTags().contains("water")) {
            if (!(horizontalDistance <= 0.7D)
                    && !previous.getTags().contains("elytra")
                    && !previous.getTags().contains("riptide")) {
                current.setPredictedHorizontalMove(Math.min(horizontalDistance + physics.sumRecentVerticalValues(), 1.5D));
                tags.add("ignore_pred2");
            } else {
                current.setPredictedHorizontalMove(Math.max(horizontalDistance, 1.5D));
                tags.add("ignore_pred");
            }
            current.setTags(tags);
            return false;
        }
        if (recentTeleportRegion && verticalDelta < 0.0D
                && Math.abs(verticalDelta - previousVertical) < 0.5D
                && !previous.isInvalid()) {
            if (horizontalDistance <= 0.7D) current.setPredictedHorizontalMove(horizontalDistance);
            current.setIgnoreMoveCheck(true);
            return false;
        }

        boolean whitelisted = evaluateMovePhysics(event, from, to, current, environment, tags,
                previousVertical, verticalDelta, physics, previous, player, inputState,
                movementHistory, now, horizontalDistance,
                attributes.isUsingItem(player));
        if (whitelisted) return false;

        applyMovementPhysics(from, to, current, player, world, attributes, horizontalDistance,
                tags, physics, now, previous, verticalDelta, movementState, inputState, environment);

        boolean verticalUnexpected = !isAtMostDifference(verticalDelta,
                velocityTolerance.predictedVerticalMove,
                physics.hasRecentData() ? 0.006D : 0.0035D);
        boolean horizontalUnexpected = horizontalDistance > velocityTolerance.predictedHorizontalMove
                || tags.contains("back_sprint") || tags.contains("hor_slight")
                || (tags.contains("noslow") && noSlowViolationBuffer >= 12)
                || tags.contains("invalid_water");

        if (allowBridge && verticalUnexpected
                && (BlockEnvironment.hasAnyBlockMatchingConfiguredPredicate(from)
                        || BlockEnvironment.hasAnyBlockMatchingConfiguredPredicate(to))) {
            verticalUnexpected = false;
            tags.add("bpe_fix");
            if (horizontalUnexpected) {
                tags.add("bpe_fix_hor");
                horizontalUnexpected = false;
            }
        }

        if (verticalUnexpected && verticalDelta > 0.0D
                && (Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove)
                        < (BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from) ? 0.3D : 0.1D)
                    || verticalDelta <= (BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                                ? 0.301D : 0.1D))
                && previous.getTags().contains("liquid")
                && !tags.contains("liquid")) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("liquid_up_x");
            verticalUnexpected = false;
        }
        if (horizontalUnexpected && tags.contains("liquid")
                && verticalDelta < -0.1D
                && horizontalDistance - velocityTolerance.predictedHorizontalMove <= 0.025D
                && horizontalDistance < 0.18D) {
            horizontalUnexpected = false;
            tags.add("liquid_down_x");
        }
        if (verticalUnexpected && verticalDelta < 0.0D
                && verticalDelta > velocityTolerance.predictedVerticalMove
                && previousVertical < 0.0D
                && previous.getTags().contains("liquid")
                && !tags.contains("liquid")) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("liquid_down_x");
            verticalUnexpected = false;
        }
        if (verticalUnexpected && verticalDelta > 0.0D && verticalDelta <= 0.9D
                && velocityTolerance.predictedVerticalMove <= 0.1D
                && velocityTolerance.predictedVerticalMove > -0.2D
                && !current.isFromGround() && !current.isToGround()
                && BlockEnvironment.hasNearbyBubbleColumn(from)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("bubble_jump");
            verticalUnexpected = false;
        }

        if (verticalUnexpected || horizontalUnexpected) {
            PlayerCheckContext checkState = this.playerState();
            if (checkState.isGliding() || checkState.previouslyGliding()) {
                verticalUnexpected = false;
                horizontalUnexpected = false;
                tags.add("elytra_fix");
                if (!checkState.isGliding() && checkState.previouslyGliding()) {
                    checkState.setPreviouslyGliding(false);
                }
                if (current.isFromGround()) {
                    checkState.setGliding(false);
                    tags.add("ely_des_end");
                }
            }
        }

        if (verticalUnexpected && previous.getTags().contains("elytra_fix")
                && tags.contains("air_mdf") && !current.isFromGround()
                && !current.isToGround() && !physics.hasRecentData()
                && verticalDelta > 0.0D && verticalDelta <= 0.6D
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) <= 0.08D
                && horizontalDistance <= 0.9D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            verticalUnexpected = false;
            tags.remove("air_mdf");
            tags.add("elytra_swap");
        }
        if ((verticalUnexpected || horizontalUnexpected) && now - lastPearlTeleportAt <= 1000L) {
            if (verticalUnexpected && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) <= 3.0D
                    && verticalDelta <= 0.6D) {
                velocityTolerance.predictedVerticalMove = verticalDelta;
                tags.add("pearl_fix");
                verticalUnexpected = false;
            }
            if (horizontalUnexpected
                    && Math.abs(horizontalDistance - velocityTolerance.predictedHorizontalMove) <= 1.0D
                    && horizontalDistance < previous.getHorizontalDistance() - 0.01D) {
                horizontalUnexpected = false;
                tags.add("pearl_fix");
            }
        }
        if (verticalUnexpected && now - lastUseAt <= 1000L
                && previous.getTags().contains("liquid")
                && tags.contains("collision")
                && velocityTolerance.predictedVerticalMove >= -0.05D
                && velocityTolerance.predictedVerticalMove <= 0.05D
                && Math.abs(verticalDelta - jumpHeight(player)) <= 0.02D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            verticalUnexpected = false;
            tags.remove("air_mdf");
            tags.add("pearl_liquid_jump");
        }

        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && (horizontalUnexpected || verticalUnexpected)
                && moveSuppressionTicks <= 2 && horizontalDistance < 3.0D && verticalDelta < 3.0D) {
            horizontalUnexpected = false;
            verticalUnexpected = false;
            tags.add("exit_vehicle");
            moveSuppressionTicks = 20;
        }
        if (pendingChorusVector != null) {
            double vectorDistance = pendingChorusVector.distance(to.toVector());
            double movementDistance = from.distance(to);
            if (movementDistance > 0.0D && vectorDistance <= 0.001D) {
                pendingChorusVector = null;
                velocityTolerance.predictedVerticalMove = verticalDelta;
                horizontalUnexpected = false;
                verticalUnexpected = false;
                moveSuppressionTicks = 0;
                tags.add("fruit_tp_fix");
            }
        }

        if (DeveloperCommand.developerMode) {
            org.bukkit.Bukkit.broadcastMessage(formatTag(org.bukkit.ChatColor.GOLD.toString(),
                    player.getName(), DiagnosticFormat.joinIterable(tags),
                    DiagnosticFormat.joinIterable(previous.getTags()),
                    NumberUtils.round3(horizontalDistance)));
        }
        if (DeveloperCommand.debugMovement) {
            org.bukkit.Bukkit.broadcastMessage(formatMovementSummary(player.getName(),
                    DiagnosticFormat.vector(
                            to.toVector().subtract(from.toVector())),
                    NumberUtils.round3(current.getHorizontalDistance()), current.getSpeedAttribute(),
                    DiagnosticFormat.formatBooleanArray(new boolean[] { current.isFromGround(),
                            current.isToGround() }),
                    GeometryMath.angularDistance(to.getYaw(), from.getYaw()),
                    Math.abs(to.getPitch() - from.getPitch()),
                    NumberUtils.round3(to.getY()),
                    BlockEnvironment.hasSpecialFallingEntityAbove(from),
                    from.getY(), to.getY(), player.getFallDistance(), NumberUtils.round3(velocityTolerance.blockFriction)));
        }

        if (previous.isInvalid()
                && previous.getTags().contains("force_setback")
                && previous.getVerticalDistance() > 0.421D && verticalDelta > 0.0D
                && isWithinTolerance(verticalDelta,
                        (previous.getVerticalDistance() - 0.08D) * 0.98D)
                && !current.isFromGround() && !physics.hasRecentData()) {
            velocityTolerance.predictedVerticalMove = 0.0D;
            tags.add("ignore_setback");
            verticalUnexpected = true;
        }

        if (verticalUnexpected || horizontalUnexpected) {
            accepted = true;
            recentVelocityTimes.add(Long.valueOf(now));
        } else {
            movementViolationBuffer = Math.max(0.0D, movementViolationBuffer - 0.075D);
            if (movementViolationBuffer > 2.5D) movementViolationBuffer = 2.5D;
        }

        if (DeveloperCommand.developerMode && accepted) {
            player.sendMessage(formatInvalidMove(org.bukkit.ChatColor.RED.toString(), horizontalUnexpected,
                    verticalUnexpected, horizontalDistance, velocityTolerance.predictedHorizontalMove,
                    verticalDelta, velocityTolerance.predictedVerticalMove,
                    DiagnosticFormat.joinIterable(tags)));
            player.sendMessage(formatRecentVelocity(org.bukkit.ChatColor.RED.toString(),
                    this.playerState().velocityTracker().samples().stream()
                            .map(sample -> DiagnosticFormat.vector(sample.toVector()))
                            .collect(java.util.stream.Collectors.joining(" | "))));
            player.sendMessage(formatTagList(org.bukkit.ChatColor.RED.toString(),
                    DiagnosticFormat.joinIterable(tags)));
        }

        if (verticalUnexpected || horizontalUnexpected) {
        String velocitySummary = physics.summarizeRecentSamples();
        boolean repeatedRecentMoves = recentVelocityTimes.isFull()
                && java.util.Arrays.stream((Long[]) recentVelocityTimes.getSamples())
                        .allMatch(timestamp -> isWithinTimeWindow(now, timestamp));

        if (verticalUnexpected) {
            boolean onNonJumpableSurface = !current.isFromGround()
                    && !current.isToGround()
                    && MaterialPolicy.isAirMaterial(
                            BlockEnvironment.getMaterialAt(
                                    to.clone().add(0.0D, -1.0D, 0.0D)))
                    && !tags.contains("jump");
            movementViolationBuffer += Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove)
                    * (onNonJumpableSurface ? 3.0D : 0.5D);
            movementViolationBuffer = Math.min(10.0D, movementViolationBuffer);
            if (tags.contains("jump") && verticalDelta > jumpHeight(player)) {
                tags.add("high_jump");
                requestSetback = true;
                current.setVerticalDistance(jumpHeight(player));
            }
            if (tags.contains("jump") && tags.contains("ground_spoof")) requestSetback = true;
            if (tags.contains("bad_jumps")) requestSetback = true;
            if (Math.abs(verticalDelta) >= 1.0D && !physics.hasRecentData()) requestSetback = true;
            if (!current.isFromGround() && !current.isToGround()
                    && verticalDelta > 0.0D && verticalDelta > previousVertical
                    && !tags.contains("collision") && !tags.contains("levitation")
                    && !previous.getTags().contains("ladder")
                    && !BlockEnvironment.isOpenableOrHasSpecialLocationState(from, false)) {
                requestSetback = true;
                tags.add("air_bst");
            }
            if (verticalDelta > 0.001D && !current.isFromGround()
                    && !current.isToGround()
                    && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                    && !previous.isFromGround()
                    && BlockEnvironment.isPredicateMaterialAtAndAbove(from)
                    && !tags.contains("levitation")
                    && !previous.getTags().contains("ladder")
                    && !BlockEnvironment.isOpenableOrHasSpecialLocationState(from, false)
                    && !current.hasPotionEffect(PotionTypes.JUMP_BOOST)
                    && now - lastPearlTeleportAt >= 1500L) {
                requestSetback = true;
                tags.add("air_mdf");
            }
            if (Math.abs(verticalDelta) >= 1.0D
                    && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) >= 1.0D) {
                requestSetback = true;
                tags.add("vt_qm");
            }
            if (movementViolationBuffer >= 0.5D || repeatedRecentMoves || requestSetback) {
                String message = String.format(
                        "unexpected vertical movement (motion: %s/%s, hor: %s, buffer: %s, ground: %s, setback: %s, vel: %s, from: %s, past: %s, p_tags: %s, tags: %s)",
                        NumberUtils.round3(verticalDelta),
                        NumberUtils.round3(
                                velocityTolerance.predictedVerticalMove),
                        NumberUtils.round3(horizontalDistance),
                        formatBooleanVelocity(NumberUtils.round3(movementViolationBuffer), repeatedRecentMoves),
                        DiagnosticFormat.formatBooleanArray(new boolean[] { current.isFromGround(),
                                current.isToGround() }),
                        DiagnosticFormat.formatBooleanArray(new boolean[] { requestSetback }), velocitySummary,
                        NumberUtils.round3(from.getY()),
                        NumberUtils.round3(previousVertical),
                        DiagnosticFormat.joinIterable(previous.getTags()),
                        DiagnosticFormat.joinIterable(tags));
                boolean cancelled = this.flag("move.vert", message, 5);
                cancelMovement = true;
                if (!cancelled) {
                    requestSetback = false;
                    cancelMovement = false;
                }
            }
        }

        if (horizontalUnexpected) {
            movementViolationBuffer += Math.max(0.15D,
                    Math.abs(horizontalDistance - velocityTolerance.predictedHorizontalMove));
            if (tags.contains("jump")
                    && horizontalDistance > velocityTolerance.predictedHorizontalMove * 1.15D) {
                movementViolationBuffer += 0.4D;
                requestSetback = true;
            }
            if (horizontalDistance > velocityTolerance.predictedHorizontalMove * 2.0D) {
                requestSetback = true;
                tags.add("hor_fast_setback");
            }
            if (!current.isFromGround() && !current.isToGround()
                    && horizontalDistance > velocityTolerance.predictedHorizontalMove * 1.1D
                    && BlockEnvironment.isPredicateMaterialAtAndAbove(from)
                    && !physics.hasRecentData()
                    && previous.getTags().contains("jump")) {
                requestSetback = true;
                tags.add("hor_accel");
            }
            if (!current.isFromGround() && !current.isToGround()
                    && horizontalDistance > previous.getHorizontalDistance()) {
                tags.add("hor_bst");
                movementViolationBuffer += 3.0D * Math.abs(horizontalDistance
                        - velocityTolerance.predictedHorizontalMove);
                requestSetback = true;
            }
            if (tags.contains("back_sprint")) {
                movementViolationBuffer += 0.15D;
                if (movementViolationBuffer > 0.3D) {
                    cancelMovement = true;
                    this.flag("move.sprint", String.format(
                            "backward sprinting (move: %s,%s, yaw: %s, tags: %s)",
                            NumberUtils.round3(to.getX() - from.getX()),
                            NumberUtils.round3(to.getZ() - from.getZ()),
                            NumberUtils.round3(to.getYaw()),
                            DiagnosticFormat.joinIterable(tags)), 3);
                }
            }
            if (movementViolationBuffer >= 0.35D || repeatedRecentMoves || requestSetback) {
                int violationLevel = repeatedRecentMoves ? 15 : 5;
                String noslow = tags.contains("noslow")
                        ? formatParenthesized(NumberUtils.round3(velocityTolerance.noSlowHorizontalLimit)) : "";
                String message = String.format(
                        "unexpected horizontal movement (motion: %s/%s, buffer: %s, vet: %s, vec: %s, ground: %s, spr: %s, add: %s, past: %s, fr: %s, setback: %s, vel: %s, from: %s, rv: %s, ptags: %s, tags: %s)",
                        NumberUtils.round3(horizontalDistance),
                        formatVelocity(NumberUtils.round3(
                                velocityTolerance.predictedHorizontalMove), noslow),
                        formatBooleanVelocity(NumberUtils.round3(movementViolationBuffer), repeatedRecentMoves),
                        NumberUtils.round3(verticalDelta),
                        NumberUtils.round3(previous.getPredictedHorizontalMove()),
                        DiagnosticFormat.formatBooleanArray(new boolean[] { current.isFromGround(),
                                current.isToGround() }),
                        DiagnosticFormat.formatBooleanArray(new boolean[] { player.isSprinting() }),
                        NumberUtils.round3(velocityTolerance.horizontalAcceleration),
                        formatCoordinates(NumberUtils.round3(previous.getVerticalDistance()), NumberUtils.round3(previous.getHorizontalDistance())),
                        velocityTolerance.blockFriction,
                         DiagnosticFormat.formatBoolean(requestSetback), velocitySummary,
                        NumberUtils.round3(from.getY()),
                physics.currentSample() == null ? "empty"
                        : NumberUtils.round3(physics.currentSample().getYValue()),
                        DiagnosticFormat.joinIterable(previous.getTags()),
                        DiagnosticFormat.joinIterable(tags));
                boolean cancelled = this.flag("move.hor", message, violationLevel);
                if (tags.contains("noslow")
                        && horizontalDistance >= velocityTolerance.predictedHorizontalMove - 0.01D) {
                    PlayerUtilities.notifyPlayerData(player);
                }
                cancelMovement = true;
                if (!cancelled) {
                    cancelMovement = false;
                    requestSetback = false;
                }
            }
        }
        }

        current.setPredictedHorizontalMove(horizontalDistance * velocityTolerance.horizontalDrag);
        current.setFrictionFactor(velocityTolerance.blockFriction);
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        current.setPredictedX(deltaX * velocityTolerance.horizontalDrag);
        current.setPredictedZ(deltaZ * velocityTolerance.horizontalDrag);
        current.setTags(tags);
        return accepted;
    }

    private boolean evaluateMovePhysics(PlayerMoveEvent event, Location from, Location to,
            MoveData current, CollisionRegion environment, Set<String> tags,
            double previousVertical, double verticalDelta, VelocityHistory physics,
            MoveData previous, Player player, PreparedMovementData inputState,
            EnumSelection movementHistory, long now, double horizontalDistance,
            boolean hasMovementAttribute) {
        double expectedBeforeJump = velocityTolerance.predictedVerticalMove;
        if (environment.secondaryFlag()) {
            tags.add("liquid");
            double expectedVertical = Math.max(0.131D,
                    previousVertical * 0.8D - 0.02D + 0.055D);
            if (verticalDelta > expectedVertical
                    && CollisionGeometry.hasCollisionWithinPlayerHeight(from).booleanValue()) {
                expectedVertical += 0.3D;
                tags.add("liquid_up");
            }
            if (verticalDelta > expectedVertical
                    && BlockEnvironment.hasAnyLiquidInMovementWindow(from)) {
                expectedVertical += 0.1D;
                tags.add("liquid_flow");
            }
            if (physics.hasRecentData()) {
                expectedVertical += physics.maximumRecentMagnitude();
                tags.add("liquid_knockback");
            }
            if (verticalDelta > expectedVertical && isSwimming()) {
                tags.add("liquid_swim");
                expectedVertical += 0.1D;
            }
            if (!isSwimming() && verticalDelta > expectedVertical) {
                if (previous.getTags().contains("swimming")) swimmingStoppedAt = now;
                if (now - swimmingStoppedAt <= 1500L) {
                    tags.add("liquid_swim_stop");
                    expectedVertical += 0.2D;
                }
            }
            if (verticalDelta > expectedVertical
                    && (current.isFromGround() || previous.isFromGround())) {
                tags.add("liquid_jump");
                expectedVertical += 0.1D;
                expectedVertical = Math.max(jumpHeight(player), expectedVertical);
            }
            if (verticalDelta > expectedVertical
                    && (BlockEnvironment.hasSpecialFallingEntityAbove(from)
                        || BlockEnvironment.hasSpecialEntityWithinRadius(from))) {
                tags.add("liquid_boat");
                expectedVertical += 0.5D;
            }
            if (verticalDelta > expectedVertical
                    && BlockEnvironment.isSpecialOrOpenableMaterial(from, false)) {
                tags.add("liquid_ladder");
                expectedVertical += 0.5D;
            }
            if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                    && verticalDelta > expectedVertical
                    && (environment.intersectsLargerMaterials(new Material[] { Material.BUBBLE_COLUMN })
                            || BlockEnvironment
                                    .hasMaterialAtOrAdjacentTo(from, Material.BUBBLE_COLUMN))) {
                tags.add("liquid_bubble");
                expectedVertical += 0.35D;
            }
            if (verticalDelta > expectedVertical && expectedVertical > 0.0D) {
                velocityTolerance.predictedVerticalMove = expectedVertical;
                tags.add("liquid_vert_bad");
            } else {
                velocityTolerance.predictedVerticalMove = verticalDelta;
            }
        }

        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && waterExitGraceTicks > 0 && BlockEnvironment.hasConfiguredBlockStateInMovementWindow(from)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("liquid_client");
        }

        expectedBeforeJump = velocityTolerance.predictedVerticalMove;
        boolean jumpStateProcessed = current.isFromGround() && verticalDelta > 0.0D
                && !tags.contains("liquid");
        if (jumpStateProcessed) {
            expectedBeforeJump = velocityTolerance.predictedVerticalMove;
            velocityTolerance.predictedVerticalMove = jumpHeight(player);
            tags.add("jump");
            if (inputState.isOnSlime() || inputState.isOnBed()) {
                if (Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) > 0.0035D) {
                    MoveData previousAirState = movementHistory.at(1);
                    if (previousAirState == null) {
                        velocityTolerance.predictedVerticalMove = verticalDelta;
                    } else {
                        double rebound = (previousAirState.getVerticalDistance() - 0.08D) * 0.98D;
                        double reboundAgain = (-rebound - 0.08D) * 0.98D;
                        if (isWithinTolerance(verticalDelta, reboundAgain)
                                || verticalDelta <= jumpHeight(player)) {
                            velocityTolerance.predictedVerticalMove = verticalDelta;
                        }
                    }
                    tags.add("slime");
                }
            }
            if (!BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                    && !BlockGeometry.hasNonOpenAdjacentMaterial(from)
                    && !Matrix.getInstance().getClientBlockManager().contains(
                            BlockEnvironment.getBlockAt(
                                    from.getWorld(), from.getBlockX(), from.getBlockY() - 1, from.getBlockZ()))
                    && !BlockEnvironment.hasSpecialFallingEntityAbove(from)
                    && BlockEnvironment.isPredicateMaterialAtAndAbove(from)
                    && environment.largerCollisionMaterials().size() == 1
                    && !BlockGeometry.collidesWithin(from, 0.4D)
                    && !BlockGeometry.hasLiquidSupport(from)) {
                tags.remove("jump");
                tags.add("gf_jump");
                velocityTolerance.predictedVerticalMove = expectedBeforeJump;
                requestSetback = true;
            }
        }

        if (jumpStateProcessed && tags.contains("jump")
                && previous.getTags().contains("jump")
                && Math.abs(verticalDelta - previous.getVerticalDistance()) < 0.01D
                && environment.largerCollisionMaterials().size() == 1
                && !BlockGeometry.hasNonOpenAdjacentMaterial(from)
                && !BlockGeometry.collidesWithin(from, 0.4D)
                && !Matrix.getInstance().getClientBlockManager().contains(
                        BlockEnvironment.getBlockAt(
                                from.getWorld(), from.getBlockX(), from.getBlockY() - 1, from.getBlockZ()))
                && !BlockEnvironment.hasSpecialFallingEntityAbove(from)) {
            tags.remove("jump");
            tags.add("gf_jump_repeat");
            velocityTolerance.predictedVerticalMove = expectedBeforeJump;
            requestSetback = true;
        }

        if (verticalDelta >= 0.41D && verticalDelta <= 0.61D && !current.isFromGround()
                && !tags.contains("jump") && previous.getTags().contains("teleport")
                && Math.abs(verticalDelta - jumpHeight(player)) <= 0.0035D
                && !previous.getTags().contains("tp_jump")
                && BlockGeometry.hasLiquidSupport(from)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("jump");
            tags.add("tp_jump");
        }

        if (isWithinTolerance(verticalDelta, jumpHeight(player))
                && isWithinTolerance(velocityTolerance.predictedVerticalMove, 0.333D)
                && !current.isFromGround() && !tags.contains("jump")
                && now - lastTeleportAt <= 1000L
                && BlockGeometry.hasLiquidSupport(from)
                && pendingTeleportVector != null
                && pendingTeleportVector.distance(from.toVector()) == 0.0D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("jump");
            tags.add("tp_jump");
        }

        if (pendingTeleportVector != null
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) > 0.0035D
                && to.toVector().equals(pendingTeleportVector)
                && current.getHorizontalDistance() <= 2.0D) {
            tags.add("teleport");
            pendingTeleportVector = null;
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }

        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)) {
            org.bukkit.util.Vector displacement = current.getTo().clone()
                    .subtract(current.getFrom());
            Set<?> matchingVelocities = this
                    .playerState().inventoryTracker().classifyVerticalMotion(
                            displacement, velocityTolerance.predictedVerticalMove);
            if (!matchingVelocities.isEmpty()) {
                String velocityText = formatLabel(this.playerState().playerName(),
                        DiagnosticFormat.joinEnums(matchingVelocities));
                DebugLogger.printDebug(velocityText);
                if (matchingVelocities.contains(VelocityExplosionType.VELOCITY)) {
                    tags.add("velocity");
                    velocityTolerance.predictedVerticalMove = verticalDelta;
                }
                if (matchingVelocities.contains(VelocityExplosionType.EXPLOSION)) {
                    tags.add("explosion");
                    tags.add("velocity");
                    velocityTolerance.predictedVerticalMove = verticalDelta;
                }
            }
        }

        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && physics.hasRecentData()) {
            Set<?> verticalVelocityMatches = physics.findRecentSamplesAtDistance(verticalDelta);
            if (!verticalVelocityMatches.isEmpty()) {
                MotionVectorState nearest = verticalVelocityMatches.stream()
                        .map(MotionVectorState.class::cast)
                        .min(java.util.Comparator.comparingLong(
                                sample -> sample.getTimestamp())).get();
                velocityTolerance.predictedVerticalMove = nearest.getYValue();
                if (horizontalDistance > 0.03D) nearest.setInternalFlag(true);
                tags.add("velocity");
                tags.add("vel_y_axis");
            }
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && physics.hasRecentData()) {
            Set<?> horizontalVelocityMatches = physics.selectSamplesForMagnitude(
                    verticalDelta - velocityTolerance.predictedVerticalMove);
            if (!horizontalVelocityMatches.isEmpty()) {
                horizontalVelocityMatches.forEach(
                        sample -> markInteraction((MotionVectorState) sample));
                velocityTolerance.predictedVerticalMove = verticalDelta;
                tags.remove("jump");
                tags.add("velocity");
                tags.add("explosion");
                tags.add("exp_v_delta");
            }
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && physics.hasRecentData()) {
            double expectedDifference = velocityTolerance.predictedVerticalMove - verticalDelta;
            double knockbackY = physics.sumRecentEligibleMagnitudes();
            if (knockbackY > 0.0D && expectedDifference > 0.0D
                    && expectedDifference <= knockbackY + 0.08D
                    && (previous.getTags().contains("explosion")
                        || tags.contains("jump") || verticalDelta < 0.0D)) {
                velocityTolerance.predictedVerticalMove = verticalDelta;
                tags.remove("jump");
                tags.add("velocity");
                tags.add("explosion");
                tags.add("exp_tail");
            }
        }
        if (verticalDelta > 0.0D && physics.hasRecentData()
                && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && BlockEnvironment.hasOaWeightWithinUnitHeadVolume(player)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("velocity");
            tags.add("vel_coll");
        }
        if (verticalDelta < 0.0D && physics.hasRecentData()
                && physics.firstRecentNegativeSample() != null) {
            tags.add("velocity");
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("vel_down");
        }

        if (verticalDelta > 0.0D && verticalDelta <= 0.6D
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) > 0.0035D
                && !tags.contains("velocity")
                && CollisionGeometry.hasSupportingShapeAt(player, to,
                        this.playerState())
                && horizontalDistance <= 1.0D
                && (BlockGeometry.hasNonOpenAdjacentMaterial(from)
                        || current.isFromGround()
                        || previous.getVerticalDistance() < -0.2D
                        || previous.isFromGround())) {
            tags.add("step");
            velocityTolerance.predictedVerticalMove = verticalDelta;
            current.setVerticalDistance(0.0D);
        }
        if (verticalDelta > 0.0D && verticalDelta <= 0.6D
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) > 0.0035D
                && CollisionGeometry.hasSupportingShapeAt(player, to,
                        this.playerState())
                && (BlockGeometry.hasNonOpenAdjacentMaterial(from)
                    || current.isFromGround()
                    || previous.getVerticalDistance() < -0.2D || previous.isFromGround())
                && physics.hasRecentData()) {
            tags.add("step");
            tags.add("step_vel");
            velocityTolerance.predictedVerticalMove = verticalDelta;
            current.setVerticalDistance(0.0D);
        }
        if (verticalDelta == 0.0D && velocityTolerance.predictedVerticalMove > 0.4D
                && current.isFromGround() && current.isToGround()
                && physics.hasRecentData()
                && previous.getVerticalDistance() == 0.5D
                && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)) {
            tags.add("step");
            tags.add("step_dmg");
            velocityTolerance.predictedVerticalMove = 0.0D;
        }

        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            if (current.hasPotionEffect(PotionTypes.LEVITATION)) {
                recentLevitationAt = now;
                velocityTolerance.predictedVerticalMove = verticalDelta;
                tags.add("levitation");
                int levitationLevel = PlayerUtilities.potionLevel(
                        this.playerState(), PotionTypes.LEVITATION);
                if (levitationLevel > 127) levitationLevel = (byte) levitationLevel;
                double levitationExpected = previous.getVerticalDistance()
                        + (0.05D * levitationLevel - previous.getVerticalDistance()) * 0.2D;
                levitationExpected *= 0.98D;
                if (Math.abs(levitationExpected - verticalDelta) > 0.025D
                        && !BlockEnvironment.isOpenableOrHasSpecialLocationState(from, false)) {
                    levitationErrorBuffer += Math.abs(levitationExpected - verticalDelta);
                    if (levitationErrorBuffer > 0.5D && !physics.hasRecentData()) {
                        velocityTolerance.predictedVerticalMove = levitationExpected;
                    }
                    if (BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                            && verticalDelta * levitationExpected > 0.0D) {
                        velocityTolerance.predictedVerticalMove = verticalDelta;
                        tags.add("levitation_wk");
                    }
                } else {
                    if (levitationErrorBuffer > 0.75D) levitationErrorBuffer = 0.75D;
                    levitationErrorBuffer = Math.max(0.0D, levitationErrorBuffer - 0.01D);
                }
            } else {
                levitationErrorBuffer = 0.0D;
            }
        }
        if (previous.getTags().contains("levitation")
                && !tags.contains("levitation")) return true;

        double expectedYAfterCollision = from.getY() + velocityTolerance.predictedVerticalMove;
        double expectedVertical = velocityTolerance.predictedVerticalMove;
        if (!tags.contains("step") && !isWithinTolerance(verticalDelta, expectedVertical)) {
            velocityTolerance.predictedVerticalMove = CollisionGeometry
                    .resolveVerticalMovement(player,
                            this.playerState(),
                            from.getWorld(), from.toVector(), to.toVector().setY(expectedYAfterCollision));
            if (Math.abs(velocityTolerance.predictedVerticalMove - expectedVertical) >= 0.01D) {
                tags.add("collision");
                if (isWithinTolerance(verticalDelta, expectedVertical)
                        && verticalDelta > velocityTolerance.predictedVerticalMove + 0.001D
                        && verticalDelta < 0.5D) {
                    tags.add("coll_ex");
                    velocityTolerance.predictedVerticalMove = verticalDelta;
                }
            }
        }

        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && isJumpHeightPlausible(player, from, to, current, tags,
                        verticalDelta, expectedVertical)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            current.setVerticalDistance(expectedVertical);
            tags.add("collision");
            tags.add("head_jump");
        }
        if (!isWithinTolerance(velocityTolerance.predictedVerticalMove, verticalDelta)
                && inputState.isOnBed()
                && verticalDelta < 0.42D && verticalDelta > -0.4375D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("bed_coll");
        }
        if (!isWithinTolerance(velocityTolerance.predictedVerticalMove, verticalDelta)
                && environment.intersectsCollisionMaterials(new Material[] { Material.SNOW })
                && verticalDelta <= 0.42D && verticalDelta > -0.01D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("snow_coll");
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && verticalDelta == 0.0D && current.isToGround()
                && previous.getTags().contains("explosion")
                && previous.getTags().contains("velocity")
                && velocityTolerance.predictedVerticalMove > 0.0D
                && velocityTolerance.predictedVerticalMove <= 0.08D
                && physics.hasRecentData()) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("velocity");
            tags.add("exp_ground");
        }

        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)) {
            boolean modernClimbable = this.playerState().clientVersion()
                    .isNewerThan(ClientVersion.V_1_8);
            if (BlockEnvironment.isSpecialOrOpenableMaterial(from, modernClimbable)) {
                velocityTolerance.predictedVerticalMove = verticalDelta;
                if (verticalDelta > 0.118D && !tags.contains("jump")
                        && !tags.contains("velocity")) {
                    velocityTolerance.predictedVerticalMove = 0.118D;
                }
                if (verticalDelta >= jumpHeight(player) && tags.contains("jump")
                        && !tags.contains("velocity")) {
                    velocityTolerance.predictedVerticalMove = jumpHeight(player);
                }
                tags.add("ladder");
            }
        }
        if (isWithinTolerance(velocityTolerance.predictedVerticalMove, verticalDelta)
                && tags.contains("jump") && !tags.contains("velocity")
                && !BlockGeometry.hasNonOpenAdjacentMaterial(from)
                && BlockEnvironment.isSpecialOrOpenableMaterial(from,
                    this.playerState().clientVersion().isNewerThan(ClientVersion.V_1_8))) {
            velocityTolerance.predictedVerticalMove = 0.118D;
            tags.add("ladder");
            tags.add("ladder_gf");
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && Math.abs(verticalDelta) <= 0.118D
                && BlockEnvironment.hasMaterialAtOrAdjacentTo(from, Material.VINE)) {
            tags.add("ladder_fix");
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && isLadderJumpPlausible(from, to, current, previous, tags,
                        verticalDelta, velocityTolerance.predictedVerticalMove)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("ladder");
            tags.add("ladder_carry");
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)) {
            Material material = BlockEnvironment.getMaterialAt(from);
            if (material == Material.LANTERN
                    && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) <= 0.25D) {
                tags.add("lantern");
                velocityTolerance.predictedVerticalMove = verticalDelta;
            }
            if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_16)) {
                Material soulMaterial = BlockEnvironment.getMaterialAt(from);
                if (soulMaterial == Material.SOUL_LANTERN
                        && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) <= 0.25D) {
                    tags.add("lantern");
                    velocityTolerance.predictedVerticalMove = verticalDelta;
                }
            }
        }

        if (verticalDelta <= 0.0D && verticalDelta >= -0.2D) {
            if (expectedVertical > 0.0D && Math.abs(velocityTolerance.predictedVerticalMove) <= 0.035D
                    && tags.contains("collision")) {
                velocityTolerance.predictedVerticalMove = verticalDelta;
                tags.add("collision_fall");
            }
            if (previous.getTags().contains("collision")
                    && previous.getVerticalDistance() > 0.0D) {
                velocityTolerance.predictedVerticalMove = verticalDelta;
                tags.add("collision_fall");
            }
        }
        if (verticalDelta <= -0.078D && verticalDelta >= -0.079D
                && velocityTolerance.predictedVerticalMove <= -0.305D
                && velocityTolerance.predictedVerticalMove >= -0.306D
                && current.isFromGround() && !current.isToGround()
                && previous.getVerticalDistance() <= 0.0D
                && previous.getTags().contains("collision")
                && BlockEnvironment.hasAnyBlockMatchingConfiguredPredicate(from)) {
            tags.add("collision_fall");
            tags.add("break_fall");
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }
        if (verticalDelta == 0.0D
                && velocityTolerance.predictedVerticalMove < -0.077D
                && velocityTolerance.predictedVerticalMove > -0.079D && current.isFromGround()
                && current.isToGround()
                && BlockEnvironment.isConfiguredBlockInsideEntityBox(from)) {
            tags.add("collision_fall");
            tags.add("disappear_fall");
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }

        if (verticalDelta >= -2.0D && verticalDelta < velocityTolerance.predictedVerticalMove - 0.0035D
                && velocityTolerance.predictedVerticalMove <= 0.0D
                && tags.contains("collision") && previous.getVerticalDistance() < 0.0D
                && isWithinTolerance(verticalDelta,
                        (previous.getVerticalDistance() - 0.08D) * 0.98D)) {
            tags.add("vt_ssb");
            velocityTolerance.predictedVerticalMove = verticalDelta;
            Location correction = to.clone();
            correction.setY(from.getY() + velocityTolerance.predictedVerticalMove);
            if (cancelViolationLimit >= 0) event.setTo(correction);
        }

        if (!current.isFromGround() && verticalDelta <= 0.0D
                && current.getHorizontalDistance() <= 0.08D
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) > 0.0035D
                && Math.abs(verticalDelta) <= 1.0D) {
            double difference = Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove);
            if (velocityTolerance.predictedVerticalMove * velocityTolerance.predictedVerticalMove <= 0.0009D
                    || difference <= 0.185D) {
                fallCorrectionTicks++;
                if (fallCorrectionTicks <= 3) {
                    velocityTolerance.predictedVerticalMove = verticalDelta;
                    tags.add("fall_fix");
                }
            }
        }

        if (current.isFromGround() && verticalDelta == 0.0D
                && velocityTolerance.predictedVerticalMove < 0.0D
                && velocityTolerance.predictedVerticalMove >= -0.027D
                && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("place_float");
        }

        if (verticalDelta > 0.0D && from.getY() % 1.0D != 0.0D
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) > 0.0035D
                && !tags.contains("slime")
                && (Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) <= 0.025D
                    || Math.abs(verticalDelta - jumpHeight(player)) <= 0.025D)
                && horizontalDistance <= 0.37D && previous.getVerticalDistance() <= 0.0D
                && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                && BlockEnvironment.hasOaLocationPredicateInMovementWindow(from)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            current.setVerticalDistance(jumpHeight(player));
            tags.add("place_jump");
            fallCorrectionTicks = 0;
        }

        if (verticalDelta > 0.0D && current.isFromGround()
                && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && tags.contains("jump") && physics.hasRecentData()
                && (physics.firstSampleNearMagnitude(verticalDelta + 0.0157D) != null
                        || physics.firstSampleNearMagnitude(verticalDelta + 0.025D) != null)) {
            tags.add("velocity");
            tags.add("place_vel_fix");
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }

        if (verticalDelta > 0.0D && !current.isFromGround()
                && !current.isToGround()
                && velocityTolerance.predictedVerticalMove < 0.0D
                && isWithinTolerance(verticalDelta - velocityTolerance.predictedVerticalMove,
                        jumpHeight(player))
                && tags.contains("collision") && now - initialMovementTimestamp >= 500L) {
            tags.add("jump");
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("jump_small");
            current.setVerticalDistance(jumpHeight(player));
            initialMovementTimestamp = now;
        }
        if (verticalDelta == 0.0D && velocityTolerance.predictedVerticalMove > -0.11D
                && velocityTolerance.predictedVerticalMove < -0.07D && current.isFromGround()
                && BlockGeometry.hasNonOpenAdjacentMaterial(from)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("edge_fix");
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && Math.abs(verticalDelta + 0.078D) <= 0.001D
                && current.isFromGround() && !current.isToGround()
                && Math.abs(velocityTolerance.predictedVerticalMove - verticalDelta) <= 0.05D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("edge_fix");
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && Math.abs(verticalDelta + 0.078D) <= 0.001D
                && current.isFromGround() && !current.isToGround()
                && previous.isToGround()
                && velocityTolerance.predictedVerticalMove < verticalDelta) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("edge_fix");
            tags.add("ground_fall");
        }
        if (verticalDelta == 0.0D && velocityTolerance.predictedVerticalMove < 0.0D
                && current.isFromGround() && !current.isToGround()
                && Math.abs(velocityTolerance.predictedVerticalMove - verticalDelta) <= 0.03D
                && current.getHorizontalDistance() == 0.0D) {
            Location correction = to.clone();
            correction.setY(from.getY() + velocityTolerance.predictedVerticalMove);
            if (BlockGeometry.hasNonOpenAdjacentMaterial(correction)) {
                if (cancelViolationLimit >= 0) event.setTo(correction);
                tags.add("small_fix");
                velocityTolerance.predictedVerticalMove = verticalDelta;
                PlayerUtilities.updateControlState(player, true);
            }
        }
        if (velocityTolerance.predictedVerticalMove == 0.0D && verticalDelta < -0.077D
                && verticalDelta > -0.079D && tags.contains("collision")
                && BlockEnvironment.isConfiguredBlockInsideEntityBox(from)) {
            tags.add("trapdoor");
            velocityTolerance.predictedVerticalMove = verticalDelta;
            if (cancelViolationLimit >= 0) event.setTo(from);
        }
        if (waterExitGraceTicks > 0 && !current.isFromGround() && !current.isToGround()
                && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && Math.abs(verticalDelta) <= 0.35D && verticalDelta <= 0.0D
                && horizontalDistance <= 0.2D && waterCorrectionCount < 5) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("water_fix");
            waterCorrectionCount++;
        }
        if (isLiquidSprintPlausible(current, previous, tags, hasMovementAttribute,
                verticalDelta, velocityTolerance.predictedVerticalMove, horizontalDistance, player)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("swim_use_exit");
        }

        Vector playerVelocity = player.getVelocity();
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_15)
                && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && (environment.intersectsLargerMaterials(new Material[] { Material.HONEY_BLOCK })
                    || BlockEnvironment.hasMaterialAtOrAdjacentTo(
                            from, Material.HONEY_BLOCK))) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            double honeyJump = 0.2D;
            int jumpBoost = PlayerUtilities
                    .potionLevel(this.playerState(), PotionTypes.JUMP_BOOST);
            if (jumpBoost > 0) {
                honeyJump += 0.1D;
                honeyJump += 0.1D * jumpBoost;
            }
            if (verticalDelta <= honeyJump) tags.add("honey_fix");
            if (verticalDelta > 0.0D && Math.abs(playerVelocity.getY() + 0.05D) < 0.002D
                    && verticalDelta > 0.1D && !physics.hasRecentData()) {
                velocityTolerance.predictedVerticalMove = playerVelocity.getY();
                tags.add("honey");
            }
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && environment.collisionMaterials().contains(Material.SCAFFOLDING)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            if (verticalDelta > 0.5D + Math.max(0.0D, playerVelocity.getY())
                    && !physics.hasRecentData() && !tags.contains("step")) {
                velocityTolerance.predictedVerticalMove = 0.5D + Math.max(0.0D, playerVelocity.getY());
                tags.add("scaffold");
            }
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && environment.intersectsLargerMaterials(new Material[] { Material.SWEET_BERRY_BUSH })) {
            tags.add("berry_bush");
            if (verticalDelta <= jumpHeight(player)) velocityTolerance.predictedVerticalMove = verticalDelta;
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_16)
                && environment.intersectsLargerMaterials(
                        BlockEnvironment.getVineMaterials())) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            if (verticalDelta > 0.42D && verticalDelta > playerVelocity.getY() * 2.0D
                    && !BlockEnvironment.hasFa8MaterialAroundOrBelow(from)) {
                velocityTolerance.predictedVerticalMove = playerVelocity.getY();
                tags.add("vine");
            }
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && environment.intersectsLargerMaterials(new Material[] { Material.BAMBOO })
                && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && tags.contains("collision")
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) <= 0.1D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("bamboo_col");
        }
        if (verticalDelta < 0.0D && velocityTolerance.predictedVerticalMove > 0.0D
                && !current.isFromGround() && !current.isToGround()
                && Math.abs(verticalDelta + 0.078D) <= 0.0035D
                && velocityTolerance.predictedVerticalMove < 0.12D
                && previous.getVerticalDistance() >= 0.19D
                && previous.getVerticalDistance() <= 0.29D
                && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)) {
            velocityTolerance.predictedVerticalMove = -0.078D;
            tags.add("fix_collision_fall");
        }

        if (verticalDelta <= -1.0D
                && (double) player.getFallDistance() <= Math.abs(verticalDelta) + 0.422D
                && playerVelocity.getY() > -0.2D && current.isFromGround()
                && current.getHorizontalDistance() < 0.274D
                && !MaterialPolicy.isAirMaterial(
                        BlockEnvironment.getMaterialAt(
                                from.clone().add(0.0D, -1.0D, 0.0D)))) {
            tags.add("reverse_step");
        }
        if (verticalDelta > 0.0D && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && current.isFromGround() && previous.isFromGround()
                && previous.getVerticalDistance() > 0.0D
                && verticalDelta <= previous.getVerticalDistance()
                && BlockEnvironment.isPredicateMaterialAtAndAbove(from)
                && BlockEnvironment.isPredicateMaterialAtAndAbove(to)
                && !BlockEnvironment.hasFa8MaterialAroundOrBelow(from)
                && !previous.getTags().contains("velocity")
                && playerVelocity.getY() < 0.0D && current.getHorizontalDistance() < 0.2D
                && !BlockEnvironment.hasSpecialFallingEntityAbove(from)) {
            tags.add("ground_spoof");
            velocityTolerance.predictedVerticalMove = playerVelocity.getY();
        }
        if (this.playerState().clientVersion() == ClientVersion.V_1_8
                && environment.intersectsCollisionMaterials(new Material[] { Material.ANVIL })
                && verticalDelta <= 0.42D && verticalDelta >= 0.0D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("anvil_fix");
        }
        if (inputState.isPiston() && verticalDelta <= 2.5D) {
            tags.add("piston");
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)) {
            recordGeyserCollisionBoxes(player, from, to, verticalDelta, now, tags);
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && BlockEnvironment.hasFa5qMaterialInSupportNeighborhood(from)
                && Math.abs(verticalDelta) <= 0.5D) {
            tags.add("shulker");
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }
        if (verticalDelta == 0.0D && velocityTolerance.predictedVerticalMove <= -0.062D
                && velocityTolerance.predictedVerticalMove >= -0.063D && current.isFromGround()
                && current.getHorizontalDistance() <= 0.3D
                && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(to)) {
            tags.add("farm");
            velocityTolerance.predictedVerticalMove = verticalDelta;
        }
        if (current.getHorizontalDistance() > 0.11D
                && current.getHorizontalDistance() == previous.getHorizontalDistance()) {
            repeatedHorizontalMoveCount++;
            if (repeatedHorizontalMoveCount >= 2 && !current.isFromGround()
                    && !previous.isFromGround() && !inputState.isPiston()
                    && !BlockEnvironment.isOpenableOrHasSpecialLocationState(from, false)
                    && !BlockEnvironment.hasSpecialFallingEntityAbove(from)
                    && Math.abs(verticalDelta + 0.02D) > 0.001D
                    && Math.abs(current.getHorizontalDistance() - 0.141D) > 0.001D
                    && !tags.contains("ladder") && !tags.contains("liquid")
                    && !tags.contains("geyser") && !physics.hasRecentData()) {
                tags.add("noclip");
                velocityTolerance.predictedVerticalMove = verticalDelta - 1.0D;
            }
        } else {
            repeatedHorizontalMoveCount = 0;
        }
        if (!isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && BlockEnvironment.hasSpecialFallingEntityAbove(from)
                && Math.abs(verticalDelta - velocityTolerance.predictedVerticalMove) <= 0.43D) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("entity_collision");
        }
        if (verticalDelta == 0.0D && velocityTolerance.predictedVerticalMove < -0.07D
                && current.isToGround()
                && Math.abs(velocityTolerance.predictedVerticalMove + 0.078D) <= 0.002D
                && BlockGeometry.hasSpaceOrCollision(to, 0.30000001192092896D)) {
            velocityTolerance.predictedVerticalMove = 0.0D;
            tags.add("ground_pass");
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_17)
                && !isWithinTolerance(verticalDelta, velocityTolerance.predictedVerticalMove)
                && BlockEnvironment.hasMaterialAtOrAdjacentTo(
                        to, Material.POWDER_SNOW)) {
            velocityTolerance.predictedVerticalMove = verticalDelta;
            tags.add("powder_snow");
        }
        if (MaterialPolicy.hasIceType(
                from.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType())) {
            tags.add("on_ice");
        }
        return false;

    }

    private void applyMovementPhysics(org.bukkit.Location from, org.bukkit.Location to,
            MoveData currentAttributes,
            org.bukkit.entity.Player player, org.bukkit.World world,
            WorldAccess movementAttributes, double horizontal,
            java.util.Set<String> tags, VelocityHistory knockback,
            long now, MoveData previousAttributes,
            double vertical, MutableGeometryState movementState,
            PreparedMovementData pistonState,
            CollisionRegion collisionState) {
        float baseSpeed = currentAttributes.getSpeedAttribute();
        if (!player.isSprinting()) {
            baseSpeed *= 0x1.4cccccp0F;
            currentAttributes.setSpeedAttribute(baseSpeed);
        }
    
        org.bukkit.block.Block supportBlock = BlockEnvironment
                .getBlockAt(world,
                        MovementMetrics.toInt(from.getX()),
                        MovementMetrics.toInt(
                                from.getY() - 0.5000001D),
                        MovementMetrics.toInt(from.getZ()));
        velocityTolerance.blockFriction = movementAttributes.getBlockFriction(supportBlock);
        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_20)) {
            velocityTolerance.blockFriction = movementAttributes.updateSupportingBlock(
                    player, currentAttributes.isFromGround(),
                    currentAttributes.getTo().clone()
                            .subtract(currentAttributes.getFrom()));
        }
    
        velocityTolerance.horizontalDrag = 0.91F;
        if (currentAttributes.isFromGround()) {
            velocityTolerance.horizontalDrag = velocityTolerance.blockFriction * 0.91F;
        }
        float speedFactor = 0x1.4d5b12p-3F
                / (velocityTolerance.horizontalDrag
                        * velocityTolerance.horizontalDrag
                        * velocityTolerance.horizontalDrag);
        if (currentAttributes.isFromGround() || tags.contains("tp_jump")) {
            velocityTolerance.horizontalAcceleration = baseSpeed * speedFactor;
            if (tags.contains("jump")) {
                velocityTolerance.horizontalAcceleration += 0.2F;
            }
        } else {
            velocityTolerance.horizontalAcceleration = 0x1.a1cacp-6F;
        }
        boolean recentHorizontalImpulse = false;
        if (strictKnockbackHandling) {
            if (tags.contains("velocity")) {
                velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                        + knockback.sumRecentVerticalValues());
            }
        } else if (tags.contains("velocity")) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + knockback.sumRecentVerticalValues());
        } else if (now - damageWindowStartedAt <= 1500L) {
            recentHorizontalImpulse = previousAttributes.getPredictedHorizontalMove()
                    + (double) velocityTolerance.horizontalAcceleration < horizontal - 0.001D;
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + knockback.sumRecentMagnitudes());
            if (recentHorizontalImpulse) {
                knockback.markOldestRecentAsApplied();
            }
        }
        velocityTolerance.predictedHorizontalMove = previousAttributes.getPredictedHorizontalMove()
                + velocityTolerance.horizontalAcceleration;
        boolean liquidSurfaceReturn = isLiquidJumpAllowed(currentAttributes, previousAttributes,
                tags, vertical, horizontal,
                velocityTolerance.predictedHorizontalMove, player);
        if (liquidSurfaceReturn) {
            tags.add("liquid_surface_return");
        }
        velocityTolerance.noSlowHorizontalLimit = 0.0D;
        if (checkNoSlow) {
            if (movementAttributes.isUsingItem(player)
                    || (player.isSneaking()
                        && !PlayerUtilities.hasSwiftSneak(player))) {
                if (!tags.contains("liquid") && !liquidSurfaceReturn && !knockback.hasRecentData()
                        && !PlayerUtilities.anyHandMatches(
                                player, MovementPhysicsCheck::hasMovementEffect)) {
                    double noSlowMaximum = previousAttributes.getPredictedHorizontalMove()
                            + velocityTolerance.horizontalAcceleration * 0.5D + (double) 0.005F;
                    if (vertical != 0.0D && Math.abs(vertical) < 0.08D) {
                        noSlowMaximum += 0.055D;
                    }
                    noSlowMaximum = Math.max(0.043D, noSlowMaximum);
                    if (waterExitGraceTicks > 0) {
                        noSlowMaximum += 0.06D;
                    }
                    boolean onIce = tags.contains("on_ice");
                    if (onIce && Math.abs(to.getYaw() - from.getYaw()) > 15.0F) {
                        noSlowMaximum += 0.03D;
                    }
                    velocityTolerance.noSlowHorizontalLimit = noSlowMaximum;
                    if (DeveloperCommand.developerMode) {
                        org.bukkit.Bukkit.broadcastMessage(String.format(
                                "%s hand raised, hor: %s, max: %s, noslow: %s",
                                player.getName(), horizontal, noSlowMaximum, noSlowViolationBuffer));
                    }
                    if (horizontal > noSlowMaximum) {
                        tags.add("noslow");
                        if (movementAttributes.isUsingItem(player)) {
                            tags.add("noslow_hand");
                        }
                        if (player.isSneaking()) {
                            tags.add("noslow_sneak");
                        }
                        noSlowViolationBuffer += 2;
                    } else {
                        noSlowViolationBuffer = Math.max(0, noSlowViolationBuffer - 1);
                    }
                } else {
                    noSlowViolationBuffer = Math.max(0, noSlowViolationBuffer - 1);
                }
            } else {
                noSlowViolationBuffer = Math.max(0, noSlowViolationBuffer - 1);
            }
        } else {
            noSlowViolationBuffer = Math.max(0, noSlowViolationBuffer - 1);
        }
    
        if (noSlowViolationBuffer >= 12 && player.isSneaking()) {
            player.setSneaking(false);
            this.cancelMovement = true;
            this.requestSetback = true;
        }
        if (noSlowViolationBuffer >= 12 && movementAttributes.isUsingItem(player)) {
            PlayerUtilities.randomizeHeldSlot(player);
            noSlowViolationBuffer -= 2;
            this.flag("move.noslow", "noslow",
                    diagnosticDetails(
                            "v", String.valueOf(noSlowViolationBuffer), "h",
                            DiagnosticFormat.formatDouble(horizontal), "m",
                            DiagnosticFormat.formatDouble(
                                    velocityTolerance.noSlowHorizontalLimit)), 1);
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && knockback.hasRecentData()
                && tags.contains("jump") && !tags.contains("velocity")
                && !previousAttributes.getTags().contains("jump")) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + knockback.sumRecentVerticalValues());
            tags.add("jump_vel");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && currentAttributes.isFromGround()
                && tags.contains("collision") && !tags.contains("jump") && vertical == 0.0D
                && BlockEnvironment.hasForbiddenMaterialNeighbor(from)) {
            velocityTolerance.horizontalAcceleration += 0.2F;
            tags.add("hor_jump_fix");
        }
        if (horizontal < 0.1D && previousAttributes.getPredictedHorizontalMove() == 0.0D
                && horizontal > velocityTolerance.predictedHorizontalMove) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.1D);
            tags.add("hor_start");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && horizontal < 0.337D
                && currentAttributes.hasPotionEffect(
                        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionTypes.SPEED)
                && BlockEnvironment.hasAnyBlockMatchingConfiguredPredicate(to)) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + Math.abs(horizontal - velocityTolerance.predictedHorizontalMove));
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.01D);
            tags.add("hor_bfs");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && tags.contains("teleport")) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.3D);
            tags.add("hor_tp_fix");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && previousAttributes.getTags().contains("teleport")
                && previousAttributes.getPredictedHorizontalMove() == 0.0D) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.128D);
            tags.add("hor_pre_tp");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && now - this.recentLevitationAt <= 1000L) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + 0.25D + knockback.sumRecentVerticalValues());
            tags.add("levi_bst");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && !currentAttributes.isFromGround()
                && !currentAttributes.isToGround() && !tags.contains("jump")
                && !tags.contains("velocity")
                && (previousAttributes.getTags().contains("jump")
                    || previousAttributes.getTags().contains("jump_vel"))
                && previousAttributes.getTags().contains("velocity")
                && previousAttributes.getHorizontalDistance() < horizontal) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + knockback.sumRecentVerticalValues());
            tags.add("hor_jump_vel_bug");
        }
        if (movementState.volume) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.2D);
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && tags.contains("step")) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.35D);
            tags.add("hor_step");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && previousAttributes.getTags().contains("water")
                && knockback.hasRecentData()) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + knockback.sumRecentVerticalValues());
            tags.add("water_leaving");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && waterExitGraceTicks > 0 && horizontal <= 0.35D) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.06D);
            tags.add("water_exit_hor");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && previousAttributes.isSneaking() && horizontal <= 0.15D) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.15D);
            tags.add("hor_sneak");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && pistonState.isPiston()) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 1.5D);
            tags.add("hor_piston");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove && tags.contains("berry_bush")) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.1D);
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && PlayerUtilities.isNearbyRelevantEntity(player)) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.2D);
            tags.add("en_coll");
        }
        if (previousAttributes.getSpeedAttribute() > currentAttributes.getSpeedAttribute() + 0.05D) {
            tags.add("attr_end");
            this.speedAttributeChangedAt = now;
        }
        if (now - this.speedAttributeChangedAt < 1000L) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration
                    + previousAttributes.getHorizontalDistance());
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && collisionState.intersectsCollisionMaterials(org.bukkit.Material.LADDER)) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.15D);
            tags.add("ladder_bst");
        }
        if (horizontal > velocityTolerance.predictedHorizontalMove
                && !currentAttributes.isFromGround()
                && !currentAttributes.isToGround()
                && previousAttributes.getTags().isEmpty()
                && BlockEnvironment.hasFa5ySolidBlockNearOrBelow(from)
                && vertical >= 0.24D && vertical <= 0.25D
                && BlockEnvironment.hasBoxBlockMatchingPredicate(from)) {
            velocityTolerance.horizontalAcceleration = (float) (velocityTolerance.horizontalAcceleration + 0.32D);
            tags.add("edge_bst");
        }
        velocityTolerance.predictedHorizontalMove = previousAttributes.getPredictedHorizontalMove()
                + velocityTolerance.horizontalAcceleration + 0.0005D;
        if (BlockEnvironment.getMaterialAt(from) == org.bukkit.Material.SOUL_SAND
                && PlayerUtilities.soulSpeed(player) <= 0) {
            if (velocityTolerance.blockFriction == 0.6F) {
                velocityTolerance.predictedHorizontalMove *= vertical == 0.0D ? 0.8D : 0.95D;
                tags.add("hor_soul_slow");
            } else if (velocityTolerance.blockFriction > 0.6F) {
                velocityTolerance.predictedHorizontalMove += 0.3D;
                tags.add("soul_sand_boost");
            }
        }
        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_15)
                && !pistonState.isPiston() && horizontal > 0.25D && horizontal < 0.3D
                && BlockEnvironment.getMaterial(from.getBlock())
                        == org.bukkit.Material.HONEY_BLOCK) {
            velocityTolerance.predictedHorizontalMove *= vertical == 0.0D ? 0.8D : 0.95D;
            tags.add("hor_honey_slow");
        }
        if (!tags.contains("noslow")) {
            velocityTolerance.predictedHorizontalMove = Math.max(0.25D, velocityTolerance.predictedHorizontalMove);
        }
        org.bukkit.util.Vector strafeVector = null;
        if (horizontal < velocityTolerance.predictedHorizontalMove) {
            double deltaX = to.getX() - from.getX();
            double deltaZ = to.getZ() - from.getZ();
            strafeVector = new org.bukkit.util.Vector(deltaX, 0.0D, deltaZ)
                    .subtract(new org.bukkit.util.Vector(previousAttributes.getPredictedX(), 0.0D,
                            previousAttributes.getPredictedZ()));
            if (strafeVector.getX() != 0.0D || strafeVector.getZ() != 0.0D) {
                if (now - lastInventoryClickAt <= 50L
                        && !this.playerState().velocityTracker().hasRecentData()
                        && !PlayerUtilities.isNearbyRelevantEntity(player)
                        && currentAttributes.isFromGround()
                        && !tags.contains("slime") && !tags.contains("honey")
                        && (!(horizontal < (tags.contains("on_ice") ? 0.15D : 0.1D))
                            || player.isSneaking())) {
                    boolean inventoryMoveCondition = previousAttributes.isToGround()
                            && !previousAttributes.isFromGround()
                            && currentAttributes.isToGround();
                    boolean recentSneakStart = now - this.groundedInventoryMoveAt <= 100L;
                    if (!inventoryMoveCondition && !recentSneakStart && checkInventoryMovement) {
                        this.flag("move.invmove", "click inventory while moving", 10);
                        this.cancelMovement = true;
                    } else if (inventoryMoveCondition) {
                        this.groundedInventoryMoveAt = now;
                    }
                }
            }
            if (horizontal > 0.2D && horizontal < 0.35D
                    && !currentAttributes.isFromGround()
                    && !currentAttributes.isToGround()
                    && tags.isEmpty() && previousAttributes.getTags().isEmpty()
                    && collisionState.collisionMaterials().isEmpty()
                    && collisionState.largerCollisionMaterials().isEmpty()) {
                double previousVelocityMagnitude = Math.sqrt(Math.pow(previousAttributes.getPredictedX(), 2.0D)
                        + Math.pow(previousAttributes.getPredictedZ(), 2.0D));
                double strafeMagnitudeFloor = 0x1.f3b645a1cac08p-6;
                double strafeMagnitude = strafeVector.length();
                boolean nearGroundSpeed = (double) currentAttributes.getSpeedAttribute() >= 0.1D
                        && (double) currentAttributes.getSpeedAttribute() <= 0.131D;
                if (strafeMagnitude > strafeMagnitudeFloor
                        && previousVelocityMagnitude > 0.2D && previousVelocityMagnitude < 0.31D
                        && nearGroundSpeed
                        && !PlayerUtilities.isNearbyRelevantEntity(player)) {
                    this.flag("move.stf", String.format("strafe (s: %s, d: %s)",
                            NumberUtils.round3(horizontal),
                            NumberUtils.round3(strafeMagnitude)), 5);
                    this.cancelMovement = true;
                    if (horizontal > 0.3D) {
                        this.requestSetback = true;
                    }
                }
            }
            if (horizontal > 0.2D && horizontal < 0.35D
                    && !currentAttributes.isFromGround() && !currentAttributes.isToGround()
                    && DeveloperCommand.debugStrafe) {
                org.bukkit.Bukkit.broadcastMessage(String.format(
                        "[Strafe/%s] tags: %s, p_tags: %s, coll: %s, b_coll: %s, pred: %s, strafe: %s",
                        player.getName(),
                        DiagnosticFormat.joinIterable(tags),
                        DiagnosticFormat.joinIterable(previousAttributes.getTags()),
                        DiagnosticFormat.joinIterable(collisionState.collisionMaterials()
                                .stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())),
                        DiagnosticFormat.joinIterable(collisionState.largerCollisionMaterials()
                                .stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())),
                        NumberUtils.round3(
                                Math.sqrt(Math.pow(previousAttributes.getPredictedX(), 2.0D)
                                        + Math.pow(previousAttributes.getPredictedZ(), 2.0D))),
                        NumberUtils.round3(strafeVector.length())));
            }
        }
        if (strafeVector != null && horizontal >= 0.25D && (strafeVector.getX() != 0.0D
                || strafeVector.getZ() != 0.0D)
                && vertical == 0.0D && !this.playerState().velocityTracker().hasRecentData()
                && currentAttributes.isFromGround() && player.isSprinting()
                && !tags.contains("teleport") && !collisionState.secondaryFlag()) {
            int[] movementDirection = classifyMovementDirection(to.getYaw(),
                    strafeVector.getX(),
                    strafeVector.getZ());
            if (movementDirection[0] < 0) {
                tags.add("back_sprint");
            }
        }
        if (pistonState.getCollisionCache().secondaryFlag()) {
            float swimSpeed;
            if (this.playerState().clientVersion().isNewerThanOrEquals(
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_13)) {
                if (player.isSprinting() || wasSprinting) {
                    swimSpeed = 0.9F;
                } else {
                    swimSpeed = 0.8F;
                }
            } else {
                swimSpeed = 0.8F;
            }
            float swimAddition = 0.02F;
            float depthStrider = PlayerUtilities.maxArmorEnchantment(
                    player, org.bukkit.enchantments.Enchantment.DEPTH_STRIDER);
            if (depthStrider > 0.0F) {
                tags.add("depth_strider");
            }
            if (!currentAttributes.isFromGround()) {
                depthStrider *= 0.5F;
            }
            if (depthStrider > 0.0F) {
                float depthBase = (!player.isSprinting() && !tags.contains("depth_strider"))
                        ? baseSpeed / 0x1.4cccccp0F : baseSpeed;
                swimSpeed += (0x1.178d52p-1F - swimSpeed) * depthStrider / 3.0F;
                swimAddition += (depthBase - swimAddition) * depthStrider / 3.0F;
                swimAddition += 0.1F;
            }
            if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_13)
                    && PlayerUtilities.potionLevel(this.playerState(),
                            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionTypes.DOLPHINS_GRACE) > 0) {
                swimSpeed = 0.96F;
                tags.add("dolphin_grace");
            }
            float fluidAddition = (float) (Math.max(swimAddition, 0x1.47d806p-6F)
                    + knockback.sumRecentVerticalValues());
            if (isSwimming()) {
                tags.add("swimming");
            } else {
                if (previousAttributes.getTags().contains("swimming")) {
                    this.swimmingStoppedAt = now;
                }
                if (now - this.swimmingStoppedAt <= 1500L) {
                    tags.add("swimming_stop");
                }
            }
            if (this.playerState().clientVersion().isOlderThanOrEquals(
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_8)) {
                fluidAddition += 0.06F;
                tags.add("water_sprint");
            } else if (!player.isSprinting()) {
                fluidAddition += 0.06F;
            }
            double swimBase = isSwimming() ? 0.14D : 0.1D;
            double fluidMaximum = Math.max(swimBase,
                    previousAttributes.getPredictedHorizontalMove() + fluidAddition);
            if (horizontal > fluidMaximum
                    && isSwimmingPose(player, movementAttributes, tags, previousAttributes)) {
                if (horizontal - fluidMaximum <= 0.03D) {
                    swimBase += 0.03D;
                    fluidMaximum = Math.max(swimBase,
                            previousAttributes.getPredictedHorizontalMove() + fluidAddition);
                    tags.add("swim_use_item");
                }
            }
            if (horizontal > fluidMaximum && collisionState.collisionMaterials().contains(org.bukkit.Material.AIR)) {
                fluidAddition += 0x1.581062p-6F;
                tags.add("water_surf");
            }
            if (horizontal > fluidMaximum
                    && (BlockEnvironment.hasAnyLiquidInMovementWindow(from)
                        || BlockEnvironment.hasUnlevelledLiquidWithSolidCardinalNeighbor(from))) {
                fluidAddition += 0.1F;
                tags.add("flowing_water");
            }
            if (horizontal > fluidMaximum && BlockEnvironment.hasAnyBlockMatchingConfiguredPredicate(from)) {
                fluidAddition += 0.1F;
                tags.add("water_clt");
            }
            if (horizontal > fluidMaximum && PlayerUtilities.isNearbyRelevantEntity(player)) {
                fluidAddition += 0.1F;
                tags.add("water_coll");
            }
            if (horizontal > fluidMaximum && tags.contains("depth_strider")
                    && !previousAttributes.getTags().contains("depth_strider_bst")) {
                fluidAddition += 0.1F;
                tags.add("depth_strider_bst");
            }
            fluidMaximum = Math.max(swimBase,
                    previousAttributes.getPredictedHorizontalMove() + fluidAddition);
            boolean invalidWater = horizontal > fluidMaximum;
            if (DeveloperCommand.developerMode) {
                String fluidPrefix = invalidWater ? "§c" : "§a";
                String fluidMessage = String.format(
                                "fluid => hor: %s, max: %s, pred: %s, add: %s， col: %s, vet: %s, attr: %s, diff: %s, spr: %s, f1: %s, y: %s, swim: %s",
                                horizontal, fluidMaximum, previousAttributes.getPredictedHorizontalMove(), fluidAddition,
                                collisionState.collisionMaterials().stream().map(Enum::name)
                                        .collect(java.util.stream.Collectors.joining(",")),
                                DiagnosticFormat.formatDouble(vertical),
                                Float.valueOf(baseSpeed / 0x1.4cccccp0F),
                                DiagnosticFormat.formatDouble(horizontal - fluidMaximum),
                                Boolean.valueOf(player.isSprinting()),
                                Float.valueOf(NumberUtils.round3(swimSpeed)),
                                Double.valueOf(NumberUtils.round3(from.getY())),
                                Boolean.valueOf(isSwimming()));
                org.bukkit.Bukkit.broadcastMessage(this.concatenate(fluidPrefix, fluidMessage));
            }
            if (invalidWater) {
                tags.add("invalid_water");
            }
            velocityTolerance.horizontalDrag = swimSpeed;
            velocityTolerance.horizontalAcceleration = fluidAddition;
            velocityTolerance.predictedHorizontalMove = fluidMaximum;
        }
    }
    
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        wasSprinting = event.isSprinting();
    }

    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        this.playerState().movementContext().capacity = null;
        this.playerState().moveHistory().clear();
        this.playerState().locationFlags().clear();
        this.playerState().movementContext().timeout
                .getCoordinates().clear();
        recentCollisionBoxes = Collections.emptyList();
        lastCollisionUpdate = 0L;
    }

    public void onVehicleExit(VehicleExitEvent event) {
        moveSuppressionTicks = 0;
        Vehicle vehicle = event.getVehicle();
        ignoreVehicleUntilExit = !(vehicle instanceof Boat) && !(vehicle instanceof Horse);
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            List<Entity> passengers = vehicle.getPassengers();
            if (passengers.size() > 1 && passengers.get(1).equals(event.getExited())) {
                ignoreVehicleUntilExit = true;
            }
        }
    }

    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo().equals(event.getFrom())) return;
        lastTeleportAt = MillisecondSampler.currentTimeMillis();
        pendingTeleportVector = java.util.Objects.requireNonNull(event.getTo()).toVector();
        this.playerState().movementContext().capacity = event.getTo().clone();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            lastPearlTeleportAt = MillisecondSampler.currentTimeMillis();
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_12)
                && event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            pendingChorusVector = event.getTo().toVector();
        }
        if (DeveloperCommand.developerMode) {
            DebugLogger.printDebug(
                    formatTeleport(pendingTeleportVector.toString(), event.getCause().name()));
        }
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getItem() == null) return;
        if (event.getItem().getType() != Material.ENDER_PEARL) return;
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            lastUseAt = MillisecondSampler.currentTimeMillis();
        }
    }

    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (event.isFlying()) collisionGraceTicks = 4;
    }

    public void onPlayerRespawn(PlayerRespawnEvent event) {
        this.playerState().moveHistory().clear();
        this.playerState().movementContext().capacity = null;
        recentCollisionBoxes = Collections.emptyList();
        lastCollisionUpdate = 0L;
    }

    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null
                && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            lastInventoryClickAt = MillisecondSampler.currentTimeMillis();
        }
    }

    public void reloadConfiguration() {
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection config =
                this.configuration();
        cancelViolationLimit = config.getInt("cancel_vl", 30);
        allowDamage = config.getBoolean("apply_damage", true);
        checkNoSlow = config.getBoolean("check_noslow", true);
        allowVoidSetback = config.getBoolean("allow_setback_to_void", true);
        strictKnockbackHandling = config.getBoolean("strict_kb_handling", false);
        allowBridge = config.getBoolean("allow_bpe_bridge", false);
        checkInventoryMovement = config.getBoolean("check_inventory_move", true);
    }

    public void onTick() {
        if (++locationFlagCleanupTicks >= 23) {
            locationFlagCleanupTicks = 0;
            this.playerState().locationFlags().removeExpiredEntries();
        }
        if (PlayerUtilities.isInVehicleOrPetVehicle(
                this.player())) {
            moveSuppressionTicks = 0;
            this.playerState().setInVehicle(true);
            Entity vehicle = this.player().getVehicle();
            ignoreVehicleUntilExit = vehicle != null && !(vehicle instanceof Boat) && !(vehicle instanceof Horse);
        } else {
            this.playerState().setInVehicle(false);
        }
    }

    public void onTickStart() {
        lastTickTimestamp = System.currentTimeMillis();
    }

    public void advanceFirstMoveBudget() {
        if (++firstMoveBudget > 15) firstMoveBudget = 15;
    }

    public void setFirstMoveBudget(int ticks) {
        firstMoveBudget = ticks;
    }

    public void onCorrection(DuplicatePosition correction) {
        recentCorrections.add(correction);
    }

    public void onMovementCancelled(boolean cancelled) {
        if (hasRecentTeleport && !cancelled) {
            movementCancellationEndedAt = MillisecondSampler.currentTimeMillis();
        }
        hasRecentTeleport = cancelled;
    }

    private boolean isClimbable(Location location) {
        boolean modernClient = this.playerState().clientVersion()
                .isNewerThan(ClientVersion.V_1_8);
        return BlockEnvironment.isSpecialOrOpenableMaterial(location, modernClient)
                || BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.LADDER)
                || BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.VINE);
    }

    private boolean isLocationSupported(Player player, Location location) {
        Location probe = location.clone().add(0.0D,
                this.playerState().eyeHeight(), 0.0D);
        return BlockEnvironment.hasForbiddenMaterialNeighbor(probe);
    }

    private double minimumJumpVelocity(MoveData movement, Player player) {
        float base = 0.02001F;
        if (this.playerState().clientVersion()
                .isOlderThanOrEquals(ClientVersion.V_1_8) || !player.isSprinting()) {
            base += 0.06F;
        }
        return Math.max(0.1D, movement.getPredictedHorizontalMove() + (double) base);
    }

    private boolean isLiquidJumpAllowed(MoveData previous, MoveData current,
            Set<?> currentTags, double previousY, double verticalDelta, double expectedY,
            Player player) {
        if (!currentTags.contains("liquid") && !previous.isFromGround()
                && !previous.isToGround()) {
            Set<?> movementTags = current.getTags();
            boolean risingInLiquid = movementTags.contains("liquid_up_x")
                    || movementTags.contains("liquid") || movementTags.contains("water_sprint");
            if (!risingInLiquid || previousY >= 0.0D || current.getVerticalDistance() <= 0.0D) return false;
            double minimum = minimumJumpVelocity(current, player);
            return verticalDelta <= Math.max(expectedY, minimum) + 0.0005D;
        }
        return false;
    }

    private boolean isJumpHeightPlausible(Player player, Location from, Location to,
            MoveData attributes, Set<?> tags, double startY, double endY) {
        if (!tags.contains("jump") || !attributes.isFromGround()
                || attributes.isToGround()) return false;
        double maxJump = jumpHeight(player);
        double delta = endY - startY;
        if (startY <= 0.0D || endY > maxJump + 0.02D
                || delta <= 0.0035D || delta > 0.08D) return false;
        if (isLocationSupported(player, from) || isLocationSupported(player, to)) return true;
        return BlockEnvironment.hasOaWeightWithinUnitHeadVolume(player);
    }

    private double jumpHeight(Player player) {
        double base = 0.42D;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_21)) {
            org.bukkit.attribute.AttributeInstance attribute = player.getAttribute(
                    BukkitAttributes.INDEX
                    .jumpStrengthValue());
            if (attribute != null) base = attribute.getValue();
        }
        return base + 0.1D * PlayerUtilities.potionLevel(
                this.playerState(), PotionTypes.JUMP_BOOST);
    }

    private boolean isJumpVelocityInRange(double velocity) {
        return velocity >= -0.16D && velocity <= 0.12D;
    }

    private static boolean isWithinTolerance(double left, double right) {
        return Math.abs(left - right) <= 0.0035D;
    }

    private boolean isLiquidSprintPlausible(MoveData previous,
            MoveData current, Set<?> tags, boolean enabled, double previousY,
            double verticalDelta, double expectedY, Player player) {
        if (this.playerState().clientVersion()
                    .isOlderThan(ClientVersion.V_1_13)
                || !enabled || waterExitGraceTicks <= 0 || tags.contains("liquid")
                || previous.isFromGround() || previous.isToGround()
                || !current.getTags().contains("liquid_up_x")) {
            return false;
        }
        if (current.getVerticalDistance() <= 0.0D || previousY <= 0.0D
                || previousY >= verticalDelta) return false;
        if (previousY > 0.06D || verticalDelta - previousY > 0.06D) return false;
        return expectedY <= Math.max(0.13D, minimumJumpVelocity(current, player) + 0.03D);
    }

    private static boolean isAtMostDifference(double left, double right, double tolerance) {
        return Math.abs(left - right) <= tolerance;
    }

    private static boolean isWithinTimeWindow(long now, Long then) {
        return now - then <= 10000L;
    }

    private static boolean isNearSix(double left, double right) {
        return Math.abs(left - right) < 5.0D;
    }

    private static String formatCoordinates(double x, double z) {
        return new StringBuilder().append(x).append(",").append(z).toString();
    }

    private static String formatRecentVelocity(String prefix, String value) {
        return new StringBuilder(prefix).append("Recent Velocity: ").append(value).toString();
    }

    private static String formatTeleport(String location, String cause) {
        return new StringBuilder("[Tp] ").append(location).append(' ').append(cause).toString();
    }

    private static String formatGeyserId(int id) {
        return new StringBuilder("geyser_x").append(id).toString();
    }

    private static String formatVelocity(double velocity, String suffix) {
        return new StringBuilder().append(velocity).append(suffix).toString();
    }

    private static String formatInvalidMove(String prefix, boolean horizontalValid, boolean verticalValid,
            double horizontal, double expectedHorizontal, double vertical, double expectedVertical,
            String details) {
        return new StringBuilder(prefix).append("Invliad Move: ").append(horizontalValid).append('/')
                .append(verticalValid).append(" | hor: ").append(horizontal).append('/')
                .append(expectedHorizontal).append(" | vet: ").append(vertical).append('/')
                .append(expectedVertical).append(" | ").append(details).toString();
    }

    private static String formatBooleanVelocity(double velocity, boolean flag) {
        return new StringBuilder().append(velocity).append('/').append(flag).toString();
    }

    private static String formatTag(String prefix, String tag, String value, String playerTag, double speed) {
        return new StringBuilder(prefix).append("Tags(").append(tag).append("): ").append(value)
                .append(" | P_TAG: ").append(playerTag).append(" | ").append(speed).toString();
    }

    private static String formatRiptide(double actual, double expected, long elapsed) {
        return new StringBuilder("riptide boost / a:").append(actual).append('/').append(expected)
                .append(", d:").append(elapsed).toString();
    }

    private static String formatCollision(String message) {
        return new StringBuilder("\u00a77[\u00a76Collision\u00a77] \u00a7a").append(message).toString();
    }

    private static String formatTeleportMove(double distance) {
        return new StringBuilder("teleport move: ").append(distance).toString();
    }

    private static String formatIllegalWebMovement(double distance) {
        return new StringBuilder("illegal web movement: ").append(distance).toString();
    }

    private static String formatMovementSummary(String movement, String speedName, double speed,
            float acceleration, String groundState, float yaw, float pitch, double targetY,
            boolean collidedWithEntity, double fromY, double toY, float fallDistance,
            float friction) {
        return new StringBuilder("\u00a77[\u00a76Move\u00a77] ").append(movement)
                .append("-> move: ").append(speedName).append(", speed: ").append(speed)
                .append(", att: ").append(acceleration).append(", ground: ").append(groundState)
                .append(", yaw/pitch: ").append(yaw).append(',').append(pitch)
                .append(", to: ").append(targetY).append(", col_ent: ").append(collidedWithEntity)
                .append(", y: ").append(fromY).append("->").append(toY)
                .append(", fall: ").append(fallDistance).append(", fr: ").append(friction).toString();
    }

    private static String formatLabel(String label, String detail) {
        return new StringBuilder("[~] ").append(label).append(": ").append(detail).toString();
    }

    private static String concatenate(String prefix, String value) {
        return new StringBuilder(prefix).append(value).toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> diagnosticDetails(Object... pairs) {
        return (Map<String, Object>) (Map<?, ?>) CollectionsSupport.linkedMapOf(pairs);
    }

    private static boolean hasMovementEffect(ItemStack item) {
        return MaterialPolicy.spearMaterials.contains(item.getType());
    }

    private boolean isSwimmingPose(Player player, WorldAccess swimmingState,
            Set<?> tags, MoveData observedState) {
        return this.playerState().clientVersion()
                .isNewerThanOrEquals(ClientVersion.V_1_13)
                && tags.contains("liquid") && tags.contains("swimming")
                && observedState.getTags().contains("swimming")
                && swimmingState.isUsingItem(player);
    }

    private boolean isLadderJumpPlausible(Location from, Location to,
            MoveData playerState, MoveData observedState,
            Set<?> tags, double verticalDelta, double initialVelocity) {
        Set<?> observedTags = observedState.getTags();
        if (!observedTags.contains("ladder") || playerState.isFromGround()
                || playerState.isToGround() || tags.contains("jump")
                || tags.contains("velocity")) return false;
        if (!isJumpVelocityInRange(verticalDelta)
                || !isJumpVelocityInRange(observedState.getVerticalDistance())) return false;
        if (initialVelocity < -0.24D || initialVelocity > 0.12D) return false;
        return isClimbable(from) || isClimbable(to);
    }

    private void recordGeyserCollisionBoxes(Player player, Location from, Location to,
            double yaw, long now, Set<String> tags) {
        if (this.playerState().clientVersion()
                .isOlderThan(ClientVersion.V_26_2)) return;
        AxisAlignedBox entityBox = BlockCollisionGeometry
                .getEntityBounds(player);
        double halfWidth = entityBox.width() * 0.5D;
        double height = entityBox.height();
        List<BoundingBoxExpansion> candidates = BlockEnvironment
                .findWaterCollisionBoxes(player.getWorld(),
                        new AxisAlignedBox(from.toVector(), halfWidth, height));
        if (candidates.isEmpty()) {
            candidates = BlockEnvironment.findWaterCollisionBoxes(
                    player.getWorld(), new AxisAlignedBox(to.toVector(), halfWidth, height));
        }
        if (!candidates.isEmpty()) {
            recentCollisionBoxes = candidates;
            lastCollisionUpdate = now;
        } else if (now - lastCollisionUpdate <= 150L) {
            candidates = recentCollisionBoxes;
        }
        if (candidates.isEmpty()) return;

        List<BoundingBoxExpansion> ordered = new java.util.ArrayList<>(candidates);
        ordered.sort(java.util.Comparator.comparingDouble(
                BoundingBoxExpansion::getExpansion));
        double candidateYaw = velocityTolerance.predictedVerticalMove;
        double expandedYaw = candidateYaw;
        int crossedBoxes = 0;
        for (BoundingBoxExpansion box : ordered) {
            if (expandedYaw < box.getExpansion()) {
                expandedYaw += (double) 0.2F;
                crossedBoxes++;
            }
        }
        for (int step = 1; step <= crossedBoxes; step++) {
            double expectedYaw = candidateYaw + step * (double) 0.2F;
            if (isWithinTolerance(yaw, expectedYaw)) {
                velocityTolerance.predictedVerticalMove = yaw;
                tags.add("geyser");
                if (step > 1) tags.add(formatGeyserId(step));
                return;
            }
        }
    }

    private static void markInteraction(MotionVectorState interaction) {
        interaction.setInternalFlag(true);
    }

    private static String formatParenthesized(double value) {
        return new StringBuilder("(").append(value).append(')').toString();
    }

    private static String formatTagList(String prefix, String tags) {
        return new StringBuilder(prefix).append("Tags: ").append(tags).toString();
    }

    private static int[] classifyMovementDirection(float yaw, double x, double z) {
        Vector movement = new Vector(x, 0.0D, z);
        if (movement.length() < (double) 0.01F) return new int[] { 0, 0 };
        movement.normalize();
        double angle = Math.toDegrees(new Vector(-Math.sin(Math.toRadians(yaw)), 0.0D,
                Math.cos(Math.toRadians(yaw))).angle(movement));
        int forward = 0;
        int strafe = 0;
        if (isNearSix(angle, 0.0D) || isNearSix(angle, 45.0D) || isNearSix(angle, 360.0D)) {
            forward = 1;
        } else if (isNearSix(angle, 180.0D) || isNearSix(angle, 135.0D)) {
            forward = -1;
        }
        if (isNearSix(angle, 45.0D) || isNearSix(angle, 135.0D) || isNearSix(angle, 90.0D)) {
            double cross = -Math.sin(Math.toRadians(yaw)) * movement.getZ()
                    - Math.cos(Math.toRadians(yaw)) * movement.getX();
            strafe = cross > 0.0D ? 1 : -1;
        }
        return new int[] { forward, strafe };
    }
}
