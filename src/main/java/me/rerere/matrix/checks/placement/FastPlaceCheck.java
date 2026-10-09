package me.rerere.matrix.checks.placement;

import java.util.LinkedHashMap;
import java.util.Map;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;
import me.rerere.matrix.util.BukkitPlayerAccess;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;


public class FastPlaceCheck extends CheckRuntimeState {
    private int placementsInWindow;
    private long windowStartedAt;
    private static boolean enabled;
    private static int violationWeight;
    private static int maxPlacesPerSecond;
    private static final long WINDOW_DURATION_MILLIS = 995L;

    public FastPlaceCheck(PlayerCheckContext playerContext) {
        super(playerContext);
        this.setCheckType(CheckCategory.BLOCK);
    }

    public void loadSettings() {
        enabled = this.configuration().getBoolean("modules.fastplace.enable", true);
        violationWeight = this.configuration().getInt("modules.fastplace.vl_weight");
        maxPlacesPerSecond = this.configuration().getInt("modules.fastplace.max_place_per_second", 10);
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!enabled) {
            return;
        }
        if (!event.getBlockPlaced().getType().isBlock()) {
            return;
        }
        if (event.getBlockPlaced().getType() == Material.FIRE) {
            return;
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && event.getBlockPlaced().getType() == Material.SCAFFOLDING) {
            return;
        }
        if (event.getPlayer().isInsideVehicle()) {
            return;
        }

        placementsInWindow++;
        if (placementsInWindow > maxPlacesPerSecond
                && !BukkitPlayerAccess.isExternalEventCall()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("places", String.valueOf(placementsInWindow));
            details.put("max_pps", String.valueOf(maxPlacesPerSecond));
            if (super.flag(
                    "block.fastplace", "place block too fast", details, violationWeight)) {
                event.setCancelled(true);
            }
        }

        long now = System.currentTimeMillis();
        if (now - windowStartedAt > WINDOW_DURATION_MILLIS) {
            placementsInWindow = 0;
            windowStartedAt = System.currentTimeMillis();
        }
    }

}
