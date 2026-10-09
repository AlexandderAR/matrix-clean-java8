package me.rerere.matrix.player;

/** One captured rotation and displacement sample used by movement checks. */
public final class MovementSample {
    private final float yaw;
    private final float pitch;
    private final float deltaYaw;
    private final float deltaPitch;
    private final float attackYaw;
    private final float attackPitch;
    private final double deltaX;
    private final double deltaY;
    private final double deltaZ;
    private final double rx;
    private final double rz;
    private final boolean attack;

    public MovementSample(float yaw, float pitch, float deltaYaw, float deltaPitch,
                      float attackYaw, float attackPitch, double deltaX, double deltaY,
                      double deltaZ, double rx, double rz, boolean attack) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
        this.attackYaw = attackYaw;
        this.attackPitch = attackPitch;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.rx = rx;
        this.rz = rz;
        this.attack = attack;
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getDeltaYaw() { return deltaYaw; }
    public float getDeltaPitch() { return deltaPitch; }
    public float getAttackYaw() { return attackYaw; }
    public float getAttackPitch() { return attackPitch; }
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
    public double getDeltaZ() { return deltaZ; }
    public double getRx() { return rx; }
    public double getRz() { return rz; }
    public boolean isAttack() { return attack; }
    public float component1() { return yaw; }
    public float component2() { return pitch; }
    public float component3() { return deltaYaw; }
    public float component4() { return deltaPitch; }
    public float component5() { return attackYaw; }
    public float component6() { return attackPitch; }
    public double component7() { return deltaX; }
    public double component8() { return deltaY; }
    public double component9() { return deltaZ; }
    public double component10() { return rx; }
    public double component11() { return rz; }
    public boolean component12() { return attack; }

    public MovementSample withValues(float yaw, float pitch, float deltaYaw, float deltaPitch,
                                 float attackYaw, float attackPitch, double deltaX, double deltaY,
                                 double deltaZ, double rx, double rz, boolean attack) {
        return new MovementSample(yaw, pitch, deltaYaw, deltaPitch, attackYaw, attackPitch,
                deltaX, deltaY, deltaZ, rx, rz, attack);
    }

    /** Kotlin data-class copy$default behavior, including each independent mask bit. */
    public static MovementSample copy$default(MovementSample source, float yaw, float pitch,
            float deltaYaw, float deltaPitch, float attackYaw, float attackPitch,
            double deltaX, double deltaY, double deltaZ, double rx, double rz,
            boolean attack, int mask, Object marker) {
        if ((mask & 1) != 0) yaw = source.yaw;
        if ((mask & 2) != 0) pitch = source.pitch;
        if ((mask & 4) != 0) deltaYaw = source.deltaYaw;
        if ((mask & 8) != 0) deltaPitch = source.deltaPitch;
        if ((mask & 16) != 0) attackYaw = source.attackYaw;
        if ((mask & 32) != 0) attackPitch = source.attackPitch;
        if ((mask & 64) != 0) deltaX = source.deltaX;
        if ((mask & 128) != 0) deltaY = source.deltaY;
        if ((mask & 256) != 0) deltaZ = source.deltaZ;
        if ((mask & 512) != 0) rx = source.rx;
        if ((mask & 1024) != 0) rz = source.rz;
        if ((mask & 2048) != 0) attack = source.attack;
        return source.withValues(yaw, pitch, deltaYaw, deltaPitch, attackYaw, attackPitch,
                deltaX, deltaY, deltaZ, rx, rz, attack);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MovementSample)) return false;
        MovementSample that = (MovementSample) other;
        return Float.compare(yaw, that.yaw) == 0
                && Float.compare(pitch, that.pitch) == 0
                && Float.compare(deltaYaw, that.deltaYaw) == 0
                && Float.compare(deltaPitch, that.deltaPitch) == 0
                && Float.compare(attackYaw, that.attackYaw) == 0
                && Float.compare(attackPitch, that.attackPitch) == 0
                && Double.compare(deltaX, that.deltaX) == 0
                && Double.compare(deltaY, that.deltaY) == 0
                && Double.compare(deltaZ, that.deltaZ) == 0
                && Double.compare(rx, that.rx) == 0
                && Double.compare(rz, that.rz) == 0
                && attack == that.attack;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(yaw);
        result = 31 * result + Float.hashCode(pitch);
        result = 31 * result + Float.hashCode(deltaYaw);
        result = 31 * result + Float.hashCode(deltaPitch);
        result = 31 * result + Float.hashCode(attackYaw);
        result = 31 * result + Float.hashCode(attackPitch);
        result = 31 * result + Double.hashCode(deltaX);
        result = 31 * result + Double.hashCode(deltaY);
        result = 31 * result + Double.hashCode(deltaZ);
        result = 31 * result + Double.hashCode(rx);
        result = 31 * result + Double.hashCode(rz);
        result = 31 * result + Boolean.hashCode(attack);
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder("SampleTick(yaw=").append(yaw)
                .append(", pitch=").append(pitch)
                .append(", dYaw=").append(deltaYaw)
                .append(", dPitch=").append(deltaPitch)
                .append(", aYaw=").append(attackYaw)
                .append(", aPitch=").append(attackPitch)
                .append(", dx=").append(deltaX)
                .append(", dy=").append(deltaY)
                .append(", dz=").append(deltaZ)
                .append(", rx=").append(rx)
                .append(", rz=").append(rz)
                .append(", attack=").append(attack).append(')').toString();
    }
}
