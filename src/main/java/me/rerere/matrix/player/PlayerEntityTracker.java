package me.rerere.matrix.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.Equipment;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.ItemStack;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.UserProfile;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSwingAnimation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.sound.Sounds;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3i;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3d;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.GameMode;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.InteractionHand;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.component.builtin.item.ItemSwingAnimation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.component.builtin.item.ItemSwingAnimation$Type;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation$EntityAnimationType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo$Action;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo$PlayerData;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate$Action;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate$PlayerInfo;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes$Property;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.checks.combat.CombatItemCatalog;
import me.rerere.matrix.logging.DebugLogger;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.Matrix;

/** Per-player protocol state used while maintaining the server-side player entity. */
public final class PlayerEntityTracker {
    private final ArrayList<EntityData<?>> trackedMetadata;
    private final UUID profileId;
    private int randomizedLatencyOffset;
    private int healthResetTicksRemaining;
    private long lastDamageAnimationAt;
    private Location lastLocation;
    private final List<WrapperPlayServerUpdateAttributes$Property> entityAttributes;
    public boolean removed;
    private final Player player;
    private final int entityId;
    private float trackedHealth;
    private final Map<EquipmentSlot, ItemStack> metadataByIndex;
    public boolean visible;
    private final String playerName;
    private static String playerState;

    public PlayerEntityTracker(Player player, String playerName, UUID profileId, int entityId,
                               Location location) {
        this.trackedMetadata = new ArrayList<>();
        this.metadataByIndex = new java.util.HashMap<>();
        this.entityAttributes = new ArrayList<>();
        this.healthResetTicksRemaining = 0;
        lastDamageAnimationAt = 0L;
        this.player = player;
        this.playerName = playerName;
        this.profileId = profileId;
        this.entityId = entityId;
        this.lastLocation = location;
        this.trackedHealth = 20.0F;
        removed = false;
        this.randomizedLatencyOffset = 10 + new java.util.Random().nextInt(30);
    }

    public Player getPlayer() { return player; }
    public int getRandomizedLatencyOffset() { return randomizedLatencyOffset; }
    public int getUpdateSequence() { return healthResetTicksRemaining; }
    public UserProfile createUserProfile() { return new UserProfile(profileId, playerName); }
    public UUID getProfileId() { return profileId; }
    public ArrayList<EntityData<?>> getTrackedMetadata() { return trackedMetadata; }
    public boolean isRemoved() { return removed; }
    public int getEntityId() { return entityId; }
    public boolean isVisible() { return visible; }
    public float getTrackedValue() { return trackedHealth; }
    public long getLastUpdateTime() { return lastDamageAnimationAt; }
    public List<WrapperPlayServerUpdateAttributes$Property> getEntityAttributes() { return entityAttributes; }
    public Map<EquipmentSlot, ItemStack> getMetadataByIndex() { return metadataByIndex; }
    public Location getLastLocation() { return lastLocation; }
    public String getPlayerName() { return playerName; }

    public static void setPlayerState(String value) { playerState = value; }
    public static String getPlayerState() { return playerState; }

    public void sendEntityMetadata() {
        WrapperPlayServerEntityMetadata packet =
                new WrapperPlayServerEntityMetadata(entityId, trackedMetadata);
        PacketEventSupport.writePacketIfPresent(player, packet);
    }

    public void sendEquipmentPacket(EquipmentSlot slot, ItemStack item) {
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(entityId,
                Arrays.asList(new Equipment(slot, item)));
        PacketEventSupport.writePacketIfPresent(player, packet);
    }

    private void sendAttributeUpdate() {
        WrapperPlayServerUpdateAttributes packet = new WrapperPlayServerUpdateAttributes(
                getEntityId(), entityAttributes);
        PacketEventSupport.writePacketIfPresent(player, packet);
    }

    public void moveTowardLocation(Location target, boolean onGround, double maximumStep) {
        Vector difference = target.toVector().subtract(lastLocation.toVector());
        double distance = GeometryMath.horizontalLength(difference);
        if (distance > maximumStep) {
            difference.normalize().multiply(maximumStep);
            Location intermediate = lastLocation.clone().add(difference);
            intermediate.setY(target.getY());
            intermediate.setYaw(target.getYaw());
            intermediate.setPitch(target.getPitch());
            sendLocation(intermediate, onGround);
        } else {
            sendLocation(target, onGround);
        }
    }

    public void pulseDamageAnimation() {
        if (System.currentTimeMillis() - lastDamageAnimationAt < 1000L) return;
        lastDamageAnimationAt = System.currentTimeMillis();
        updateTrackedValue(trackedHealth - 1.0F);
        WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(entityId,
                WrapperPlayServerEntityAnimation$EntityAnimationType.HURT);
        PacketEventSupport.writePacketIfPresent(player, animation);
    }

    public void randomizeEquipment() {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.ItemStack[] mainHand =
                CombatItemCatalog.HELD_ITEMS;
        sendEquipmentPacket(EquipmentSlot.MAIN_HAND,
                mainHand[new java.util.Random().nextInt(mainHand.length)]);
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.ItemStack[] boots =
                CombatItemCatalog.BOOTS;
        sendEquipmentPacket(EquipmentSlot.BOOTS, boots[new java.util.Random().nextInt(boots.length)]);
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.ItemStack[] leggings =
                CombatItemCatalog.LEGGINGS;
        sendEquipmentPacket(EquipmentSlot.LEGGINGS,
                leggings[new java.util.Random().nextInt(leggings.length)]);
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.ItemStack[] chestPlate =
                CombatItemCatalog.CHESTPLATES;
        sendEquipmentPacket(EquipmentSlot.CHEST_PLATE,
                chestPlate[new java.util.Random().nextInt(chestPlate.length)]);
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.item.ItemStack[] helmet =
                CombatItemCatalog.HELMETS;
        sendEquipmentPacket(EquipmentSlot.HELMET,
                helmet[new java.util.Random().nextInt(helmet.length)]);
    }

    private byte getMetadataFlags() {
        int flags = visible ? 8 : 0;
        if (removed) flags |= 32;
        return (byte) flags;
    }

    private void setEntityData(int metadataIndex, Object metadataValue, EntityDataType<?> metadataType) {
        for (Object metadataObject : trackedMetadata) {
            EntityData entry = (EntityData) metadataObject;
            if (entry.getIndex() == metadataIndex) {
                entry.setValue(metadataValue);
                entry.setType(metadataType);
                return;
            }
        }
        trackedMetadata.add(new EntityData(metadataIndex, metadataType, metadataValue));
    }

    private static String formatSpawnDebug(String playerName, int entityId, String profileId) {
        return new StringBuilder("spawn npc: ").append(playerName).append(" id: ").append(entityId)
                .append(" uuid: ").append(profileId).toString();
    }

    public void sendPotionEffect(org.bukkit.potion.PotionEffect effect) {
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.potion.PotionType packetType;
        if (effect.getType() == org.bukkit.potion.PotionEffectType.INVISIBILITY) {
            packetType = PotionTypes.INVISIBILITY;
        } else if (effect.getType() == org.bukkit.potion.PotionEffectType.SPEED) {
            packetType = PotionTypes.SPEED;
        } else {
            packetType = PotionTypes.INVISIBILITY;
        }
        WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(entityId, packetType,
                effect.getAmplifier(), effect.getDuration(), (byte) (effect.hasParticles() ? 1 : 0));
        PacketEventSupport.writePacketIfPresent(player, packet);
    }

    public void sendLocation(Location target, boolean onGround) {
        if (healthResetTicksRemaining > 0) {
            healthResetTicksRemaining--;
            if (healthResetTicksRemaining <= 0) {
                updateTrackedValue(20.0F);
            }
        }

        double movementLimit = Matrix.getServerVersion().isNewerThan(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_8_8)
                ? 8.0 : 4.0;
        double deltaMagnitude = Math.abs(target.getX() - lastLocation.getX())
                + Math.abs(target.getY() - lastLocation.getY())
                + Math.abs(target.getZ() - lastLocation.getZ());
        double distance = MovementMetrics.locationDistance(target, lastLocation);
        if (distance > 0.62D) pulseDamageAnimation();
        if (deltaMagnitude > movementLimit) {
            teleportEntity(target, onGround);
            lastLocation = target.clone();
            return;
        }

        boolean rotationChanged = lastLocation.getYaw() != target.getYaw()
                || lastLocation.getPitch() != target.getPitch();
        boolean positionChanged = lastLocation.getX() != target.getX()
                || lastLocation.getY() != target.getY()
                || lastLocation.getZ() != target.getZ();
        if (positionChanged && rotationChanged) {
            WrapperPlayServerEntityRelativeMoveAndRotation packet =
                    new WrapperPlayServerEntityRelativeMoveAndRotation(entityId,
                            target.getX() - lastLocation.getX(),
                            target.getY() - lastLocation.getY(),
                            target.getZ() - lastLocation.getZ(),
                            target.getYaw(), target.getPitch(), onGround);
            PacketEventSupport.writePacketIfPresent(player, packet);
        } else if (positionChanged) {
            WrapperPlayServerEntityRelativeMove packet = new WrapperPlayServerEntityRelativeMove(entityId,
                    target.getX() - lastLocation.getX(),
                    target.getY() - lastLocation.getY(),
                    target.getZ() - lastLocation.getZ(), onGround);
            PacketEventSupport.writePacketIfPresent(player, packet);
        } else if (rotationChanged) {
            WrapperPlayServerEntityRotation packet = new WrapperPlayServerEntityRotation(
                    entityId, target.getYaw(), target.getPitch(), onGround);
            PacketEventSupport.writePacketIfPresent(player, packet);
        }
        if (rotationChanged) {
            WrapperPlayServerEntityHeadLook headLook =
                    new WrapperPlayServerEntityHeadLook(entityId, target.getYaw());
            PacketEventSupport.writePacketIfPresent(player, headLook);
        }
        lastLocation = target.clone();
    }

    private void sendSpawnMetadata() {
        byte flags = getMetadataFlags();
        setEntityData(0, Byte.valueOf(flags),
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE);

        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion version =
                Matrix.getServerVersion();
        if (version.isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17)) {
            setEntityData(9, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        } else if (version.isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_14)) {
            setEntityData(8, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        } else if (version.isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_12)) {
            setEntityData(7, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        } else {
            setEntityData(6, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        }
        sendEntityMetadata();
    }

    public void spawnPlayerEntity() {
        Object playerInfoPacket;
        if (Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_19_4)) {
            java.util.EnumSet<WrapperPlayServerPlayerInfoUpdate$Action> actions =
                    java.util.EnumSet.of(WrapperPlayServerPlayerInfoUpdate$Action.ADD_PLAYER,
                            WrapperPlayServerPlayerInfoUpdate$Action.UPDATE_GAME_MODE,
                            WrapperPlayServerPlayerInfoUpdate$Action.UPDATE_LISTED,
                            WrapperPlayServerPlayerInfoUpdate$Action.UPDATE_LATENCY);
            WrapperPlayServerPlayerInfoUpdate$PlayerInfo info =
                    new WrapperPlayServerPlayerInfoUpdate$PlayerInfo(
                            createUserProfile(), true, randomizedLatencyOffset,
                            GameMode.SURVIVAL, null, null);
            playerInfoPacket = new WrapperPlayServerPlayerInfoUpdate(actions,
                    new WrapperPlayServerPlayerInfoUpdate$PlayerInfo[] { info });
        } else {
            me.rerere.matrix.thirdparty.net.kyori.adventure.text.Component displayName =
                    me.rerere.matrix.thirdparty.net.kyori.adventure.text.Component.text(playerName);
            WrapperPlayServerPlayerInfo$PlayerData data = new WrapperPlayServerPlayerInfo$PlayerData(
                    displayName, createUserProfile(), GameMode.SURVIVAL, randomizedLatencyOffset);
            playerInfoPacket = new WrapperPlayServerPlayerInfo(
                    WrapperPlayServerPlayerInfo$Action.ADD_PLAYER,
                    new WrapperPlayServerPlayerInfo$PlayerData[] { data });
        }
        PacketEventSupport.writePacketIfPresent(player,
                (me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper) playerInfoPacket);

        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper spawnPacket;
        if (Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_20)) {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.Location packetLocation =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.Location(
                            lastLocation.getX(), lastLocation.getY(), lastLocation.getZ(),
                            lastLocation.getYaw(), lastLocation.getPitch());
            spawnPacket = new WrapperPlayServerSpawnEntity(entityId, profileId, EntityTypes.PLAYER,
                    packetLocation, lastLocation.getYaw(), 0, new Vector3d());
        } else {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.Location packetLocation =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.Location(
                            lastLocation.getX(), lastLocation.getY(), lastLocation.getZ(),
                            lastLocation.getYaw(), lastLocation.getPitch());
            spawnPacket = new WrapperPlayServerSpawnPlayer(entityId, profileId, packetLocation, trackedMetadata);
        }
        PacketEventSupport.writePacketIfPresent(player, spawnPacket);
        sendAttributeUpdate();
        playSmallFallSound();
        sendSpawnMetadata();
        DebugLogger.printDebug(
                formatSpawnDebug(playerName, entityId, String.valueOf(profileId)));
    }

    public void updateTrackedValue(float requestedValue) {
        if (requestedValue <= 0.0F) requestedValue = 1.0F;
        if (trackedHealth > requestedValue) healthResetTicksRemaining = 40;
        trackedHealth = requestedValue;

        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion version =
                Matrix.getServerVersion();
        if (version.isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17)) {
            setEntityData(9, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        } else if (version.isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_14)) {
            setEntityData(8, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        } else if (version.isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_12)) {
            setEntityData(7, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        } else {
            setEntityData(6, Float.valueOf(trackedHealth),
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
        }
        sendEntityMetadata();
    }

    public void teleportEntity(Location location, boolean onGround) {
        if (Matrix.getServerVersion().isOlderThan(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_21_2)) {
            WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(entityId,
                    new Vector3d(location.getX(), location.getY(), location.getZ()),
                    location.getYaw(), location.getPitch(), onGround);
            PacketEventSupport.writePacketIfPresent(player, teleport);
        } else {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.EntityPositionData position =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.EntityPositionData(
                            new Vector3d(location.getX(), location.getY(), location.getZ()),
                            new Vector3d(0.0, 0.0, 0.0), location.getYaw(), location.getPitch());
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync packet =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync(
                            entityId, position, onGround);
            PacketEventSupport.writePacketIfPresent(player, packet);
        }
    }

    public void removeEntity() {
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entityId);
        PacketEventSupport.writePacketIfPresent(player, destroy);
        if (Matrix.getServerVersion().isOlderThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_19_2)) {
            WrapperPlayServerPlayerInfo oldInfo = new WrapperPlayServerPlayerInfo(
                    WrapperPlayServerPlayerInfo$Action.REMOVE_PLAYER,
                    new WrapperPlayServerPlayerInfo$PlayerData[] {
                            new WrapperPlayServerPlayerInfo$PlayerData(null,
                                    createUserProfile(), null, 0)
                    });
            PacketEventSupport.writePacketIfPresent(player, oldInfo);
        } else {
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove removal =
                    new me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove(
                            new UUID[] { profileId });
            PacketEventSupport.writePacketIfPresent(player, removal);
        }
    }

    private void playSmallFallSound() {
        Location soundLocation = lastLocation;
        Vector3i position = new Vector3i(
                GeometryMath.floorToInt(soundLocation.getX() * 8.0),
                GeometryMath.floorToInt(soundLocation.getY() * 8.0),
                GeometryMath.floorToInt(soundLocation.getZ() * 8.0));
        WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(
                Sounds.ENTITY_PLAYER_SMALL_FALL, SoundCategory.PLAYER, position, 0.0F, 1.0F);
        PacketEventSupport.writePacketIfPresent(player, packet);
    }

    public void sendSwingAnimation() {
        if (Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_26_3)) {
            WrapperPlayServerSwingAnimation packet = new WrapperPlayServerSwingAnimation(entityId,
                    InteractionHand.MAIN_HAND, new ItemSwingAnimation(ItemSwingAnimation$Type.WHACK, 6));
            PacketEventSupport.writePacketIfPresent(player, packet);
            return;
        }
        WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(entityId,
                WrapperPlayServerEntityAnimation$EntityAnimationType.SWING_MAIN_ARM);
        PacketEventSupport.writePacketIfPresent(player, packet);
    }

}
