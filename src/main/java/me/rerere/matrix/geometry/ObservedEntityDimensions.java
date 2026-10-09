package me.rerere.matrix.geometry;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.TuplesKt;

/** Cached observed width and height by entity type and pose. */
public final class ObservedEntityDimensions {
    public static final ObservedEntityDimensions INSTANCE = new ObservedEntityDimensions();
    private static final Map<EntityType, EnumMap<EntityPose, Pair<Float, Float>>> dimensions =
            new HashMap<EntityType, EnumMap<EntityPose, Pair<Float, Float>>>();

    private ObservedEntityDimensions() { }

    public float getWidth(EntityType type, EntityPose pose) {
        EnumMap<EntityPose, Pair<Float, Float>> poses = dimensions.get(type);
        if (poses == null) return 0x1.333334p-1f;
        Pair<Float, Float> pair = poses.get(pose);
        if (pair == null) return 0x1.333334p-1f;
        return pair.getFirst().floatValue();
    }

    public float getHeight(EntityType type, EntityPose pose) {
        EnumMap<EntityPose, Pair<Float, Float>> poses = dimensions.get(type);
        if (poses == null) return 0x1.ccccccp0f;
        Pair<Float, Float> pair = poses.get(pose);
        if (pair == null) return 0x1.ccccccp0f;
        return pair.getSecond().floatValue();
    }

    private EntityPose getPose(LivingEntity entity) {
        if (Matrix.getServerVersion().compareTo(ServerVersion.V_1_14) < 0) {
            return EntityPose.STANDING;
        }
        return SpigotConversionUtil.fromBukkitPose(entity.getPose());
    }

    private static void observeDimensions(Entity entity) {
        LivingEntity living = (LivingEntity) entity;
        EntityType type = SpigotConversionUtil.fromBukkitEntityType(living.getType());
        EntityPose pose = INSTANCE.getPose(living);
        AxisAlignedBox bounds = BlockCollisionGeometry.getEntityBounds(living);

        EnumMap<EntityPose, Pair<Float, Float>> poses = dimensions.get(type);
        if (poses == null) {
            poses = new EnumMap<EntityPose, Pair<Float, Float>>(EntityPose.class);
            dimensions.put(type, poses);
        }

        Pair<Float, Float> observedSize = poses.get(pose);
        float previousWidth = observedSize == null ? 0.0f : observedSize.getFirst().floatValue();
        float previousHeight = observedSize == null ? 0.0f : observedSize.getSecond().floatValue();
        if (bounds.width() > (double) previousWidth
                || bounds.height() > (double) previousHeight) {
            poses.put(pose, TuplesKt.to(Float.valueOf((float) bounds.width()),
                    Float.valueOf((float) bounds.height())));
        }
    }

    public void scheduleObservation(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        FoliaScheduler.getEntityScheduler().run(entity,
                Matrix.getInstance(),
                ignoredTask -> observeDimensions(entity), null);
    }

    public boolean hasObservation(EntityType type, EntityPose pose) {
        EnumMap<EntityPose, Pair<Float, Float>> poses = dimensions.get(type);
        return poses != null && poses.containsKey(pose);
    }
}
