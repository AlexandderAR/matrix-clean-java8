package me.rerere.matrix.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.network.packet.KeepAlivePacketCancellationListener;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.player.PlanState;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/** Staff commands and temporary player-state controls. */
public final class StaffTool extends AbstractCommandDescriptor implements Listener {
    @NotNull private final HashSet<UUID> frozenPlayers = new HashSet<>();
    @NotNull private final HashMap<UUID, Long> damageReductionExpiryMillis = new HashMap<>();
    @NotNull private final HashMap<UUID, Boolean> activeFakeLag = new HashMap<>();
    @NotNull private final HashMap<UUID, me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerAbstract> packetListeners = new HashMap<>();
    @Nullable
    private final List<String> toolActionCandidates = null;

    private static final long callbackDelayTicks;

    static {
        callbackDelayTicks = 10L;
    }

    public StaffTool() {
        super("tool", "<action> <player> [tick]",
              "Staff Tool [freeze, rotate, reducedamage, shuffle, shuffleinv, starve, kb, cache]",
              "matrix.staff");
        Bukkit.getPluginManager().registerEvents(this, (Plugin) Matrix.Companion.matrixInstance());
    }

    @Override
    @Nullable
    public List<String> getTabCompletions() {
        return toolActionCandidates;
    }

    private static boolean randomBoolean() {
        return me.rerere.matrix.thirdparty.kotlin.random.Random.Default.nextBoolean();
    }

    private static String damageReductionEndedMessage(String playerName) {
        return "\u00a7aPlayer " + playerName + "'s damage reduction has ended.";
    }

    private static String fakeLagStartedMessage(String prefix, String playerName, long ticks) {
        return new StringBuilder(prefix).append("\u00a7aStarting fake lag for player ").append(playerName).append(" for ").append(ticks).append(" ticks.").toString();
    }

    private static String packetCacheStartedMessage(String prefix, String playerName, long ticks) {
        return new StringBuilder(prefix).append("\u00a7aCaching packets for player ").append(playerName).append(" for ").append(ticks).append(" ticks.").toString();
    }

    private static String unfrozenMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append(" has been unfrozen.").toString();
    }

    private static String foodRestoredMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append("'s food level has been restored.").toString();
    }

    private static String foodDepletedMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append("'s food level has been temporarily set to 0.").toString();
    }

    private static String packetCacheStoppedMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aStopped caching packets for player ").append(playerName).append(".").toString();
    }

    private static String reducedDamageStartedMessage(String playerName, long ticks) {
        return "\u00a7aPlayer " + playerName + "'s damage has been reduced to 10% for " + ticks + " ticks.";
    }

    private static String heldSlotChangedMessage(String prefix, String playerName, int slot) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append("'s selected slot has been changed to ").append(slot).append(".").toString();
    }

    private static String playerOfflineMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7cPlayer ").append(playerName).append(" is not online.").toString();
    }

    private static String knockbackMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append(" has been knocked back.").toString();
    }

    private static String inventoryShuffledMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append("'s inventory has been shuffled.").toString();
    }

    private static String rotationMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append(" has been rotated randomly.").toString();
    }

    private static String unknownActionMessage(String prefix, String action) {
        return new StringBuilder(prefix).append("\u00a7cUnknown action: ").append(action).toString();
    }

    private static String fakeLagAlreadyActiveMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7cFake lag is already active for player ").append(playerName).append(".").toString();
    }

    private static String fakeLagStoppedMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aStopped fake lag for player ").append(playerName).append(".").toString();
    }

    private static String usageMessage(String prefix) {
        return new StringBuilder(prefix).append("\u00a7cUsage: /matrix tool <action> <player> [tick]").toString();
    }

    private static String frozenPlayersMessage(List<String> names) {
        return "Frozen players: " + String.join(", ", names);
    }

    private static String frozenMessage(String prefix, String playerName) {
        return new StringBuilder(prefix).append("\u00a7aPlayer ").append(playerName).append(" has been frozen.").toString();
    }

    private static String playerNameOrEmpty(UUID id) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) return "";
        String name = player.getName();
        return name == null ? "" : name;
    }

    private void knockbackPlayer(CommandSender sender, Player player) {
        player.setVelocity(new Vector(1, 1, 0).normalize().multiply(1.0D));
        sender.sendMessage(knockbackMessage(LocaleMessages.margin, player.getName()));
    }

    private void shuffleHeldSlot(CommandSender sender, Player player) {
        int current = player.getInventory().getHeldItemSlot();
        int selected;
        do {
            selected = me.rerere.matrix.thirdparty.kotlin.random.Random.Default.nextInt(9);
        } while (selected == current);
        player.getInventory().setHeldItemSlot(selected);
        sender.sendMessage(heldSlotChangedMessage(LocaleMessages.margin, player.getName(), selected));
    }

    private void rotatePlayer(CommandSender sender, Player player) {
        float yaw = me.rerere.matrix.thirdparty.kotlin.random.Random.Default.nextFloat() * 360.0F - 180.0F;
        float pitch = me.rerere.matrix.thirdparty.kotlin.random.Random.Default.nextFloat() * 180.0F - 90.0F;
        Location location = player.getLocation();
        location.setYaw(yaw);
        location.setPitch(pitch);
        PlayerUtilities.teleportViaPlatform(player, location);
        sender.sendMessage(rotationMessage(LocaleMessages.margin, player.getName()));
    }

    private void shuffleInventory(CommandSender sender, Player player) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) items.add(inventory.getItem(slot));
        Collections.shuffle(items);
        size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) inventory.setItem(slot, items.get(slot));
        sender.sendMessage(inventoryShuffledMessage(LocaleMessages.margin, player.getName()));
    }

    private void toggleFreeze(CommandSender sender, Player player) {
        if (frozenPlayers.contains(player.getUniqueId())) {
            frozenPlayers.remove(player.getUniqueId());
            sender.sendMessage(unfrozenMessage(LocaleMessages.margin, player.getName()));
        } else {
            frozenPlayers.add(player.getUniqueId());
            sender.sendMessage(frozenMessage(LocaleMessages.margin, player.getName()));
        }
        List<String> names = new ArrayList<>();
        for (UUID frozenId : frozenPlayers) names.add(playerNameOrEmpty(frozenId));
        sender.sendMessage(frozenPlayersMessage(names));
    }

    private void temporarilyDepleteFood(Player player, CommandSender sender) {
        int previousFood = player.getFoodLevel();
        float previousSaturation = player.getSaturation();
        player.setFoodLevel(0);
        player.setSaturation(0.0F);
        sender.sendMessage(foodDepletedMessage(LocaleMessages.margin, player.getName()));
        me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler.getGlobalRegionScheduler().runDelayed((Plugin) Matrix.Companion.matrixInstance(), ignored -> restoreFoodState(player, previousFood, previousSaturation, sender), callbackDelayTicks);
    }

    private static void restoreFoodState(Player player, int foodLevel, float saturation, CommandSender sender) {
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        sender.sendMessage(foodRestoredMessage(LocaleMessages.margin, player.getName()));
    }

    private void startDamageReduction(CommandSender sender, Player player, long ticks) {
        damageReductionExpiryMillis.put(player.getUniqueId(), System.currentTimeMillis() + ticks * 50L);
        sender.sendMessage(reducedDamageStartedMessage(player.getName(), ticks));
        me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler.getGlobalRegionScheduler().runDelayed((Plugin) Matrix.Companion.matrixInstance(), ignored -> finishDamageReduction(player, sender), ticks);
    }

    private void startFakeLag(CommandSender sender, Player player, long ticks) {
        if (Boolean.TRUE.equals(activeFakeLag.get(player.getUniqueId()))) {
            sender.sendMessage(fakeLagAlreadyActiveMessage(LocaleMessages.margin, player.getName()));
            return;
        }
        activeFakeLag.put(player.getUniqueId(), Boolean.TRUE);
        sender.sendMessage(fakeLagStartedMessage(LocaleMessages.margin, player.getName(), ticks));
        me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler.getGlobalRegionScheduler().runDelayed((Plugin) Matrix.Companion.matrixInstance(), ignored -> finishFakeLag(player, sender), ticks);
    }

    private void startPacketCache(CommandSender sender, Player player, long ticks) {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerAbstract prior = packetListeners.get(player.getUniqueId());
        if (prior != null) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                    .unregisterListener((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerCommon) prior);
            packetListeners.remove(player.getUniqueId());
        }
        KeepAlivePacketCancellationListener listener = new KeepAlivePacketCancellationListener(player);
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(
                (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerCommon) listener);
        packetListeners.put(player.getUniqueId(), listener);
        sender.sendMessage(packetCacheStartedMessage(LocaleMessages.margin, player.getName(), ticks));
        me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler.getGlobalRegionScheduler().runDelayed((Plugin) Matrix.Companion.matrixInstance(), ignored -> stopPacketCache(listener, player, sender), ticks);
    }

    private void finishDamageReduction(Player player, CommandSender sender) {
        damageReductionExpiryMillis.remove(player.getUniqueId());
        sender.sendMessage(damageReductionEndedMessage(player.getName()));
    }

    private void finishFakeLag(Player player, CommandSender sender) {
        activeFakeLag.remove(player.getUniqueId());
        sender.sendMessage(fakeLagStoppedMessage(LocaleMessages.margin, player.getName()));
    }

    private void stopPacketCache(KeepAlivePacketCancellationListener listener, Player player, CommandSender sender) {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                .unregisterListener((me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerCommon) listener);
        packetListeners.remove(player.getUniqueId());
        sender.sendMessage(packetCacheStoppedMessage(LocaleMessages.margin, player.getName()));
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        boolean guard = true; // xb.7()Z is true in the canonical own callgraph evidence.
        if (guard && PlanState.INSTANCE.hasTrialOrNoPlans()) {
            ChatColor unusedColor = Matrix.Companion.isRunning() ? ChatColor.GREEN : ChatColor.RED;
            sender.sendMessage("\u00a7cYour plan does not support this feature or Your Network is not connected to the internet.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(usageMessage(LocaleMessages.margin));
            return;
        }
        String originalAction = args[1];
        String targetName = args[2];
        Player player = Bukkit.getPlayer(targetName);
        if (player == null) {
            sender.sendMessage(playerOfflineMessage(LocaleMessages.margin, targetName));
            return;
        }
        String action = originalAction.toLowerCase(Locale.getDefault());
        switch (action) {
            case "rotate": rotatePlayer(sender, player); return;
            case "freeze": toggleFreeze(sender, player); return;
            case "starve": temporarilyDepleteFood(player, sender); return;
            case "kb": knockbackPlayer(sender, player); return;
            case "shuffleinv": shuffleInventory(sender, player); return;
            case "shuffle": shuffleHeldSlot(sender, player); return;
            case "reducedamage":
            case "fakelag":
            case "cache":
                if (args.length < 4) {
                    sender.sendMessage(action.equals("reducedamage")
                            ? "\u00a7cUsage: /matrix tool reducedamage <player> <tick>"
                            : action.equals("fakelag")
                                    ? "\u00a7cUsage: /matrix tool fakelag <player> <tick>"
                                    : "\u00a7cUsage: /matrix tool cache <player> <tick>");
                    return;
                }
                Long ticks = me.rerere.matrix.thirdparty.kotlin.text.StringsKt.toLongOrNull(args[3]);
                if (ticks == null || ticks <= 0L) {
                    sender.sendMessage("\u00a7cInvalid tick value. It must be a positive number.");
                    return;
                }
                if (action.equals("reducedamage")) startDamageReduction(sender, player, ticks);
                else if (action.equals("fakelag")) startFakeLag(sender, player, ticks);
                else startPacketCache(sender, player, ticks);
                return;
            default:
                sender.sendMessage(unknownActionMessage(LocaleMessages.margin, originalAction));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketFill(@NotNull PlayerBucketFillEvent event) {
        if (Boolean.TRUE.equals(activeFakeLag.get(event.getPlayer().getUniqueId())) && randomBoolean()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (Boolean.TRUE.equals(activeFakeLag.get(event.getPlayer().getUniqueId())) && randomBoolean()) event.setCancelled(true);
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        if (Boolean.TRUE.equals(activeFakeLag.get(event.getPlayer().getUniqueId())) && randomBoolean()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        frozenPlayers.remove(event.getPlayer().getUniqueId());
        damageReductionExpiryMillis.remove(event.getPlayer().getUniqueId());
        activeFakeLag.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setTo(event.getFrom());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        org.bukkit.entity.Entity damager = event.getDamager();
        UUID id = damager.getUniqueId();
        if (!(damager instanceof Player)) return;
        if (!damageReductionExpiryMillis.containsKey(id)) return;
        Long expiryMillis = damageReductionExpiryMillis.get(id);
        if (expiryMillis == null) return;
        if (System.currentTimeMillis() > expiryMillis) damageReductionExpiryMillis.remove(id);
        else event.setDamage(event.getDamage() * 0.1D);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        if (Boolean.TRUE.equals(activeFakeLag.get(event.getPlayer().getUniqueId())) && randomBoolean()) event.setCancelled(true);
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }
}
