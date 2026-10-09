package me.rerere.matrix.compat.bukkit;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;

public final class EfficiencyEnchantment {
    public static final EfficiencyEnchantment INSTANCE = new EfficiencyEnchantment();
    private static final Lazy<Enchantment> efficiency = LazyKt.lazy(EfficiencyEnchantment::resolve);

    private EfficiencyEnchantment() {
    }

    private static Enchantment resolve() {
        try {
            Keyed entry = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
            if (entry == null) {
                throw new IllegalStateException("Required value was null.");
            }
            return (Enchantment) entry;
        } catch (Throwable failure) {
            try {
                Enchantment legacy = Enchantment.getByName("DIG_SPEED");
                if (legacy == null) {
                    throw new IllegalStateException("Required value was null.");
                }
                return legacy;
            } catch (Throwable fallbackFailure) {
                return EfficiencyEnchantment.<RuntimeException, Enchantment>throwUnchecked(fallbackFailure);
            }
        }
    }

    public Enchantment getEfficiency() {
        Lazy<Enchantment> value = efficiency;
        return value.getValue();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }
}
