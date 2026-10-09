package me.rerere.matrix.gui;

import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.gui.InventoryLayoutMenu;
import me.rerere.matrix.inventory.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class InventoryMenuBuilders {
    public static InventoryMenuItem item(Function1<InventoryMenuItem, ?> configure) {
        InventoryMenuItem item = new InventoryMenuItem();
        configure.invoke(item);
        return item;
    }

    public static InventoryLayoutMenu menu(Function1<InventoryLayoutMenu, ?> configure) {
        InventoryLayoutMenu menu = new InventoryLayoutMenu();
        configure.invoke(menu);
        return menu;
    }

    public static void example() {
        InventoryLayoutMenu menu = new InventoryLayoutMenu();
        menu.setLayout(new String[] { "#########", "#       #", "#       #", "#       #" });
        menu.setTitleSupplier(() -> "Hello");
        menu.registerButton('#', item -> {
            item.setIconSupplier(() -> ItemStackBuilder.identity(new ItemStack(Material.AIR)));
            return Unit.INSTANCE;
        });
    }
    public static void main(String[] arguments) {
        example();
    }
}
