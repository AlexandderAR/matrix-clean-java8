package me.rerere.matrix.network;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.entity.TrackedEntity;
import me.rerere.matrix.compat.MatrixFeatureBridge;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.version.ProtocolVersionBucket;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerAbstract;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.GameMode;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.UserProfile;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo$PlayerData;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate$PlayerInfo;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.ranges.RangesKt;

/** Packet listener for player entity metadata and player-info updates. */
public final class PlayerMetadataPacketListener extends PacketListenerAbstract {
    private static boolean listenerEnabled;
    public static final MatrixFeatureBridge CALLBACK_ADAPTER = new MatrixFeatureBridge((DefaultConstructorMarker) null);
    private final boolean rewriteEntityMetadata;
    private final boolean rewritePlayerInfo;
    private static boolean enabled;

    public PlayerMetadataPacketListener() {
        super();
        this.rewriteEntityMetadata = MatrixConfiguration.antiHealthIndicator;
        this.rewritePlayerInfo = MatrixConfiguration.pingObfuscation;
    }

    public static void setListenerEnabled(boolean value) {
        listenerEnabled = value;
    }

    public static boolean isListenerEnabled() {
        return listenerEnabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isDisabled() {
        return !isEnabled();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!listenerEnabled) {
            return;
        }

        if (event.getPacketType() == PacketType$Play$Server.ENTITY_METADATA) {
            if (!rewriteEntityMetadata) {
                return;
            }
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            int entityId = wrapper.getEntityId();
            PlayerCheckContext playerState = findPlayerState(event);
            if (playerState != null && playerState.getPlayer() != null
                    && playerState.getPlayer().getEntityId() == entityId) {
                return;
            }
            if (playerState == null || playerState.motionTracker() == null) {
                return;
            }
            TrackedEntity tracked = playerState.motionTracker().get(entityId);
            if (tracked == null
                    || !Objects.equals(tracked.entityType(), EntityTypes.PLAYER)) {
                return;
            }

            int metadataIndex = ProtocolVersionBucket.INSTANCE.cachedBucket();
            boolean changed = false;
            for (EntityData<?> data : wrapper.getEntityMetadata()) {
                if (data.getIndex() != metadataIndex || !Objects.equals(data.getType(), EntityDataTypes.FLOAT)) {
                    continue;
                }
                Object rawHealth = data.getValue();
                Float numericValue = (Float) Objects.requireNonNull(
                        rawHealth, "null cannot be cast to non-null type kotlin.Float");
                if (numericValue.floatValue() > 0.0F) {
                    @SuppressWarnings("unchecked")
                    EntityData<Float> floatData = (EntityData<Float>) data;
                    floatData.setValue(RangesKt.coerceAtLeast((float) (Math.random() * 20.0D), 0.1F));
                    changed = true;
                }
            }
            if (changed) {
                event.markForReEncode(true);
            }
            return;
        }

        if (event.getPacketType() == PacketType$Play$Server.PLAYER_INFO) {
            if (!rewritePlayerInfo) {
                return;
            }
            PlayerCheckContext playerState = findPlayerState(event);
            WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(event);
            List<WrapperPlayServerPlayerInfo$PlayerData> entries =
                    wrapper.getPlayerDataList();
            for (WrapperPlayServerPlayerInfo$PlayerData entry : entries) {
                UserProfile profile = entry.getUser();
                if (profile != null) {
                    profile.getUUID();
                }
            }
            for (WrapperPlayServerPlayerInfo$PlayerData entry : entries) {
                entry.setPing(0);
                if (entry.getGameMode() != GameMode.SPECTATOR) {
                    UUID ownUuid = playerState == null ? null : playerState.playerId();
                    if (!Objects.equals(ownUuid, Unit.INSTANCE)) {
                        entry.setGameMode(GameMode.SURVIVAL);
                    }
                }
            }
            if (!entries.isEmpty()) {
                event.markForReEncode(true);
            }
            return;
        }

        if (event.getPacketType() == PacketType$Play$Server.PLAYER_INFO_UPDATE) {
            if (!rewritePlayerInfo) {
                return;
            }
            PlayerCheckContext playerState = findPlayerState(event);
            WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
            for (WrapperPlayServerPlayerInfoUpdate$PlayerInfo entry : wrapper.getEntries()) {
                entry.getGameProfile().getUUID();
            }
            for (WrapperPlayServerPlayerInfoUpdate$PlayerInfo entry : wrapper.getEntries()) {
                entry.setLatency(0);
                if (entry.getGameMode() != GameMode.SPECTATOR) {
                    UUID ownUuid = playerState == null ? null : playerState.playerId();
                    if (!Objects.equals(ownUuid, Unit.INSTANCE)) {
                        entry.setGameMode(GameMode.SURVIVAL);
                    }
                }
            }
            if (!wrapper.getEntries().isEmpty()) {
                event.markForReEncode(true);
            }
        }
    }

    private static PlayerCheckContext findPlayerState(PacketSendEvent event) {
        UUID playerId = event.getUser().getUUID();
        return Matrix.Companion.matrixInstance().getPlayerDataManager().find(playerId);
    }
}
