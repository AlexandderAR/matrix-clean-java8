package me.rerere.matrix.network.packet;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketListenerAbstract;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Cancels non-keep-alive server packets for the tracked player. */
public final class KeepAlivePacketCancellationListener extends PacketListenerAbstract {
    private final Player targetPlayer;
    private static String[] stringTable;

    static {
        if (getStringTable() != null) {
            setStringTable(new String[3]);
        }
    }

    public KeepAlivePacketCancellationListener(Player targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public static void setStringTable(String[] values) {
        stringTable = values;
    }

    public static String[] getStringTable() {
        return stringTable;
    }

    public void onPacketSend(PacketSendEvent event) {
        boolean isTargetPlayer = Intrinsics.areEqual(event.getPlayer(), targetPlayer);
        if (isTargetPlayer) {
            boolean isKeepAlivePacket = event.getPacketType() == PacketType$Play$Server.KEEP_ALIVE;
            if (!isKeepAlivePacket) {
                event.setCancelled(true);
            }
        }
    }
}
