package me.rerere.matrix.player.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.collections.MapsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.prediction.MovementPredictor;
import me.rerere.matrix.checks.movement.MovementStep;
import me.rerere.matrix.util.cache.CachePolicy;
import me.rerere.matrix.util.ScoreComparator;

public final class MovementPredictionHistory {
    private static final List<float[]> PROJECTION_DIRECTIONS = CollectionsKt.listOf(new float[][] {
            {1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 0, 1}, {1, 0, -1},
            {0, 1, 1}, {0, 1, -1}, {1, 0, 2}, {1, 0, -2}});
    public static final EntityInterpolationPolicy COMPANION = new EntityInterpolationPolicy();
    private final int capacity;
    private List<AxisAlignedBox> cachedBounds;
    private List<MovementPredictor> priorPredictions;
    private List<MovementPredictor> predictions;
    private Function1<MovementPredictor, Unit> applySamples;
    private boolean reduceBounds;

    public MovementPredictionHistory(AxisAlignedBox initialBounds, CachePolicy movement,
                                     int capacity, float first, float second) {
        this.capacity = capacity;
        predictions = CollectionsKt.listOf(new MovementPredictor(initialBounds, movement, capacity, first, second));
    }
    public MovementPredictionHistory(AxisAlignedBox bounds, CachePolicy movement, int capacity,
                                     float first, float second, int defaults,
                                     DefaultConstructorMarker marker) {
        this(bounds, movement, capacity, (defaults & 8) != 0 ? 0 : first,
                (defaults & 16) != 0 ? 0 : second);
    }
    public int getCapacity() { return capacity; }
    public void clearPriorPredictions() { priorPredictions = null; applySamples = null; cachedBounds = null; }

    public void advance() {
        for (MovementPredictor prediction : predictions) {
            prediction.update();
        }
        if (priorPredictions != null) {
            for (MovementPredictor prediction : priorPredictions) {
                prediction.update();
            }
            List<MovementPredictor> copies = new ArrayList<>(priorPredictions.size());
            for (MovementPredictor prior : priorPredictions) {
                MovementPredictor copy = prior.copy();
                Intrinsics.checkNotNull(applySamples);
                applySamples.invoke(copy);
                copies.add(copy);
            }
            predictions = CollectionsKt.plus(predictions, copies);
        }
        predictions = prune(predictions);
        if (priorPredictions == null && predictions.size() == 1
                && !predictions.get(0).isPredictionAvailable()) {
            reduceBounds = false;
        }
        cachedBounds = null;
    }

    public List<AxisAlignedBox> getBounds() {
        if (cachedBounds != null) return cachedBounds;
        List<MovementPredictor> combined = CollectionsKt.plus(predictions,
                priorPredictions != null ? priorPredictions : CollectionsKt.emptyList());
        List<AxisAlignedBox> all = new ArrayList<>();
        for (MovementPredictor prediction : combined) {
            for (AxisAlignedBox projectedBox : prediction.projectedBounds()) {
                all.add(projectedBox);
            }
        }
        List<AxisAlignedBox> distinct = CollectionsKt.distinct(all);
        List<AxisAlignedBox> result;
        if (reduceBounds) {
            Iterator<AxisAlignedBox> iterator = distinct.iterator();
            if (!iterator.hasNext()) throw new UnsupportedOperationException("Empty collection can't be reduced.".intern());
            AxisAlignedBox combinedBounds = iterator.next();
            while (iterator.hasNext()) combinedBounds = combinedBounds.union(iterator.next());
            result = CollectionsKt.listOf(combinedBounds);
        } else result = distinct;
        cachedBounds = result;
        return result;
    }

    public void updateSamples(List<MovementStep> samples, Float first, Float second,
                              boolean mode) {
        List<MovementPredictor> oldCopies = new ArrayList<>(predictions.size());
        for (MovementPredictor prediction : predictions) oldCopies.add(prediction.copy());
        priorPredictions = oldCopies;
        Function1<MovementPredictor, Unit> updater = prediction -> {
            prediction.ingestSamples(samples, first, second, mode);
            return Unit.INSTANCE;
        };
        applySamples = updater;
        List<MovementPredictor> updated = new ArrayList<>(predictions.size());
        for (MovementPredictor prediction : predictions) {
            MovementPredictor copy = prediction.copy();
            updater.invoke(copy);
            updated.add(copy);
        }
        predictions = updated;
        cachedBounds = null;
    }

    private static float projection(Map<MovementPredictor, float[]> coordinates,
                                    float[] direction, MovementPredictor prediction) {
        float[] components = MapsKt.getValue(coordinates, prediction);
        float projectedValue = 0;
        for (int index = 0; index < components.length; index++) {
            projectedValue += direction[index] * components[index];
        }
        return projectedValue;
    }
    private List<MovementPredictor> prune(List<MovementPredictor> input) {
        Set<List<?>> sampleSets = new HashSet<>();
        List<MovementPredictor> unique = new ArrayList<>();
        for (MovementPredictor prediction : input) {
            List<?> featureState = prediction.featureState();
            if (sampleSets.add(featureState)) unique.add(prediction);
        }
        if (unique.size() <= 32) return unique;
        reduceBounds = true;
        List<MovementPredictor> ordered = new ArrayList<>(CollectionsKt.sortedWith(unique, new ScoreComparator()));
        Map<MovementPredictor, float[]> coordinates = new LinkedHashMap<>();
        for (MovementPredictor prediction : ordered) coordinates.put(prediction, prediction.predictionComponents());
        Set<MovementPredictor> extremes = new HashSet<>();
        for (float[] direction : PROJECTION_DIRECTIONS) {
            Iterator<MovementPredictor> minimumItems = ordered.iterator();
            if (!minimumItems.hasNext()) throw new NoSuchElementException();
            MovementPredictor minimum = minimumItems.next();
            if (minimumItems.hasNext()) {
                float minimumValue = projection(coordinates, direction, minimum);
                do {
                    MovementPredictor candidate = minimumItems.next();
                    float candidateValue = projection(coordinates, direction, candidate);
                    if (Float.compare(minimumValue, candidateValue) > 0) {
                        minimum = candidate;
                        minimumValue = candidateValue;
                    }
                } while (minimumItems.hasNext());
            }
            Iterator<MovementPredictor> maximumItems = ordered.iterator();
            if (!maximumItems.hasNext()) throw new NoSuchElementException();
            MovementPredictor maximum = maximumItems.next();
            if (maximumItems.hasNext()) {
                float maximumValue = projection(coordinates, direction, maximum);
                do {
                    MovementPredictor candidate = maximumItems.next();
                    float candidateValue = projection(coordinates, direction, candidate);
                    if (Float.compare(maximumValue, candidateValue) < 0) {
                        maximum = candidate;
                        maximumValue = candidateValue;
                    }
                } while (maximumItems.hasNext());
            }
            extremes.add(minimum);
            extremes.add(maximum);
        }
        while (ordered.size() > 32) {
            List<Integer> removable = new ArrayList<>();
            for (int index = 1; index < ordered.size() - 1; index++) {
                if (!extremes.contains(ordered.get(index))) removable.add(index);
            }
            if (removable.isEmpty()) {
                for (int index = 1; index < ordered.size() - 1; index++) removable.add(index);
            }
            Iterator<Integer> indices = removable.iterator();
            if (!indices.hasNext()) throw new NoSuchElementException();
            int selected = indices.next();
            if (indices.hasNext()) {
                float selectedDistance = adjacentDistance(ordered, selected);
                do {
                    int index = indices.next();
                    float distance = adjacentDistance(ordered, index);
                    if (Float.compare(selectedDistance, distance) > 0) {
                        selected = index;
                        selectedDistance = distance;
                    }
                } while (indices.hasNext());
            }
            ordered.remove(selected);
        }
        return ordered;
    }
    private static float adjacentDistance(List<MovementPredictor> values, int index) {
        return Math.min(values.get(index).weightedPredictionError(values.get(index - 1)),
                values.get(index).weightedPredictionError(values.get(index + 1)));
    }
}
