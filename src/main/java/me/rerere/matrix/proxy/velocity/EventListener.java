package me.rerere.matrix.proxy.velocity;

import java.util.concurrent.TimeUnit;
import java.util.Optional;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

public final class EventListener {
    public static final Companion Companion = new Companion();
    public static final ChannelIdentifier identifier = MinecraftChannelIdentifier.create("matrix", "geyser");
    private final me.rerere.matrix.proxy.velocity.MatrixVelocity plugin;
    private final ProxyServer proxyServer;

    public EventListener(me.rerere.matrix.proxy.velocity.MatrixVelocity plugin, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
    }

    private boolean isBedrockPlayer(Player player) {
        if (player.getUniqueId().toString().startsWith("00000000-0000-0000-")) return true;
        if (me.rerere.matrix.proxy.velocity.MatrixVelocity.FLOODGATE)
            return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        return me.rerere.matrix.proxy.velocity.MatrixVelocity.GEYSER
                && me.rerere.matrix.proxy.velocity.GeyserAccessor.isBedrockPlayer(player);
    }

    @Subscribe public void onSwitch(ServerConnectedEvent event) {
        if (!isBedrockPlayer(event.getPlayer())) return;
        ByteArrayDataOutput data = ByteStreams.newDataOutput();
        data.writeUTF(event.getPlayer().getUsername());
        proxyServer.getScheduler().buildTask(plugin, () -> onSwitchLambda(event, data))
                .delay(500L, TimeUnit.MILLISECONDS).schedule();
    }

    private static void onSwitchLambda(ServerConnectedEvent event, ByteArrayDataOutput data) {
        Player player = event.getPlayer();
        Optional<ServerConnection> currentServer = player.isActive() ? player.getCurrentServer() : null;
        ServerConnection connection = currentServer == null ? null : currentServer.orElse(null);
        if (connection == null) {
            System.out.println("[Matrix] Failed to send data to null server");
            return;
        }
        connection.sendPluginMessage(identifier, data.toByteArray());
        String server = connection.getServer().getServerInfo().getName();
        System.out.println("[Matrix] sent a data to " + server + " make matrix ignore " + event.getPlayer().getUsername());
    }

    @Subscribe public void onPm(PluginMessageEvent event) {
        if (!"matrix:geyser".equalsIgnoreCase(event.getIdentifier().getId())) return;
        ChannelMessageSource source = event.getSource();
        if (!(source instanceof Player)) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        Object messageSource=event.getSource();
        Intrinsics.checkNotNull(messageSource, "null cannot be cast to non-null type com.velocitypowered.api.proxy.Player");
        String username = ((Player) messageSource).getUsername();
        System.out.println("[Matrix] Prevented illegal plugin message from " + username);
    }


    public static final class Companion { private Companion() {} public Companion(me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) { this(); } }
}
