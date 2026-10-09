package me.rerere.matrix.gui;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function3;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class InventoryMenuItem {
    private Function0<ItemStack> iconSupplier;
    private Function3<InventoryMenuItem, Player, ClickType, Unit> clickHandler;
    public InventoryMenuItem() { }
    public final void setIconSupplier(Function0<ItemStack> supplier) { iconSupplier = supplier; }
    public final ItemStack getIcon() {
        Function0<ItemStack> supplier = iconSupplier;
        ItemStack stack = supplier == null ? null : supplier.invoke();
        if (stack == null) throw new IllegalStateException("Icon not set");
        return stack;
    }
    public final void setClickHandler(Function3<InventoryMenuItem, Player, ClickType, Unit> handler) { clickHandler = handler; }
    public final void onClick(InventoryClickEvent event) {
        Function3<InventoryMenuItem, Player, ClickType, Unit> handler = clickHandler;
        if (handler != null) {
            HumanEntity player = event.getWhoClicked();
            Intrinsics.checkNotNull(player, "null cannot be cast to non-null type org.bukkit.entity.Player");
            handler.invoke(this, (Player) player, event.getClick());
        }
    }
}
