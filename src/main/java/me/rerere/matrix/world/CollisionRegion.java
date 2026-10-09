package me.rerere.matrix.world;

import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.util.Vector;

/** A collision region and its material sets. */
public class CollisionRegion {
    private final Vector from;
    private final Vector to;
    private final Set<Material> collisionMaterials;
    private final boolean primaryFlag;
    private final boolean secondaryFlag;
    private final boolean tertiaryFlag;
    private final Set<Material> largerCollisionMaterials;

    public CollisionRegion(Vector from, Vector to, Set<Material> collisionMaterials,
                          boolean primaryFlag, boolean secondaryFlag, boolean tertiaryFlag,
                          Set<Material> largerCollisionMaterials) {
        if (from == null) throw new NullPointerException("from is marked non-null but is null");
        if (to == null) throw new NullPointerException("to is marked non-null but is null");
        if (collisionMaterials == null) {
            throw new NullPointerException("collisionTypes is marked non-null but is null");
        }
        if (largerCollisionMaterials == null) {
            throw new NullPointerException("biggerCollisionTypes is marked non-null but is null");
        }
        this.from = from;
        this.to = to;
        this.collisionMaterials = collisionMaterials;
        this.primaryFlag = primaryFlag;
        this.secondaryFlag = secondaryFlag;
        this.tertiaryFlag = tertiaryFlag;
        this.largerCollisionMaterials = largerCollisionMaterials;
    }

    public Set<Material> collisionMaterials() { return collisionMaterials; }
    public Set<Material> largerCollisionMaterials() { return largerCollisionMaterials; }
    public boolean secondaryFlag() { return secondaryFlag; }
    public boolean tertiaryFlag() { return tertiaryFlag; }
    public boolean primaryFlag() { return primaryFlag; }
    public Vector toVector() { return to; }
    public Vector fromVector() { return from; }

    public boolean intersectsLargerMaterials(Material... materials) {
        for (Material material : materials) {
            if (largerCollisionMaterials.contains(material)) return true;
        }
        return false;
    }

    public boolean intersectsCollisionMaterials(Material... materials) {
        for (Material material : materials) {
            if (collisionMaterials.contains(material)) return true;
        }
        return false;
    }

    public boolean containsAnyLargerSetElement(Set<?> candidateMaterials) {
        for (Object material : candidateMaterials) {
            if (largerCollisionMaterials.contains((Material) material)) return true;
        }
        return false;
    }

    public void mergeMaterials(CollisionRegion other) {
        collisionMaterials.addAll(other.collisionMaterials);
        largerCollisionMaterials.addAll(other.largerCollisionMaterials);
    }

    public boolean isCollisionRegion(Object candidate) {
        return candidate instanceof CollisionRegion;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CollisionRegion)) return false;
        CollisionRegion that = (CollisionRegion) other;
        if (!that.isCollisionRegion(this)) return false;
        if (primaryFlag() != that.primaryFlag() || secondaryFlag() != that.secondaryFlag()) return false;
        if (tertiaryFlag() != that.tertiaryFlag()) return false;
        Vector thisFrom = fromVector(), thatFrom = that.fromVector();
        if (!(thisFrom == null ? thatFrom == null : thisFrom.equals(thatFrom))) return false;
        Vector thisTo = toVector(), thatTo = that.toVector();
        if (!(thisTo == null ? thatTo == null : thisTo.equals(thatTo))) return false;
        Set<Material> thisMaterials = collisionMaterials(), thatMaterials = that.collisionMaterials();
        if (!(thisMaterials == null ? thatMaterials == null : thisMaterials.equals(thatMaterials))) return false;
        Set<Material> thisLargerMaterials = largerCollisionMaterials(), thatLargerMaterials = that.largerCollisionMaterials();
        return thisLargerMaterials == null ? thatLargerMaterials == null : thisLargerMaterials.equals(thatLargerMaterials);
    }

    @Override public int hashCode() {
        int result = 1;
        result = result * 59 + (primaryFlag() ? 79 : 97);
        result = result * 59 + (secondaryFlag() ? 79 : 97);
        result = result * 59 + (tertiaryFlag() ? 79 : 97);
        Vector fromValue = fromVector();
        result = result * 59 + (fromValue == null ? 43 : fromValue.hashCode());
        Vector toValue = toVector();
        result = result * 59 + (toValue == null ? 43 : toValue.hashCode());
        Set<Material> materials = collisionMaterials();
        result = result * 59 + (materials == null ? 43 : materials.hashCode());
        Set<Material> largerMaterials = largerCollisionMaterials();
        result = result * 59 + (largerMaterials == null ? 43 : largerMaterials.hashCode());
        return result;
    }

    @Override public String toString() {
        String materialNames = collisionMaterials.stream().map(Enum::name).collect(Collectors.joining(","));
        String largerMaterialNames = largerCollisionMaterials.stream().map(Enum::name).collect(Collectors.joining(","));
        return "liquid: " + primaryFlag + " types: " + materialNames + " out: " + largerMaterialNames;
    }
}
