package me.rerere.matrix.checks.combat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerPingAccessor;
import me.rerere.matrix.runtime.EntityCategories;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Detects player attacks that arrive within 50 ms of crystal placement. */
public final class CrystalAttackCheck extends CheckRuntimeState {
    private static boolean cancelAttack = true;
    private static boolean enabled = true;
    private static final long REACTION_THRESHOLD_MILLIS = 50L;
    private static int violationWeight = 5;
    private int reactionLevel;
    private long placementTime;
    private int trackedEntityId = -1;
    private static String message;

    public CrystalAttackCheck(PlayerCheckContext data) {
        super(data, CheckCategory.KILLAURA);
    }

    @Override
    public void reloadConfiguration() {
        ConfigurationSection section = configuration();
        enabled = section.getBoolean("modules.crystal.enable", true);
        violationWeight = configuration().getInt("modules.crystal.vl_weight", 5);
        cancelAttack = configuration().getBoolean("modules.crystal.cancel_attack", true);
    }

    public void onEntitySpawn(Entity entity) {
        if (!enabled) return;
        if (entity.getType() != EntityCategories.INSTANCE.legacyEndCrystal()) return;
        trackedEntityId = entity.getEntityId();
        placementTime = MillisecondSampler.currentTimeMillis();
    }

    public void onEntityDamageWithContext(EntityDamageByEntityEvent event) {
        if (!enabled || event.isCancelled() || event.getEntity().getEntityId() != trackedEntityId) return;
        trackedEntityId = -1;
        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) return;
        Player player = (Player) damager;
        long delay = MillisecondSampler.currentTimeMillis() - placementTime;
        int ping = PlayerPingAccessor.getPing(player);
        long reaction = delay - ping;
        if (reaction > REACTION_THRESHOLD_MILLIS) {
            reactionLevel = Math.max(0, reactionLevel - 1);
            return;
        }
        reactionLevel = Math.min(3, reactionLevel + 1);
        if (reactionLevel < 3) return;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("delay", Long.toString(delay));
        details.put("reaction", Long.toString(reaction));
        details.put("ping", Integer.toString(ping));
        flag("killaura.crystal",
                "attack a newly placed crystal too quickly // crystal_aura", details, violationWeight);
        if (cancelAttack) event.setCancelled(true);
    }

    public static void setMessage(String value) { message = value; }
    public static String getMessage() { return message; }
}
