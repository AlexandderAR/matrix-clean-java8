package me.rerere.matrix.checks.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.inventory.EquipmentSlot;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.compat.ProtocolTranslationSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.BukkitPlayerAccess;
import me.rerere.matrix.util.CollectionsSupport;

/** No-swing placement check; state, version gates, configuration, and callbacks follow ASM. */
public class NoSwingCheck extends CheckRuntimeState {
    private boolean recentPlacement;
    private int ticksSincePlacement;
    private static List<Material> excludedMaterials;
    private static int violationWeight;
    private static boolean enabled;

    public NoSwingCheck(PlayerCheckContext playerData) {
        super(playerData);
        recentPlacement = false;
        ticksSincePlacement = 0;
        this.setCheckType(CheckCategory.BLOCK);
    }

    public void reloadConfiguration() {
        enabled = this.configuration().getBoolean("modules.noswing.enable", true);
        violationWeight = this.configuration().getInt("modules.noswing.vl_weight", 1);

        excludedMaterials = new ArrayList<>();
        excludedMaterials.add(Material.FIRE);
        ServerVersion server = Matrix.getServerVersion();
        if (server.isOlderThan(ServerVersion.V_1_13)) {
            excludedMaterials.add(Material.valueOf("WATER_LILY"));
        } else {
            excludedMaterials.add(Material.LILY_PAD);
        }
        excludedMaterials.add(Material.SAND);
        excludedMaterials.add(Material.GRAVEL);
        excludedMaterials.addAll(Arrays.stream(Material.values())
                .filter(NoSwingCheck::isNonLegacyMaterial)
                .filter(NoSwingCheck::isTorchOrConcretePowder)
                .collect(Collectors.toSet()));

        ServerVersion current = Matrix.getServerVersion();
        if (current.isNewerThanOrEquals(ServerVersion.V_1_12)
                && current.isOlderThan(ServerVersion.V_1_17)) {
            excludedMaterials.add(Material.valueOf("GRASS_PATH"));
        }
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            excludedMaterials.add(Material.DIRT_PATH);
        }
    }

    private static boolean isTorchOrConcretePowder(Material material) {
        String name = material.name();
        return name.contains("TORCH") || name.contains("_CONCRETE_POWDER");
    }

    public void onPlayerAnimation(PlayerAnimationEvent event) {
        recentPlacement = false;
        ticksSincePlacement = 0;
    }

    public void onTick() {
        if (ticksSincePlacement < 20) ticksSincePlacement++;
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_26_3)) return;
        if (this.playerState().clientVersion()
                .isNewerThanOrEquals(ClientVersion.V_26_3)) return;

        ServerVersion actualServer = Matrix.getServerVersion();
        if (actualServer == ServerVersion.V_1_8_8
                && ProtocolTranslationSupport.isAvailable()
                && this.playerState().protocolVersion() >= 335) {
            return;
        }

        if (this.playerState().clientVersion()
                .isOlderThan(Matrix.getServerVersion().toClientVersion())
                && Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (!event.getBlockPlaced().getType().isSolid()) return;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && event.getBlockPlaced().getType() == Material.SCAFFOLDING) {
            return;
        }
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)
                && this.playerState().clientVersion()
                        .isNewerThanOrEquals(ClientVersion.V_1_19)
                && event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (recentPlacement
                && event.getPlayer().getGameMode() == GameMode.SURVIVAL
                && ticksSincePlacement > 5) {
            if (enabled && !BukkitPlayerAccess.isExternalEventCall()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> context = (Map<String, Object>) (Map<?, ?>) CollectionsSupport.linkedMapOf(
                        new Object[] { "placed", event.getBlockPlaced().getType().name() });
                if (this.flag(
                        "block.noswing", "no swing after place block", context, violationWeight)) {
                    event.setCancelled(true);
                }
            }
            recentPlacement = false;
        }

        if (event.canBuild() && event.getBlockPlaced().getType().isBlock()
                && !excludedMaterials.contains(event.getBlockPlaced().getType())) {
            recentPlacement = true;
        }
    }

    private static boolean isNonLegacyMaterial(Material material) {
        return !material.name().startsWith("LEGACY_");
    }

}
