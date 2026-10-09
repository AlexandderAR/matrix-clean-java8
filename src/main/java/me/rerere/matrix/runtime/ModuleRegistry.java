package me.rerere.matrix.runtime;

import me.rerere.matrix.Matrix;
import org.bukkit.entity.Player;

/** Companion instance for the module list and cached player permission query. */
public final class ModuleRegistry {
    public ModuleRegistry() { }

    public final boolean isAllowed(Player player, String permission) {
        return Matrix.Companion.matrixInstance().getPermissionManager()
                .hasCachedPermission(player, permission);
    }

}
