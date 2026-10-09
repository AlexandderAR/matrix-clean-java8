package me.rerere.matrix.checks;

import me.rerere.matrix.world.RecentBlockTracker;
import me.rerere.matrix.player.model.PreparedMovementData;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/** Mutable public state populated with neutral defaults and its associated geometry data. */
public class MutableGeometryState {
    public int integerValue;
    public Vector vector;
    public double height;
    public boolean volume;
    public boolean margin;
    public boolean opacity;
    public boolean flag;
    public final RecentBlockTracker timeout;
    public Location capacity;
    public long timeValue;
    public PreparedMovementData index;
    public boolean angle;

    public MutableGeometryState() {
        opacity = false;
        margin = false;
        angle = false;
        timeout = new RecentBlockTracker();
        volume = false;
        flag = false;
        vector = new Vector();
    }
}

