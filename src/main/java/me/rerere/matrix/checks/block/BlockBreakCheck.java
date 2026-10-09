package me.rerere.matrix.checks.block;

import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.config.CheckSetting;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.server.ServerImplementationFactory;
import me.rerere.matrix.compat.bukkit.EfficiencyEnchantment;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.util.BukkitPlayerAccess;
import me.rerere.matrix.util.CollectionsSupport;
import me.rerere.matrix.util.PotionEffects;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.WorldAccess;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Evaluates block-break timing against expected material/tool hardness. */
public final class BlockBreakCheck extends CheckRuntimeState {
    private static int fastBreakViolationWeight;
    private boolean finishedDigging;
    private boolean movingUp;
    private static int cancelViolationThreshold;
    private int blockBreakCount;
    private static boolean fastBreakEnabled;
    private long lastPotionUseAt;
    private boolean startedDigging;
    private static final Set<Material> specialMaterials;

    static {
        specialMaterials = Stream.of(Material.values())
                .filter(BlockBreakCheck::isNonLegacyMaterial)
                .filter(BlockBreakCheck::isButtonOrPiston)
                .collect(Collectors.toSet());
    }

    public BlockBreakCheck(PlayerCheckContext context) {
        super(context);
        blockBreakCount = 0;
        startedDigging = false;
        movingUp = false;
        setCheckType(CheckCategory.BLOCK);
    }

    public static boolean isNonLegacyMaterial(Material material) {
        return !material.name().startsWith("LEGACY_");
    }

    public static boolean isButtonOrPiston(Material material) {
        return material.name().contains("BUTTON") || material.name().contains("PISTON");
    }

    public void reloadConfiguration() {
        CheckCategory settings = checkType();
        CheckSetting nested = settings.getConfiguration();
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection section =
                nested.getConfigurationSection();
        cancelViolationThreshold = section.getInt("cancel_vl", 10);
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection root =
                configuration();
        fastBreakEnabled = root.getBoolean("modules.fastbreak.enable", true);
        fastBreakViolationWeight = root.getInt("modules.fastbreak.vl_weight");
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (type == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.PLAYER_DIGGING
                && wrapper != null) {
            DiggingAction action = ((WrapperPlayClientPlayerDigging) wrapper).getAction();
            if (action == DiggingAction.START_DIGGING) startedDigging = true;
            else if (action == DiggingAction.CANCELLED_DIGGING) startedDigging = false;
            else if (action == DiggingAction.FINISHED_DIGGING) {
                finishedDigging = true;
                startedDigging = false;
            }
        }
        return false;
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            blockBreakCount = 0;
            movingUp = false;
        }
    }

    public void onPlayerMove(PlayerMoveEvent event, org.bukkit.Location from, org.bukkit.Location to) {
        if (PlayerUtilities.hasPlayerDataOrEntityFlag(event.getPlayer())
                && to.getY() - from.getY() > 0.0d) movingUp = true;
    }

    public void onPlayerAnimation(PlayerAnimationEvent event) {
        blockBreakCount++;
        if (startedDigging) {
            org.bukkit.potion.PotionEffectType hasteEffect = PotionEffects.singleton.getHaste();
            if (event.getPlayer().hasPotionEffect(hasteEffect))
                lastPotionUseAt = MillisecondSampler.currentTimeMillis();
        }
    }

    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        org.bukkit.block.Block block = event.getBlock();
        if (!finishedDigging) return;
        finishedDigging = false;

        Matrix matrix = Matrix.getInstance();
        ServerImplementationFactory services = matrix.getNmsManager();
        WorldAccess hardnessProvider = services.visitor();
        float hardness = hardnessProvider.getBlockBreakSpeed(player, block);
        int expectedTicks = (int) (1.0f / hardness);

        if (blockBreakCount < expectedTicks
                && Math.abs(blockBreakCount - expectedTicks) <= 1) blockBreakCount++;
        if (movingUp && blockBreakCount < expectedTicks
                && (double) blockBreakCount > (double) expectedTicks * 0.75d) {
            blockBreakCount = expectedTicks;
        }

        java.util.Optional<org.bukkit.inventory.ItemStack> heldItem = PlayerUtilities.mainHandItem(player);
        if (heldItem.isPresent()) {
            org.bukkit.inventory.ItemStack item = (org.bukkit.inventory.ItemStack) heldItem.get();
            if (item.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = (org.bukkit.inventory.meta.ItemMeta)
                        java.util.Objects.requireNonNull(item.getItemMeta(), "Null ItemMeta #JECB");
                if (meta.hasEnchant(EfficiencyEnchantment.INSTANCE.getEfficiency())
                        && item.getItemMeta().getEnchantLevel(
                        EfficiencyEnchantment.INSTANCE.getEfficiency()) > 5) return;
            }
        }

        if (block.getType() == org.bukkit.Material.COBBLESTONE
                || block.getType() == org.bukkit.Material.STONE) {
            boolean liquidNeighbor = false;
            org.bukkit.block.BlockFace[] faces = BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES;
            for (org.bukkit.block.BlockFace face : faces) {
                if (block.getRelative(face).isLiquid()) {
                    liquidNeighbor = true;
                    break;
                }
            }
            if (block.getRelative(org.bukkit.block.BlockFace.UP).isLiquid()
                    || block.getRelative(org.bukkit.block.BlockFace.DOWN).isLiquid()) liquidNeighbor = true;
            if (liquidNeighbor) expectedTicks /= 4;
        }
        if (specialMaterials.contains(block.getType())) return;

        if (Matrix.getServerVersion().isOlderThan(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_16)
                && playerState().clientVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_16)
                && player.getItemInHand().getType().name().endsWith("_HOE")) {
            org.bukkit.Material material = block.getType();
            if (material == org.bukkit.Material.HAY_BLOCK || material == org.bukkit.Material.SPONGE) return;
            if (Matrix.getServerVersion().isNewerThan(
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_8_8)
                    && material == org.bukkit.Material.NETHER_WART_BLOCK) return;
        }
        if (playerState().clientVersion().isOlderThan(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_17)
                && Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17)
                && block.getType().name().startsWith("DEEPSLATE_")) return;

        long sincePotion = MillisecondSampler.currentTimeMillis() - lastPotionUseAt;
        if (sincePotion <= 1000L + (long) expectedTicks * 50L && blockBreakCount >= 2) expectedTicks = 2;
        if (fastBreakEnabled && blockBreakCount < expectedTicks
                && !BukkitPlayerAccess.isExternalEventCall()) {
            java.util.Map<Object, Object> details = CollectionsSupport.linkedMapOf(new Object[] {
                    "digs", String.valueOf(blockBreakCount), "expect", String.valueOf(expectedTicks),
                    "block", block.getType().name().toLowerCase(), "j", String.valueOf(movingUp)
            });
            boolean violation = flag("blk.fb.exp", "break block too fast", (java.util.Map) details,
                    fastBreakViolationWeight);
            if (violation && violationCount() >= cancelViolationThreshold) event.setCancelled(true);
        }
        blockBreakCount = 0;
        movingUp = false;
    }
}
