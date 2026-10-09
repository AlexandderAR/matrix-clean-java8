package me.rerere.matrix.checks.combat;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.ItemStack;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

/** Shared packet-level item values used by combat and equipment checks. */
public final class CombatItemCatalog {
    public static final CombatItemCatalog INSTANCE;
    public static final ItemStack[] HELMETS;
    public static final ItemStack[] LEGGINGS;
    public static final ItemStack[] BOOTS;
    public static final ItemStack[] CHESTPLATES;
    public static final ItemStack[] HELD_ITEMS;

    static {
        INSTANCE = new CombatItemCatalog();
        HELD_ITEMS = new ItemStack[] {
                item(ItemTypes.IRON_SWORD),
                item(ItemTypes.IRON_PICKAXE),
                item(ItemTypes.DIAMOND_SWORD),
                item(ItemTypes.DIAMOND_AXE),
                item(ItemTypes.IRON_AXE),
                item(ItemTypes.FISHING_ROD),
                item(ItemTypes.TORCH)
        };
        BOOTS = new ItemStack[] {
                item(ItemTypes.IRON_BOOTS),
                item(ItemTypes.LEATHER_BOOTS),
                item(ItemTypes.DIAMOND_BOOTS)
        };
        LEGGINGS = new ItemStack[] {
                item(ItemTypes.IRON_LEGGINGS),
                item(ItemTypes.DIAMOND_LEGGINGS),
                item(ItemTypes.LEATHER_LEGGINGS)
        };
        CHESTPLATES = new ItemStack[] {
                item(ItemTypes.IRON_CHESTPLATE),
                item(ItemTypes.DIAMOND_CHESTPLATE),
                item(ItemTypes.LEATHER_CHESTPLATE)
        };
        HELMETS = new ItemStack[] {
                item(ItemTypes.DIAMOND_HELMET),
                item(ItemTypes.IRON_HELMET),
                item(ItemTypes.LEATHER_HELMET)
        };
    }

    private CombatItemCatalog() {
        super();
    }

    private static ItemStack item(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.type.ItemType type) {
        return ItemStack.builder().type(type).build();
    }

}
