package me.rerere.matrix.player;

import java.util.Objects;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import me.rerere.matrix.input.PacketKnownInput;

/** Player movement and input state captured for packet checks. */
public final class PacketPlayerStateSnapshot {
    private final boolean sneaking;
    private final boolean lastSneaking;
    private final boolean sprinting;
    private final boolean lastSprinting;
    private final boolean metadataSprinting;
    private final boolean swimming;
    private final boolean lastSwimming;
    private final boolean gliding;
    private final boolean lastGliding;
    private final EntityPose pose;
    private final EntityPose lastPose;
    private final PacketKnownInput knownInput;
    private final PacketKnownInput lastKnownInput;
    private final boolean receivedPlayerInput;
    private final boolean requestedGlide;
    private final boolean lastMovementHadPosition;
    private final int movementTicks;

    public PacketPlayerStateSnapshot(boolean sneaking, boolean lastSneaking,
            boolean sprinting, boolean lastSprinting, boolean metadataSprinting,
            boolean swimming, boolean lastSwimming, boolean gliding, boolean lastGliding,
            EntityPose pose, EntityPose lastPose,
            PacketKnownInput knownInput, PacketKnownInput lastKnownInput,
            boolean receivedPlayerInput, boolean requestedGlide,
            boolean lastMovementHadPosition, int movementTicks) {
        this.sneaking = sneaking;
        this.lastSneaking = lastSneaking;
        this.sprinting = sprinting;
        this.lastSprinting = lastSprinting;
        this.metadataSprinting = metadataSprinting;
        this.swimming = swimming;
        this.lastSwimming = lastSwimming;
        this.gliding = gliding;
        this.lastGliding = lastGliding;
        this.pose = pose;
        this.lastPose = lastPose;
        this.knownInput = knownInput;
        this.lastKnownInput = lastKnownInput;
        this.receivedPlayerInput = receivedPlayerInput;
        this.requestedGlide = requestedGlide;
        this.lastMovementHadPosition = lastMovementHadPosition;
        this.movementTicks = movementTicks;
    }

    public boolean isSneaking() { return sneaking; }
    public boolean wasSneaking() { return lastSneaking; }
    public boolean isSprinting() { return sprinting; }
    public boolean wasSprinting() { return lastSprinting; }
    public boolean hasMetadataSprinting() { return metadataSprinting; }
    public boolean isSwimming() { return swimming; }
    public boolean wasSwimming() { return lastSwimming; }
    public boolean isGliding() { return gliding; }
    public boolean wasGliding() { return lastGliding; }
    public EntityPose getPose() { return pose; }
    public EntityPose getLastPose() { return lastPose; }
    public PacketKnownInput getKnownInput() { return knownInput; }
    public PacketKnownInput getLastKnownInput() { return lastKnownInput; }
    public boolean hasReceivedPlayerInput() { return receivedPlayerInput; }
    public boolean hasRequestedGlide() { return requestedGlide; }
    public boolean lastMovementHadPosition() { return lastMovementHadPosition; }
    public int getMovementTicks() { return movementTicks; }

    public PacketPlayerStateSnapshot copy(boolean sneaking, boolean lastSneaking,
            boolean sprinting, boolean lastSprinting, boolean metadataSprinting,
            boolean swimming, boolean lastSwimming, boolean gliding, boolean lastGliding,
            EntityPose pose, EntityPose lastPose,
            PacketKnownInput knownInput, PacketKnownInput lastKnownInput,
            boolean receivedPlayerInput, boolean requestedGlide,
            boolean lastMovementHadPosition, int movementTicks) {
        return new PacketPlayerStateSnapshot(sneaking, lastSneaking, sprinting,
                lastSprinting, metadataSprinting, swimming, lastSwimming, gliding,
                lastGliding, pose, lastPose, knownInput, lastKnownInput,
                receivedPlayerInput, requestedGlide, lastMovementHadPosition, movementTicks);
    }

    @Override public int hashCode() {
        int result = Boolean.hashCode(sneaking);
        result = 31 * result + Boolean.hashCode(lastSneaking);
        result = 31 * result + Boolean.hashCode(sprinting);
        result = 31 * result + Boolean.hashCode(lastSprinting);
        result = 31 * result + Boolean.hashCode(metadataSprinting);
        result = 31 * result + Boolean.hashCode(swimming);
        result = 31 * result + Boolean.hashCode(lastSwimming);
        result = 31 * result + Boolean.hashCode(gliding);
        result = 31 * result + Boolean.hashCode(lastGliding);
        result = 31 * result + pose.hashCode();
        result = 31 * result + lastPose.hashCode();
        result = 31 * result + knownInput.hashCode();
        result = 31 * result + lastKnownInput.hashCode();
        result = 31 * result + Boolean.hashCode(receivedPlayerInput);
        result = 31 * result + Boolean.hashCode(requestedGlide);
        result = 31 * result + Boolean.hashCode(lastMovementHadPosition);
        return 31 * result + movementTicks;
    }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof PacketPlayerStateSnapshot)) return false;
        PacketPlayerStateSnapshot other = (PacketPlayerStateSnapshot) value;
        return sneaking == other.sneaking && lastSneaking == other.lastSneaking
                && sprinting == other.sprinting && lastSprinting == other.lastSprinting
                && metadataSprinting == other.metadataSprinting && swimming == other.swimming
                && lastSwimming == other.lastSwimming && gliding == other.gliding
                && lastGliding == other.lastGliding && pose == other.pose && lastPose == other.lastPose
                && Objects.equals(knownInput, other.knownInput)
                && Objects.equals(lastKnownInput, other.lastKnownInput)
                && receivedPlayerInput == other.receivedPlayerInput
                && requestedGlide == other.requestedGlide
                && lastMovementHadPosition == other.lastMovementHadPosition
                && movementTicks == other.movementTicks;
    }

    @Override public String toString() {
        return formatSnapshot(sneaking, lastSneaking, sprinting, lastSprinting,
                metadataSprinting, swimming, lastSwimming, gliding, lastGliding,
                pose, lastPose, knownInput, lastKnownInput, receivedPlayerInput,
                requestedGlide, lastMovementHadPosition, movementTicks);
    }

    private static String formatSnapshot(boolean sneaking, boolean lastSneaking,
            boolean sprinting, boolean lastSprinting, boolean metadataSprinting,
            boolean swimming, boolean lastSwimming, boolean gliding, boolean lastGliding,
            EntityPose pose, EntityPose lastPose,
            PacketKnownInput knownInput, PacketKnownInput lastKnownInput,
            boolean receivedPlayerInput, boolean requestedGlide,
            boolean lastMovementHadPosition, int movementTicks) {
        return new StringBuilder("PacketPlayerStateSnapshot(sneaking=").append(sneaking)
                .append(", lastSneaking=").append(lastSneaking)
                .append(", sprinting=").append(sprinting)
                .append(", lastSprinting=").append(lastSprinting)
                .append(", metadataSprinting=").append(metadataSprinting)
                .append(", swimming=").append(swimming)
                .append(", lastSwimming=").append(lastSwimming)
                .append(", gliding=").append(gliding)
                .append(", lastGliding=").append(lastGliding)
                .append(", pose=").append(pose)
                .append(", lastPose=").append(lastPose)
                .append(", knownInput=").append(knownInput)
                .append(", lastKnownInput=").append(lastKnownInput)
                .append(", receivedPlayerInput=").append(receivedPlayerInput)
                .append(", requestedGlide=").append(requestedGlide)
                .append(", lastMovementHadPosition=").append(lastMovementHadPosition)
                .append(", movementTicks=").append(movementTicks)
                .append(')').toString();
    }
}
