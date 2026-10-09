package me.rerere.matrix.movement;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.util.CollectionUtilities;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.world.TimedLocation;
import me.rerere.matrix.Matrix;

/** Retains a bounded per-player history of movement snapshots. */
public final class PlayerMovementHistory implements Listener {
    private final Map<UUID, Queue<TimedLocation>> history;

    @SuppressWarnings("unchecked")
    public PlayerMovementHistory() {
        this.history = (Map<UUID, Queue<TimedLocation>>) (Map<?, ?>)
                CollectionUtilities.newMap();
        Bukkit.getPluginManager().registerEvents(this,
                (Plugin) Matrix.getInstance());
    }

    /** Returns the tracked queue, or an untracked empty queue in guarded mode. */
    public Queue<TimedLocation> getHistory(Player player) {
        try {
            if (!history.containsKey(player.getUniqueId())) return new ArrayDeque<>();
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        return history.get(player.getUniqueId());
    }

    /** Typed implementation for the original array getter's UUID payload. */
    public Queue<TimedLocation> getHistory(UUID id) {
        try {
            if (!history.containsKey(id)) return new ArrayDeque<>();
            return history.get(id);
        } catch (LicenseResponseException failure) {
            throw failure;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        history.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location destination = event.getTo();
        Player player = event.getPlayer();
        if (destination == null) return;
        try {
            Queue<TimedLocation> samples = history.get(player.getUniqueId());
            if (samples == null) {
                samples = new ArrayDeque<>();
                history.put(player.getUniqueId(), samples);
            }
            samples.add(new TimedLocation(copyLocation(destination),
                    MillisecondSampler.currentTimeMillis()));
            if (samples.size() > 15) samples.poll();
        } catch (LicenseResponseException failure) {
            throw failure;
        }
    }

    /** Returns a detached copy so later event mutation does not alter history. */
    public static Location copyLocation(Location source) {
        return new Location(source.getWorld(), source.getX(), source.getY(), source.getZ(),
                source.getYaw(), source.getPitch());
    }

    public Set<UUID> getTrackedPlayers() {
        return history.keySet();
    }

}
