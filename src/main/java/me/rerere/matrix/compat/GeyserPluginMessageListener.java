package me.rerere.matrix.compat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.player.PlayerCheckContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class GeyserPluginMessageListener implements PluginMessageListener {
    private static final String CHANNEL = "matrix:geyser".intern();

    public GeyserPluginMessageListener() { }
    @Override public void onPluginMessageReceived(String channel, Player sender, byte[] payload) {
        boolean channelMatches = StringsKt.equals(channel, CHANNEL, true);
        if (!channelMatches) return;
        if (!MatrixConfiguration.geyserSupport) return;
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            Throwable closeFailure = null;
            try {
                String playerName = input.readUTF();
                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null) {
                    PlayerCheckContext state = Matrix.Companion.matrixInstance().getPlayerDataManager()
                            .find(player.getUniqueId());
                    if (state != null) state.markBedrockClient();
                }
            } catch (Throwable failure) {
                closeFailure = failure;
                throw failure;
            } finally {
                CloseableKt.closeFinally(input, closeFailure);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
        }
    }
}
