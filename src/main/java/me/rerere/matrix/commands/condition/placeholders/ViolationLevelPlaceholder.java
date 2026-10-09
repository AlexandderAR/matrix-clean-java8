package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;
import me.rerere.matrix.api.MatrixAPIProvider;
import me.rerere.matrix.checks.CheckCategory;

/** Returns a player's current violation count for a named check type. */
public class ViolationLevelPlaceholder extends AbstractParameteredPlaceholder {
    public ViolationLevelPlaceholder() {
        super("vl");
    }

    public String getSub(Player player, String checkTypeName) {
        CheckCategory resolvedType = CheckCategory.fromSettingName(checkTypeName.toLowerCase());
        if (resolvedType == null) {
            System.out.println("[Matrix/ConditionalCommands] Unknown CheckType: " + checkTypeName);
            return Integer.toString(0);
        }
        return Integer.toString(MatrixAPIProvider.getAPI()
                .getViolations(player, resolvedType.toApiHackType()));
    }

}
