package me.rerere.matrix.entity;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Mutable movement-event context collected by the entity movement tracker. */
public final class MovementContextSnapshot {
    private int primaryMovementValue;
    private Location originLocation;
    private int secondaryMovementValue;
    private LivingEntity target;
    private List<?> movementPositions;
    private String targetType;
    private boolean statusFlag;
    private List<?> recentMoves;
    private Player player;

    public MovementContextSnapshot(Player player, LivingEntity target,
            List<?> movementPositions, List<?> recentMoves,
            int primaryMovementValue, int secondaryMovementValue,
            Location originLocation, String targetType, boolean statusFlag) {
        if (player == null) {
            throw new NullPointerException("player is marked non-null but is null");
        }
        if (target == null) {
            throw new NullPointerException("target is marked non-null but is null");
        }
        if (movementPositions == null) {
            throw new NullPointerException("movePosList is marked non-null but is null");
        }
        if (recentMoves == null) {
            throw new NullPointerException("recentMoves is marked non-null but is null");
        }
        if (originLocation == null) {
            throw new NullPointerException("originLocation is marked non-null but is null");
        }
        if (targetType == null) {
            throw new NullPointerException("targetType is marked non-null but is null");
        }
        this.player = player;
        this.target = target;
        this.movementPositions = movementPositions;
        this.recentMoves = recentMoves;
        this.primaryMovementValue = primaryMovementValue;
        this.secondaryMovementValue = secondaryMovementValue;
        this.originLocation = originLocation;
        this.targetType = targetType;
        this.statusFlag = statusFlag;
    }

    public String getTargetType() { return this.targetType; }
    public int getPrimaryMovementValue() { return this.primaryMovementValue; }
    public int primaryMovementValue() { return this.primaryMovementValue; }
    public void setMovementPositions(List<?> positions) { this.movementPositions = positions; }
    public List<?> getMovementPositions() { return this.movementPositions; }
    public Player getPlayer() { return this.player; }
    public Player player() { return this.player; }
    public void setTarget(LivingEntity target) { this.target = target; }
    public LivingEntity getTarget() { return this.target; }
    public List<?> getRecentMoves() { return this.recentMoves; }
    public List<?> recentMoves() { return this.recentMoves; }
    public void setStatusFlag(boolean statusFlag) { this.statusFlag = statusFlag; }
    public boolean isStatusFlagSet() { return this.statusFlag; }
    public LivingEntity target() { return this.target; }
    public List<?> movementPositions() { return this.movementPositions; }
    public int getSecondaryMovementValue() { return this.secondaryMovementValue; }
    public Location getOriginLocation() { return this.originLocation; }
    public void setPlayer(Player player) { this.player = player; }
    public void setPrimaryMovementValue(int value) { this.primaryMovementValue = value; }
    public void setRecentMoves(List<?> moves) { this.recentMoves = moves; }
    public boolean statusFlag() { return this.statusFlag; }
    public int primaryValue() { return this.primaryMovementValue; }
    public Location originLocation() { return this.originLocation; }
    public void setOriginLocation(Location location) { this.originLocation = location; }
}
