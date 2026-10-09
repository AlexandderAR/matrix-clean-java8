package me.rerere.matrix.world;

import org.bukkit.attribute.Attribute;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.util.reflect.ReflectionLookup;

/** Lazy resolution of Bukkit attribute enum constants across server API versions. */
public final class BukkitAttributes {
    public static final BukkitAttributes INDEX = new BukkitAttributes();
    private static final Lazy<Attribute> JUMP_STRENGTH = LazyKt.lazy((Function0<Attribute>) BukkitAttributes::jumpStrength);
    private static final Lazy<Attribute> MOVEMENT_SPEED = LazyKt.lazy((Function0<Attribute>) BukkitAttributes::movementSpeed);
    private static final Lazy<Attribute> INTERACTION_RANGE = LazyKt.lazy((Function0<Attribute>) BukkitAttributes::interactionRange);

    private BukkitAttributes() { }

    private static Attribute movementSpeed() {
        String[] names = {"MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED"};
        return resolveAttribute(names);
    }
    private static Attribute interactionRange() {
        String[] names = {"ENTITY_INTERACTION_RANGE", "PLAYER_ENTITY_INTERACTION_RANGE"};
        return resolveAttribute(names);
    }
    private static Attribute jumpStrength() {
        String[] names = {"HORSE_JUMP_STRENGTH", "JUMP_STRENGTH", "GENERIC_JUMP_STRENGTH"};
        return resolveAttribute(names);
    }

    private static Attribute resolveAttribute(String[] names) {
        try {
            return (Attribute) ReflectionLookup.getStaticConstant(Attribute.class, names);
        } catch (IllegalAccessException failure) {
            return BukkitAttributes.<RuntimeException, Attribute>throwUnchecked(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }
    public Attribute jumpStrengthValue() { return (Attribute) JUMP_STRENGTH.getValue(); }
    public Attribute interactionRangeValue() { return (Attribute) INTERACTION_RANGE.getValue(); }
    public Attribute movementSpeedValue() { return (Attribute) MOVEMENT_SPEED.getValue(); }
}




