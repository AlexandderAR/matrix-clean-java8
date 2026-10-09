package me.rerere.matrix.listeners;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import java.util.Map;
import java.util.UUID;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;

/** Drains pending player work at the server tick boundary. */
public final class TickEndEventHandler implements org.bukkit.event.Listener {
    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        if (FoliaSupport.INSTANCE.isUsingFolia()) {
            Map<UUID, PlayerCheckContext> sessions = Matrix.Companion.matrixInstance()
                    .getPlayerDataManager().sessions();
            for (Map.Entry<UUID, PlayerCheckContext> entry : sessions.entrySet()) {
                UUID playerId = entry.getKey();
                PlayerCheckContext context = entry.getValue();
                Entity entity;
                try {
                    entity = (Entity) context.getPlayer();
                } catch (Throwable failure) {
                    throw unchecked(failure);
                }
                if (Bukkit.isOwnedByCurrentRegion(entity)) {
                    context.transactionQueue().flushPendingWork();
                }
            }
            return;
        }

        Map<UUID, PlayerCheckContext> sessions = Matrix.Companion.matrixInstance()
                .getPlayerDataManager().sessions();
        for (Map.Entry<UUID, PlayerCheckContext> entry : sessions.entrySet()) {
            UUID playerId = entry.getKey();
            entry.getValue().transactionQueue().flushPendingWork();
        }
    }

    private static RuntimeException unchecked(Throwable failure) {
        return TickEndEventHandler.<RuntimeException>throwAny(failure);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T throwAny(Throwable failure) throws T {
        throw (T) failure;
    }

    public TickEndEventHandler() {}
}

