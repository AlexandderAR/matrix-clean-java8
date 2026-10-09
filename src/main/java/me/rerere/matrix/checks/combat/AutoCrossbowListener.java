package me.rerere.matrix.checks.combat;

import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.collections.MapsKt;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.IntArrayHolder;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.WorldAccess;

/** Detects crossbow shots made shortly after a hotbar swap. */
public final class AutoCrossbowListener extends CheckRuntimeState {
    public static final IntArrayHolder configuration;
    private static boolean cancelAction;
    private static int violationLevel;
    private final long[] hotbarSwapTimes = new long[9];

    static {
        configuration = new IntArrayHolder(
                (me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker) null);
        cancelAction = true;
        violationLevel = 1;
    }

    public AutoCrossbowListener(PlayerCheckContext playerContext) {
        super(playerContext, CheckCategory.DELAY);
    }

    public void reloadConfiguration() {
        ConfigurationSection section = this.configuration();
        cancelAction = section.getBoolean("cancel_action", true);
        section = this.configuration();
        violationLevel = section.getInt("check_auto_crossbow", 1);
    }

    private void recordCrossbowUse(Player player) {
        if (!cancelAction) return;
        WorldAccess worldAccess = Matrix.Companion.matrixInstance().getNmsManager().visitor();
        if (worldAccess.isUsingItem(player)) worldAccess.stopUsingItem(player);
    }

    public void onInventoryClick(InventoryClickEvent event) {
        PlayerInventory inventory = (PlayerInventory) event.getWhoClicked().getInventory();
        if (event.isCancelled() || violationLevel < 0 || event.getAction() != InventoryAction.HOTBAR_SWAP
                || !Objects.equals(event.getClickedInventory(), inventory)) return;
        int slot = event.getSlot();
        if (slot < 9 || slot >= 36) return;
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() != Material.CROSSBOW) return;
        int button = event.getHotbarButton();
        if (button < 0 || button >= 9) return;
        hotbarSwapTimes[event.getHotbarButton()] = MillisecondSampler.currentTimeMillis();
    }

    public void onBowShoot(EntityShootBowEvent event) {
        if (violationLevel < 0) return;
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType() != Material.CROSSBOW) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        recordCrossbowUse(player);
        if (event.isCancelled() || event.getHand() != EquipmentSlot.HAND) return;
        int slot = player.getInventory().getHeldItemSlot();
        long previousSwapTime = hotbarSwapTimes[slot];
        hotbarSwapTimes[slot] = 0L;
        if (previousSwapTime == 0L) return;
        long delay = MillisecondSampler.currentTimeMillis() - previousSwapTime;
        if (delay >= 200L) return;
        Map<String, Object> detail = MapsKt.mapOf(
                new Pair<String, Object>("delay", String.valueOf(delay)),
                new Pair<String, Object>("slot", String.valueOf(slot)));
        flag("delay.crossbow", "perform actions faster than expected // auto_crossbow", detail, violationLevel);
        if (cancelAction) event.setCancelled(true);
    }
}
