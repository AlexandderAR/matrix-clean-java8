package me.rerere.matrix.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.inventory.ItemStackBuilder;
import me.rerere.matrix.util.ItemStackConsumer;

/** Creates an ItemStack and applies the supplied Kotlin callback to it. */
public final class ItemStackFactory {
    private ItemStackFactory() { }

    public static ItemStack create(ItemStack supplied,
            Function1<? super ItemStackBuilder, ?> configure) {
        ItemStack result = supplied == null ? new ItemStack(Material.AIR) : supplied;
        result = ItemStackBuilder.identity(result);
        configure.invoke(ItemStackBuilder.from(result));
        return result;
    }

    public static ItemStack create(Material material,
            Function1<? super ItemStackBuilder, ?> configure) {
        return create(material, configure, 0);
    }

    /** Creates an item while applying the Kotlin default for the optional configure callback. */
    @SuppressWarnings("unchecked")
    public static ItemStack create(Material material,
            Function1<? super ItemStackBuilder, ?> configure, int defaultArgumentMask) {
        if ((defaultArgumentMask & 2) != 0)
            configure = (Function1<? super ItemStackBuilder, ?>) ItemStackConsumer.INSTANCE;
        ItemStack result = ItemStackBuilder.identity(new ItemStack(material));
        configure.invoke(ItemStackBuilder.from(result));
        return result;
    }
}
