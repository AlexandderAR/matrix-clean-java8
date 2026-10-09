package me.rerere.matrix.geometry;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.world.MaterialPolicy;

/** Axis-aligned box recovered from the six coordinate fields and geometry operations in ASM. */
public final class AxisAlignedBox {
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    public AxisAlignedBox(double x1, double y1, double z1,
                          double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minY = Math.min(y1, y2);
        this.maxY = Math.max(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);
    }

    /** X/Z are centered on the vector; Y extends from its base by height. */
    public AxisAlignedBox(Vector base, double horizontalRadius, double height) {
        this.minX = base.getX() - horizontalRadius;
        this.maxX = base.getX() + horizontalRadius;
        this.minZ = base.getZ() - horizontalRadius;
        this.maxZ = base.getZ() + horizontalRadius;
        this.minY = base.getY();
        this.maxY = base.getY() + height;
    }

    public double minX() { return minX; }
    public double minY() { return minY; }
    public double minZ() { return minZ; }
    public double maxX() { return maxX; }
    public double maxY() { return maxY; }
    public double maxZ() { return maxZ; }
    public double width() { return Math.abs(maxX - minX); }
    public double height() { return Math.abs(maxY - minY); }
    public double depth() { return Math.abs(maxZ - minZ); }

    public void setMinY(double value) { minY = value; }

    public AxisAlignedBox union(AxisAlignedBox other) {
        return new AxisAlignedBox(Math.min(minX, other.minX), Math.min(minY, other.minY),
                Math.min(minZ, other.minZ), Math.max(maxX, other.maxX),
                Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ));
    }

    public AxisAlignedBox symmetricallyExpandedCopy(double dx, double dy, double dz) {
        return new AxisAlignedBox(minX - dx, minY - dy, minZ - dz,
                maxX + dx, maxY + dy, maxZ + dz);
    }

    /** Mutates the box by subtracting from minima and adding to maxima. */
    public AxisAlignedBox grow(double dx, double dy, double dz) {
        minX -= dx; maxX += dx;
        minY -= dy; maxY += dy;
        minZ -= dz; maxZ += dz;
        return this;
    }

    /** Mutates each minimum/maximum outwards according to the delta sign. */
    public AxisAlignedBox expand(double dx, double dy, double dz) {
        if (dx < 0) minX += dx; else if (dx > 0) maxX += dx;
        if (dy < 0) minY += dy; else if (dy > 0) maxY += dy;
        if (dz < 0) minZ += dz; else if (dz > 0) maxZ += dz;
        return this;
    }

    public AxisAlignedBox inset(double amount) {
        if (amount <= 0.0) throw new IllegalArgumentException("size must be positive");
        double insetMinX = minX + amount;
        double insetMinY = minY + amount;
        double insetMinZ = minZ + amount;
        double insetMaxX = maxX - amount;
        double insetMaxY = maxY - amount;
        double insetMaxZ = maxZ - amount;
        if (insetMinX > insetMaxX || insetMinY > insetMaxY || insetMinZ > insetMaxZ) {
            throw new IllegalArgumentException("size is too big");
        }
        return new AxisAlignedBox(insetMinX, insetMinY, insetMinZ,
                insetMaxX, insetMaxY, insetMaxZ);
    }

    public AxisAlignedBox copy() {
        return new AxisAlignedBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static AxisAlignedBox at(Location location) {
        return new AxisAlignedBox(location.getX(), location.getY(), location.getZ(),
                location.getX(), location.getY(), location.getZ());
    }

    public double verticalSpanOverlap(AxisAlignedBox other) {
        return Math.abs(Math.min(other.maxY, maxY) - Math.max(other.minY, minY));
    }

    public double overlapX(AxisAlignedBox other) {
        return Math.min(Math.abs(other.minX - maxX), Math.abs(other.maxX - minX));
    }

    public double overlapY(AxisAlignedBox other) {
        return Math.min(Math.abs(other.minY - maxY), Math.abs(other.maxY - minY));
    }

    public double overlapZ(AxisAlignedBox other) {
        return Math.min(Math.abs(other.minZ - maxZ), Math.abs(other.maxZ - minZ));
    }

    public double distance(Vector point) {
        double dx = Math.min(Math.abs(point.getX() - maxX), Math.abs(point.getX() - minX));
        double dy = Math.min(Math.abs(point.getY() - maxY), Math.abs(point.getY() - minY));
        double dz = Math.min(Math.abs(point.getZ() - maxZ), Math.abs(point.getZ() - minZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double diagonalLength() {
        return Math.sqrt(Math.pow(maxX - minX, 2.0) + Math.pow(maxY - minY, 2.0)
                + Math.pow(maxZ - minZ, 2.0));
    }

    public Vector minimumCorner() { return new Vector(minX, minY, minZ); }
    public Vector maximumCorner() { return new Vector(maxX, maxY, maxZ); }
    public Vector center() { return new Vector((maxX + minX) / 2.0,
            (maxY + minY) / 2.0, (maxZ + minZ) / 2.0); }
    public double[] centerCoordinates() { return new double[] { (maxX + minX) / 2.0,
            (maxY + minY) / 2.0, (maxZ + minZ) / 2.0 }; }

    public List<Block> cornerBlocks(World world) {
        List<Block> cornerBlocks = new ArrayList<>();
        cornerBlocks.add(new Location(world, minX, minY, minZ).getBlock());
        cornerBlocks.add(new Location(world, minX, maxY, minZ).getBlock());
        cornerBlocks.add(new Location(world, maxX, maxY, maxZ).getBlock());
        cornerBlocks.add(new Location(world, maxX, minY, maxZ).getBlock());
        return cornerBlocks;
    }

    @Override public Object clone() { return copy(); }

    public void setMinX(double value) { minX = value; }
    public void setMaxZ(double value) { maxZ = value; }
    public void setMaxX(double value) { maxX = value; }
    public void setMinZ(double value) { minZ = value; }
    public void setMaxY(double value) { maxY = value; }

    public void translate(double dx, double dy, double dz) {
        minX += dx; minY += dy; minZ += dz;
        maxX += dx; maxY += dy; maxZ += dz;
    }

    public AxisAlignedBox lowerYBy(double amount) {
        minY -= amount;
        return this;
    }

    public AxisAlignedBox raiseMaxYBy(double amount) {
        maxY += amount;
        return this;
    }

    public double horizontalDiagonal() {
        return Math.sqrt(Math.pow(maxX - minX, 2.0) + Math.pow(maxZ - minZ, 2.0));
    }

    public static AxisAlignedBox aroundVector(Vector vector, float horizontal, float height) {
        double half = horizontal / 2.0f;
        return new AxisAlignedBox(vector.getX() - half, vector.getY(), vector.getZ() - half,
                vector.getX() + half, vector.getY() + (double) height,
                vector.getZ() + half);
    }

    private Location centerLocation(World world) {
        double[] center = centerCoordinates();
        return new Location(world, center[0], center[1], center[2]);
    }

    public static boolean isMode() {
        return false;
    }

    public static boolean isInverseMode() {
        return true;
    }

    public double distance(Location location) {
        Location center = centerLocation(location.getWorld());
        return location.distance(center);
    }

    @Override public String toString() {
        return "{minX=" + minX + ", minY=" + minY + ", minZ=" + minZ
                + ", maxX=" + maxX + ", maxY=" + maxY + ", maxZ=" + maxZ + "}";
    }

    @Override public int hashCode() {
        int result = 1;
        double[] values = { minX, minY, minZ, maxX, maxY, maxZ };
        for (double value : values) {
            long bits = Double.doubleToLongBits(value);
            result = 59 * result + (int) (bits >>> 32 ^ bits);
        }
        return result;
    }

    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof AxisAlignedBox)) return false;
        AxisAlignedBox box = (AxisAlignedBox) other;
        return Double.compare(minX, box.minX) == 0
                && Double.compare(minY, box.minY) == 0
                && Double.compare(minZ, box.minZ) == 0
                && Double.compare(maxX, box.maxX) == 0
                && Double.compare(maxY, box.maxY) == 0
                && Double.compare(maxZ, box.maxZ) == 0;
    }


    public boolean contains(Vector point) {
        double x = point.getX();
        double y = point.getY();
        double z = point.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public org.bukkit.util.Vector clipSegmentAgainstBox(
            Ray segment,
            float lowerTime, float upperTime) {
        double inverseX = 1.0 / segment.getDirection().getX();
        double inverseY = 1.0 / segment.getDirection().getY();
        double inverseZ = 1.0 / segment.getDirection().getZ();
        boolean negativeX = inverseX < 0.0;
        boolean negativeY = inverseY < 0.0;
        boolean negativeZ = inverseZ < 0.0;
        Vector lower = new Vector(minX, minY, minZ);
        Vector upper = new Vector(maxX, maxY, maxZ);

        double firstX = ((negativeX ? upper : lower).getX()
                - segment.getOrigin().getX()) * inverseX;
        double secondX = ((negativeX ? lower : upper).getX()
                - segment.getOrigin().getX()) * inverseX;
        double firstY = ((negativeY ? upper : lower).getY()
                - segment.getOrigin().getY()) * inverseY;
        double secondY = ((negativeY ? lower : upper).getY()
                - segment.getOrigin().getY()) * inverseY;
        if (firstX > secondY || firstY > secondX) return null;

        double entry = firstX;
        if (firstY > entry) entry = firstY;
        double exit = secondX;
        if (secondY < exit) exit = secondY;

        double firstZ = ((negativeZ ? upper : lower).getZ()
                - segment.getOrigin().getZ()) * inverseZ;
        double secondZ = ((negativeZ ? lower : upper).getZ()
                - segment.getOrigin().getZ()) * inverseZ;
        if (entry > secondZ || firstZ > exit) return null;
        if (firstZ > entry) entry = firstZ;
        if (secondZ < exit) exit = secondZ;

        if (Double.isNaN(entry) || Double.isNaN(upperTime) || entry >= upperTime) return null;
        if (Double.isNaN(exit) || Double.isNaN(lowerTime) || exit <= lowerTime) return null;
        return segment.pointAt(entry);
    }

    public boolean intersectsAny(AxisAlignedBox... boxes) {
        int length = boxes.length;
        for (int index = 0; index < length; index++) {
            if (intersects(boxes[index])) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        AxisAlignedBox first = new AxisAlignedBox(0.0, -1.0, 0.0, 1.0, 1.0, 1.0);
        AxisAlignedBox second = new AxisAlignedBox(0.0, 0.0, 0.0, 1.0, 0.0, 1.0);
        boolean overlap = first.intersects(second);
        boolean point = isPointVolume(second);
        System.out.println(overlap + "/" + point);
    }

    public boolean scanWorldBlocks(World world) {
        int maximumX = (int) Math.ceil(maxX);
        int maximumY = (int) Math.ceil(maxY);
        int maximumZ = (int) Math.ceil(maxZ);
        int firstX = (int) Math.floor(minX);
        for (int x = firstX; x <= maximumX; x++) {
            int firstY = (int) Math.floor(minY);
            for (int y = firstY; y <= maximumY; y++) {
                int firstZ = (int) Math.floor(minZ);
                for (int z = firstZ; z <= maximumZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    boolean nonAir = !MaterialPolicy.isAirMaterial(block.getType());
                    if (nonAir) return true;
                    if (Matrix.getInstance().getClientBlockManager().contains(block)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isPointVolume(AxisAlignedBox box) {
        return box.maxX == box.minX
                && box.maxZ == box.minZ
                && box.maxY == box.minY;
    }

    public boolean intersects(AxisAlignedBox other) {
        if (isPointVolume(this)) return false;
        if (isPointVolume(other)) return false;

        double overlapMinX = Math.max(minX, other.minX);
        double overlapMinY = Math.max(minY, other.minY);
        double overlapMinZ = Math.max(minZ, other.minZ);
        double overlapMaxX = Math.min(maxX, other.maxX);
        double overlapMaxY = Math.min(maxY, other.maxY);
        double overlapMaxZ = Math.min(maxZ, other.maxZ);

        if (height() == 0.0) {
            return overlapMaxX > overlapMinX && overlapMaxZ > overlapMinZ
                    && maxY > other.minY && maxY < other.maxY;
        }
        return overlapMaxX > overlapMinX && overlapMaxY > overlapMinY
                && overlapMaxZ > overlapMinZ;
    }
}
