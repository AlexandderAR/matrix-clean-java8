package me.rerere.matrix.prediction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.TuplesKt;

/** Generates horizontal movement candidates from input, friction, jumping, and velocity. */
public final class MovementCandidateGenerator implements me.rerere.matrix.player.model.MovementCheck {
    private final ArrayList<Pair<Float, Float>> neighborOffsets;

    public MovementCandidateGenerator() {
        this.neighborOffsets = me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.arrayListOf(
            TuplesKt.to(0.0f, 0.0f), TuplesKt.to(1.0f, 0.0f), TuplesKt.to(-1.0f, 0.0f),
            TuplesKt.to(0.0f, 1.0f), TuplesKt.to(0.0f, -1.0f), TuplesKt.to(1.0f, 1.0f),
            TuplesKt.to(-1.0f, 1.0f), TuplesKt.to(1.0f, -1.0f), TuplesKt.to(-1.0f, -1.0f));
    }

    private static String pairText(Object first, Object second) {
        return new StringBuilder().append(first).append(',').append(second).toString();
    }

    private static String keySummary(org.bukkit.ChatColor color, String keys, boolean jump,
                                     String speed, String yaw, org.bukkit.ChatColor distanceColor,
                                     String distance) {
        return new StringBuilder("keys: ").append(color).append(keys).append(" / jump:")
            .append(jump).append(", speed:").append(speed).append(",yaw:").append(yaw)
            .append(" / dist: ").append(distanceColor).append(distance).toString();
    }

    private static String moveSummary(String moveX, String moveZ, String velocityX, String velocityZ) {
        return new StringBuilder("move: ").append(moveX).append(',').append(moveZ)
            .append(" / vel: ").append(velocityX).append(',').append(velocityZ).toString();
    }

    private static String predictedMoveSummary(String expectedX, String expectedZ) {
        return new StringBuilder(" / pred_move: ").append(expectedX).append(", ")
            .append(expectedZ).toString();
    }

    private static String velocitySummary(String velocity) {
        return new StringBuilder(" / [VEL] ").append(velocity).toString();
    }

    private me.rerere.matrix.util.DelegatingMutableList collectionView() {
        return new me.rerere.matrix.util.DelegatingMutableList(this.neighborOffsets);
    }

    private Pair<Double, Double> rotate(float x, float z, float scale, float yaw) {
        float lengthSquared = x * x + z * z;
        if (Float.isNaN(lengthSquared) || lengthSquared < 0.0001f) {
            return TuplesKt.to(0.0d, 0.0d);
        }
        float length = (float) Math.sqrt(lengthSquared);
        if (length < 1.0f) length = 1.0f;
        float factor = scale / length;
        x *= factor;
        z *= factor;
        float radians = yaw * (float) Math.PI / 180.0f;
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        return TuplesKt.to((double) (x * cos - z * sin), (double) (z * cos + x * sin));
    }

    @Override
    public me.rerere.matrix.prediction.PredictionResult evaluate(
            me.rerere.matrix.player.model.PredictionContext context) {
        me.rerere.matrix.player.PlayerCheckContext playerContext =
            context.getPlayer();
        me.rerere.matrix.world.LocationFlagCache locationFlags =
            playerContext.locationFlags();
        boolean fromFlag = locationFlags.getOrCompute(context.getFrom());
        boolean toFlag = locationFlags.getOrCompute(context.getTo());
        me.rerere.matrix.world.WorldAccess attributeConfig = me.rerere.matrix.Matrix.getInstance()
            .getNmsManager().visitor();
        float entityAttribute = attributeConfig.getMovementSpeed(
            (org.bukkit.entity.LivingEntity) playerContext.getPlayer());
        boolean leavingFlag = fromFlag && !toFlag;

        me.rerere.matrix.util.DelegatingMutableList velocityCandidates =
            playerContext.inventoryTracker().candidates(
            context.getVelocity());
        org.bukkit.util.Vector velocity =
            context.getVelocity();
        double velocityLengthSquared = velocity.lengthSquared();
        if (velocityLengthSquared > 0.0d && velocityLengthSquared < 0.03d) {
            velocityCandidates.add(TuplesKt.to(new org.bukkit.util.Vector(0.0d, 0.0d, 0.0d), null));
        }

        me.rerere.matrix.util.DelegatingMutableList movementOffsets = collectionView();
        float friction = fromFlag
            ? 0.91f * me.rerere.matrix.world.BlockEnvironment.frictionAtLocation(
                attributeConfig, playerContext, playerContext.getPlayer().getWorld(),
                context.getFrom())
            : 0.91f;
        float frictionScale = 0.16277136f / (friction * friction * friction);

        me.rerere.matrix.util.DelegatingMutableList<Boolean> sprintModes =
            me.rerere.matrix.util.BooleanListDefaults.getDefaults();
        ArrayList<Float> speedFactors = new ArrayList<>();
        for (Boolean sprintMode : sprintModes.backingList()) {
            boolean sprint = sprintMode.booleanValue();
            if (fromFlag) {
                float speed = entityAttribute * (sprint ? 1.3f : 1.0f);
                speedFactors.add(speed * frictionScale);
            } else {
                speedFactors.add(sprint ? 0.02f : 0.025999999f);
            }
        }
        me.rerere.matrix.util.DelegatingMutableList<Float> speedFactorList =
            new me.rerere.matrix.util.DelegatingMutableList(speedFactors);

        me.rerere.matrix.util.DelegatingMutableList jumpOffsets;
        if (leavingFlag) {
            me.rerere.matrix.util.DelegatingMutableList<Boolean> jumpModes =
                me.rerere.matrix.util.BooleanListDefaults.getDefaults();
            ArrayList<Pair<Double, Double>> generated = new ArrayList<>();
            for (Boolean item : jumpModes.backingList()) {
                boolean jump = item.booleanValue();
                if (jump) {
                    float yawRadians = (float) (Math.PI / 180.0d) * context.getYaw();
                    generated.add(TuplesKt.to(
                        (double) (-((float) Math.sin(yawRadians)) * 0.2f),
                        (double) ((float) Math.cos(yawRadians) * 0.2f)));
                } else {
                    generated.add(TuplesKt.to(0.0d, 0.0d));
                }
            }
            jumpOffsets = new me.rerere.matrix.util.DelegatingMutableList(generated);
        } else {
            jumpOffsets = me.rerere.matrix.util.DelegatingMutableList.companion.mutableCopy(
                TuplesKt.to(0.0d, 0.0d));
        }

        ArrayList<Pair<Pair<Double, Double>, Pair<Float, Float>>> rotated = new ArrayList<>();
        for (Object speedValue : (List<?>) speedFactorList.backingList()) {
            float speed = ((Number) speedValue).floatValue();
            for (Object offsetValue : (List<?>) movementOffsets.backingList()) {
                Pair<?, ?> offset = (Pair<?, ?>) offsetValue;
                float first = ((Number) offset.getFirst()).floatValue();
                float second = ((Number) offset.getSecond()).floatValue();
                Pair<Double, Double> rotatedDirection = rotate(second * 0.98f, first * 0.98f, speed,
                    context.getYaw());
                rotated.add(TuplesKt.to(rotatedDirection, TuplesKt.to(first, second)));
            }
        }
        me.rerere.matrix.util.DelegatingMutableList rotatedList = new me.rerere.matrix.util.DelegatingMutableList(rotated);

        ArrayList<Pair<Pair<Double, Double>, Pair<Float, Float>>> jumped = new ArrayList<>();
        for (Object jumpValue : (List<?>) jumpOffsets.backingList()) {
            Pair<?, ?> jump = (Pair<?, ?>) jumpValue;
            for (Object rotatedMoveValue : (List<?>) rotatedList.backingList()) {
                Pair<?, ?> rotatedMove = (Pair<?, ?>) rotatedMoveValue;
                Pair<?, ?> displacement = (Pair<?, ?>) rotatedMove.getFirst();
                Pair<?, ?> direction = (Pair<?, ?>) rotatedMove.getSecond();
                double x = ((Number) jump.getFirst()).doubleValue()
                    + ((Number) displacement.getFirst()).doubleValue();
                double z = ((Number) jump.getSecond()).doubleValue()
                    + ((Number) displacement.getSecond()).doubleValue();
                jumped.add(TuplesKt.to(TuplesKt.to(x, z), (Pair<Float, Float>) direction));
            }
        }
        me.rerere.matrix.util.DelegatingMutableList jumpedList = new me.rerere.matrix.util.DelegatingMutableList(jumped);

        ArrayList<Pair<Pair<Double, Double>, Pair<Float, Float>>> withVelocity = new ArrayList<>();
        for (Object velocityCandidate : (List<?>) velocityCandidates.backingList()) {
            Pair<?, ?> impulse = (Pair<?, ?>) velocityCandidate;
            org.bukkit.util.Vector impulseVector = (org.bukkit.util.Vector) impulse.getFirst();
            for (Object jumpedMoveValue : (List<?>) jumpedList.backingList()) {
                Pair<?, ?> jumpedMove = (Pair<?, ?>) jumpedMoveValue;
                Pair<?, ?> displacement = (Pair<?, ?>) jumpedMove.getFirst();
                Pair<?, ?> direction = (Pair<?, ?>) jumpedMove.getSecond();
                withVelocity.add(TuplesKt.to(
                    TuplesKt.to(impulseVector.getX() + ((Number) displacement.getFirst()).doubleValue(),
                        impulseVector.getZ() + ((Number) displacement.getSecond()).doubleValue()),
                    (Pair<Float, Float>) direction));
            }
        }
        me.rerere.matrix.util.DelegatingMutableList velocityMoves = new me.rerere.matrix.util.DelegatingMutableList(withVelocity);

        ArrayList<Pair<org.bukkit.util.Vector, Pair<Float, Float>>> absoluteCandidates = new ArrayList<>();
        for (Object movementCandidate : (List<?>) velocityMoves.backingList()) {
            Pair<?, ?> candidate = (Pair<?, ?>) movementCandidate;
            Pair<?, ?> displacement = (Pair<?, ?>) candidate.getFirst();
            Pair<?, ?> direction = (Pair<?, ?>) candidate.getSecond();
            org.bukkit.Location from = context.getFrom();
            org.bukkit.Location to = context.getTo();
            absoluteCandidates.add(TuplesKt.to(new org.bukkit.util.Vector(
                from.getX() + ((Number) displacement.getFirst()).doubleValue(), to.getY(),
                from.getZ() + ((Number) displacement.getSecond()).doubleValue()),
                (Pair<Float, Float>) direction));
        }
        me.rerere.matrix.util.DelegatingMutableList candidateList = new me.rerere.matrix.util.DelegatingMutableList(absoluteCandidates);

        Iterator<?> candidateIterator = ((List<?>) candidateList.backingList()).iterator();
        if (!candidateIterator.hasNext()) throw new java.util.NoSuchElementException();
        Pair<?, ?> nearestCandidate = (Pair<?, ?>) candidateIterator.next();
        if (candidateIterator.hasNext()) {
            org.bukkit.util.Vector firstVector = (org.bukkit.util.Vector) nearestCandidate.getFirst();
            double nearestX = firstVector.getX() - context.getTo().getX();
            double nearestZ = firstVector.getZ() - context.getTo().getZ();
            double nearestDistance = Math.sqrt(nearestX * nearestX + nearestZ * nearestZ);
            do {
                Pair<?, ?> candidate = (Pair<?, ?>) candidateIterator.next();
                org.bukkit.util.Vector candidateVector = (org.bukkit.util.Vector) candidate.getFirst();
                double candidateX = candidateVector.getX() - context.getTo().getX();
                double candidateZ = candidateVector.getZ() - context.getTo().getZ();
                double distance = Math.sqrt(candidateX * candidateX + candidateZ * candidateZ);
                if (Double.compare(nearestDistance, distance) > 0) {
                    nearestCandidate = candidate;
                    nearestDistance = distance;
                }
            } while (candidateIterator.hasNext());
        }

        org.bukkit.util.Vector nearestVector = (org.bukkit.util.Vector) nearestCandidate.getFirst();
        Pair<?, ?> chosenDirection = (Pair<?, ?>) nearestCandidate.getSecond();
        double targetDistance = nearestVector.distance(context.getTo().toVector());
        org.bukkit.ChatColor distanceColor = targetDistance > 0.01d
            ? org.bukkit.ChatColor.RED : org.bukkit.ChatColor.GREEN;
        double movementLength = context.getMovement().length();
        float directionA = ((Number) chosenDirection.getFirst()).floatValue();
        float directionB = ((Number) chosenDirection.getSecond()).floatValue();
        String keys = (directionA > 0.5f ? "W" : directionA < -0.5f ? "S" : "#")
            + (directionB > 0.5f ? "A" : directionB < -0.5f ? "D" : "#");
        org.bukkit.Bukkit.broadcastMessage(keySummary(
            org.bukkit.ChatColor.GRAY, keys, leavingFlag,
            me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(movementLength),
            me.rerere.matrix.util.text.DiagnosticFormat.formatFloat(
                context.getYaw()),
            distanceColor,
            me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(targetDistance)));

        if (targetDistance > 0.01d && !fromFlag && !toFlag) {
            StringBuilder details = new StringBuilder();
            org.bukkit.util.Vector movement = context.getMovement();
            details.append(moveSummary(
                me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(movement.getX()),
                me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(movement.getZ()),
                me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(velocity.getX()),
                me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(velocity.getZ())));
            Pair<?, ?> delta = TuplesKt.to(nearestVector.getX()
                - context.getFrom().getX(),
                nearestVector.getZ() - context.getFrom().getZ());
            details.append(predictedMoveSummary(
                me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(((Number) delta.getFirst()).doubleValue()),
                me.rerere.matrix.util.text.DiagnosticFormat.formatDouble(((Number) delta.getSecond()).doubleValue())));
            details.append(" / keyboard accel: ");
            for (Object rotatedMoveValue : (List<?>) rotatedList.backingList()) {
                Pair<?, ?> factor = (Pair<?, ?>) ((Pair<?, ?>) rotatedMoveValue).getFirst();
                details.append(pairText(factor.getFirst(), factor.getSecond()));
            }
            details.append(" / jump accel: ");
            for (Object jumpValue : (List<?>) jumpOffsets.backingList()) {
                Pair<?, ?> jump = (Pair<?, ?>) jumpValue;
                details.append(pairText(jump.getFirst(), jump.getSecond()));
            }
            for (Object velocityCandidate : playerContext.inventoryTracker().candidates(context.getVelocity())) {
                Pair<?, ?> candidate = (Pair<?, ?>) velocityCandidate;
                org.bukkit.util.Vector diagnosticVelocity = (org.bukkit.util.Vector) candidate.getFirst();
                me.rerere.matrix.player.VelocityData diagnosticData =
                    (me.rerere.matrix.player.VelocityData) candidate.getSecond();
                details.append(velocitySummary(
                    me.rerere.matrix.util.text.DiagnosticFormat.vector(diagnosticVelocity)));
            }
            org.bukkit.Bukkit.broadcastMessage(details.toString());
        }

        ArrayList<org.bukkit.util.Vector> resultVectors = new ArrayList<>();
        for (Object candidateValue : (List<?>) candidateList.backingList()) {
            resultVectors.add((org.bukkit.util.Vector) ((Pair<?, ?>) candidateValue).getFirst());
        }
        me.rerere.matrix.util.DelegatingMutableList resultList = new me.rerere.matrix.util.DelegatingMutableList(resultVectors);
        return new me.rerere.matrix.prediction.PredictionResult(
            resultList, TuplesKt.to(Boolean.TRUE, Boolean.TRUE),
            new org.bukkit.util.Vector(
                context.getMovement().getX() * friction,
                context.getMovement().getY(),
                context.getMovement().getZ() * friction));
    }
}
