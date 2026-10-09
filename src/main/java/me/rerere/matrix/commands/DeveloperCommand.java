package me.rerere.matrix.commands;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.logging.DebugLogger;
import me.rerere.matrix.listeners.BlockWorldListener;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.MutableGeometryState;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.network.packet.TransactionPingQueue;

import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Developer command and its diagnostic toggles. */
public class DeveloperCommand extends AbstractCommandDescriptor {
    public static boolean developerMode;
    public static boolean denyBlockBreak;
    public static boolean denyPlace;
    public static boolean blockEventFlag;
    public static boolean debugScaffold;
    public static boolean debugAim;
    public static boolean debugSensitivity;
    public static boolean debugInteraction;
    public static boolean debugMovement;
    public static boolean debugBaritone;
    public static boolean debugPattern;
    public static boolean debugCollision;
    public static boolean debugStrafe;
    public static boolean movementOption;
    public static boolean bedrockModeEnabled;
    private static boolean auxiliaryFlag;
    public static String flyingTarget;
    public static BukkitTask lagTask;
    public static final boolean ASSERTIONS_DISABLED;

    static {
        ASSERTIONS_DISABLED = !DeveloperCommand.class.desiredAssertionStatus();
        developerMode = false;
        debugMovement = false;
        movementOption = false;
        debugAim = false;
        debugSensitivity = false;
        debugInteraction = false;
        debugCollision = false;
        debugScaffold = false;
        debugPattern = false;
        bedrockModeEnabled = false;
        debugStrafe = false;
        debugBaritone = false;
        flyingTarget = null;
        denyBlockBreak = false;
        denyPlace = false;
        blockEventFlag = false;
    }

    public DeveloperCommand() {
        super("dev", "", "toggle developer mode", "matrix.dev");
        this.setEnabled(true);
    }

    @Override
    public List<String> getTabCompletions() {
        // The canonical saved JAR fails at this allocation before storing any entries.
        return Arrays.asList(new String[-2085009897]);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            developerMode = !developerMode;
            String stateColor = String.valueOf(developerMode ? ChatColor.GREEN : ChatColor.RED);
            sender.sendMessage(formatDeveloperMode(developerMode
                    ? formatEnabled(stateColor)
                    : formatDisabled(stateColor)));
            if (developerMode) {
                DebugLogger.printDebug("Debug mode enabled!");
            }
            return;
        }
        if (args.length <= 1) return;

        if (args[1].equals("pred")) {
            BlockWorldListener.movementPredictionEnabled =
                    !BlockWorldListener.movementPredictionEnabled;
            sender.sendMessage(formatPrediction(BlockWorldListener.movementPredictionEnabled
                    ? formatEnabled(String.valueOf(ChatColor.GREEN))
                    : formatDisabled(String.valueOf(ChatColor.RED))));
        }
        if (args[1].equals("baritone")) {
            debugBaritone = !debugBaritone;
            sender.sendMessage(formatBaritone(debugBaritone
                    ? formatEnabled(String.valueOf(ChatColor.GREEN))
                    : formatDisabled(String.valueOf(ChatColor.RED))));
        }
        if (args[1].equals("memleak")) {
            for (Object session : Matrix.getInstance().getPlayerDataManager().sessions().values()) {
                PlayerCheckContext playerData = (PlayerCheckContext) session;
                System.out.println(playerData.playerName());
            }
        }
        if (args[1].equals("folia")) {
            Player player = (Player) sender;
            if (!ASSERTIONS_DISABLED) {
                assert me.rerere.matrix.compat.FoliaSupport
                        .nearbyEntityById(player, player.getEntityId()) != null;
            }
            player.getWorld().getNearbyEntities(player.getLocation(), 10.0, 10.0, 10.0);
            for (Player online : Bukkit.getOnlinePlayers()) {
                System.out.println(formatPlayerEntity(online.getName(), Integer.valueOf(online.getEntityId()),
                        String.valueOf(me.rerere.matrix.compat.FoliaSupport
                                .nearbyEntityById(player, online.getEntityId()))));
            }
            player.sendMessage(formatTested(String.valueOf(ChatColor.GREEN)));
        }
        if (args[1].equals("break")) {
            denyBlockBreak = !denyBlockBreak;
            sender.sendMessage(formatDenyBreak(Boolean.valueOf(denyBlockBreak)));
        }
        if (args[1].equalsIgnoreCase("pattern")) {
            debugPattern = !debugPattern;
            sender.sendMessage(formatPattern(Boolean.valueOf(debugPattern)));
        }
        if (args[1].equals("scale")) {
            if (args.length == 2) {
                Player player = (Player) sender;
                player.getAttribute(Attribute.SCALE).setBaseValue(
                        player.getAttribute(Attribute.SCALE).getDefaultValue());
                sender.sendMessage(formatScale(Double.valueOf(
                        player.getAttribute(Attribute.SCALE).getBaseValue())));
            } else {
                double value = Double.parseDouble(args[2]);
                Player player = (Player) sender;
                player.getAttribute(Attribute.SCALE).setBaseValue(value);
                sender.sendMessage(formatScale(Double.valueOf(
                        player.getAttribute(Attribute.SCALE).getBaseValue())));
            }
        }
        if (args[1].equals("step")) {
            if (args.length == 2) {
                Player player = (Player) sender;
                player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(
                        player.getAttribute(Attribute.STEP_HEIGHT).getDefaultValue());
                sender.sendMessage(formatStep(Double.valueOf(
                        player.getAttribute(Attribute.STEP_HEIGHT).getBaseValue())));
            } else {
                double value = Double.parseDouble(args[2]);
                Player player = (Player) sender;
                player.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(value);
                sender.sendMessage(formatStep(Double.valueOf(
                        player.getAttribute(Attribute.STEP_HEIGHT).getBaseValue())));
            }
        }
        if (args[1].equals("place")) {
            denyPlace = !denyPlace;
            sender.sendMessage(formatDenyPlace(Boolean.valueOf(denyPlace)));
        }
        if (args[1].equals("bedrock")) {
            PlayerCheckContext playerData = Matrix.getInstance().getPlayerDataManager()
                    .find(((Player) sender).getUniqueId());
            playerData.markBedrockClient();
        }
        if (args[1].equals("vel")) {
            ((Player) sender).setVelocity(new Vector(
                    -0x1.0147ae147ae14p-3, 0x1.d7ced916872bp-2, -0x1.32c49ba5e353fp1));
            sender.sendMessage("Apply vel");
        }
        if (args[1].equals("netty")) sendNettyInformation(sender);
        if (args[1].equals("analysis")) {
            bedrockModeEnabled = !bedrockModeEnabled;
            sender.sendMessage(formatAnalysis(Boolean.valueOf(bedrockModeEnabled)));
        }
        if (args[1].equals("strafe")) {
            debugStrafe = !debugStrafe;
            sender.sendMessage(formatStrafe(Boolean.valueOf(debugStrafe)));
        }
        if (args[1].equals("horse")) {
            Player player = (Player) sender;
            Location location = player.getLocation().clone();
            Horse horse = (Horse) location.getWorld().spawnEntity(location, EntityType.HORSE);
            horse.setTamed(true);
            horse.setOwner(player);
            horse.setAdult();
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            player.sendMessage("Spawned horse");
        }
        if (args[1].equals("tntjump")) {
            Player player = (Player) sender;
            World world = player.getWorld();
            Location location = player.getLocation().clone()
                    .add(-0x1.0p-1, -0x1.999999999999ap-4, -0x1.0p-1);
            Bukkit.getScheduler().runTaskLater(Matrix.getInstance(),
                    () -> explodeFourTimes(world, location, sender), 8384984333636567963L);
        }
        if (args[1].equals("sens_tp")) {
            Player player = (Player) sender;
            Location location = player.getLocation().clone();
            location.setYaw(0.0F);
            location.setPitch(0.0F);
            player.teleport(location);
            sender.sendMessage("Teleported!");
        }
        if (args[1].equals("bwtnt")) {
            blockEventFlag = !blockEventFlag;
            sender.sendMessage(formatBlockTnt(Boolean.valueOf(blockEventFlag)));
        }
        if (args[1].equals("killall")) {
            for (LivingEntity livingEntity : ((Player) sender).getWorld().getLivingEntities()) {
                if (!(livingEntity instanceof Player)) livingEntity.remove();
            }
            sender.sendMessage("Killed all entities");
        }
        if (args[1].equals("folia-test")) {
            Player player = (Player) sender;
            PlayerCheckContext playerData =
                    Matrix.getInstance().getPlayerDataManager().find(player.getUniqueId());
            CheckRegistry profile = playerData.checks();
            PlayerCheckContext refreshed = profile.getConfiguration();
            MutableGeometryState movement = refreshed.movementContext();
            movement.capacity = new Location(player.getWorld(), player.getLocation().getX() + 100.0,
                    player.getLocation().getY(), player.getLocation().getZ() + 10000.0,
                    player.getLocation().getYaw(), player.getLocation().getPitch());
            player.sendMessage("Set legit location");
        }
        if (args[1].equals("tpasync")) {
            Player player = (Player) sender;
            Location elevatedLocation = player.getLocation().clone();
            elevatedLocation.setY(elevatedLocation.getY() + 3.0);
            PlayerUtilities.teleportViaPlatform(
                    player, elevatedLocation);
        }
        if (args[1].equals("move")) {
            debugMovement = !debugMovement;
            sender.sendMessage(formatMovement(String.valueOf(ChatColor.GREEN), Boolean.valueOf(debugMovement)));
        }
        if (args[1].equals("hitbox")) {
            if (args.length == 2) {
                sender.sendMessage(formatTargetRequired(String.valueOf(ChatColor.RED)));
                return;
            }
            String targetName = args[2];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(formatPlayerNotFound(String.valueOf(ChatColor.RED)));
                return;
            }
            PlayerCheckContext targetData =
                    Matrix.getInstance().getPlayerDataManager().find(target.getUniqueId());
            targetData.setDebugEnabled(!targetData.debugEnabled());
            sender.sendMessage(formatHitbox(String.valueOf(ChatColor.GREEN), targetData.playerName(),
                    Boolean.valueOf(targetData.debugEnabled())));
        }
        if (args[1].equals("tpspam")) {
            Player player = (Player) sender;
            Location location = player.getLocation().clone();
            for (int i = 0; i < 1312887414; i++) {
                PlayerUtilities.teleportViaPlatform(player, location);
            }
            sender.sendMessage("Teleported x16");
        }
        if (args[1].equalsIgnoreCase("ping")) {
            PlayerCheckContext playerData =
                    Matrix.getInstance().getPlayerDataManager().find(((Player) sender).getUniqueId());
            playerData.enqueueTransaction(() -> received(sender));
            sender.sendMessage("sent a transaction");
        }
        if (args[1].equals("flying")) {
            if (args.length == 2) {
                flyingTarget = null;
                sender.sendMessage(formatFlyingDisabled(String.valueOf(ChatColor.GREEN)));
            } else {
                flyingTarget = args[2];
                sender.sendMessage(formatFlyingPacket(String.valueOf(ChatColor.GREEN), flyingTarget));
            }
        }
        if (args[1].equals("gliding")) {
            PlayerCheckContext playerData =
                    Matrix.getInstance().getPlayerDataManager().find(((Player) sender).getUniqueId());
            playerData.setGliding(!playerData.isGliding());
            sender.sendMessage(formatGliding(Boolean.valueOf(playerData.isGliding())));
        }
        if (args[1].equals("sfdt")) {
            debugScaffold = !debugScaffold;
            sender.sendMessage(formatScaffold(String.valueOf(ChatColor.GREEN), Boolean.valueOf(debugScaffold)));
        }
        if (args[1].equals("aim")) {
            debugAim = !debugAim;
            sender.sendMessage(formatDebugAim(String.valueOf(ChatColor.GREEN), Boolean.valueOf(debugAim)));
        }
        if (args[1].equals("sens")) {
            debugSensitivity = !debugSensitivity;
            sender.sendMessage(formatSensitivity(String.valueOf(ChatColor.GREEN), debugSensitivity));
        }
        if (args[1].equals("interact")) {
            debugInteraction = !debugInteraction;
            sender.sendMessage(formatInteract(String.valueOf(ChatColor.GREEN), Boolean.valueOf(debugInteraction)));
        }
        if (args[1].equals("coll")) {
            debugCollision = !debugCollision;
            sender.sendMessage(formatCollision(String.valueOf(ChatColor.GREEN), Boolean.valueOf(debugCollision)));
        }
        if (args[1].equals("levitation")) {
            ((Player) sender).addPotionEffect(new PotionEffect(
                    PotionEffectType.LEVITATION, -384537111, -1059281354));
        }
        if (args[1].equals("spawnmassive")) {
            // The recovered loop bound is negative, so its spawn body is unreachable.
            sender.sendMessage("Spawned 10000 zombies");
        }
        if (args[1].equals("lag")) {
            if (lagTask == null) {
                sender.sendMessage("Start to simulate lagging...");
                lagTask = Bukkit.getScheduler().runTaskTimer(Matrix.getInstance(), DeveloperCommand::runLagTick, 0L, 1L);
            } else {
                lagTask.cancel();
                lagTask = null;
                sender.sendMessage("Stop simulating lag");
            }
        }

        if (args[1].equals("transaction")) {
            String targetName = args[2];
            Player target = Bukkit.getPlayer(targetName);
            PlayerCheckContext targetData =
                    Matrix.getInstance().getPlayerDataManager().find(target.getUniqueId());
            if (targetData != null) {
                TransactionPingQueue transactions = targetData.transactionQueue();
                boolean enabled = !transactions.notificationsEnabled();
                transactions.setNotifications(enabled);
                sender.sendMessage(formatTransaction(
                        transactions.notificationsEnabled(), targetName));
            }
        }
    }

    private void sendNettyInformation(CommandSender sender) {
        sender.sendMessage("Netty Information:");
    }

    private static void runLagTick() {
        try {
            Thread.sleep((long) (Math.random() * 30.0) - 862330854307110809L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void explodeFourTimes(World world, Location location, CommandSender sender) {
        for (int i = 0; i < 4; i++) {
            world.createExplosion(location, 4.0F, false, false);
        }
        sender.sendMessage("TNT Jump!");
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit received(CommandSender sender) {
        sender.sendMessage("Received!");
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    public static void setAuxiliaryFlag(boolean enabled) { auxiliaryFlag = enabled; }
    public static boolean isAuxiliaryDisabled() { return !auxiliaryFlag; }
    public static boolean getAuxiliaryFlag() { return auxiliaryFlag; }

    private static String formatDebugAim(String color, boolean enabled) { return color + "Debug Aim: " + enabled; }
    private static String formatPrediction(String color) { return color + " prediction engine!"; }
    private static String formatBlockTnt(boolean enabled) { return "simulate_bw_tnt = " + enabled; }
    private static String formatTransaction(boolean enabled, String target) { return "Debug Transaction set to " + enabled + " for " + target; }
    private static String formatAnalysis(boolean enabled) { return "debug analysis = " + enabled; }
    private static String formatStrafe(boolean enabled) { return "debug strafe = " + enabled; }
    private static String formatTested(String color) { return color + "Tested"; }
    private static String formatHitbox(String color, String target, boolean enabled) { return color + "Hitbox Debug(" + target + "): " + enabled; }
    private static String formatPlayerNotFound(String color) { return color + "Player not found"; }
    private static String formatScale(double value) { return "scale = " + value; }
    private static String formatInteract(String color, boolean enabled) { return color + "Debug Interact: " + enabled; }
    private static String formatDenyBreak(boolean denied) { return "deny break = " + denied; }
    private static String formatDenyPlace(boolean denied) { return "deny place = " + denied; }
    private static String formatPlayerEntity(String name, int entityId, String entity) { return "Player: " + name + '#' + entityId + ": " + entity; }
    private static String formatMovement(String color, boolean enabled) { return color + "Debug Movement: " + enabled; }
    private static String formatFlyingPacket(String color, String target) { return color + "Debug Flying Packet: " + target; }
    private static String formatDeveloperMode(String color) { return color + " developer mode!"; }
    private static String formatFlyingDisabled(String color) { return color + "Disabled Flying Debug"; }
    private static String formatPattern(boolean enabled) { return "pattern debug = " + enabled; }
    private static String formatGliding(boolean enabled) { return "gliding = " + enabled; }
    private static String formatDisabled(String color) { return color + "Disabled"; }
    private static String formatBaritone(String color) { return color + " baritone debug mode!"; }
    private static String formatSensitivity(String color, boolean enabled) { return color + "Debug SENS: " + enabled; }
    private static String formatTargetRequired(String color) { return color + "Please specify a target"; }
    private static String formatScaffold(String color, boolean enabled) { return color + "Debug Scaffold(T): " + enabled; }
    private static String formatCollision(String color, boolean enabled) { return color + "Debug Collision: " + enabled; }
    private static String formatStep(double value) { return "step = " + value; }
    private static String formatEnabled(String color) { return color + "Enabled"; }

}
