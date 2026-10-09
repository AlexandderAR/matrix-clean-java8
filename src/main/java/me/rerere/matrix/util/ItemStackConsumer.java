package me.rerere.matrix.util;

import me.rerere.matrix.inventory.ItemStackBuilder;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import org.bukkit.inventory.ItemStack;

/** Kotlin function object that reads an item builder's stack value. */
public final class ItemStackConsumer implements Function1 {
    public static final ItemStackConsumer INSTANCE;

    public ItemStackConsumer() {
    }

    @Override
    public Object invoke(Object value) {
        ItemStack stack = ((ItemStackBuilder) value).getItemStack();
        consumeStack(stack);
        return Unit.INSTANCE;
    }

    public final void consumeStack(ItemStack stack) {
    }

    static {
        INSTANCE = new ItemStackConsumer();
    }
}
