package me.rerere.matrix.checks.combat;

import me.rerere.matrix.util.time.MillisecondSampler;

public class HitboxViolationIncrease {
    private final int amount;
    private long increaseTime;

    public HitboxViolationIncrease(int amount, long increaseTime) {
        this.amount = amount;
        this.increaseTime = increaseTime;
    }

    public int getAmount() {
        return amount;
    }

    public long getIncreaseTime() {
        return increaseTime;
    }

    public void setIncreaseTime(long timestamp) {
        increaseTime = timestamp;
    }

    public boolean canEqual(Object other) {
        return other instanceof HitboxViolationIncrease;
    }

    public static HitboxViolationIncrease create(int amount) {
        return new HitboxViolationIncrease(amount, MillisecondSampler.currentTimeMillis());
    }

    @Override
    public String toString() {
        int currentAmount = getAmount();
        long currentTime = getIncreaseTime();
        return new StringBuilder("HitBoxV3.VlIncrease(amount=").append(currentAmount)
                .append(", increaseTime=").append(currentTime).append(")").toString();
    }

    @Override
    public int hashCode() {
        int hash = 59 + getAmount();
        long currentTime = getIncreaseTime();
        return hash * 59 + (int) ((currentTime >>> 32) ^ currentTime);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (!(object instanceof HitboxViolationIncrease)) return false;
        HitboxViolationIncrease other = (HitboxViolationIncrease) object;
        boolean compatible = other.canEqual(this);
        if (!compatible) return false;
        int currentAmount = getAmount();
        if (currentAmount != other.getAmount()) return false;
        long currentTime = getIncreaseTime();
        long otherTime = other.getIncreaseTime();
        return currentTime == otherTime;
    }
}
