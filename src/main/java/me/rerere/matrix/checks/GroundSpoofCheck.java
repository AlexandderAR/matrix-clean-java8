package me.rerere.matrix.checks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.network.IllegalPacketCheck;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.BlockLocationCache;
import me.rerere.matrix.world.ChunkActivityCache;
import me.rerere.matrix.world.BlockGeometry;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.logging.DebugLogger;
import me.rerere.matrix.metrics.RollingStatistics;

/** Detects ground-spoof movement using world geometry and packet state. */
public final class GroundSpoofCheck extends CheckRuntimeState {
    private static final double BLOCK_EDGE_MARGIN = 0.3;
    private static boolean allowBlockPlaceBridge;
    private static boolean checkNoFall;

    private boolean resetPosition;
    private final Cache<Location, Material> recentMaterials;
    private PlayerMoveEvent previousMove;
    private long lastGlideTime;
    private long lastTeleportTime;
    private long lastPlaceTime;
    private long lastGroundSpoofTime;

    public GroundSpoofCheck(PlayerCheckContext playerState) {
        super(playerState);
        this.resetPosition = false;
        this.recentMaterials = CacheBuilder.newBuilder()
                .maximumSize(8)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
        this.previousMove = null;
        setCheckType(CheckCategory.MOVE);
    }

    /** World changes invalidate locations retained by the short-lived cache. */
    @Override
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        recentMaterials.invalidateAll();
    }

    public void handleLocationUpdate(Player player, Location location) throws Throwable {
        PlayerMoveEvent synthetic = new PlayerMoveEvent(player, location, location);
        handleGroundSpoofMove(player, synthetic, true);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (resetPosition) {
            resetPosition = false;
            try {
                Player player = this.playerState().getPlayer();
                Location current = player.getLocation().clone();
                if (IllegalPacketCheck.hasFiniteCoordinates(current)) {
                    handleLocationUpdate(player, current);
                }
            } catch (Throwable failure) {
                GroundSpoofCheck.<RuntimeException>throwUnchecked(failure);
                return;
            }
        }

        Player player = event.getPlayer();
        if (player.isFlying() || PlayerUtilities.isInVehicleOrPetVehicle(player)) {
            previousMove = null;
            return;
        }
        ServerVersion server = Matrix.getServerVersion();
        if (server.isNewerThan(ServerVersion.V_1_8_8) && player.isGliding()) {
            previousMove = null;
            lastGlideTime = MillisecondSampler.currentTimeMillis();
            return;
        }
        if (MillisecondSampler.currentTimeMillis() - lastGlideTime < 2000L) {
            previousMove = null;
            return;
        }
        if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL
                && player.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
            previousMove = null;
            return;
        }
        if (player.isDead() || player.isSleeping()) {
            previousMove = null;
            return;
        }
        if (server.isNewerThanOrEquals(ServerVersion.V_1_13) && player.isSwimming()) {
            previousMove = null;
            return;
        }

        boolean packetGround = PlayerUtilities.hasPlayerEntityFlag(player);
        if (server == ServerVersion.V_1_8_8) {
            if (previousMove != null) {
                try {
                    handleGroundSpoofMove(player, previousMove, packetGround);
                } catch (Throwable failure) {
                    GroundSpoofCheck.<RuntimeException>throwUnchecked(failure);
                }
            }
            previousMove = new PlayerMoveEvent(player, event.getFrom(), event.getTo().clone());
        } else {
            try {
                handleGroundSpoofMove(player, event, packetGround);
            } catch (Throwable failure) {
                GroundSpoofCheck.<RuntimeException>throwUnchecked(failure);
            }
        }
    }

    private void handleGroundSpoofMove(Player player, PlayerMoveEvent event, boolean packetGround) throws Throwable {
        if (!checkNoFall || !packetGround) return;
        if (MillisecondSampler.currentTimeMillis() - lastTeleportTime < 1500L) return;

        Location destination = event.getTo();
        if (destination.getWorld() == null) return;
        Block destinationBlock = destination.getBlock();
        Material destinationMaterial = destinationBlock.getType();
        if (MaterialPolicy.isAirMaterial(destinationMaterial)) {
            Block below = destinationBlock.getRelative(BlockFace.DOWN);
            Material belowMaterial = below.getType();
            if (destinationMaterial != Material.AIR) recentMaterials.put(destination, destinationMaterial);
            if (belowMaterial != Material.AIR) recentMaterials.put(below.getLocation(), belowMaterial);
            if (!MaterialPolicy.isAirMaterial(belowMaterial)
                    && below.getType().isSolid() && !below.isLiquid()) return;
        }

        PlayerCheckContext playerState = this.playerState();
        MutableGeometryState geometry = playerState.movementContext();
        Vector movementDelta = destination.toVector().subtract(event.getFrom().toVector());
        if (isNeutralMovement(player, event, movementDelta)
                || isSmallGroundMovement(event, movementDelta)) return;
        if (BlockGeometry.collidesWithShape(destination, playerState)) return;

        boolean specialAirCase = false;
        String supportType = null;
        boolean foundStandableBlock = false;
        for (Block candidate : BlockEnvironment.getBlocksAroundLocation(destination)) {
            if (MaterialPolicy.isAirMaterial(candidate.getType())) continue;
            if (BlockCollisionGeometry.hasNoCollision(candidate)) continue;
            if (!BlockCollisionGeometry.isStandingClearance(candidate, destination)) continue;
            foundStandableBlock = true;
            DebugLogger.printDebug(standableBlockMessage(candidate.getType().name()));
            break;
        }
        if (!foundStandableBlock && isNearCachedGhostBlock(destination)) {
            specialAirCase = true;
            supportType = "air";
        }
        DebugLogger.printDebug(groundSpoofSummary(player.getName(), specialAirCase, supportType, foundStandableBlock));

        if (!specialAirCase) return;
        Location origin = event.getFrom();
        if (hasNearbyBlock(destination) || hasNearbyBlock(origin)
                || BlockEnvironment.hasFa5kMaterialInMovementBox(destination)) return;

        boolean specialMovement = BlockEnvironment.hasSpecialFallingEntityAbove(destination);
        if (specialMovement) lastGroundSpoofTime = MillisecondSampler.currentTimeMillis();
        ChunkActivityCache chunkCache = Matrix.getInstance().getChunkManager();
        double tps = RollingStatistics.metric();
        long timeout = (Double.isNaN(tps) || tps >= 18.0) ? 10000L : 15000L;
        boolean chunkActive = chunkCache.wasRecordedRecently(destination.getChunk(), timeout);
        if (specialMovement || chunkActive
                || MillisecondSampler.currentTimeMillis() - lastGroundSpoofTime <= 500L) return;

        boolean sameLocationObject = destination == origin;
        boolean descending = destination.getY() - origin.getY() < -0.4;
        String detail = groundSpoofDetail(
                DiagnosticFormat.movementSigns(event),
                DiagnosticFormat.vector(movementDelta),
                DiagnosticFormat.formatLocation(destination),
                supportType, sameLocationObject);
        boolean violated = this.flag("move.gf", detail, descending ? 35 : 5);
        if (violated) {
            BlockEnvironment.updatePlayerAdjacentBlockStates(destination);
            geometry.opacity = true;
            geometry.margin = sameLocationObject;
        }
    }

    public void reloadConfiguration() {
        ConfigurationSection config = super.configuration();
        allowBlockPlaceBridge = config.getBoolean("allow_bpe_bridge", false);
        config = super.configuration();
        checkNoFall = config.getBoolean("check_nofall", true);
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        PlayerCheckContext state = this.playerState();
        state.movementContext().capacity = null;
    }

    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        ServerVersion server = Matrix.getServerVersion();
        if (server == ServerVersion.V_1_8_8 && event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            previousMove = null;
        }
        if (GeometryMath.horizontalManhattanDistance(event.getFrom(), event.getTo()) > 20.0) {
            lastTeleportTime = MillisecondSampler.currentTimeMillis();
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || !MaterialPolicy.containsTrapdoor(clicked.getType())) return;
        BlockLocationCache blockState = Matrix.getInstance().getClientBlockManager();
        World world = event.getPlayer().getWorld();
        blockState.queueAir(world, clicked.getX(), clicked.getY(), clicked.getZ());
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        BlockLocationCache blockState = Matrix.getInstance().getClientBlockManager();
        blockState.queueAir(event.getPlayer().getWorld(), broken.getX(), broken.getY(), broken.getZ());
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        Player player = event.getPlayer();
        if (placed.getLocation().equals(event.getBlockAgainst().getLocation())) return;
        if (event.isCancelled()) {
            clearBlockCache(player, placed);
            return;
        }
        ServerVersion server = Matrix.getServerVersion();
        if (server.isOlderThan(ServerVersion.V_26_3)
                && playerStateVersionIsOlderThan26_3(this.playerState())
                && MillisecondSampler.currentTimeMillis() - lastPlaceTime > 1000L
                && BlockEnvironment.isPredicateMaterialAtAndAbove(player.getLocation())
                && !BlockEnvironment.hasFa5ySolidBlockNearOrBelow(player.getLocation())) {
            return;
        }
        clearBlockCache(player, placed);
    }

    private static boolean playerStateVersionIsOlderThan26_3(PlayerCheckContext state) {
        return state.clientVersion().isOlderThan(ClientVersion.V_26_3);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable failure) throws T {
        throw (T) failure;
    }

    private static void clearBlockCache(Player player, Block block) {
        BlockLocationCache blockState = Matrix.getInstance().getClientBlockManager();
        blockState.queueAir(player.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    private static boolean isStandableMaterial(Block block) {
        Material material = BlockEnvironment.getMaterial(block);
        return MaterialPolicy.containsBedBridge(material);
    }

    private boolean isNeutralMovement(Player player, PlayerMoveEvent event, Vector delta) {
        if (player.isSleeping()) return true;
        if (Math.abs(delta.getY()) > 0.09) return false;
        double horizontal = Math.sqrt(delta.getX() * delta.getX() + delta.getZ() * delta.getZ());
        if (horizontal > 0.03) return false;
        return BlockEnvironment.hasFa5mMaterialNearPlayer(player)
                || hasGroundSupport(event.getFrom())
                || hasGroundSupport(event.getTo());
    }

    private boolean isSmallGroundMovement(PlayerMoveEvent event, Vector delta) {
        if (delta.getY() < -0.2 || delta.getY() > 0.2) return false;
        double horizontal = Math.sqrt(delta.getX() * delta.getX() + delta.getZ() * delta.getZ());
        return !(horizontal > 0.35)
                && (isExcludedSupportLocation(event.getFrom()) || isExcludedSupportLocation(event.getTo()));
    }

    private static boolean hasGroundSupport(Location location) {
        Block feet = location.getBlock();
        Block below = feet.getRelative(BlockFace.DOWN);
        if (isStandableMaterial(feet) || isStandableMaterial(below)) return true;
        for (BlockFace face : BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES) {
            if (isStandableMaterial(feet.getRelative(face))
                    || isStandableMaterial(below.getRelative(face))) return true;
        }
        return false;
    }

    private boolean hasNearbyBlock(Location location) {
        BlockLocationCache blockState = Matrix.getInstance().getClientBlockManager();
        for (Block block : BlockEnvironment.getBlocksInMovementWindow(location)) {
            if (!blockState.contains(block)) continue;
            if (allowBlockPlaceBridge || isInsideBlock(block, location.toVector())) return true;
        }
        return false;
    }

    private boolean isNearCachedGhostBlock(Location location) throws Throwable {
        ServerVersion server = Matrix.getServerVersion();
        if (server.isNewerThanOrEquals(ServerVersion.V_1_14)
                && BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.SCAFFOLDING)) return false;
        if (server.isNewerThanOrEquals(ServerVersion.V_1_12)
                && BlockEnvironment.hasFa5qMaterialInSupportNeighborhood(location)) return false;
        if (BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.LAVA)) return false;
        if (server.isNewerThanOrEquals(ServerVersion.V_1_17)
                && BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.POWDER_SNOW)) return false;

        Player player = this.playerState().getPlayer();
        double horizontalReach = BlockCollisionGeometry.getEntityBounds(player).width() + 0.502;
        for (Map.Entry<Location, Material> entry : recentMaterials.asMap().entrySet()) {
            Location remembered = entry.getKey();
            if (entry.getValue() == Material.AIR || remembered.getBlock().getType() != Material.AIR) continue;
            if (!(remembered.getY() < location.getY())
                    || !(remembered.getY() >= location.getY() - 1.5)) continue;
            if (Math.abs(remembered.getX() + 0.5 - location.getX()) < horizontalReach
                    && Math.abs(remembered.getZ() + 0.5 - location.getZ()) < horizontalReach) return false;
        }
        return true;
    }

    private boolean isExcludedSupportLocation(Location location) {
        boolean newerClient = this.playerState().clientVersion().isNewerThan(ClientVersion.V_1_8);
        if (BlockEnvironment.isSpecialOrOpenableMaterial(location, newerClient)) return true;
        return BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.LADDER)
                || BlockEnvironment.hasMaterialAtOrAdjacentTo(location, Material.VINE);
    }

    private static String groundSpoofSummary(String playerName, boolean cheat, String position, boolean canStand) {
        return new StringBuilder(playerName)
                .append(" GroundSpoofCheck: realGround != toGround, cheat = ")
                .append(cheat).append(", pt = ").append(position)
                .append(", can_stand = ").append(canStand).toString();
    }

    private static String groundSpoofDetail(
            String expectedMovement, String actualMovement, String location, String supportMaterial,
            boolean packetGround) {
        return new StringBuilder("ground spoof (expect: ").append(expectedMovement)
                .append(", move: ").append(actualMovement)
                .append(", at: ").append(location)
                .append(", type: ").append(supportMaterial)
                .append(", pkt: ").append(packetGround).append(")").toString();
    }

    private static String standableBlockMessage(String blockType) {
        return new StringBuilder("Standable block = ").append(blockType).toString();
    }

    /** Strict interior check with the bytecode's 0.3 block-edge margin. */
    private static boolean isInsideBlock(Block block, Vector point) {
        return point.getX() > block.getX() - BLOCK_EDGE_MARGIN
                && point.getZ() > block.getZ() - BLOCK_EDGE_MARGIN
                && point.getX() < block.getX() + 1 + BLOCK_EDGE_MARGIN
                && point.getZ() < block.getZ() + 1 + BLOCK_EDGE_MARGIN;
    }

    /** ASM records the current wall-clock value in the per-instance placement timestamp. */
    @Override
    public void onPlayerAnimation(org.bukkit.event.player.PlayerAnimationEvent event) {
        lastPlaceTime = MillisecondSampler.currentTimeMillis();
    }


}
