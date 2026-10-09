package me.rerere.matrix.compat;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.plugins.crazyenchantments.CrazyEnchantmentsHooker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

/** Registers the CrazyEnchantments compatibility listener when that plugin is enabled. */
public final class CompatibilityManager {
    public CompatibilityManager() {}

    public void load() {
        PluginManager manager = Bukkit.getPluginManager();
        if (manager.getPlugin("CrazyEnchantments") != null
                && java.util.Objects.requireNonNull(manager.getPlugin("CrazyEnchantments")).isEnabled()) {
            Bukkit.getPluginManager().registerEvents(new CrazyEnchantmentsHooker(),
                    Matrix.getInstance());
            Matrix.logMessage(
                    "(Compatibility) Hooked with CrazyEnchantments");
        }
    }
}
