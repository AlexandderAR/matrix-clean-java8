package me.rerere.matrix.util;

import java.util.Comparator;
import me.rerere.matrix.prediction.MovementPredictor;

/** Orders movement predictors by their float prediction score. */
public final class ScoreComparator implements Comparator<Object> {

    public ScoreComparator() {}

    @Override
    public int compare(Object first, Object second) {
        float firstScore = ((MovementPredictor) first).predictionAt();
        float secondScore = ((MovementPredictor) second).predictionAt();
        return Float.compare(firstScore, secondScore);
    }
}
