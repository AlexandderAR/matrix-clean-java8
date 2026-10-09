package me.rerere.matrix.geometry;

import org.bukkit.util.Vector;

/** A ray represented by a mutable Bukkit origin and direction vector. */
public class Ray implements Cloneable {
    private Vector origin;
    private Vector direction;
    public Ray(Vector origin, Vector direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public Vector getOrigin() { return origin; }
    public Vector getDirection() { return direction; }

    public Vector pointAt(double distance) {
        Vector scaledDirection = new Vector(direction.getX(), direction.getY(), direction.getZ());
        Vector copiedOrigin = new Vector(origin.getX(), origin.getY(), origin.getZ());
        return copiedOrigin.add(scaledDirection.multiply(distance));
    }

    public boolean isRay(Object value) { return value instanceof Ray; }

    @Override
    public Ray clone() { return cloneDeep(); }

    public Ray cloneDeep() {
        try {
            Ray copy = (Ray) super.clone();
            copy.origin = origin.clone();
            copy.direction = direction.clone();
            return copy;
        } catch (CloneNotSupportedException failure) {
            failure.printStackTrace();
            return null;
        }
    }

    private static String format(String origin, String direction) {
        return new StringBuilder("Ray(origin=").append(origin)
                .append(", direction=").append(direction).append(')').toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        Vector rayOrigin = getOrigin();
        result = 59 * result + (rayOrigin == null ? 43 : rayOrigin.hashCode());
        Vector rayDirection = getDirection();
        result = 59 * result + (rayDirection == null ? 43 : rayDirection.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Ray)) return false;
        Ray that = (Ray) other;
        if (!that.isRay(this)) return false;
        Vector thisOrigin = getOrigin();
        Vector otherOrigin = that.getOrigin();
        if (thisOrigin == null ? otherOrigin != null : !thisOrigin.equals(otherOrigin)) return false;
        Vector thisDirection = getDirection();
        Vector otherDirection = that.getDirection();
        if (thisDirection == null ? otherDirection != null : !thisDirection.equals(otherDirection)) return false;
        return true;
    }

    @Override
    public String toString() {
        return format(String.valueOf(getOrigin()), String.valueOf(getDirection()));
    }

}
