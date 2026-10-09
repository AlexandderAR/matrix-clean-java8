package me.rerere.matrix.checks.combat;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Detects rapid splash-potion use and records the packet timing used by the check. */
public final class AutoSplashCheck extends CheckRuntimeState {
    private float previousPitch;
    private long lastActionAt;
    private long lastItemSwitchAt;
    private static boolean cancelAction;
    private static int checkThreshold;
    public AutoSplashCheck(PlayerCheckContext playerData) {
        super(playerData);
        setCheckType(CheckCategory.DELAY);
    }

    @Override
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        int newSlot = event.getNewSlot();
        int oldSlot = event.getPreviousSlot();
        int distance = Math.min(Math.abs(newSlot - oldSlot), 9 - Math.abs(newSlot - oldSlot));
        if (distance > 1) lastItemSwitchAt = MillisecondSampler.currentTimeMillis();
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || checkThreshold < 0 || !isSplashPotion(event.getItem())) return;
        if (MillisecondSampler.currentTimeMillis() - lastItemSwitchAt > 50L
                || MillisecondSampler.currentTimeMillis() - lastActionAt > 50L) return;
        flag("delay.splash", "perform actions faster than expected // auto_splash", checkThreshold);
        if (cancelAction) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            PlayerUtilities.resyncInventory(event.getPlayer());
        }
        lastItemSwitchAt = 0L;
        lastActionAt = 0L;
    }

    public void onPacketRotation(PacketTypeCommon packetType, float yaw, float pitch) {
        float pitchChange = Math.abs(pitch - previousPitch);
        if (pitch >= 80.0F && pitchChange > 65.0F) lastActionAt = MillisecondSampler.currentTimeMillis();
        previousPitch = pitch;
    }

    public void reloadConfiguration() {
        cancelAction = configuration().getBoolean("cancel_action", true);
        checkThreshold = configuration().getInt("check_auto_splash", 2);
    }

    /** True for legacy potion damage values or modern splash potions with PotionMeta. */
    private static boolean isSplashPotion(ItemStack item) {
        if (Matrix.getServerVersion() == ServerVersion.V_1_8_8) {
            if (item.getType() != Material.POTION) return false;
            return item.getDurability() == 16421 || item.getDurability() == 16453;
        }
        if (item.getType() != Material.SPLASH_POTION) return false;
        ItemMeta meta = item.getItemMeta();
        return meta instanceof PotionMeta;
    }

}
