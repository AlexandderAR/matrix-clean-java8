package me.rerere.matrix.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

/** Velocity entry point for Matrix proxy support. */
@Plugin(id = "matrixbungee", name = "MatrixBungee", version = "1.0.0",
        description = "Matrix Geyser Support for Velocity Proxy", authors = {"RE"},
        dependencies = {@Dependency(id = "geyser", optional = true),
                @Dependency(id = "floodgate", optional = true)})
public class MatrixVelocity {
    public static boolean GEYSER;
    public static boolean FLOODGATE;

    private final ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent initializationEvent) {
        server.getEventManager().register(this, new EventListener(this, server));

        if (server.getPluginManager().getPlugin("floodgate").isPresent()) {
            FLOODGATE = true;
            System.out.println("Detected Floodgate");
        }
        if (server.getPluginManager().getPlugin("geyser").isPresent()) {
            GEYSER = true;
            System.out.println("Detected Geyser");
        }
    }

    @Inject
    public MatrixVelocity(ProxyServer server) {
        this.server = server;
        System.out.println("    __  ______  __________  _____  __");
        System.out.println("   /  |/  /   |/_  __/ __ /  _/ |/ /");
        System.out.println("  / /|_/ / /| | / / / /_/ // / |   / ");
        System.out.println(" / /  / / ___ |/ / / _, _// / /   |  ");
        System.out.println("/_/  /_/_/  |_/_/ /_/ |_/___//_/|_|  ");
        System.out.println();
        System.out.println("Loaded MatrixVelocity for Geyser Support!");
    }
}
