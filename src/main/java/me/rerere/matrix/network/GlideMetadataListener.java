package me.rerere.matrix.network;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.inventory.ItemStack;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerAbstract;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.compat.Mainthread;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;

public final class GlideMetadataListener extends PacketListenerAbstract implements org.bukkit.event.Listener {
    private static final long GLIDE_FLAG_MASK = 4118709195451137920L;

    public GlideMetadataListener() { super(); }

    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType != PacketType$Play$Client.ENTITY_ACTION) return;
        Object rawPlayer = event.getPlayer();
        if (!(rawPlayer instanceof Player)) return;
        WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
        Object playerValue = event.getPlayer();
        Intrinsics.checkNotNull(
                playerValue, "null cannot be cast to non-null type org.bukkit.entity.Player");
        Player player = (Player) playerValue;
        if (packet.getAction() == WrapperPlayClientEntityAction$Action.START_FLYING_WITH_ELYTRA) {
            Mainthread.executeOnEventLoop(player, () -> refreshFromElytra(player));
        }
    }

    private static void refreshFromElytra(Player player) {
        PlayerCheckContext state = sessionRegistry().find(player.getUniqueId());
        if (state==null) return;
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            state.setGliding(true);
        }
    }

    @EventHandler public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Object playerValue = event.getEntity();
        Intrinsics.checkNotNull(
                playerValue, "null cannot be cast to non-null type org.bukkit.entity.Player");
        Player player = (Player) playerValue;
        PlayerCheckContext state = sessionRegistry().find(player.getUniqueId());
        if (state==null) return;
        CheckRegistry checks = state.checks();
        for (CheckRuntimeState check : checks.checks()) {
            if (check.checkType().isEnabled() && !state.shouldBypass(check.checkType())
                    && check.checkType().isEnabledIn(player.getWorld())) {
                check.onMovementCancelled(event.isGliding());
            }
        }
    }

    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType$Play$Server.ENTITY_METADATA) return;
        Object rawPlayer = event.getPlayer();
        if (!(rawPlayer instanceof Player)) return;
        Object playerValue = event.getPlayer();
        Intrinsics.checkNotNull(
                playerValue, "null cannot be cast to non-null type org.bukkit.entity.Player");
        Player player = (Player) playerValue;
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        if (packet.getEntityId() != player.getEntityId()) return;
        EntityData flagMetadata = null;
        for (Object rawData : packet.getEntityMetadata()) {
            EntityData metadata = (EntityData) rawData;
            if (metadata.getIndex() == 0) {
                flagMetadata = metadata;
                break;
            }
        }
        if (flagMetadata == null) return;
        Object flagValue = flagMetadata.getValue();
        if (!(flagValue instanceof Byte)) return;
        byte entityFlags = ((Byte) flagValue).byteValue();
        PlayerCheckContext state = sessionRegistry().find(player.getUniqueId());
        if (state==null) return;
        boolean gliding = (((entityFlags & (int) GLIDE_FLAG_MASK) & 0xFF) != 0);
        state.enqueueTransaction(() -> scheduleGlideState(player, state, gliding));
    }

    private static Unit scheduleGlideState(Player player, PlayerCheckContext state, boolean gliding) {
        Mainthread.executeOnEventLoop(player, () -> applyGlideState(state, gliding));
        return Unit.INSTANCE;
    }
    private static void applyGlideState(PlayerCheckContext state, boolean gliding) { state.setGliding(gliding); }

    private static PlayerSessionRegistry sessionRegistry() {
        return Matrix.Companion.matrixInstance().getPlayerDataManager();
    }

}
