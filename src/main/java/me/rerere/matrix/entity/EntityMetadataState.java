package me.rerere.matrix.entity;

import java.util.List;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.rerere.matrix.input.PacketKnownInput;
import me.rerere.matrix.player.PacketPlayerStateSnapshot;
import org.bukkit.entity.Player;

/** Tracks client and server entity flags, pose metadata, and the last two input snapshots. */
public final class EntityMetadataState {
    private static final int FLAGS_METADATA_INDEX = 0;
    private static final int POSE_METADATA_INDEX = 6;
    private static final int SPRINTING_FLAG_MASK = 8;
    private static final int SWIMMING_FLAG_MASK = 16;
    private static final int ELYTRA_FLAG_MASK = 128;
    private PacketKnownInput currentInput;
    private boolean wasSneaking;
    private boolean serverSprinting;
    private boolean wasSprinting;
    private PacketKnownInput previousInput;
    private boolean sneaking;
    private boolean swimming;
    private boolean previousSwimming;
    private EntityPose currentPose;
    private EntityPose previousPose;
    private boolean previousGliding;
    private boolean elytraActionSeen;
    private boolean stateFlag;
    private boolean hasInput;
    private boolean gliding;
    private int tickCounter;

    public EntityMetadataState() {
        currentPose = EntityPose.STANDING;
        previousPose = EntityPose.STANDING;
        currentInput = emptyInput();
        previousInput = emptyInput();
    }

    private static PacketKnownInput emptyInput() {
        return PacketKnownInput.empty();
    }

    public PacketKnownInput getCurrentInput() { return currentInput; }

    public boolean wasSneaking() { return wasSneaking; }

    public void setGliding(boolean value) {
        gliding = value;
    }

    public EntityPose getPreviousPose() { return previousPose; }

    public void resetFromSnapshot(boolean sneakValue, boolean sprintValue, boolean swimValue, boolean glideValue) {
        sneaking = sneakValue;
        wasSneaking = sneakValue;
        sprinting = sprintValue;
        wasSprinting = sprintValue;
        serverSprinting = sprintValue;
        swimming = swimValue;
        previousSwimming = swimValue;
        gliding = glideValue;
        previousGliding = glideValue;
        currentPose = swimValue ? EntityPose.SWIMMING : EntityPose.STANDING;
        previousPose = currentPose;
        currentInput = emptyInput();
        previousInput = emptyInput();
        hasInput = false;
        elytraActionSeen = false;
        stateFlag = false;
        tickCounter = 0;
    }

    public EntityPose getCurrentPose() { return currentPose; }

    public boolean isSwimming() {
        return swimming;
    }

    /** Reads the pose and flags fields from a server metadata packet and reports a state change. */
    public boolean applyMetadata(WrapperPlayServerEntityMetadata packet) {
        PacketPlayerStateSnapshot before = snapshot();
        List<EntityData<?>> metadata = packet.getEntityMetadata();
        EntityData<?> flagsEntry = firstWithIndex(metadata, FLAGS_METADATA_INDEX);
        Object rawFlags = flagsEntry == null ? null : flagsEntry.getValue();
        if (rawFlags instanceof Byte) {
            int flags = ((Byte) rawFlags).byteValue();
            serverSprinting = (flags & SPRINTING_FLAG_MASK) != 0;
            setSwimming((flags & SWIMMING_FLAG_MASK) != 0);
            setGliding((flags & ELYTRA_FLAG_MASK) != 0);
        }

        EntityData<?> poseEntry = firstWithIndex(packet.getEntityMetadata(), POSE_METADATA_INDEX);
        Object rawPose = poseEntry == null ? null : poseEntry.getValue();
        if (rawPose instanceof EntityPose) {
            EntityPose pose = (EntityPose) rawPose;
            currentPose = pose;
            setSwimming(pose == EntityPose.SWIMMING);
        }
        PacketPlayerStateSnapshot after = snapshot();
        return before.isSneaking() != after.isSneaking()
                || before.isSprinting() != after.isSprinting()
                || before.hasMetadataSprinting() != after.hasMetadataSprinting()
                || before.isSwimming() != after.isSwimming()
                || before.isGliding() != after.isGliding()
                || before.getPose() != after.getPose();
    }

    private static EntityData<?> firstWithIndex(List<EntityData<?>> metadata, int metadataIndex) {
        for (EntityData<?> entry : metadata) {
            if (entry.getIndex() == metadataIndex) {
                return entry;
            }
        }
        return null;
    }

    public boolean wasGliding() { return previousGliding; }
    public boolean isGliding() { return gliding; }
    public boolean wasSprinting() { return wasSprinting; }
    public int getTickCounter() { return tickCounter; }

    /** Applies one client action using the selected ordinary switch cases. */
    public void applyEntityAction(WrapperPlayClientEntityAction$Action action) {
        if (action == null) return;
        switch (action) {
            case START_SNEAKING:
                setSneaking(true);
                return;
            case STOP_SNEAKING:
                setSneaking(false);
                return;
            case START_SPRINTING:
                setSprinting(true);
                return;
            case STOP_SPRINTING:
                setSprinting(false);
                return;
            case START_FLYING_WITH_ELYTRA:
                elytraActionSeen = true;
                return;
            default:
                return;
        }
    }

    public void setSneaking(boolean value) { sneaking = value; }
    public boolean wasInputGliding() { return previousGliding; }
    public boolean getHasInput() { return hasInput; }
    public void setSprinting(boolean value) { sprinting = value; }

    /** Updates the cached input record and the sprint/sneak values carried by the packet. */
    public void updateInput(WrapperPlayClientPlayerInput packet) {
        currentInput = new PacketKnownInput(packet.isForward(), packet.isBackward(),
                packet.isLeft(), packet.isRight(), packet.isJump(), packet.isShift(), packet.isSprint());
        hasInput = true;
        setSneaking(packet.isShift());
        setSprinting(packet.isSprint());
    }

    public boolean isSprinting() { return sprinting; }
    public PacketPlayerStateSnapshot snapshot() {
        return new PacketPlayerStateSnapshot(sneaking, wasSneaking, sprinting, wasSprinting,
                serverSprinting, swimming, previousSwimming, gliding, previousGliding,
                currentPose, previousPose, currentInput, previousInput,
                hasInput, elytraActionSeen, stateFlag, tickCounter);
    }

    public boolean wasSwimming() { return previousSwimming; }
    public boolean wasElytraAction() { return elytraActionSeen; }
    public boolean isSneaking() { return sneaking; }
    public boolean hadInput() { return hasInput; }
    public PacketKnownInput getPreviousInput() { return previousInput; }
    public boolean isStateFlagSet() { return stateFlag; }
    public boolean isMetadataSprinting() { return serverSprinting; }

    /** Copies the current values to the previous-tick slots and advances the local tick counter. */
    public void advanceTick(boolean nextStateFlag) {
        wasSneaking = sneaking;
        wasSprinting = sprinting;
        previousSwimming = swimming;
        previousGliding = gliding;
        previousPose = currentPose;
        previousInput = currentInput;
        stateFlag = nextStateFlag;
        elytraActionSeen = false;
        tickCounter++;
    }

    /** Resynchronizes action flags from a Bukkit player; failed gliding queries default to false. */
    public void synchronizeFromPlayer(Player player) {
        boolean playerSneaking = player.isSneaking();
        boolean playerSprinting = player.isSprinting();
        boolean playerGliding;
        try {
            playerGliding = player.isGliding();
        } catch (Throwable ignored) {
            playerGliding = false;
        }
        resetFromSnapshot(playerSneaking, playerSprinting, false, playerGliding);
    }

    private boolean sprinting;

    public void setSwimming(boolean value) {
        swimming = value;
        if (value) {
            currentPose = EntityPose.SWIMMING;
        } else if (currentPose == EntityPose.SWIMMING) {
            currentPose = EntityPose.STANDING;
        }
    }
}
