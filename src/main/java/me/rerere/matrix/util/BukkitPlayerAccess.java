package me.rerere.matrix.util;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BukkitPlayerAccess {
    public BukkitPlayerAccess() { }
    public static int getOnlinePlayerCount() { return Bukkit.getOnlinePlayers().size(); }
    public static Set<Player> filterOnlinePlayers(Predicate<Player> predicate) {
        return Bukkit.getOnlinePlayers().stream().filter(predicate).collect(Collectors.toSet());
    }
    public static boolean isExternalEventCall() {
        StackTraceElement[] trace = new Exception().getStackTrace();
        boolean foundEventDispatcher = false;
        for (StackTraceElement frame : trace) {
            if (foundEventDispatcher) {
                String name = frame.getClassName();
                if (name.startsWith("net.minecraft.server") || name.startsWith("org.bukkit")) return false;
                return true;
            }
            if (frame.getClassName().equalsIgnoreCase("org.bukkit.plugin.SimplePluginManager")
                    && frame.getMethodName().equalsIgnoreCase("callEvent")) foundEventDispatcher = true;
        }
        return false;
    }
}
