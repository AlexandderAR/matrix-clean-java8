package me.rerere.matrix.player.model;

import me.rerere.matrix.world.CollisionRegion;

/** Immutable movement facts prepared for a movement check. */
public class PreparedMovementData {
    private final boolean piston;
    private final boolean onSlime;
    private final boolean onBed;
    private final CollisionRegion collisionCache;

    public PreparedMovementData(boolean piston, boolean onSlime, boolean onBed,
                                CollisionRegion collisionCache) {
        if (collisionCache == null) {
            throw new NullPointerException("collisionCache is marked non-null but is null");
        }
        this.piston = piston;
        this.onSlime = onSlime;
        this.onBed = onBed;
        this.collisionCache = collisionCache;
    }

    public boolean isPiston() { return piston; }
    public boolean isOnSlime() { return onSlime; }
    public boolean isOnBed() { return onBed; }
    public CollisionRegion getCollisionCache() { return collisionCache; }

    public boolean canEqual(Object candidate) {
        return candidate instanceof PreparedMovementData;
    }

    private static String formatPreparedData(boolean piston, boolean onSlime,
                                             boolean onBed, String collisionCache) {
        return new StringBuilder("PreparedData(isPiston=").append(piston)
                .append(", onSlime=").append(onSlime)
                .append(", onBed=").append(onBed)
                .append(", collisionCache=").append(collisionCache).append(")").toString();
    }

    @Override
    public String toString() {
        return formatPreparedData(isPiston(), isOnSlime(), isOnBed(),
                String.valueOf(getCollisionCache()));
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 59 + (isPiston() ? 79 : 97);
        hash = hash * 59 + (isOnSlime() ? 79 : 97);
        hash = hash * 59 + (isOnBed() ? 79 : 97);
        CollisionRegion cache = getCollisionCache();
        return hash * 59 + (cache == null ? 43 : cache.hashCode());
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (!(object instanceof PreparedMovementData)) return false;
        PreparedMovementData other = (PreparedMovementData) object;
        if (!other.canEqual(this)) return false;
        if (isPiston() != other.isPiston()) return false;
        if (isOnSlime() != other.isOnSlime()) return false;
        if (isOnBed() != other.isOnBed()) return false;
        CollisionRegion cache = getCollisionCache();
        CollisionRegion otherCache = other.getCollisionCache();
        if (cache == null) return otherCache == null;
        return cache.equals(otherCache);
    }
}
