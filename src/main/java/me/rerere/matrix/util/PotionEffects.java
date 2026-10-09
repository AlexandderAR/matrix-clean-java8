package me.rerere.matrix.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;

/** Lazy Bukkit potion effect handles reconstructed from the class initializer and accessors. */
public final class PotionEffects {
    private static final Lazy<?> hasteLazy;
    private static final Lazy<?> jumpBoostLazy;
    public static final PotionEffects singleton;

    static {
        singleton = new PotionEffects();
        hasteLazy = LazyKt.lazy((Function0<PotionEffectType>) PotionEffects::haste);
        jumpBoostLazy = LazyKt.lazy((Function0<PotionEffectType>) PotionEffects::jumpBoost);
    }

    private PotionEffects() {}

    private static PotionEffectType resolve(String key, String legacyName) {
        try {
            PotionEffectType effect = Registry.EFFECT.get(NamespacedKey.minecraft(key));
            if (effect != null) return effect;
        } catch (Throwable ignored) {
            // Try the legacy name when the registry lookup is unavailable or fails.
        }
        PotionEffectType legacy = PotionEffectType.getByName(legacyName);
        if (legacy == null) throw new IllegalStateException("Required value was null.");
        return legacy;
    }

    private static PotionEffectType haste() { return resolve("haste", "FAST_DIGGING"); }
    private static PotionEffectType jumpBoost() { return resolve("jump_boost", "JUMP"); }

    public final PotionEffectType getJumpBoost() {
        return (PotionEffectType) jumpBoostLazy.getValue();
    }

    public final PotionEffectType getHaste() {
        return (PotionEffectType) hasteLazy.getValue();
    }
}
