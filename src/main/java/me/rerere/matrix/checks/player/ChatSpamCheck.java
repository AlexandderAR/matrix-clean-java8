package me.rerere.matrix.checks.player;

import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Reports chat messages that arrive too close together or repeat rapidly. */
public class ChatSpamCheck extends CheckRuntimeState {
    private static int cancelViolationLevel;
    private long previousMessageTime;
    private String previousMessage;
    private static int minimumTimeSeconds;

    public ChatSpamCheck(PlayerCheckContext playerState) {
        super(playerState);
        super.setCheckType(CheckCategory.CHAT);
    }

    public void onAsyncChat(AsyncPlayerChatEvent event) {
        int violation = 0;
        long elapsedMillis = System.currentTimeMillis() - previousMessageTime;
        if (elapsedMillis < (long) minimumTimeSeconds * 1000L) {
            violation = 1;
            flag("chat.delay", formatDelay(elapsedMillis), 1);
        }
        if (previousMessage != null && event.getMessage().equalsIgnoreCase(previousMessage)
                && elapsedMillis < 30000L) {
            violation = 1;
            flag("chat.same", "send the same chat content", 1);
        }
        previousMessage = event.getMessage();
        previousMessageTime = System.currentTimeMillis();
        if (violation != 0 && cancelViolationLevel != -1) {
            PlayerCheckContext playerContext = playerState();
            ViolationTotals violations = playerContext.playerStorage();
            if (violations.totalFor(checkType()) >= cancelViolationLevel) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                player.sendMessage(LocaleMessages.spamWarning);
            }
        }
    }

    public void loadSettings() {
        ConfigurationSection delaySettings = checkType().getConfiguration().getConfigurationSection();
        minimumTimeSeconds = delaySettings.getInt("min_time", 3);
        ConfigurationSection cancellationSettings = checkType().getConfiguration()
                .getConfigurationSection();
        cancelViolationLevel = cancellationSettings.getInt("cancel_vl", 3);
    }

    private static String formatDelay(long delayMillis) {
        return "chat too fast | delay: " + delayMillis;
    }
}
