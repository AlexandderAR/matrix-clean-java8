package me.rerere.matrix.compat;

import fr.nocsy.mcpets.api.MCPetsAPI;
import fr.nocsy.mcpets.data.Pet;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

/** Optional integration with MCPets. */
public final class MCPetCompatibility {
    public static final MCPetCompatibility INSTANCE = new MCPetCompatibility();
    private static final Lazy<Boolean> enabled$delegate = LazyKt.lazy(
            (Function0<Boolean>) MCPetCompatibility::isMCPetsPluginPresent);

    private MCPetCompatibility() {
    }

    private final boolean getEnabled() {
        return enabled$delegate.getValue();
    }

    private static final boolean isMCPetsPluginPresent() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        return pluginManager.getPlugin("MCPets") != null;
    }

    public final boolean isMountedOnPet(Player player) {
        if (!getEnabled()) {
            return false;
        }
        Pet pet = MCPetsAPI.getActivePet(player.getUniqueId());
        return pet != null && pet.hasMount(player);
    }
}

