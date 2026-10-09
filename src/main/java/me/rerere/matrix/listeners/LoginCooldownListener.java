package me.rerere.matrix.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

/** Tracks reconnect timestamps and applies the configured fast-relog cooldown. */
public final class LoginCooldownListener implements Runnable, Listener {
    private final ConcurrentHashMap<UUID, Long> lastLoginMillis = new ConcurrentHashMap<>();
    public LoginCooldownListener() { }

    private static Unit rememberNow(LoginCooldownListener self, UUID playerId) {
        self.lastLoginMillis.put(playerId, System.currentTimeMillis());
        return Unit.INSTANCE;
    }

    public void recordLogin(UUID playerId) {
        FoliaSupport.INSTANCE.runAtGlobalScheduler(
            (Function0<Unit>) () -> rememberNow(this, playerId));
    }

    private static void removeLogin(LoginCooldownListener self, UUID playerId) {
        self.lastLoginMillis.remove(playerId);
    }

    @Override public void run() {
        List<UUID> expired = new ArrayList<>();
        for (java.util.Map.Entry<UUID, Long> entry : lastLoginMillis.entrySet()) {
            UUID playerId = entry.getKey();
            long loginMillis = entry.getValue().longValue();
            if (System.currentTimeMillis() - loginMillis
                    > (long) (MatrixConfiguration.relogTime * 1000)) {
                expired.add(playerId);
            }
        }
        expired.forEach(id -> removeLogin(this, id));
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!lastLoginMillis.containsKey(playerId)) return;
        long nowMillis = System.currentTimeMillis();
        Long loginMillis = lastLoginMillis.get(playerId);
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics.checkNotNull(loginMillis);
        long elapsed = nowMillis - loginMillis.longValue();
        long cooldownMillis = 1000L * MatrixConfiguration.relogTime;
        if (elapsed >= cooldownMillis) return;
        if (event.getPlayer().hasPermission("matrix.fastrelog")) return;

        long secondsLeft = (1000L * MatrixConfiguration.relogTime - elapsed) / 1000L;
        if (secondsLeft == 0L) secondsLeft = 1L;
        event.setResult(Result.KICK_OTHER);
        String message = StringsKt.replace(LocaleMessages.relogMessage,
                "%time_left%", String.valueOf(secondsLeft), false);
        event.setKickMessage(message);
    }

}
