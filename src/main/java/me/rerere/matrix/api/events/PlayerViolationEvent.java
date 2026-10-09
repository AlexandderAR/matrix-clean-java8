package me.rerere.matrix.api.events;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.rerere.matrix.api.HackType;

public class PlayerViolationEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final HackType hackType;
    private final Player player;
    private final Map<String, String> tooltips;
    private boolean cancelled;
    private int violations;
    private final String message;
    private final String component;

    public PlayerViolationEvent(Player player, String component, HackType hackType,
                                String message, Map<String, String> tooltips, int violations) {
        super(!Bukkit.isPrimaryThread());
        this.cancelled = false;
        this.player = player;
        this.hackType = hackType;
        this.component = component;
        this.message = message;
        this.tooltips = tooltips;
        this.violations = violations;
    }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
    public Map<String, String> getTooltips() { return tooltips; }
    public String getComponent() { return component; }
    public int getViolations() { return violations; }
    public HackType getHackType() { return hackType; }
    public void setViolations(int violations) { this.violations = violations; }
    public String getMessage() { return message; }
    @Override public boolean isCancelled() { return cancelled; }
    public Player getPlayer() { return player; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}

