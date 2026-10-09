package me.rerere.matrix.player.model;

import me.rerere.matrix.prediction.PredictionResult;

/** Callback contract for evaluating a movement event state. */
@FunctionalInterface
public interface MovementCheck {
    PredictionResult evaluate(PredictionContext movementState);
}
