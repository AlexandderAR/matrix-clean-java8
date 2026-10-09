package me.rerere.matrix.checks;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.InventoryViewCompat;
import me.rerere.matrix.inventory.InventoryResolver;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.time.MillisecondSampler;

public final class InventoryActionDelayCheck extends CheckRuntimeState {
    private long lastInteractionTime;
    private long lastCreativeInventoryActionTime;
    private long lastArmorMoveTime;
    private long lastHeldItemChangeTime;
    private boolean cancelNextClick;
    private static boolean cancelAction;
    private static int inventoryPenalty;

    public InventoryActionDelayCheck(PlayerCheckContext player) {
        super(player, CheckCategory.DELAY);
    }

    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getClick() == ClickType.SHIFT_LEFT
                && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getSlotType() == InventoryType.SlotType.CONTAINER) {
            InventoryResolver view = InventoryViewCompat.fromClickEvent(event);
            ItemStack current = event.getCurrentItem();
            if (view.getTopInventory().getType() == InventoryType.CRAFTING
                    && view.getBottomInventory().getType() == InventoryType.PLAYER
                    && current != null && isArmor(current.getType())) {
                long elapsed = MillisecondSampler.currentTimeMillis() - lastArmorMoveTime;
                lastArmorMoveTime = MillisecondSampler.currentTimeMillis();
                if (elapsed < 75L && inventoryPenalty > 0) {
                    flag("delay.inv",
                            "perform actions faster than expected // inventory (type C)", inventoryPenalty);
                    event.setResult(Event.Result.DENY);
                }
            }
        }
        if (cancelNextClick) {
            cancelNextClick = false;
            if (cancelAction) {
                event.setCancelled(true);
                event.getWhoClicked().closeInventory();
            }
        }
    }

    private static boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_CHESTPLATE") || name.endsWith("_HELMET")
                || name.endsWith("_BOOTS") || name.endsWith("_LEGGINGS");
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper wrapper) {
        boolean cancel = false;
        if (type == PacketType$Play$Client.HELD_ITEM_CHANGE
                || type == PacketType$Play$Client.CLOSE_WINDOW
                || type == PacketType$Play$Client.CLICK_WINDOW
                || PacketEventSupport.isBlockPlacementPacket(type)
                || PacketEventSupport.isLegacyAchievementOpen(event)) {
            long now = MillisecondSampler.currentTimeMillis();
            if (Matrix.getServerVersion() == ServerVersion.V_1_8_8
                    && type == PacketType$Play$Client.CLICK_WINDOW
                    && MillisecondSampler.currentTimeMillis() - lastCreativeInventoryActionTime <= 50L) {
                int slot = ((WrapperPlayClientClickWindow) wrapper).getSlot();
                if (slot != 13) {
                    flag("delay.inv",
                            "perform actions faster than expected // inventory (type A, p: " + slot + ")", inventoryPenalty);
                    cancelNextClick = true;
                }
            }
            if (type == PacketType$Play$Client.HELD_ITEM_CHANGE
                    && now - lastHeldItemChangeTime <= 5L && now - lastInteractionTime <= 5L) {
                flag("delay.inv",
                        "perform actions faster than expected // inventory (type B)", 0);
                cancel = true;
            }
            if (PacketEventSupport.isLegacyAchievementOpen(event)) lastCreativeInventoryActionTime = now;
            if (PacketEventSupport.isBlockPlacementPacket(type)) lastInteractionTime = now;
            if (type == PacketType$Play$Client.HELD_ITEM_CHANGE) lastHeldItemChangeTime = now;
        }
        return cancel;
    }

    public void reloadConfiguration() {
        cancelAction = configuration().getBoolean("cancel_action", true);
        inventoryPenalty = configuration().getInt("check_inventory_action", 1);
    }
}
