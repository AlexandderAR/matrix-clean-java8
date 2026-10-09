package me.rerere.matrix.compat.plugins.crazyenchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.rerere.matrix.api.HackType;
import me.rerere.matrix.api.events.PlayerViolationEvent;
import com.badbones69.crazyenchantments.paper.api.utils.EventUtils;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class CrazyEnchantmentsHooker implements Listener {
    private final Map<UUID, Long> exemptPlayers = new HashMap<>();

    @EventHandler
    public void onUseEnchant(BlockBreakEvent event) {
        if (EventUtils.isIgnoredEvent(event)) {
            Player player = event.getPlayer();
            exemptPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onFlag(PlayerViolationEvent event) {
        Player player = event.getPlayer();
        if ((event.getHackType() == HackType.INTERACT || event.getHackType() == HackType.BLOCK)
                && exemptPlayers.containsKey(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            Object recordedTimestamp = exemptPlayers.get(player.getUniqueId());
            Intrinsics.checkNotNull(recordedTimestamp);
            long elapsed = now - ((Number) recordedTimestamp).longValue();
            if (elapsed <= 150L) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        exemptPlayers.remove(event.getPlayer().getUniqueId());
    }
}
