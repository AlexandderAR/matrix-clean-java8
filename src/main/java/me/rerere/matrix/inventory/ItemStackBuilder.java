package me.rerere.matrix.inventory;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.util.text.ChatColorFormatter;

/** Mutable ItemStack wrapper used to build named, lore-bearing items. */
public final class ItemStackBuilder {
    private final ItemStack itemStack;

    private ItemStackBuilder(ItemStack itemStack) { this.itemStack = itemStack; }

    public static ItemStack addLoreLine(ItemStack item, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.add(formatLore(line, 1));
            } else {
                lore = java.util.Collections.singletonList(line);
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static void setDisplayName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(formatLore(name, 1));
        }
        item.setItemMeta(meta);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ItemStackBuilder)) return false;
        return Intrinsics.areEqual(itemStack, ((ItemStackBuilder) other).itemStack);
    }

    public static ItemStack addUnsafeEnchantment(ItemStack item, Enchantment enchantment, int level) {
        item.addUnsafeEnchantment(enchantment, level);
        return item;
    }

    public static void setAmount(ItemStack item, int amount) { item.setAmount(amount); }
    public static int getAmount(ItemStack item) { return item.getAmount(); }
    public static boolean itemStacksEqual(Object first, Object second) { return Intrinsics.areEqual(first, second); }
    public static void setDurability(ItemStack item, int durability) { item.setDurability((short) durability); }
    public static short getDurability(ItemStack item) { return item.getDurability(); }

    @Override
    public String toString() { return itemBuilderDescription(itemStack); }

    public static String getDisplayNameOrMaterial(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = meta.getDisplayName();
            if (displayName != null) return displayName;
        }
        return item.getType().name();
    }

    public static ItemStack identity(ItemStack item) { return item; }

    public static String formatDisplayNameOrMaterial(ItemStack item) {
        return itemBuilderDescription(item);
    }

    public static ItemStack mutateMeta(ItemStack item, Function1<? super ItemMeta, ?> action) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) action.invoke(meta);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getItemStack() { return itemStack; }

    public static ItemStack setSkullOwner(ItemStack item, Player owner) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_12)) {
                ((SkullMeta) meta).setOwningPlayer((OfflinePlayer) owner);
            } else {
                ((SkullMeta) meta).setOwner(owner != null ? owner.getName() : null);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    public static int itemStackHash(ItemStack item) { return item.hashCode(); }
    public static boolean itemBuilderEquals(ItemStack first, Object other) {
        if (!(other instanceof ItemStackBuilder)) return false;
        return Intrinsics.areEqual(first, ((ItemStackBuilder) other).getItemStack());
    }

    public static ItemStack appendLore(ItemStack item, List<String> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.addAll(formatLoreList(lines, 1));
            } else {
                lore = lines;
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static String itemBuilderDescription(Object item) {
        return "ItemBuilder(itemStack=" + item + ')';
    }

    public static ItemStackBuilder from(ItemStack item) { return new ItemStackBuilder(item); }
    public ItemStack itemStack() { return itemStack; }

    public static ItemStack replaceLore(ItemStack item, List<String> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> formatted = new ArrayList<>(lines.size());
            for (String line : lines) formatted.add(formatLore(line, 1));
            meta.setLore(formatted);
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public int hashCode() { return itemStack.hashCode(); }
    private static String formatLore(String line, int colorFlag) {
        return ChatColorFormatter.formatString(line, colorFlag);
    }

    private static List<String> formatLoreList(List<String> lines, int colorFlag) {
        return ChatColorFormatter.translateList(lines, colorFlag);
    }
}
