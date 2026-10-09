package me.rerere.matrix.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.util.Vector;
import me.rerere.matrix.listeners.VelocityExplosionType;
import me.rerere.matrix.network.packet.TransactionPingQueue;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.VelocityData;
import me.rerere.matrix.util.DelegatingMutableList;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.collections.SetsKt;

/** Tracks outgoing velocity impulses and the time at which each impulse is applied. */
public final class VelocityHandler {
    private static final String UNKNOWN_PACKET_PREFIX = "Unknown packet type: ";
    private final PlayerCheckContext playerContext;
    private final List<VelocityData> impulses = new ArrayList<>();
    private List<?> auxiliaryValues = new ArrayList<>();

    public VelocityHandler(PlayerCheckContext playerContext) {
        this.playerContext = playerContext;
    }

    private void removeExpired(long lifetime, long appliedLifetime) {
        long now = System.currentTimeMillis();
        impulses.removeIf(impulse -> {
            long age = now - impulse.getTime();
            if (age > lifetime) return true;
            return impulse.isApplied() && now - impulse.getAppliedAt() > appliedLifetime;
        });
    }

    /** Ages the main impulse list, and optionally clears it according to the caller flag. */
    public void update(boolean clear) {
        if (impulses.isEmpty()) return;
        prune(this, 0L, 0L, 3);
        if (clear) impulses.removeIf(VelocityData::isApplied);
    }

    public static boolean invokeAppliedPredicate(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<?, ?> predicate,
            Object value) {
        return ((Boolean) ((me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<Object, ?>)
                predicate).invoke(value)).booleanValue();
    }

    private static String unknownPacketMessage(PacketTypeCommon packetType) {
        return UNKNOWN_PACKET_PREFIX + packetType;
    }

    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        VelocityData data;
        if (packetType == PacketType$Play$Server.ENTITY_VELOCITY) {
            WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(event);
            Vector3d velocity = packet.getVelocity();
            data = new VelocityData(velocity.x, velocity.y, velocity.z,
                    VelocityExplosionType.VELOCITY, 0L, null, false, 0L, 240, null);
        } else if (packetType == PacketType$Play$Server.EXPLOSION) {
            WrapperPlayServerExplosion packet = new WrapperPlayServerExplosion(event);
            Vector3d knockback = packet.getKnockback();
            if (knockback == null) return;
            data = new VelocityData(knockback.x, knockback.y, knockback.z,
                    VelocityExplosionType.EXPLOSION, 0L, null, false, 0L, 240, null);
        } else {
            throw new IllegalStateException(unknownPacketMessage(packetType));
        }

        TransactionPingQueue transactionQueue = playerContext.transactionQueue();
        transactionQueue.workWithCallbacks(
                () -> {
                    return addImpulse(this, data);
                },
                () -> {
                    return markApplied(data);
                });
        prune(this, 0L, 0L, 3);
    }

    public static boolean invokePredicate(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<?, ?> predicate,
            Object value) {
        return ((Boolean) ((me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<Object, ?>)
                predicate).invoke(value)).booleanValue();
    }

    /** Returns the impulse categories consistent with a vertical motion sample. */
    public Set<VelocityExplosionType> classifyVerticalMotion(Vector observed, double baselineY) {
        Set<VelocityExplosionType> matchingTypes = new HashSet<>();
        @SuppressWarnings("unchecked")
        List<VelocityData> recent = (List<VelocityData>) (List<?>) auxiliaryValues;
        boolean hasVelocity = recent.stream().anyMatch(data -> data.getType() == VelocityExplosionType.VELOCITY);
        boolean hasExplosion = recent.stream().anyMatch(data -> data.getType() == VelocityExplosionType.EXPLOSION);
        if (hasVelocity && hasExplosion) {
            return SetsKt.setOf(VelocityExplosionType.VELOCITY, VelocityExplosionType.EXPLOSION);
        }

        final double tolerance = 0.001;
        if (hasVelocity) {
            for (VelocityData data : recent) {
                if (data.getType() != VelocityExplosionType.VELOCITY) continue;
                double impulseY = data.getY();
                double observedY = observed.getY();
                boolean matchesVelocity = Math.abs(impulseY - observedY) <= tolerance
                        || (impulseY < 0.0 && Math.abs(observedY) <= tolerance)
                        || Math.abs(impulseY - observedY - 0.078) <= tolerance;
                if (matchesVelocity) {
                    matchingTypes.add(VelocityExplosionType.VELOCITY);
                    break;
                }
            }
        }

        if (hasExplosion) {
            double totalVerticalKnockback = 0.0;
            for (VelocityData data : recent) {
                if (data.getType() == VelocityExplosionType.EXPLOSION) {
                    totalVerticalKnockback += Math.abs(data.getY());
                }
            }
            if (Math.abs(observed.getY() - baselineY) <= totalVerticalKnockback + tolerance) {
                matchingTypes.add(VelocityExplosionType.EXPLOSION);
            }
        }
        return matchingTypes;
    }

    public boolean isApplied(VelocityData impulse) {
        return impulse.isApplied();
    }

    /** The low two mask bits select the default retention windows. */
    public static void prune(VelocityHandler handler, long lifetime, long appliedLifetime, int flags) {
        if ((flags & 1) != 0) lifetime = 7500L;
        if ((flags & 2) != 0) appliedLifetime = 1500L;
        handler.removeExpired(lifetime, appliedLifetime);
    }

    /** Creates a defensive snapshot after applying the original age-based cleanup. */
    public List<VelocityData> getRecentImpulses() {
        prune(this, 0L, 0L, 3);
        return impulses.isEmpty() ? CollectionsKt.emptyList() : new ArrayList<>(impulses);
    }

    public void setAuxiliaryValues(List<?> values) {
        auxiliaryValues = values;
    }

    public static Unit markApplied(VelocityData data) {
        data.setApplied(true);
        data.setAppliedAt(System.currentTimeMillis());
        return Unit.INSTANCE;
    }

    private static boolean expired(long now, long maximumAge, long maximumAppliedAge,
            VelocityData data) {
        long age = now - data.getTime();
        if (age > maximumAge) return true;
        return data.isApplied() && now - data.getAppliedAt() > maximumAppliedAge;
    }

    /** Builds the packet-velocity candidates used by the movement check. */
    public DelegatingMutableList<me.rerere.matrix.thirdparty.kotlin.Pair<Vector, VelocityData>> candidates(Vector observed) {
        List<me.rerere.matrix.thirdparty.kotlin.Pair<Vector, VelocityData>> candidates = new ArrayList<>();
        candidates.add(new me.rerere.matrix.thirdparty.kotlin.Pair<>(observed.clone(), null));
        List<VelocityData> velocityImpulses = new ArrayList<>();
        for (VelocityData impulseData : impulses) {
            if (impulseData.getType() == VelocityExplosionType.VELOCITY) {
                velocityImpulses.add(impulseData);
            }
        }
        for (VelocityData impulseData : velocityImpulses) {
            Vector impulse = new Vector(impulseData.getX(), impulseData.getY(), impulseData.getZ());
            candidates.add(new me.rerere.matrix.thirdparty.kotlin.Pair<>(impulse, impulseData));
        }
        return new DelegatingMutableList<>(candidates);
    }

    public List<?> getAuxiliaryValues() { return auxiliaryValues; }

    public static Unit addImpulse(VelocityHandler owner, VelocityData data) {
        owner.impulses.add(data);
        return Unit.INSTANCE;
    }

}
