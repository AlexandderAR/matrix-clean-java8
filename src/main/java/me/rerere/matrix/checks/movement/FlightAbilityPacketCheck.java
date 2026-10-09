package me.rerere.matrix.checks.movement;

import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;

/** Rejects client ability packets that claim flight while flight is disallowed. */
public class FlightAbilityPacketCheck extends CheckRuntimeState {
    private static int violationWeight;
    private static boolean enabled;
    private static final int defaultWeight = 20;

    public FlightAbilityPacketCheck(PlayerCheckContext player) {
        super(player);
        this.setCheckType(CheckCategory.MOVE);
    }

    public void reloadConfiguration() {
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection config = this.configuration();
        enabled = config.getBoolean("ability.enable", true);
        config = this.configuration();
        violationWeight = config.getInt("ability.vl_weight", (int) defaultWeight);
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (enabled && packetType == me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client.PLAYER_ABILITIES) {
            org.bukkit.entity.Player player = this.player();
            if (!player.getAllowFlight()) {
                this.flag("move.ability", "send invalid ability packets (simple flight hacking)", violationWeight);
            }
        }
        return false;
    }
}
