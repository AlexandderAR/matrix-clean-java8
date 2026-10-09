package me.rerere.matrix.player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.callbacks.RunnableFunctionReference;
import me.rerere.matrix.callbacks.CancelableAction;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.MutableGeometryState;
import me.rerere.matrix.checks.VerboseDetail;
import me.rerere.matrix.checks.entity.TrackedEntityRegistry;
import me.rerere.matrix.checks.movement.PacketMovementCondition;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.compat.BedrockPlayerSupport;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.entity.EntityMetadataState;
import me.rerere.matrix.network.packet.TransactionPingQueue;
import me.rerere.matrix.network.VelocityHandler;
import me.rerere.matrix.player.model.MovementCheck;
import me.rerere.matrix.player.model.PacketMovementTracker;
import me.rerere.matrix.player.model.PreparedMovementData;
import me.rerere.matrix.prediction.MovementCandidateGenerator;
import me.rerere.matrix.replay.PlayerMovementState;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.User;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;
import me.rerere.matrix.util.EnumSelection;
import me.rerere.matrix.util.ObjectSampleBuffer;
import me.rerere.matrix.util.ProgressGate;
import me.rerere.matrix.world.LocationFlagCache;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Player identity, packet timing, movement state and per-category bypasses. */
public final class PlayerCheckContext {
    private final Map<CheckCategory, Long> bypassUntil;
    private long lastTransactionMark;
    private final PacketMovementCondition interactionTracker;
    private final TransactionPingQueue transactionQueue;
    private Player player;
    private final ViolationTotals playerStorage;
    private UUID verboseRecipient;
    private boolean gliding;
    private GameMode gameMode;
    private boolean collidingBelow;
    private boolean verboseEnabled;
    private long previousLatency;
    private String clientBrand;
    private boolean debugEnabled;
    private boolean permissionBypass;
    private GameMode previousGameMode;
    private boolean previouslyGliding;
    private boolean inVehicle;
    private final VelocityHistory velocityTracker;
    private final EnumSelection moveHistory;
    private long smoothedLatency;
    private final ObjectSampleBuffer packetHistory;
    private long latency;
    private final CheckRegistry checks;
    private final CancelableAction pingManager;
    private final PacketMovementTracker combatTracker;
    private long lastGlidingTime;
    private final ProgressGate packetBuffer;
    private final ConcurrentHashMap<CheckCategory, Boolean> categoryBypasses;
    private final ConcurrentLinkedQueue<Runnable> pendingActions;
    private Location lastLocation;
    private final String playerName;
    private final VelocityHandler inventoryTracker;
    private final TrackedEntityRegistry motionTracker;
    private boolean bedrockClient;
    private final Map<PotionType, Object> potionEffects;
    private final LocationFlagCache locationFlags;
    private final User packetUser;
    private MutableGeometryState movementContext;
    private boolean checksActive;
    private final UUID playerId;
    private final EntityMetadataState packetState;
    private final PlayerMovementState movementState;
    private long lastActivityTime;

    public PlayerCheckContext(UUID playerId, String playerName, User packetUser) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.packetUser = packetUser;
        player = Bukkit.getPlayerExact(this.playerName);
        checks = new CheckRegistry(this);
        playerStorage = new ViolationTotals(this.playerId);
        pingManager = new CancelableAction(this);
        pendingActions = new ConcurrentLinkedQueue<>();
        bypassUntil = new EnumMap<>(CheckCategory.class);
        categoryBypasses = new ConcurrentHashMap<>();
        velocityTracker = new VelocityHistory();
        inventoryTracker = new VelocityHandler(this);
        locationFlags = new LocationFlagCache();
        moveHistory = new EnumSelection();
        combatTracker = new PacketMovementTracker(this);
        interactionTracker = new PacketMovementCondition(this);
        potionEffects = new HashMap<>();
        PlayerMovementState initialMovementState = new PlayerMovementState();
        initialMovementState.resetLocation(getPlayer().getLocation());
        movementState = initialMovementState;
        packetState = new EntityMetadataState();
        motionTracker = new TrackedEntityRegistry(this);
        movementCheck = new MovementCandidateGenerator();
        movementContext = new MutableGeometryState();
        checksActive = true;
        transactionQueue = new TransactionPingQueue(this);
        long initialTransactionMark = KotlinTimeAccess.markNow();
        long bypassWindow = DurationKt.toDuration(60, DurationUnit.SECONDS);
        lastTransactionMark = KotlinTimeAccess.subtractDuration(initialTransactionMark, bypassWindow);
        smoothedLatency = KotlinTimeAccess.zeroDuration();
        latency = KotlinTimeAccess.zeroDuration();
        previousLatency = KotlinTimeAccess.zeroDuration();
        packetHistory = new ObjectSampleBuffer(VerboseDetail.class, 3);
        inVehicle = PlayerUtilities.isInVehicleOrPetVehicle(player);
        gameMode = getPlayer().getGameMode();
        gliding = serverVersion().isNewerThan(ServerVersion.V_1_8_8) && getPlayer().isGliding();
        previouslyGliding = gliding;
        packetBuffer = new ProgressGate(55);
        boolean sneaking = getPlayer().isSneaking();
        boolean sprinting = getPlayer().isSprinting();
        packetState.resetFromSnapshot(sneaking, sprinting, false, gliding);
        movementState.setEyeHeight(eyeHeight());
        if (BedrockPlayerSupport.geyserEnabled
                && BedrockPlayerSupport.isBedrockPlayer(getPlayer())) {
            bedrockClient = true;
            matrix().getLogger().info("(!) Player " + this.playerName
                    + " is connecting with Bedrock client, Matrix will not detect him");
        }
        if (this.playerId.toString().startsWith("00000000-0000-")) {
            bedrockClient = true;
            matrix().getLogger().info("(!) Player " + this.playerName
                    + " is connecting with Bedrock client, Matrix will not detect him (Detected by UUID/FloodGate)");
        }
        pingManager.schedule();
    }

    private final MovementCheck movementCheck;

    private static Matrix matrix() { return Matrix.getInstance(); }
    private static ServerVersion serverVersion() { return Matrix.getServerVersion(); }

    public final Player cachedPlayer() { return player; }
    public final Player getPlayer() { return resolvePlayer(); }
    private Player resolvePlayer() {
        Player cached = player;
        if (cached != null) return cached;
        Player online = Bukkit.getPlayer(playerId);
        if (online == null) {
            matrix().getLogger().warning("(!) Player " + playerName + " is not online, but he triggers PlayerJoinEvent");
            Runnable removal = () -> matrix().getPlayerDataManager().remove(playerId);
            FoliaSupport.INSTANCE.runTaskAsync(new RunnableFunctionReference(removal));
        }
        player = online;
        Player result = player;
        Intrinsics.checkNotNull(result);
        return result;
    }
    public final void setPlayer(Player player) { this.player = player; }
    public final UUID playerId() { return playerId; }
    public final String playerName() { return playerName; }
    public final String clientBrand() { return clientBrand; }
    public final void setClientBrand(String brand) { clientBrand = brand; }
    public final UUID verboseRecipient() { return verboseRecipient; }
    public final void setVerboseRecipient(UUID recipient) { verboseRecipient = recipient; }
    public final boolean verboseEnabled() { return verboseEnabled; }
    public final void setVerboseEnabled(boolean enabled) { verboseEnabled = enabled; }
    public final GameMode gameMode() { return gameMode; }
    public final void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }
    public final GameMode previousGameMode() { return previousGameMode; }
    public final void setPreviousGameMode(GameMode gameMode) { previousGameMode = gameMode; }
    public final Location lastLocation() { return lastLocation; }
    public final void setLastLocation(Location location) { lastLocation = location; }
    public final boolean isGliding() { return gliding; }
    public final boolean previouslyGliding() { return previouslyGliding; }
    public final void setPreviouslyGliding(boolean gliding) { previouslyGliding = gliding; }
    public final void setGliding(boolean gliding) {
        previouslyGliding = this.gliding;
        this.gliding = gliding;
        packetState.setGliding(gliding);
    }
    public final boolean isInVehicle() { return inVehicle; }
    public final void setInVehicle(boolean inVehicle) { this.inVehicle = inVehicle; }
    public final boolean isCollidingBelow() { return collidingBelow; }
    public final void setCollidingBelow(boolean colliding) { collidingBelow = colliding; }
    public final boolean debugEnabled() { return debugEnabled; }
    public final void setDebugEnabled(boolean enabled) { debugEnabled = enabled; }
    public final boolean permissionBypass() { return permissionBypass; }
    public final void setPermissionBypass(boolean bypass) { permissionBypass = bypass; }
    public final boolean isBedrockClient() { return bedrockClient; }
    public final void setBedrockClient(boolean bedrockClient) { this.bedrockClient = bedrockClient; }
    public final boolean checksActive() { return checksActive; }
    public final void setChecksActive(boolean active) { checksActive = active; }
    public final boolean isSneaking() { return packetState.isSneaking(); }
    public final boolean isSwimming() { return packetState.isSwimming(); }
    public final boolean isSprinting() { return packetState.isSprinting(); }
    public final void setSneaking(boolean sneaking) {
        packetState.setSneaking(sneaking);
    }
    public final void setSprinting(boolean sprinting) {
        packetState.setSprinting(sprinting);
    }
    public final void setSwimming(boolean swimming) {
        packetState.setSwimming(swimming);
    }
    public final ObjectSampleBuffer packetHistory() { return packetHistory; }
    public final MovementCheck movementCheck() { return movementCheck; }
    public final MutableGeometryState movementContext() { return movementContext; }
    public final LocationFlagCache locationFlags() { return locationFlags; }
    public final CheckRegistry checks() { return checks; }
    public final EnumSelection moveHistory() { return moveHistory; }
    public final PlayerMovementState movementState() { return movementState; }
    public final CancelableAction pingManager() { return pingManager; }
    public final ProgressGate packetBuffer() { return packetBuffer; }
    public final EntityMetadataState packetState() { return packetState; }
    public final VelocityHandler inventoryTracker() { return inventoryTracker; }
    public final VelocityHistory velocityTracker() { return velocityTracker; }
    public final ViolationTotals playerStorage() { return playerStorage; }
    public final PacketMovementCondition interactionTracker() { return interactionTracker; }
    public final PacketMovementTracker combatTracker() { return combatTracker; }
    public final TransactionPingQueue transactionQueue() { return transactionQueue; }
    public final TrackedEntityRegistry motionTracker() { return motionTracker; }
    public final User packetUser() { return packetUser; }
    public final ClientVersion clientVersion() { return packetUser.getClientVersion(); }
    public final int protocolVersion() { return clientVersion().getProtocolVersion(); }
    public final Map<PotionType, Object> potionEffects() { return potionEffects; }
    public final boolean hasPotionEffect(PotionType type) { return potionEffects.containsKey(type); }
    public final ConcurrentHashMap<CheckCategory, Boolean> categoryBypasses() { return categoryBypasses; }
    public final long lastTransactionMark() { return lastTransactionMark; }
    public final long smoothedLatency() { return smoothedLatency; }
    public final long latencyMilliseconds() { return KotlinTimeAccess.wholeMilliseconds(smoothedLatency()); }
    public final long latency() { return latency; }
    public final long previousLatency() { return previousLatency; }
    public final long lastGlidingTime() { return lastGlidingTime; }
    public final void setLastGlidingTime(long value) { lastGlidingTime = value; }
    public final long lastActivityTime() { return lastActivityTime; }
    public final void setLastActivityTime(long value) { lastActivityTime = value; }
    public final void updateGameMode() {
        gameMode = getPlayer().getGameMode();
        if (gliding) lastGlidingTime = System.currentTimeMillis();
    }
    public final void resetMovementContext() {
        PreparedMovementData retained = movementContext.index;
        movementContext = new MutableGeometryState();
        movementContext.index = retained;
    }
    public final void enqueueAction(Runnable action) { pendingActions.add(action); }
    public final void processActions() {
        if (pendingActions.isEmpty()) return;
        for (;;) {
            Runnable action = pendingActions.poll();
            if (action == null) return;
            try { action.run(); } catch (Exception failure) { failure.printStackTrace(); }
        }
    }
    public final void enqueueTransaction(Function0<?> action) {
        transactionQueue.workWithCallbacks(action, () -> Unit.INSTANCE);
    }
    public final void updateLatency(long transactionMark) {
        lastTransactionMark = transactionMark;
        long elapsedLatency = KotlinTimeAccess.elapsedNow(transactionMark);
        smoothedLatency = KotlinTimeAccess.addDurations(KotlinTimeAccess.multiplyDuration(smoothedLatency, 0.7),
                KotlinTimeAccess.multiplyDuration(elapsedLatency, 0.3));
        previousLatency = latency;
        latency = elapsedLatency;
    }
    public final void sendPing(int sequence) {
        ServerVersion version = serverVersion();
        PacketWrapper<?> packet = version.compareTo(ServerVersion.V_1_16_5) <= 0
                && version.compareTo(ServerVersion.V_1_8_8) >= 0
                ? new WrapperPlayServerWindowConfirmation(0, (short) sequence, false)
                : new WrapperPlayServerPing(sequence);
        packetUser.writePacket(packet);
    }
    public final boolean shouldBypass(CheckCategory category) {
        Long until = bypassUntil.get(category);
        if (until != null && until >= System.currentTimeMillis()) return true;
        if (bedrockClient) return true;
        if (MatrixConfiguration.disableBypassPermission) return false;
        if (permissionBypass) return true;
        Boolean bypass = categoryBypasses.get(category);
        return bypass != null ? bypass : false;
    }
    public final void bypassUntil(CheckCategory category, long until) { bypassUntil.put(category, until); }
    public final void markBedrockClient() {
        if (bedrockClient) return;
        bedrockClient = true;
        matrix().getLogger().info("(!) Player " + playerName
                + " is connecting with Bedrock client, Matrix will not detect him (Detected by MatrixBungee)");
    }
    public final double eyeHeight() {
        if (gliding || isSwimming()) return 0.4;
        if (isSneaking()) return clientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? 1.27 : 1.54;
        return 1.62;
    }
    public final List<Double> possibleEyeHeights() {
        if (clientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            return CollectionsKt.mutableListOf(new Double[] {0.4, 1.27, 1.62});
        }
        if (clientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            return CollectionsKt.mutableListOf(new Double[] {0.4, 1.54, 1.62});
        }
        return CollectionsKt.listOf(new Double[] {1.54, 1.62});
    }
}
