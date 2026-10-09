package me.rerere.matrix.checks.player;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;

/** Detects repeated rapid use of throwable items and applies the configured cancellation policy. */
public class FastThrowCheck extends CheckRuntimeState {
    private static boolean cancelAction;
    private int rapidUseCount;
    private long previousUseTime;
    private static int checkFastThrow;
    private static final Set<Material> THROWABLE_ITEMS;
    private static final long windowMillis = 50L;

    static {
        checkFastThrow = 0;
        cancelAction = false;
        Set<Material> materials = new HashSet<>();
        materials.add(Material.EGG);
        materials.add(Material.ENDER_PEARL);
        if (Matrix.getServerVersion().isOlderThan(ServerVersion.V_1_13)) {
            materials.add(Material.valueOf("SNOW_BALL"));
            materials.add(Material.valueOf("EYE_OF_ENDER"));
        } else {
            materials.add(Material.SNOWBALL);
            materials.add(Material.ENDER_EYE);
        }
        THROWABLE_ITEMS = EnumSet.copyOf(materials);
    }

    public FastThrowCheck(me.rerere.matrix.player.PlayerCheckContext manager) {
        super(manager);
        super.setCheckType(CheckCategory.DELAY);
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (checkFastThrow < 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        checkFastThrow(player, item, event);
    }

    private void checkFastThrow(Player player, ItemStack item, PlayerInteractEvent event) {
        long now = MillisecondSampler.currentTimeMillis();
        long elapsedMillis = now - previousUseTime;
        if (elapsedMillis > windowMillis) {
            rapidUseCount = Math.max(0, rapidUseCount - 1);
        } else {
            Material material = item.getType();
            if (material != Material.AIR && THROWABLE_ITEMS.contains(material)) {
                rapidUseCount++;
                if (rapidUseCount > 2) {
                    String detail = buildViolationMessage(elapsedMillis, material.name().toLowerCase());
                    this.flag("delay.throw", detail, checkFastThrow);
                    if (cancelAction) {
                        event.setCancelled(true);
                        event.setUseItemInHand(Event.Result.DENY);
                        PlayerUtilities.resyncInventory(player);
                    }
                }
            }
        }
        previousUseTime = now;
    }

    private static String buildViolationMessage(long elapsed, String material) {
        return "perform actions faster than expected // fast_throw (d: " + elapsed
                + ", t: " + material + ")";
    }

    public void reloadConfiguration() {
        cancelAction = this.configuration().getBoolean("cancel_action", true);
        checkFastThrow = this.configuration().getInt("check_fast_throw", 1);
    }
}
