package me.rerere.matrix.combat;

import java.util.List;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.config.DefaultSettingProperty;
import me.rerere.matrix.config.FeatureSwitch;
import me.rerere.matrix.listeners.CallbackEventFactory;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.network.packet.PacketRegistration;
import me.rerere.matrix.network.packet.PacketWrapperSnapshot;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.RecentElements;
import me.rerere.matrix.util.time.TimeMarkOperations;
import me.rerere.matrix.thirdparty.kotlin.reflect.KProperty;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;

/** Combat check for configurable packet timing and action flaws A through E. */
public final class CombatFlawCheck extends CheckRuntimeState {
    private final DefaultSettingProperty flawTypeAEnabledDelegate;
    private final DefaultSettingProperty flawTypeAVlDelegate;
    private final DefaultSettingProperty flawTypeBEnabledDelegate;
    private final DefaultSettingProperty flawTypeBVlDelegate;
    private final DefaultSettingProperty flawTypeCEnabledDelegate;
    private final DefaultSettingProperty flawTypeCVlDelegate;
    private final DefaultSettingProperty flawTypeDEnabledDelegate;
    private final DefaultSettingProperty flawTypeDVlDelegate;
    private final DefaultSettingProperty flawTypeEEnabledDelegate;
    private final DefaultSettingProperty flawTypeEVlDelegate;
    private final PacketRegistration actionCounterA;
    private final PacketRegistration actionCounterB;

    private double previousX, previousY, previousZ;
    private double previousDeltaX, previousDeltaY, previousDeltaZ;
    private int pendingEntityId, pendingActionCount, pendingCooldown;
    private int targetEntityId;
    private long lastSprintAction;
    private boolean cancelNextConsume;
    private static boolean combatChecksEnabled;
    private static final KProperty[] CONFIG_PROPERTIES = new KProperty[] {
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeAEnable", "getFlawTypeAEnable()Z", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeAVl", "getFlawTypeAVl()I", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeBEnable", "getFlawTypeBEnable()Z", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeBVl", "getFlawTypeBVl()I", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeCEnable", "getFlawTypeCEnable()Z", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeCVl", "getFlawTypeCVl()I", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeDEnable", "getFlawTypeDEnable()Z", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeDVl", "getFlawTypeDVl()I", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeEEnable", "getFlawTypeEEnable()Z", 0)),
        Reflection.property1(new PropertyReference1Impl(CombatFlawCheck.class, "flawTypeEVl", "getFlawTypeEVl()I", 0))
    };
    public static final FeatureSwitch HEALTH_MARKER = new FeatureSwitch(null);

    public CombatFlawCheck(PlayerCheckContext playerData) {
        super(playerData, CheckCategory.KILLAURA);
        flawTypeAEnabledDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_a.enable", Boolean.TRUE);
        flawTypeAVlDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_a.vl", Integer.valueOf(3));
        flawTypeBEnabledDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_b.enable", Boolean.TRUE);
        flawTypeBVlDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_b.vl", Integer.valueOf(10));
        flawTypeCEnabledDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_c.enable", Boolean.TRUE);
        flawTypeCVlDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_c.vl", Integer.valueOf(10));
        flawTypeDEnabledDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_d.enable", Boolean.TRUE);
        flawTypeDVlDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_d.vl", Integer.valueOf(10));
        flawTypeEEnabledDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_e.enable", Boolean.TRUE);
        flawTypeEVlDelegate = CallbackEventFactory.createSecondary(this, "modules.flaw.type_e.vl", Integer.valueOf(5));
        actionCounterA = new PacketRegistration(3);
        actionCounterB = new PacketRegistration(4);
    }

    public boolean getFlawTypeAEnable() {
        return ((Boolean) flawTypeAEnabledDelegate.getValue(this, CONFIG_PROPERTIES[0])).booleanValue();
    }

    public int getFlawTypeAVl() {
        return ((Number) flawTypeAVlDelegate.getValue(this, CONFIG_PROPERTIES[1])).intValue();
    }

    public boolean getFlawTypeBEnable() {
        return ((Boolean) flawTypeBEnabledDelegate.getValue(this, CONFIG_PROPERTIES[2])).booleanValue();
    }

    public int getFlawTypeBVl() {
        return ((Number) flawTypeBVlDelegate.getValue(this, CONFIG_PROPERTIES[3])).intValue();
    }

    public boolean getFlawTypeCEnable() {
        return ((Boolean) flawTypeCEnabledDelegate.getValue(this, CONFIG_PROPERTIES[4])).booleanValue();
    }

    public int getFlawTypeCVl() {
        return ((Number) flawTypeCVlDelegate.getValue(this, CONFIG_PROPERTIES[5])).intValue();
    }

    public boolean getFlawTypeDEnable() {
        return ((Boolean) flawTypeDEnabledDelegate.getValue(this, CONFIG_PROPERTIES[6])).booleanValue();
    }

    public int getFlawTypeDVl() {
        return ((Number) flawTypeDVlDelegate.getValue(this, CONFIG_PROPERTIES[7])).intValue();
    }

    public boolean getFlawTypeEEnable() {
        return ((Boolean) flawTypeEEnabledDelegate.getValue(this, CONFIG_PROPERTIES[8])).booleanValue();
    }

    public int getFlawTypeEVl() {
        return ((Number) flawTypeEVlDelegate.getValue(this, CONFIG_PROPERTIES[9])).intValue();
    }

    private static String formatCastError(Class<?> actual, Object expected) {
        return new StringBuilder("Cannot cast ").append(actual).append(" to ").append(expected).toString();
    }

    public static boolean combatChecksEnabled() { return combatChecksEnabled; }
    public static void setCombatChecksEnabled(boolean enabled) { combatChecksEnabled = enabled; }

    /** Handles an entity action and arms cancellation for a matching consume event. */
    public void onEntityAction(Entity entity, List<?> ignoredActions) {
        Player player = player();
        if (!Matrix.Companion.matrixInstance().getNmsManager().visitor().isUsingItem(player)) return;
        PlayerCheckContext data = playerData();
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion version = data.clientVersion();
        if (!version.isNewerThanOrEquals(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_12)) return;
        if (!playerData().clientVersion().isOlderThan(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_21)) return;
        Map<String, Object> details = me.rerere.matrix.thirdparty.kotlin.collections.MapsKt.mapOf(
                me.rerere.matrix.thirdparty.kotlin.TuplesKt.to("action", "attack"));
        if (flag("ka.consume", "consume with other actions", details, 5)) {
            cancelNextConsume = true;
        }
    }

    /** Checks the pending nearby-player entity id after deferred action processing. */
    public void processPendingEntity() {
        if (getFlawTypeEEnable() && pendingEntityId > 0) {
            Entity nearby = FoliaSupport.nearbyEntityById(player(), pendingEntityId);
            if (nearby != null && nearby.getType() == org.bukkit.entity.EntityType.PLAYER && !player().isFlying() && combatChecksEnabled) {
                Map<String, Object> details = me.rerere.matrix.thirdparty.kotlin.collections.MapsKt.mapOf(
                        me.rerere.matrix.thirdparty.kotlin.TuplesKt.to("type", "E"));
                report("ka.flaw.e", "the player exhibits a flaw in combat", details, getFlawTypeEVl());
            }
            pendingEntityId = 0;
        }
    }

    /** Cancels exactly the next consume event when onEntityAction armed it. */
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (cancelNextConsume) {
            cancelNextConsume = false;
            event.setCancelled(true);
        }
    }

    /** Checks packet timing, action sequences, sprint timing, and movement after attacks. */
    public boolean onPacketReceive(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent receiveEvent,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper wrapper) {
        if (wrapper == null) return false;

        RecentElements<PacketWrapperSnapshot> packetHistory = actionCounterB.registry();
        List<PacketWrapperSnapshot> recentPackets = null;
        boolean diggingRelease = false;
        if (packetType == PacketType$Play$Client.PLAYER_DIGGING
                && wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging) {
            diggingRelease = ((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging) wrapper).getAction()
                    == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM;
        }
        boolean recordable = isFlyingPacket(packetType)
                || packetType == PacketType$Play$Client.INTERACT_ENTITY
                || packetType == PacketType$Play$Client.ATTACK
                || diggingRelease
                || packetType == PacketType$Play$Client.ANIMATION
                || packetType == PacketType$Play$Client.PUNCH;
        if (recordable) {
            PacketWrapperSnapshot stamped = new PacketWrapperSnapshot(wrapper, System.currentTimeMillis());
            packetHistory.add(stamped);
            if (packetHistory.isAtCapacity()) {
                recentPackets = me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.toList(packetHistory);
                if (getFlawTypeBEnable()) {
                    if (PacketEventSupport.isAttackWrapper(packetOf(recentPackets, 0))
                            && packetOf(recentPackets, 1) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
                            && durationLessThan120Millis(playerData().smoothedLatency())) {
                        long releaseInterval = elapsed(packetTime(recentPackets, 1), packetTime(recentPackets, 0));
                        if (releaseInterval >= 40L && releaseInterval < 61L && combatChecksEnabled) {
                            report("ka.flaw.b", "the player exhibits a flaw in combat",
                                    stringMap("type", "B", "pattern", "release"), getFlawTypeBVl());
                        }
                    }
                }

                if (getFlawTypeCEnable()
                        && packetOf(recentPackets, 0) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
                        && packetOf(recentPackets, 1) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
                        && (packetOf(recentPackets, 2) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation
                            || packetOf(recentPackets, 2) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPunch)
                        && PacketEventSupport.isAttackWrapper(packetOf(recentPackets, 3))
                        && elapsed(packetTime(recentPackets, 3), packetTime(recentPackets, 2)) <= 5L
                        && elapsed(packetTime(recentPackets, 2), packetTime(recentPackets, 1)) <= 5L
                        && elapsed(packetTime(recentPackets, 1), packetTime(recentPackets, 0)) > 40L
                        && combatChecksEnabled) {
                    report("ka.flaw.c", "the player exhibits a flaw in combat",
                            stringMap("type", "C", "pattern", "rba"), getFlawTypeCVl());
                }

                if (getFlawTypeBEnable()
                        && packetOf(recentPackets, 3) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
                        && packetOf(recentPackets, 2) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
                        && packetOf(recentPackets, 1) instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
                        && packetTime(recentPackets, 2) - packetTime(recentPackets, 1) < 10L
                        && packetTime(recentPackets, 3) - packetTime(recentPackets, 2) > 45L
                        && combatChecksEnabled) {
                    report("ka.flaw.b", "the player exhibits a flaw in combat",
                            stringMap("type", "B", "pattern", "timing"), getFlawTypeBVl());
                }
            }
        }

        if (packetType == PacketType$Play$Client.ENTITY_ACTION) {
            if (!(wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction)) {
                throw castFailure(wrapper, me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.class);
            }
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction entityAction =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction) wrapper;
            if (entityAction.getAction() == WrapperPlayClientEntityAction$Action.START_SPRINTING) {
                lastSprintAction = System.currentTimeMillis();
            }
        }

        if (PacketEventSupport.isAttackEvent(receiveEvent)) {
            long millisSinceSprint = TimeMarkOperations.elapsedSince(lastSprintAction);
            if (millisSinceSprint <= 5L) {
                pendingActionCount = Math.min(pendingActionCount + 1, 5);
            } else {
                pendingActionCount = Math.max(pendingActionCount - 1, 0);
            }
            if (pendingActionCount >= 3 && combatChecksEnabled && getFlawTypeDEnable()) {
                report("ka.flaw.d", "the player exhibits a flaw in combat | type D", getFlawTypeDVl());
            }
        }

        if (isFlyingPacket(packetType)) {
            if (!(wrapper instanceof me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation)) {
                throw castFailure(wrapper, me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation.class);
            }
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation movement =
                    (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation) wrapper;
            boolean positionChanged = movement.hasPositionChanged();
            double currentX = positionChanged ? movement.getPosition().x : previousX;
            double currentY = positionChanged ? movement.getPosition().y : previousY;
            double currentZ = positionChanged ? movement.getPosition().z : previousZ;
            double deltaX = currentX - previousX;
            double deltaY = currentY - previousY;
            double deltaZ = currentZ - previousZ;
            if (pendingCooldown > 0) {
                double movementDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                double previousMovementDistance = Math.sqrt(
                        previousDeltaX * previousDeltaX
                                + previousDeltaY * previousDeltaY
                                + previousDeltaZ * previousDeltaZ);
                if (pendingCooldown == 3
                        && Math.abs(movementDistance - previousMovementDistance) < 0.001D
                        && movementDistance >= 0.28D && movementDistance <= 0.281D
                        && targetEntityId > 0) {
                    pendingEntityId = targetEntityId;
                }
            }
            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
            previousDeltaX = deltaX;
            previousDeltaY = deltaY;
            previousDeltaZ = deltaZ;
            if (pendingCooldown > 0) pendingCooldown--;
        }
        if (PacketEventSupport.isAttackEvent(receiveEvent)) {
            pendingCooldown = 3;
            targetEntityId = PacketEventSupport.getAttackEntityId(wrapper);
        }
        return false;
    }

    private static boolean isFlyingPacket(
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType) {
        return me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(packetType);
    }
    private static me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper packetOf(
            List<PacketWrapperSnapshot> packets, int index) {
        return packets.get(index).getPacketWrapper();
    }
    private static long packetTime(List<PacketWrapperSnapshot> packets, int index) {
        return packets.get(index).getTimestamp();
    }
    private static long elapsed(long later, long earlier) {
        return TimeMarkOperations.subtractEncodedValues(later, earlier);
    }
    private static boolean durationLessThan120Millis(long encodedDuration) {
        long decodedDuration = encodedDuration >> 1;
        return (encodedDuration & 1L) == 0L ? decodedDuration < 120000000L : decodedDuration < 120L;
    }
    private static Map<String, Object> stringMap(String key1, String value1, String key2, String value2) {
        me.rerere.matrix.thirdparty.kotlin.Pair[] pairs = new me.rerere.matrix.thirdparty.kotlin.Pair[] {
            me.rerere.matrix.thirdparty.kotlin.TuplesKt.to(key1, value1),
            me.rerere.matrix.thirdparty.kotlin.TuplesKt.to(key2, value2)
        };
        return me.rerere.matrix.thirdparty.kotlin.collections.MapsKt.mapOf(pairs);
    }
    private static ClassCastException castFailure(Object value, Class<?> expected) {
        Class<?> actual = value.getClass();
        Object expectedKClass = (me.rerere.matrix.thirdparty.kotlin.reflect.KClass) (Object) expected;
        return new ClassCastException(formatCastError(actual, expectedKClass));
    }

    private PlayerCheckContext playerData() { return playerState(); }
    private boolean report(String key, String message, Map<String, Object> details, int vl) {
        return flag(key, message, details, vl);
    }
    private boolean report(String key, String message, int vl) {
        return flag(key, message, vl);
    }
}
