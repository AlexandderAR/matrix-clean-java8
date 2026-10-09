package me.rerere.matrix.commands.condition.placeholders;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PlaceholderTimeOnline extends AbstractStandardPlaceholder implements Listener {
    private final Map<Player, Long> loginTimeByPlayer = new HashMap<>();

    public PlaceholderTimeOnline() { super("time_online"); }
    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        loginTimeByPlayer.remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loginTimeByPlayer.put(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        loginTimeByPlayer.remove(event.getPlayer());
    }

    @Override
    public double getStat(Player player) {
        long now = System.currentTimeMillis();
        long joinedAt = loginTimeByPlayer.get(player);
        return (double) (now - joinedAt);
    }

    public void init(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            loginTimeByPlayer.put(player, System.currentTimeMillis());
        }
    }
}
