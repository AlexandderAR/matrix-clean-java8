package me.rerere.matrix.movement;

import java.lang.ref.WeakReference;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import com.google.common.collect.Sets;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionType;
import me.rerere.matrix.checks.movement.MovementMetrics;

/** Mutable input and prediction values for one player movement. */
public class MoveData {
    private final Player player;
    private WeakReference<World> world;
    private Vector from, to;
    private double verticalDistance, horizontalDistance;
    private boolean fromGround, toGround, ignoreMoveCheck, invalid;
    private Set<String> tags;
    private double predictedHorizontalMove, frictionFactor;
    private float speedAttribute;
    private double predictedX, predictedZ;
    private boolean sneaking, sprinting;
    private final Set<PotionType> potionEffects;

    public MoveData(Player player, WeakReference<World> world, Vector from, Vector to,
            double verticalDistance, double horizontalDistance,
            boolean fromGround, boolean toGround, boolean ignoreMoveCheck, boolean invalid,
            Set<String> tags, double predictedHorizontalMove, double frictionFactor,
            float speedAttribute, double predictedX, double predictedZ,
            boolean sneaking, boolean sprinting, Set<PotionType> potionEffects) {
        if (player == null) throw new NullPointerException("player is marked non-null but is null");
        if (potionEffects == null) throw new NullPointerException("potionEffects is marked non-null but is null");
        this.player = player;
        this.world = world;
        this.from = from;
        this.to = to;
        this.verticalDistance = verticalDistance;
        this.horizontalDistance = horizontalDistance;
        this.fromGround = fromGround;
        this.toGround = toGround;
        this.ignoreMoveCheck = ignoreMoveCheck;
        this.invalid = invalid;
        this.tags = tags;
        this.predictedHorizontalMove = predictedHorizontalMove;
        this.frictionFactor = frictionFactor;
        this.speedAttribute = speedAttribute;
        this.predictedX = predictedX;
        this.predictedZ = predictedZ;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
        this.potionEffects = potionEffects;
    }

    public Player getPlayer() { return player; }
    public WeakReference<World> getWorld() { return world; }
    public Vector getFrom() { return from; }
    public Vector getTo() { return to; }
    public void setTo(Vector destination) { to = destination; }
    public double getVerticalDistance() { return verticalDistance; }
    public void setVerticalDistance(double distance) { verticalDistance = distance; }
    public double getHorizontalDistance() { return horizontalDistance; }
    public boolean isFromGround() { return fromGround; }
    public boolean isToGround() { return toGround; }
    public boolean isIgnoreMoveCheck() { return ignoreMoveCheck; }
    public void setIgnoreMoveCheck(boolean ignored) { ignoreMoveCheck = ignored; }
    public boolean isInvalid() { return invalid; }
    public void setInvalid(boolean invalidMove) { invalid = invalidMove; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> movementTags) { tags = movementTags; }
    public double getPredictedHorizontalMove() { return predictedHorizontalMove; }
    public void setPredictedHorizontalMove(double predictedDistance) { predictedHorizontalMove = predictedDistance; }
    public double getFrictionFactor() { return frictionFactor; }
    public void setFrictionFactor(double friction) { frictionFactor = friction; }
    public float getSpeedAttribute() { return speedAttribute; }
    public void setSpeedAttribute(float speed) { speedAttribute = speed; }
    public double getPredictedX() { return predictedX; }
    public void setPredictedX(double movementX) { predictedX = movementX; }
    public double getPredictedZ() { return predictedZ; }
    public void setPredictedZ(double movementZ) { predictedZ = movementZ; }
    public boolean isSneaking() { return sneaking; }
    public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }
    public boolean isSprinting() { return sprinting; }
    public Set<PotionType> getPotionEffects() { return potionEffects; }
    public int getPotionEffectCount() { return potionEffects.size(); }
    public boolean hasPotionEffect(PotionType effect) { return potionEffects.contains(effect); }
    public boolean canEqual(Object other) { return other instanceof MoveData; }

    public static MoveData fromEvent(PlayerMoveEvent event, boolean fromGround, boolean toGround,
            boolean ignoreMoveCheck, float speedAttribute, boolean sneaking, boolean sprinting,
            Set<PotionType> potionEffects) {
        return new MoveData(event.getPlayer(), null, event.getFrom().toVector(), event.getTo().toVector(),
                event.getTo().getY() - event.getFrom().getY(),
                MovementMetrics.locationDistance(event.getFrom(), event.getTo()),
                fromGround, toGround, ignoreMoveCheck, false, Sets.newLinkedHashSet(),
                0.0, 0x1.333334p-1, speedAttribute, 0.0, 0.0, sneaking, sprinting, potionEffects);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof MoveData)) return false;
        MoveData data = (MoveData) other;
        if (!data.canEqual(this)) return false;
        if (Double.compare(getVerticalDistance(), data.getVerticalDistance()) != 0) return false;
        if (Double.compare(getHorizontalDistance(), data.getHorizontalDistance()) != 0) return false;
        if (isFromGround() != data.isFromGround()) return false;
        if (isToGround() != data.isToGround()) return false;
        if (isIgnoreMoveCheck() != data.isIgnoreMoveCheck()) return false;
        if (isInvalid() != data.isInvalid()) return false;
        if (Double.compare(getPredictedHorizontalMove(), data.getPredictedHorizontalMove()) != 0) return false;
        if (Double.compare(getFrictionFactor(), data.getFrictionFactor()) != 0) return false;
        if (Float.compare(getSpeedAttribute(), data.getSpeedAttribute()) != 0) return false;
        if (Double.compare(getPredictedX(), data.getPredictedX()) != 0) return false;
        if (Double.compare(getPredictedZ(), data.getPredictedZ()) != 0) return false;
        if (isSneaking() != data.isSneaking()) return false;
        if (isSprinting() != data.isSprinting()) return false;
        if (!sameField(getPlayer(), data.getPlayer())) return false;
        if (!sameField(getWorld(), data.getWorld())) return false;
        if (!sameField(getFrom(), data.getFrom())) return false;
        if (!sameField(getTo(), data.getTo())) return false;
        if (!sameField(getTags(), data.getTags())) return false;
        return sameField(getPotionEffects(), data.getPotionEffects());
    }

    private static boolean sameField(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    private static int booleanHash(boolean value) {
        return value ? 79 : 97;
    }

    private static int objectHash(Object value) {
        return value == null ? 43 : value.hashCode();
    }

    @Override
    public int hashCode() {
        long verticalBits = Double.doubleToLongBits(getVerticalDistance());
        int hash = 59 + (int) ((verticalBits >>> 32) ^ verticalBits);
        hash = hash * 59 + Double.hashCode(getHorizontalDistance());
        hash = hash * 59 + booleanHash(isFromGround());
        hash = hash * 59 + booleanHash(isToGround());
        hash = hash * 59 + booleanHash(isIgnoreMoveCheck());
        hash = hash * 59 + booleanHash(isInvalid());
        hash = hash * 59 + Double.hashCode(getPredictedHorizontalMove());
        hash = hash * 59 + Double.hashCode(getFrictionFactor());
        hash = hash * 59 + Float.floatToIntBits(getSpeedAttribute());
        hash = hash * 59 + Double.hashCode(getPredictedX());
        hash = hash * 59 + Double.hashCode(getPredictedZ());
        hash = hash * 59 + booleanHash(isSneaking());
        hash = hash * 59 + booleanHash(isSprinting());
        hash = hash * 59 + objectHash(getPlayer());
        hash = hash * 59 + objectHash(getWorld());
        hash = hash * 59 + objectHash(getFrom());
        hash = hash * 59 + objectHash(getTo());
        hash = hash * 59 + objectHash(getTags());
        hash = hash * 59 + objectHash(getPotionEffects());
        return hash;
    }

    @Override
    public String toString() {
        String playerText = String.valueOf(getPlayer());
        String worldText = String.valueOf(getWorld());
        String fromText = String.valueOf(getFrom());
        String toText = String.valueOf(getTo());
        double vertical = getVerticalDistance();
        double horizontal = getHorizontalDistance();
        boolean initialGround = isFromGround();
        boolean finalGround = isToGround();
        boolean ignored = isIgnoreMoveCheck();
        boolean invalidMove = isInvalid();
        String tagsText = String.valueOf(getTags());
        double predictedHorizontal = getPredictedHorizontalMove();
        double friction = getFrictionFactor();
        float speed = getSpeedAttribute();
        double predictedMovementX = getPredictedX();
        double predictedMovementZ = getPredictedZ();
        boolean sneak = isSneaking();
        boolean sprint = isSprinting();
        String effectsText = String.valueOf(getPotionEffects());
        return "MoveData(player=" + playerText + ", world=" + worldText + ", from=" + fromText
                + ", to=" + toText + ", verticalDist=" + vertical + ", horizontalDist=" + horizontal
                + ", fromGround=" + initialGround + ", toGround=" + finalGround
                + ", ignoreMoveCheck=" + ignored + ", invalid=" + invalidMove + ", tags=" + tagsText
                + ", prePredictHorizonMove=" + predictedHorizontal + ", frictionFactor=" + friction
                + ", speedAttribute=" + speed + ", predX=" + predictedMovementX + ", predZ=" + predictedMovementZ
                + ", sneak=" + sneak + ", sprint=" + sprint + ", potionEffects=" + effectsText + ")";
    }
}
