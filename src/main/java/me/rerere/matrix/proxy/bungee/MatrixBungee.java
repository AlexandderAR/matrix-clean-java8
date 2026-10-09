package me.rerere.matrix.proxy.bungee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.geysermc.floodgate.api.FloodgateApi;

/** BungeeCord integration that forwards Matrix's Geyser compatibility payload. */
public class MatrixBungee extends Plugin implements Listener {
    private static boolean floodgateDetected;

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        logger.info("\u00a75    __  ______  __________  _____  __");
        logger.info("\u00a75   /  |/  /   |/_  __/ __ /  _/ |/ /");
        logger.info("\u00a75  / /|_/ / /| | / / / /_/ // / |   / ");
        logger.info("\u00a75 / /  / / ___ |/ / / _, _// / /   |  ");
        logger.info("\u00a75/_/  /_/_/  |_/_/ /_/ |_/___//_/|_|  ");
        logger.info("");
        logger.info("Detecting the Environment");
        checkGeyser();
        logger.info("Registering the listeners");
        getProxy().getPluginManager().registerListener(this, this);
        if (getProxy().getPluginManager().getPlugin("floodgate") != null) {
            floodgateDetected = true;
            logger.info("Detected Floodgate");
        }
        logger.info("MatrixBungee Loaded <3");
    }

    @EventHandler
    public void onChangeServer(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        Server server = player.getServer();
        if (!isBedrockPlayer(player)) {
            getLogger().info("[Debug] " + player.getName()
                    + " is not a bedrock player! (f:" + floodgateDetected + ")");
            return;
        }

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(payload);
        try {
            output.writeUTF(player.getName());
        } catch (IOException failure) {
            failure.printStackTrace();
        }
        getProxy().getScheduler().schedule(this, () -> {
            server.sendData("matrix:geyser", payload.toByteArray());
            getLogger().info("[GeyserCompatibility] sent data to server "
                    + server.getInfo().getName() + " for " + player.getName());
        }, 500L, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equalsIgnoreCase("matrix:geyser")
                && event.getSender() instanceof ProxiedPlayer) {
            event.setCancelled(true);
            ProxiedPlayer player = (ProxiedPlayer) event.getSender();
            getLogger().info("[MatrixBungee] Prevented a illegal channel message ("
                    + player.getName() + ")");
        }
    }

    private boolean isBedrockPlayer(ProxiedPlayer player) {
        if (floodgateDetected) {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        }
        return GeyserPlayerAccess.isBedrockPlayer(player);
    }

    private void checkGeyser() {
        Plugin plugin = ProxyServer.getInstance().getPluginManager().getPlugin("Geyser-BungeeCord");
        if (plugin != null) {
            getLogger().info("Detected GeyserMC");
        }
    }

    static {
        floodgateDetected = false;
    }
}
