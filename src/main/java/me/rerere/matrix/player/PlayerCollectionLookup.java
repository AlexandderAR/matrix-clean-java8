package me.rerere.matrix.player;

import java.util.Collection;
import me.rerere.matrix.model.FeatureDescriptor;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

/** Supplies a collection associated with a player. */
@FunctionalInterface
public interface PlayerCollectionLookup {
    @NotNull
    Collection<FeatureDescriptor> getFor(@NotNull Player player);
}
