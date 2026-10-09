package me.rerere.matrix.movement;

/** Mutable prediction values shared by the movement calculation stages. */
public class MovementPredictionState {
    public double predictedHorizontalMove;
    public float horizontalAcceleration;
    public double noSlowHorizontalLimit;
    public float blockFriction;
    public float horizontalDrag;
    public double predictedVerticalMove;

    public MovementPredictionState() {}
}
