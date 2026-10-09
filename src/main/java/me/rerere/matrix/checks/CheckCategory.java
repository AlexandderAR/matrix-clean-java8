package me.rerere.matrix.checks;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.rerere.matrix.api.HackType;
import me.rerere.matrix.config.CheckSetting;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.config.HackTypeSettingLookup;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntries;
import me.rerere.matrix.thirdparty.kotlin.enums.EnumEntriesKt;
import org.bukkit.World;

/** The built-in Matrix categories and their configuration metadata. */
public enum CheckCategory {
    KILLAURA("KillAura", 15, "Detect whether the player uses KillAura/AimBot and other combat cheating"),
    CLICK("Click", 10, "Prevent players from clicking too fast, or using AutoClicker to attack other entities"),
    HITBOX("HitBox", 12, "Check whether the player can normally attack the opponent's HitBox, it prevent players using Reach or HitBox"),
    MOVE("Move", 50, "Check if the player's movement is normal (Very Important)"),
    BADPACKETS("BadPackets", 50, "Check if the player has sent too many packets or abnormal packets"),
    DELAY("Delay", 3, "Check if the player has accelerated certain actions, such as FastEat"),
    BLOCK("Block", 20, "Check if the player destroys or places the block properly"),
    SCAFFOLD("Scaffold", 15, "Check if the player is using Scaffold/Tower cheating"),
    VELOCITY("Velocity", 12, "Check if the player received the server velocity(knockback) normally"),
    CHAT("Chat", 5, "Simply prevent spamming"),
    INTERACT("Interact", 20, "Check if the player's interaction with the entity or block is normal"),
    PHASE("Phase", 15, "Check if the player is trying to move into the block"),
    AUTOBOT("AutoBot", 5, "Check if the player is playing the game with an automated robot Mod"),
    ELYTRA("Elytra", 15, "Check if the player moves normally when using elytra in (1.9+)"),
    VEHICLE("Vehicle", 10, "Check if the player moves normally when using the vehicle (1.9+)");

    private String displayName;
    private int categoryWeight;
    private final String description;
    private boolean enabled = true;
    private Map<String, Object> commandOptions = new LinkedHashMap<String, Object>();
    private Set<String> disabledWorlds;
    private int decayAmount;
    private int decayInterval;
    private int activateOnlineAmount;
    private CheckSetting configuration;

    public static final HackTypeSettingLookup settingLookup;
    private static final List<CheckCategory> disabledDuringLowTps;
    private static final CheckCategory[] CATEGORY_VALUES;
    private static final EnumEntries<CheckCategory> ENUM_ENTRIES;

    CheckCategory(String displayName, int baseDecayAmount, String description) {
        this.displayName = displayName;
        this.categoryWeight = baseDecayAmount;
        this.description = description;
    }

    /** Whether this category is enabled after Matrix's low-TPS suppression rule. */
    public boolean isEnabled() {
        if (MatrixConfiguration.tpsProtectionEnabled
                && RollingStatistics.metric() < MatrixConfiguration.minimumTps
                && disabledDuringLowTps.contains(this)) {
            return false;
        }
        return enabled;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEnabledIn(World world) {
        if (disabledWorlds == null) return true;
        Set<String> worldNames = disabledWorlds;
        return worldNames != null && !worldNames.contains(world.getName());
    }

    public String getDisplayName() { return displayName; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }

    public HackType toApiHackType() {
        for (HackType hackType : HackType.values()) {
            if (hackType.name().equalsIgnoreCase(displayName)) return hackType;
        }
        throw new IllegalStateException("HackType Not Match With CheckType, Please contact author!");
    }

    public int getCategoryWeight() { return categoryWeight; }

    public int getDecayAmount() { return decayAmount; }

    public void setDecayAmount(Integer value) { decayAmount = value.intValue(); }

    public int getDecayInterval() { return decayInterval; }

    public void setDecayInterval(Integer value) { decayInterval = value.intValue(); }

    public int getActivateOnlineAmount() { return activateOnlineAmount; }

    public void setActivateOnlineAmount(Integer value) { activateOnlineAmount = value.intValue(); }

    public Map<String, Object> getCommandOptions() { return commandOptions; }

    public void setCommandOptions(Map<String, Object> options) { commandOptions = options; }

    public Set<String> getDisabledWorlds() { return disabledWorlds; }

    public void setDisabledWorlds(Set<String> worldNames) { disabledWorlds = worldNames; }

    public void setConfiguration(CheckSetting configuration) { this.configuration = configuration; }

    public CheckSetting getConfiguration() { return configuration; }

    public void setCategoryWeight(Integer value) { categoryWeight = value.intValue(); }

    public static CheckCategory fromSettingName(String name) {
        return settingLookup.bySettingName(name);
    }

    private static CheckCategory[] categoryValues() {
        return CATEGORY_VALUES.clone();
    }

    public static EnumEntries<CheckCategory> enumEntries() {
        return ENUM_ENTRIES;
    }

    static {
        CATEGORY_VALUES = values();
        ENUM_ENTRIES = EnumEntriesKt.enumEntries(CATEGORY_VALUES);
        settingLookup = new HackTypeSettingLookup((me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker) null);
        disabledDuringLowTps = Arrays.asList(INTERACT, BLOCK, VELOCITY, AUTOBOT);
    }
}
