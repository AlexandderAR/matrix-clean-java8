package me.rerere.matrix.prediction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.rerere.matrix.geometry.Ray;
import me.rerere.matrix.movement.PlayerSnapshot;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.model.PositionRotation;
import me.rerere.matrix.thirdparty.kotlin.TuplesKt;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.coroutines.Continuation;
import me.rerere.matrix.thirdparty.kotlin.coroutines.intrinsics.IntrinsicsKt;
import me.rerere.matrix.thirdparty.kotlin.coroutines.jvm.internal.RestrictedSuspendLambda;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function2;
import me.rerere.matrix.thirdparty.kotlin.sequences.SequenceScope;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.world.CoordinateOffsets;
import org.bukkit.util.Vector;

/** Lazy coroutine that emits collision-probe vectors for each shape, offset, and edge pair. */
public final class CollisionProbeSequence extends RestrictedSuspendLambda
        implements Function2<Object, Object, Object> {
    private static final String INVALID_RESUME;

    static {
        INVALID_RESUME = "call to 'resume' before 'invoke' with coroutine".intern();
    }

    private final PlayerSnapshot collisionProfile;
    private final List<?> verticalOffsets;
    private final PlayerCheckContext playerData;
    public int label;

    private Object savedScope;
    private Object dimensionPairs;
    private Object shapeIterator;
    private Object currentShape;
    private Object footprintOffsetIterator;
    private Object footprintBase;
    private Object verticalOffsetIterator;
    private Object elevatedBase;
    private Object dimensionPairIterator;

    public CollisionProbeSequence(PlayerSnapshot profile, List<?> verticalOffsets,
                                  PlayerCheckContext playerData, Continuation<Object> completion) {
        super(2, completion);
        this.collisionProfile = profile;
        this.verticalOffsets = verticalOffsets;
        this.playerData = playerData;
    }

    private Object invokeScope(SequenceScope<?> scope, Continuation<?> continuation) {
        CollisionProbeSequence coroutine = (CollisionProbeSequence) (Object) create(scope, continuation);
        return coroutine.invokeSuspend(Unit.INSTANCE);
    }

    @Override
    public Object invoke(Object scope, Object continuation) {
        return invokeScope((SequenceScope<?>) scope, (Continuation<?>) continuation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Continuation<Unit> create(Object scope, Continuation<?> continuation) {
        CollisionProbeSequence coroutine = new CollisionProbeSequence(
                collisionProfile, verticalOffsets, playerData,
                (Continuation<Object>) continuation);
        coroutine.savedScope = scope;
        return (Continuation<Unit>) (Continuation<?>) coroutine;
    }

    @Override
    public Object invokeSuspend(Object result) {
        Object suspended = IntrinsicsKt.getCOROUTINE_SUSPENDED();

        SequenceScope<Object> scope;
        List<Pair<Float, Float>> dimensionPairs;
        Iterator<?> shapes;
        PositionRotation shape;
        Iterator<?> footprints;
        Vector base;
        Iterator<?> heights;
        Vector offsetBase;
        Iterator<?> dimensionsIterator;
        boolean freshCall = label == 0;

        if (freshCall) {
            ResultKt.throwOnFailure(result);
            scope = (SequenceScope<Object>) savedScope;
            PositionRotation first = collisionProfile.currentAliasA();
            PositionRotation second = collisionProfile.previousAliasA();
            List<PositionRotation> shapeCandidates = CollectionsKt.distinct(
                    (Iterable<PositionRotation>) CollectionsKt.listOf(first, second));
            List<Pair<Float, Float>> rawDimensions = new ArrayList<Pair<Float, Float>>(4);
            rawDimensions.add(TuplesKt.to(Float.valueOf(first.getYaw()),
                    Float.valueOf(first.component3())));
            rawDimensions.add(TuplesKt.to(Float.valueOf(second.getYaw()),
                    Float.valueOf(first.component3())));
            rawDimensions.add(TuplesKt.to(Float.valueOf(first.getYaw()),
                    Float.valueOf(second.component3())));
            rawDimensions.add(TuplesKt.to(Float.valueOf(second.getYaw()),
                    Float.valueOf(second.component3())));
            dimensionPairs = CollectionsKt.distinct(rawDimensions);
            shapes = shapeCandidates.iterator();
            shape = null;
            footprints = null;
            base = null;
            heights = null;
            offsetBase = null;
            dimensionsIterator = null;
        } else if (label == 1) {
            scope = (SequenceScope<Object>) savedScope;
            dimensionPairs = (List<Pair<Float, Float>>) this.dimensionPairs;
            shapes = (Iterator<?>) shapeIterator;
            shape = (PositionRotation) currentShape;
            footprints = (Iterator<?>) footprintOffsetIterator;
            base = (Vector) footprintBase;
            heights = (Iterator<?>) verticalOffsetIterator;
            offsetBase = (Vector) elevatedBase;
            dimensionsIterator = (Iterator<?>) dimensionPairIterator;
            ResultKt.throwOnFailure(result);
        } else {
            throw new IllegalStateException(INVALID_RESUME);
        }

        if (!freshCall) {
            while (dimensionsIterator.hasNext()) {
                Pair<?, ?> pair = (Pair<?, ?>) dimensionsIterator.next();
                Object yielded = yieldProbe(scope, dimensionPairs, shapes, shape, footprints,
                        base, heights, offsetBase, dimensionsIterator, pair);
                if (yielded == suspended) return suspended;
            }
            while (heights.hasNext()) {
                double vertical = ((Number) heights.next()).doubleValue();
                offsetBase = copy(base).add(new Vector(0.0D, vertical, 0.0D));
                dimensionsIterator = dimensionPairs.iterator();
                while (dimensionsIterator.hasNext()) {
                    Pair<?, ?> pair = (Pair<?, ?>) dimensionsIterator.next();
                    Object yielded = yieldProbe(scope, dimensionPairs, shapes, shape, footprints,
                            base, heights, offsetBase, dimensionsIterator, pair);
                    if (yielded == suspended) return suspended;
                }
            }
            while (footprints.hasNext()) {
                Pair<?, ?> footprint = (Pair<?, ?>) footprints.next();
                double x = ((Number) footprint.component1()).doubleValue();
                double z = ((Number) footprint.component2()).doubleValue();
                base = GeometryMath.copy(shape.getPosition()).add(new Vector(x, 0.0D, z));
                heights = verticalOffsets.iterator();
                while (heights.hasNext()) {
                    double vertical = ((Number) heights.next()).doubleValue();
                    offsetBase = copy(base).add(new Vector(0.0D, vertical, 0.0D));
                    dimensionsIterator = dimensionPairs.iterator();
                    while (dimensionsIterator.hasNext()) {
                        Pair<?, ?> pair = (Pair<?, ?>) dimensionsIterator.next();
                        Object yielded = yieldProbe(scope, dimensionPairs, shapes, shape, footprints,
                                base, heights, offsetBase, dimensionsIterator, pair);
                        if (yielded == suspended) return suspended;
                    }
                }
            }
        }

        while (shapes.hasNext()) {
            shape = (PositionRotation) shapes.next();
            footprints = ((List<?>) CoordinateOffsets.getOffsets()).iterator();
            while (footprints.hasNext()) {
                Pair<?, ?> footprint = (Pair<?, ?>) footprints.next();
                double x = ((Number) footprint.component1()).doubleValue();
                double z = ((Number) footprint.component2()).doubleValue();
                base = GeometryMath.copy(shape.getPosition()).add(new Vector(x, 0.0D, z));
                heights = verticalOffsets.iterator();
                while (heights.hasNext()) {
                    double vertical = ((Number) heights.next()).doubleValue();
                    offsetBase = copy(base).add(new Vector(0.0D, vertical, 0.0D));
                    dimensionsIterator = dimensionPairs.iterator();
                    while (dimensionsIterator.hasNext()) {
                        Pair<?, ?> pair = (Pair<?, ?>) dimensionsIterator.next();
                        Object yielded = yieldProbe(scope, dimensionPairs, shapes, shape, footprints,
                                base, heights, offsetBase, dimensionsIterator, pair);
                        if (yielded == suspended) return suspended;
                    }
                }
            }
        }
        return Unit.INSTANCE;
    }

    private Object yieldProbe(SequenceScope<Object> scope, List<Pair<Float, Float>> dimensionPairs,
                              Iterator<?> shapes, PositionRotation shape, Iterator<?> footprints,
                              Vector base, Iterator<?> heights, Vector offsetBase,
                              Iterator<?> dimensionsIterator, Pair<?, ?> pair) {
        float yaw = ((Number) pair.component1()).floatValue();
        float pitch = ((Number) pair.component2()).floatValue();
        Ray probe = new Ray(offsetBase, GeometryMath.directionFromClientVersion(
                playerData, yaw, pitch));
        this.savedScope = scope;
        this.dimensionPairs = dimensionPairs;
        this.shapeIterator = shapes;
        this.currentShape = shape;
        this.footprintOffsetIterator = footprints;
        this.footprintBase = base;
        this.verticalOffsetIterator = heights;
        this.elevatedBase = offsetBase;
        this.dimensionPairIterator = dimensionsIterator;
        this.label = 1;
        return scope.yield(probe, this);
    }

    private static Vector copy(Vector vector) {
        return GeometryMath.copy(vector);
    }

}
