package me.rerere.matrix.checks.inventory;

import org.bukkit.event.Event;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.support.ConstructorAccessMarker;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;

/** Tracks offhand totem changes and flags implausibly rapid repeated actions. */
public final class AutoTotemCheck extends CheckRuntimeState {
    private static int cancelAction;
    private static boolean cancelEnabled;
    private static int thresholdMode;
    private long lastTotemActionTime;
    public static final ConstructorAccessMarker configurationMarker;
    private static final long actionWindowMillis;

    static {
        setThresholdMode(0);
        actionWindowMillis = 150L;
        configurationMarker = new ConstructorAccessMarker((DefaultConstructorMarker) null);
    }

    public AutoTotemCheck(PlayerCheckContext context) {
        super(context);
        this.setCheckType(CheckCategory.DELAY);
    }

    @Override
    public void reloadConfiguration() {
        ConfigurationSection section = this.configuration();
        cancelEnabled = section.getBoolean("cancel_action", true);
        cancelAction = this.configuration().getInt("check_auto_totem", 1);
    }

    public void onInventoryClick(InventoryClickEvent event) {
        if (cancelAction < 0) return;
        if (event.getClickedInventory() == null) return;
        Inventory clicked = event.getClickedInventory();
        Intrinsics.checkNotNull(clicked);
        if (clicked.getType() != InventoryType.PLAYER) return;

        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            if (event.getRawSlot() != 45 && isTotem(event.getCurrentItem())) {
                lastTotemActionTime = MillisecondSampler.currentTimeMillis();
            }
            if (event.getRawSlot() == 45 && event.getHotbarButton() >= 0) {
                Inventory hotbar = event.getClickedInventory();
                Intrinsics.checkNotNull(hotbar);
                if (isTotem(hotbar.getItem(event.getHotbarButton()))) {
                    checkRapidTotemEvent(event);
                }
            }
        }
        if (event.getAction() == InventoryAction.PLACE_ALL && event.getRawSlot() == 45
                && event.getCursor() != null) {
            ItemStack cursor = event.getCursor();
            Intrinsics.checkNotNull(cursor);
            if (cursor.getType().name().contains("TOTEM")) {
                checkRapidTotemEvent(event);
            }
        }
        if (event.getAction() == InventoryAction.PICKUP_ALL
                && event.getCurrentItem() != null) {
            ItemStack current = event.getCurrentItem();
            Intrinsics.checkNotNull(current);
            if (current.getType().name().contains("TOTEM")) {
                lastTotemActionTime = System.currentTimeMillis();
            }
        }
    }

    private static boolean isTotem(ItemStack item) {
        Material type = item == null ? null : item.getType();
        return type != null && type.name().contains("TOTEM");
    }

    private void checkRapidTotemEvent(InventoryClickEvent event) {
        long elapsedMillis = System.currentTimeMillis() - lastTotemActionTime;
        if (elapsedMillis < actionWindowMillis) {
            this.flag("inv.totem", "perform actions faster than expected // auto_totem", cancelAction);
            if (cancelEnabled) event.setResult(Event.Result.DENY);
        }
    }

    public static void setThresholdMode(int value) { thresholdMode = value; }
    public static int thresholdMode() { return thresholdMode; }

    public static int violationThreshold() {
        return thresholdMode() == 0 ? 70 : 0;
    }
}
