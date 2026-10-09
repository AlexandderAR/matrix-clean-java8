package me.rerere.matrix.commands.condition.placeholders;

import java.util.regex.Pattern;
import org.bukkit.entity.Player;

/** Permission placeholder; true is encoded as 1.0 and false as 0.0. */
public final class PermissionPlaceholder extends AbstractParameteredPlaceholder {
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("-perm:([A-Za-z0-9.]*)-");

    public PermissionPlaceholder() { super("perm"); }

    @Override public String getSub(Player player, String permission) {
        return player.hasPermission(permission) ? "1.0" : "0.0";
    }
}
