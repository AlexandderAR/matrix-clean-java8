package me.rerere.matrix.prediction;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import org.bukkit.util.Vector;

/** Prediction output carrying the expected position, input movement, and resulting velocity. */
public final class PredictionResult {
    private final List<?> expectPosition;
    private final Pair<?, ?> wasd;
    private final Vector velocityUpdated;

    public PredictionResult(List<?> expectPosition, Pair<?, ?> wasd, Vector velocityUpdated) {
        this.expectPosition = expectPosition;
        this.wasd = wasd;
        this.velocityUpdated = velocityUpdated;
    }

    public static PredictionResult copy$default(PredictionResult receiver,
            List<?> copiedPosition, Pair<?, ?> copiedWasd, Vector copiedVelocity,
            int defaultMask, Object marker) {
        try {
            if ((defaultMask & 1) != 0) {
                copiedPosition = receiver.expectPosition;
            }
            if ((defaultMask & 2) != 0) {
                copiedWasd = receiver.wasd;
            }
            if ((defaultMask & 4) != 0) {
                copiedVelocity = receiver.velocityUpdated;
            }
            return receiver.copyWithValues(copiedPosition, copiedWasd, copiedVelocity);
        } catch (me.rerere.matrix.license.LicenseResponseException failure) {
            throw failure;
        }
    }

    public PredictionResult copyWithValues(List<?> expectedPosition, Pair<?, ?> inputMovement,
            Vector updatedVelocity) {
        return new PredictionResult(expectedPosition, inputMovement, updatedVelocity);
    }

    public List<?> getExpectPosition() { return this.expectPosition; }
    public List<?> expectPosition() { return this.expectPosition; }
    public Pair<?, ?> getWasd() { return this.wasd; }
    public Pair<?, ?> wasd() { return this.wasd; }
    public Vector getVelocityUpdated() { return this.velocityUpdated; }
    public Vector velocityUpdated() { return this.velocityUpdated; }

    private static String formatPredictionResult(List<?> expectedPosition, Pair<?, ?> inputMovement,
            Vector updatedVelocity) {
        return "PredictionResult(expectPosition=" + expectedPosition
                + ", wasd=" + inputMovement
                + ", velocityUpdated=" + updatedVelocity + ")";
    }

    @Override
    public String toString() {
        return formatPredictionResult(this.expectPosition, this.wasd, this.velocityUpdated);
    }

    @Override
    public int hashCode() {
        int result = this.expectPosition.hashCode();
        result = 31 * result + this.wasd.hashCode();
        result = 31 * result + this.velocityUpdated.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        try {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PredictionResult)) {
                return false;
            }
            PredictionResult that = (PredictionResult) other;
            if (!equal(this.expectPosition, that.expectPosition)) {
                return false;
            }
            if (!equal(this.wasd, that.wasd)) {
                return false;
            }
            return equal(this.velocityUpdated, that.velocityUpdated);
        } catch (me.rerere.matrix.license.LicenseResponseException failure) {
            throw failure;
        }
    }

    private static boolean equal(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
