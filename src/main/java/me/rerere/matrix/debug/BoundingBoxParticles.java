package me.rerere.matrix.debug;

import java.util.Iterator;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.util.DoubleRangeSequence;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.particle.Particle;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3f;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.ranges.ClosedRange;
import me.rerere.matrix.thirdparty.kotlin.ranges.RangesKt;
import org.bukkit.entity.Player;

public final class BoundingBoxParticles {
    private BoundingBoxParticles() {}

    private static Iterable<Double> steppedRange(ClosedRange<Double> range, double step) {
        return new DoubleRangeSequence(range, step);
    }

    public static void drawDefaults(AxisAlignedBox bounds, Player player,
                                    ParticleType<?> particleType, double step, int mask) {
        if ((mask & 4) != 0) step = 0.1;
        draw(bounds, player, particleType, step);
    }

    public static void draw(AxisAlignedBox bounds, Player player, ParticleType<?> particleType, double step) {
        Particle<?> particle = new Particle<>(particleType);
        for (Double sample : steppedRange(RangesKt.rangeTo(bounds.minX(), bounds.maxX()), step)) {
            double x = sample.doubleValue();
            for (Object edgeCoordinate : CollectionsKt.listOf(new Double[]{bounds.minY(), bounds.maxY()})) {
                double y = ((Number) edgeCoordinate).doubleValue();
                Iterator<?> oppositeEdges = CollectionsKt.listOf(new Double[]{bounds.minZ(), bounds.maxZ()}).iterator();
                while (oppositeEdges.hasNext()) {
                    double z = ((Number) oppositeEdges.next()).doubleValue();
                    WrapperPlayServerParticle packet = new WrapperPlayServerParticle(particle, false,
                            new Vector3d(x, y, z), new Vector3f(0.01f, 0.01f, 0.01f), 0.0f, 1);
                    PacketEventSupport.writePacketIfPresent(player, packet);
                }
            }
        }
        for (Double sample : steppedRange(RangesKt.rangeTo(bounds.minY(), bounds.maxY()), step)) {
            double y = sample.doubleValue();
            for (Object edgeCoordinate : CollectionsKt.listOf(new Double[]{bounds.minX(), bounds.maxX()})) {
                double x = ((Number) edgeCoordinate).doubleValue();
                Iterator<?> oppositeEdges = CollectionsKt.listOf(new Double[]{bounds.minZ(), bounds.maxZ()}).iterator();
                while (oppositeEdges.hasNext()) {
                    double z = ((Number) oppositeEdges.next()).doubleValue();
                    WrapperPlayServerParticle packet = new WrapperPlayServerParticle(particle, false,
                            new Vector3d(x, y, z), Vector3f.zero(), 0.0f, 1);
                    PacketEventSupport.writePacketIfPresent(player, packet);
                }
            }
        }
        for (Double sample : steppedRange(RangesKt.rangeTo(bounds.minZ(), bounds.maxZ()), step)) {
            double z = sample.doubleValue();
            for (Object edgeCoordinate : CollectionsKt.listOf(new Double[]{bounds.minX(), bounds.maxX()})) {
                double x = ((Number) edgeCoordinate).doubleValue();
                Iterator<?> oppositeEdges = CollectionsKt.listOf(new Double[]{bounds.minY(), bounds.maxY()}).iterator();
                while (oppositeEdges.hasNext()) {
                    double y = ((Number) oppositeEdges.next()).doubleValue();
                    WrapperPlayServerParticle packet = new WrapperPlayServerParticle(particle, false,
                            new Vector3d(x, y, z), Vector3f.zero(), 0.0f, 1);
                    PacketEventSupport.writePacketIfPresent(player, packet);
                }
            }
        }
    }
}
