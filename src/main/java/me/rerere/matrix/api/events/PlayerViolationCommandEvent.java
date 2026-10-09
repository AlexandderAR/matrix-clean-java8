package me.rerere.matrix.api.events;

import me.rerere.matrix.api.HackType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerViolationCommandEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;
    private final Player player;
    private final HackType hackType;
    private final String command;

    public PlayerViolationCommandEvent(Player player, HackType hackType, String command) {
        super(!Bukkit.isPrimaryThread());
        this.cancelled = false;
        this.player = player;
        this.hackType = hackType;
        this.command = command;
    }

    public Player getPlayer() {
        return player;
    }

    public HackType getHackType() {
        return hackType;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
