package me.rerere.matrix.player;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.PacketEvents;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.User;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.compat.MCPetCompatibility;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.world.WorldAccess;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.metrics.RollingStatistics;

/** Bukkit player, inventory, movement, and scheduling helpers. */
public class PlayerUtilities {
    public PlayerUtilities() {}

    /** Classifies the player's coarse pose with the observed server-version branches. */
    public static boolean hasStandingOrCompatiblePose(Player player) {
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
            Pose pose = player.getPose();
            return pose == Pose.STANDING || pose == Pose.SNEAKING;
        }
        if (!Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) return true;
        return !player.isGliding();
    }

    public static Optional<ItemStack> mainHandItem(Player player) {
        Objects.requireNonNull(player, "null player params @getItemInHand()");
        ItemStack item;
        if (Matrix.getServerVersion() == ServerVersion.V_1_8_8)
            item = player.getItemInHand();
        else
            item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return Optional.empty();
        return Optional.of(item);
    }

    /** Emits the encoded control update through the platform-specific adapter. */
    public static void updateControlState(Player player, boolean state) {
        Matrix.getInstance().getNmsManager().visitor()
                .setOnGround(player, state);
    }

    public static void notifyPlayerData(Player player) {
        Matrix.getInstance().getNmsManager().visitor()
                .stopUsingItem(player);
    }

    public static void randomizeHeldSlot(Player player) {
        int randomSlot = new Random().nextInt(9);
        int previousSlot = player.getInventory().getHeldItemSlot();
        player.getInventory().setHeldItemSlot(randomSlot);
        FoliaSupport.INSTANCE.runDelayedSyncForPlayer(player, new Function0<Unit>() {
            @Override public Unit invoke() {
                restoreHeldSlot(player, previousSlot);
                return Unit.INSTANCE;
            }
        }, 5L);
    }

    private static void warnTeleportFailure(Player player, Boolean teleportSucceeded) {
        if (!teleportSucceeded.booleanValue()) {
            Matrix.getInstance().getLogger().warning(
                    teleportFailureMessage(player.getName()));
        }
    }

    public static ClientVersion clientVersion(Player player) {
        if (player != null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null && user.getClientVersion() != null) return user.getClientVersion();
        }
        return Matrix.getServerVersion().toClientVersion();
    }

    /** Search downward for a non-passable block and move the player above it. */
    public static void correctFallLocation(Player player) {
        Location currentLocation = player.getLocation().clone();
        Location destination = null;
        int blockY = currentLocation.getBlockY();
        while (blockY >= BlockEnvironment.MODERN_MINIMUM_Y) {
            Material material = BlockEnvironment.getMaterialAtViaAlias(
                    currentLocation.getWorld(), currentLocation.getBlockX(), blockY,
                    currentLocation.getBlockZ());
            if (!MaterialPolicy.isAirMaterial(material)) {
                destination = new Location(currentLocation.getWorld(),
                        currentLocation.getBlockX() + 0.5, (double) (blockY + 1),
                        currentLocation.getBlockZ() + 0.5);
                break;
            }
            blockY--;
        }
        if (destination == null)
            destination = new Location(currentLocation.getWorld(), currentLocation.getBlockX(),
                    -10.0, currentLocation.getBlockZ());
        double fallDelta = Math.abs(currentLocation.getY() - destination.getY());
        player.setFallDistance((float) (player.getFallDistance() + fallDelta));
        teleportViaPlatform(player, destination);
    }

    /** Teleport to the floor, preserve facing, update fall distance and optionally apply damage. */
    public static void moveToGround(Player player, boolean applyDamage) {
        Location currentLocation = player.getLocation().clone();
        Location destination = null;
        int blockY = currentLocation.getBlockY();
        while (blockY >= BlockEnvironment.MODERN_MINIMUM_Y) {
            Material material = BlockEnvironment.getMaterialAtViaAlias(
                    currentLocation.getWorld(), currentLocation.getBlockX(), blockY,
                    currentLocation.getBlockZ());
            if (!MaterialPolicy.isAirMaterial(material)) {
                destination = new Location(currentLocation.getWorld(),
                        currentLocation.getBlockX() + 0.5, (double) (blockY + 1),
                        currentLocation.getBlockZ() + 0.5);
                break;
            }
            blockY--;
        }
        if (destination == null)
            destination = new Location(currentLocation.getWorld(), currentLocation.getBlockX(),
                    -10.0, currentLocation.getBlockZ());
        double fallDelta = Math.abs(currentLocation.getY() - destination.getY());
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());
        player.setFallDistance((float) (player.getFallDistance() + fallDelta));
        if (applyDamage) player.damage((double) player.getFallDistance());
        teleportViaPlatform(player, destination);
    }

    public static int maxArmorEnchantment(Player player, Enchantment enchantment) {
        int maximum = 0;
        for (ItemStack armorItem : player.getInventory().getArmorContents()) {
            if (armorItem == null || !armorItem.hasItemMeta()) continue;
            ItemMeta itemMeta = Objects.requireNonNull(
                    armorItem.getItemMeta(), "null item meta / #WUFB34F");
            if (itemMeta.hasEnchant(enchantment)) {
                maximum = Math.max(maximum, armorItem.getEnchantmentLevel(enchantment));
            }
        }
        if (Matrix.getServerVersion().isNewerThan(ServerVersion.V_1_8_8)) {
            maximum = Math.max(maximum, player.getInventory().getItemInMainHand().getEnchantmentLevel(enchantment));
            maximum = Math.max(maximum, player.getInventory().getItemInOffHand().getEnchantmentLevel(enchantment));
        } else {
            maximum = Math.max(maximum, player.getInventory().getItemInHand().getEnchantmentLevel(enchantment));
        }
        return maximum;
    }

    public static int potionLevel(PlayerCheckContext playerContext, PotionType type) {
        Integer level = (Integer) playerContext.potionEffects().get(type);
        return level == null ? 0 : level.intValue() + 1;
    }

    private static Unit updateInventory(Player player) {
        player.updateInventory();
        return Unit.INSTANCE;
    }

    public static boolean isNearbyRelevantEntity(Player player) {
        if (Matrix.getServerVersion() == ServerVersion.V_1_8_8) return false;
        if (RollingStatistics.metric() < 10.0) return false;
        player.getWorld();
        for (Entity entity : BlockEnvironment.getEntitiesWithinRadius(player.getLocation(), 2.0)) {
            if (entity.getEntityId() == player.getEntityId()) continue;
            if (entity instanceof LivingEntity || entity instanceof Boat) return true;
        }
        return false;
    }

    public static void teleportViaPlatform(Player player, Location destination) {
        if (destination == null) return;
        boolean usingFolia = FoliaSupport.INSTANCE.isUsingFolia();
        if (usingFolia) {
            FoliaSupport.INSTANCE.teleportAsync(player, destination)
                    .thenAccept(success -> warnTeleportFailure(player, (Boolean) success));
        } else {
            player.teleport(destination);
        }
    }

    public static int soulSpeed(Player player) {
        ServerVersion version = Matrix.getServerVersion();
        if (version.isOlderThanOrEquals(ServerVersion.V_1_15_2)) return 0;
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return 0;
        ItemStack boots = player.getEquipment().getBoots();
        if (boots == null) return 0;
        return boots.getEnchantmentLevel(Enchantment.SOUL_SPEED);
    }

    public static boolean hasSwiftSneak(Player player) {
        if (!Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_19)) return false;
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings == null) return false;
        ItemMeta meta = leggings.getItemMeta();
        return meta != null && leggings.getItemMeta().hasEnchant(Enchantment.SWIFT_SNEAK);
    }

    public static boolean hasOnlyPassableBlocksBelow(Location location) {
        Location cursor = location.clone();
        int blockY = cursor.getBlockY();
        while (blockY >= BlockEnvironment.MODERN_MINIMUM_Y) {
            Material material = BlockEnvironment.getMaterialAtViaAlias(
                    cursor.getWorld(), cursor.getBlockX(), blockY, cursor.getBlockZ());
            if (!MaterialPolicy.isAirMaterial(material)) return false;
            blockY--;
        }
        return true;
    }

    public static boolean anyHandMatches(Player player, Predicate<ItemStack> predicate) {
        if (Matrix.getServerVersion() == ServerVersion.V_1_8_8) {
            ItemStack held = player.getItemInHand();
            return held.getType() != Material.AIR && predicate.test(held);
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() != Material.AIR && predicate.test(main)) return true;
        ItemStack off = player.getInventory().getItemInOffHand();
        return off.getType() != Material.AIR && predicate.test(off);
    }

    /** Finds Depth Strider on armor; positive mode requires hasEnchant and can skip AIR. */
    public static int depthStrider(int positiveMode, int skipAir, Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int index = 0; index < armor.length;) {
            ItemStack armorItem = armor[index];
            // The reference advances the armor cursor only for nonnegative mode.
            if (positiveMode < 0) continue;
            index++;
            if (armorItem == null) continue;
            if (skipAir > 0 && armorItem.getType() == Material.AIR) continue;
            if (!armorItem.hasItemMeta()) continue;
            ItemMeta itemMeta = armorItem.getItemMeta();
            if (itemMeta.hasEnchant(Enchantment.DEPTH_STRIDER)) {
                return armorItem.getItemMeta().getEnchantLevel(Enchantment.DEPTH_STRIDER);
            }
        }
        return -1;
    }

    public static boolean hasPlayerDataOrEntityFlag(Player player) {
        ServerVersion version = Matrix.getServerVersion();
        if (version.isNewerThanOrEquals(ServerVersion.V_1_12)) {
            PlayerSessionRegistry registry = Matrix.getInstance().getPlayerDataManager();
            PlayerCheckContext data = registry.find(player.getUniqueId());
            if (data != null) return data.isCollidingBelow();
        }
        return hasPlayerEntityFlag(player);
    }

    public static boolean isSwimming(Player player) {
        if (!Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) return false;
        return player.getPose() == Pose.SWIMMING;
    }

    public static float entityWidth(Player player) {
        return Matrix.getInstance().getNmsManager().visitor().getMovementSpeed(player);
    }

    public static boolean isGliding(Player player) {
        if (!Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_12)) return false;
        return player.isGliding();
    }

    private static String teleportFailureMessage(String playerName) {
        return "Teleport " + playerName + " failed";
    }

    public static Set<Player> nearbyPlayers(Location center) {
        Set<Player> result = new HashSet<>();
        World world = Objects.requireNonNull(center.getWorld());
        for (Player other : world.getPlayers()) {
            boolean withinVerticalRange = Math.abs(other.getLocation().getY() - center.getY()) <= 4.0;
            if (!withinVerticalRange) continue;
            boolean withinDistance = GeometryMath.horizontalManhattanDistance(other.getLocation(), center) <= 4.0;
            if (withinDistance) result.add(other);
        }
        return result;
    }

    public static boolean isInVehicleOrPetVehicle(Player player) {
        if (player == null) return false;
        return player.isInsideVehicle() || MCPetCompatibility.INSTANCE.isMountedOnPet(player);
    }

    private static Unit restoreHeldSlot(Player player, int slot) {
        player.getInventory().setHeldItemSlot(slot);
        return Unit.INSTANCE;
    }

    public static boolean isPlayerFirstPassenger(Player player, Vehicle vehicle) {
        return vehicle.getPassengers().get(0).equals(player);
    }

    public static Set<?> keySet(Map<?, ?> map) { return map.keySet(); }

    public static boolean hasPlayerEntityFlag(Player player) {
        return Matrix.getInstance().getNmsManager().visitor().isOnGround(player);
    }

    public static void resyncInventory(Player player) {
        FoliaSupport.INSTANCE.runSyncForPlayer(player, new Function0<Unit>() {
            @Override public Unit invoke() {
                updateInventory(player);
                return Unit.INSTANCE;
            }
        });
    }

}
