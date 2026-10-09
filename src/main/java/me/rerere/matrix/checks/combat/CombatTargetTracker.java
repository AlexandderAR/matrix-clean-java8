package me.rerere.matrix.checks.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerEntityTracker;
import me.rerere.matrix.util.BukkitPlayerAccess;
import me.rerere.matrix.util.IdentityUtilities;
import me.rerere.matrix.thirdparty.kotlin.Unit;

/** Maintains the synthetic player entity used by the NPC combat check. */
public final class CombatTargetTracker extends CheckRuntimeState {
    private static final long RECENT_ATTACK_WINDOW_MILLIS = 1000L;
    private static final Map<Player, Integer> COMMAND_TARGETS = new HashMap<>();
    private static Predicate<Player> NON_SPECTATOR;

    private static int violationWeight;
    private static boolean enabled;
    private static double movementDistance;
    private static double movementRandomness;
    private static double attackDistance;
    private static double maximumSpeed;
    private static boolean asynchronous;
    private static boolean useRealEntityId;
    private static boolean commandOnly;
    private static boolean spawnOnPlayerAttack;
    private static int baseCheckTicks;
    private static int attackCheckTicks;
    private static int hitTargetCheckTicks;
    private static boolean visible;
    private static boolean forceRandomName;
    private static boolean moveUpAfterAttack;

    private PlayerEntityTracker target;
    private int remainingTicks;
    private double verticalOffset;
    private double verticalVelocity;
    private long lastAttackTime;

    public CombatTargetTracker(PlayerCheckContext playerData) {
        super(playerData);
        resetVerticalMotion();
        super.setCheckType(CheckCategory.KILLAURA);
    }

    public static void registerCommandTarget(Player player, int durationTicks) {
        COMMAND_TARGETS.put(player, durationTicks);
    }

    /** Returns whether the NPC combat check is enabled for command dispatch. */
    public static boolean isEnabled() {
        return enabled;
    }

    public void resetVerticalMotion() {
        verticalVelocity = 0.0;
        verticalOffset = 0.0;
    }
    public void clearTarget() {
        if (target != null) {
            target.removeEntity();
            target = null;
            remainingTicks = 0;
            resetVerticalMotion();
        }
    }

    public void updateVerticalMotion() {
        if (verticalOffset == 0.0 && verticalVelocity == 0.0) {
            if (Math.random() < 0.04) verticalVelocity = 0.42;
            return;
        }
        verticalOffset += verticalVelocity;
        verticalVelocity = (verticalVelocity - 0.08) * 0.98;
        if (verticalOffset <= 0.0) {
            verticalOffset = 0.0;
            verticalVelocity = 0.0;
        }
    }

    public Location forceFieldLocation(Location location) {
        Vector backwards = location.getDirection().setY(0).normalize().multiply(-1);
        backwards.multiply(10.0 + new Random().nextDouble() * 6.0);
        return location.add(backwards);
    }

    public Location attackSpawnLocation(Location location) {
        boolean recentAttack = System.currentTimeMillis() - lastAttackTime < RECENT_ATTACK_WINDOW_MILLIS;
        Vector backwards = location.getDirection().setY(0).normalize().multiply(-1);
        if (recentAttack) backwards.multiply(attackDistance + new Random().nextDouble());
        else backwards.multiply(movementDistance + new Random().nextDouble() * movementRandomness);
        Location spawn = location.add(backwards);
        if (recentAttack && moveUpAfterAttack) spawn.add(0.0, 2.42, 0.0);
        spawn.setYaw(location.getYaw());
        spawn.setPitch(location.getPitch());
        return spawn;
    }

    public void reloadConfiguration() {
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection settings = super.configuration();
        violationWeight = settings.getInt("modules.npc.vl_weight", 5);
        enabled = settings.getBoolean("modules.npc.enable", true);
        movementDistance = settings.getDouble("modules.npc.movement.distance_base", 3.0);
        movementRandomness = settings.getDouble("modules.npc.movement.distance_random_size", 1.0);
        attackDistance = settings.getDouble("modules.npc.movement.distance_attack_base", 2.0);
        maximumSpeed = settings.getDouble("modules.npc.movement.max_speed", 0.5);
        asynchronous = settings.getBoolean("modules.npc.async", true);
        useRealEntityId = settings.getBoolean("modules.npc.real_entity_id", true);
        commandOnly = settings.getBoolean("modules.npc.only_command_trigger", false);
        spawnOnPlayerAttack = settings.getBoolean("modules.npc.spawn_on_attack_player", true);
        baseCheckTicks = settings.getInt("modules.npc.check_ticks.base", 100);
        attackCheckTicks = settings.getInt("modules.npc.check_ticks.attack_add", 40);
        hitTargetCheckTicks = settings.getInt("modules.npc.check_ticks.hit_bot_add", 50);
        visible = settings.getBoolean("modules.npc.visible", true);
        forceRandomName = settings.getBoolean("modules.npc.force_random_name", false);
        moveUpAfterAttack = settings.getBoolean("modules.npc.movement.move_up_after_attack", false);
    }

    public Unit pulseDamageAnimation() {
        target.pulseDamageAnimation();
        return Unit.INSTANCE;
    }

    public void moveTarget(Location location, boolean attack) {
        if (target == null) return;
        try {
            target.moveTowardLocation(location, attack, maximumSpeed);
            if (Math.random() < 0.1)
                target.sendSwingAnimation();
        } catch (NullPointerException ignored) { }
    }

    private String chooseName(Player player) {
        Set<Player> players = BukkitPlayerAccess.filterOnlinePlayers(NON_SPECTATOR);
        if (players.size() > 1 && !forceRandomName)
            return IdentityUtilities.chooseOtherPlayer(players, player);
        return IdentityUtilities.randomToken();
    }

    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        if (target == null) return;
        remainingTicks = 0;
        target.removeEntity();
    }

    /** Periodic NPC movement and command-triggered spawn lifecycle. */
    public void onTick() {
        if (target != null && remainingTicks > 0) {
            if (--remainingTicks == 0) {
                target.removeEntity();
                target = null;
                resetVerticalMotion();
            } else {
                Player player = super.playerState().getPlayer();
                Location current = player.getLocation().clone();
                current = attackSpawnLocation(current);
                updateVerticalMotion();
                current.add(0.0, verticalOffset, 0.0);
                current.setPitch((float) (Math.random() * 30.0 - 0.5));
                current.setYaw((float) (current.getYaw() + Math.random()));
                boolean attacking = verticalOffset == 0.0;
                if (asynchronous) {
                    Location destination = current;
                    Matrix.getInstance().getAsyncPool().enqueue(() -> moveTarget(destination, attacking));
                } else {
                    target.moveTowardLocation(current, attacking, maximumSpeed);
                    if (Math.random() < 0.1)
                        target.sendSwingAnimation();
                }
            }
        }

        if (COMMAND_TARGETS.isEmpty()) return;
        Player player = super.playerState().getPlayer();
        if (!COMMAND_TARGETS.containsKey(player)) return;
        int requestedDurationTicks = COMMAND_TARGETS.get(player);
        COMMAND_TARGETS.remove(player);
        if (target != null) {
            target.removeEntity();
            target = null;
        }
        String name = chooseName(player);
        UUID profile = UUID.randomUUID();
        int entityId = IdentityUtilities.randomEntityId(!useRealEntityId);
        Location spawn = forceFieldLocation(player.getLocation().clone());
        target = new PlayerEntityTracker(player, name, profile, entityId, spawn);
        target.removed = !visible;
        target.visible = true;
        target.spawnPlayerEntity();
        target.sendSwingAnimation();
        if (visible) target.randomizeEquipment();
        else {
            target.sendPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY,
                    Integer.MAX_VALUE, 0, false, false));
        }
        remainingTicks = requestedDurationTicks;
    }

    public void onAttackWithContext(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!enabled || event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (spawnOnPlayerAttack && event.getEntityType() != org.bukkit.entity.EntityType.PLAYER) return;
        if (target != null || commandOnly) {
            remainingTicks += attackCheckTicks;
            return;
        }
        Player attacker = (Player) event.getDamager();
        Location attackerLocation = attacker.getLocation();
        boolean playerVictim = event.getEntityType() == org.bukkit.entity.EntityType.PLAYER;
        String name = playerVictim ? event.getEntity().getName()
                : IdentityUtilities.randomToken();
        UUID profile = UUID.randomUUID();
        int entityId = IdentityUtilities.randomEntityId(!useRealEntityId);
        Location spawn = attackSpawnLocation(attackerLocation.clone());
        target = new PlayerEntityTracker(attacker, name, profile, entityId, spawn);
        target.removed = !visible;
        target.visible = true;
        target.spawnPlayerEntity();
        target.sendSwingAnimation();
        if (visible)
            target.randomizeEquipment();
        remainingTicks = baseCheckTicks;
    }

    public boolean onPacketReceive(me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon packetType,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent event,
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper wrapper) {
        if (target == null || !PacketEventSupport.isAttackEvent(event)) return false;
        int entityId = PacketEventSupport.getAttackEntityId(wrapper);
        if (entityId != target.getEntityId()) return false;
        FoliaSupport.INSTANCE.runTaskAsync(() -> pulseDamageAnimation());
        super.flag("ka.ff", "(npc) force field", violationWeight);
        remainingTicks += hitTargetCheckTicks;
        lastAttackTime = System.currentTimeMillis();
        return true;
    }

    static {
        NON_SPECTATOR = CombatTargetTracker::isNotSpectator;
    }

    private static boolean isNotSpectator(Player player) {
        return player.getGameMode() != GameMode.SPECTATOR;
    }
}
