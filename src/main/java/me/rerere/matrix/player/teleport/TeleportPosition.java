package me.rerere.matrix.player.teleport;

import me.rerere.matrix.util.time.MillisecondSampler;
import org.bukkit.util.Vector;

/** A position and server teleport acknowledgement snapshot. */
public final class TeleportPosition {
    private Vector position;
    private long teleportTime;
    private int teleportId;
    private boolean accepted;

    public TeleportPosition(Vector position, int teleportId) {
        this.teleportTime = MillisecondSampler.currentTimeMillis();
        this.accepted = false;
        if (position == null) {
            throw new NullPointerException("position is marked non-null but is null");
        }
        this.position = position;
        this.teleportId = teleportId;
    }

    public Vector position() {
        return position;
    }

    public void setPosition(Vector position) {
        this.position = position;
    }

    public int teleportId() {
        return teleportId;
    }

    public void setTeleportId(int teleportId) {
        this.teleportId = teleportId;
    }

    public long teleportTime() {
        return teleportTime;
    }

    public void setTeleportTime(long teleportTime) {
        this.teleportTime = teleportTime;
    }

    public boolean accepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof TeleportPosition)) return false;
        TeleportPosition that = (TeleportPosition) other;
        return teleportId == that.teleportId && teleportTime == that.teleportTime
                && (position == null ? that.position == null : position.equals(that.position));
    }

    @Override public int hashCode() {
        int result = 59 + teleportId;
        result = 59 * result + (int) (teleportTime ^ (teleportTime >>> 32));
        result = 59 * result + (position == null ? 43 : position.hashCode());
        return result;
    }

    @Override public String toString() {
        return "PacketMoveHandler.TeleportPos(position=" + String.valueOf(position)
                + ", teleportId=" + teleportId + ", teleportTime=" + teleportTime
                + ", accepted=" + accepted + ")";
    }
}
