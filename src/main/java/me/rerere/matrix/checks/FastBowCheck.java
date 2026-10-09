package me.rerere.matrix.checks;

import org.bukkit.event.entity.EntityShootBowEvent;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Fast-bow rate check reconstructed from canonical ASM. */
public class FastBowCheck extends CheckRuntimeState {
    private int recentShotCount;
    private long lastPacketTime;
    private long previousPacketTime;
    private static int bowCheckEnabled;
    private static boolean cancelAction;

    public FastBowCheck(PlayerCheckContext playerData) {
        super(playerData);
        recentShotCount = 0;
        this.setCheckType(CheckCategory.DELAY);
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (PacketEventSupport.isPlayerFlying(packetType)) {
            long now = MillisecondSampler.currentTimeMillis();
            long elapsed = now - previousPacketTime;
            if (elapsed <= 40L) recentShotCount++;
            else recentShotCount = 0;
            previousPacketTime = now;
        }
        if (PacketEventSupport.isBlockPlacementPacket(packetType)) {
            lastPacketTime = MillisecondSampler.currentTimeMillis();
        }
        return false;
    }

    private static String describeViolation(int count, float force) {
        return new StringBuilder("perform actions faster than expected // fast_bow (")
            .append(count).append(", force: ").append(force).append(")").toString();
    }

    public void reloadConfiguration() {
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection config =
            this.configuration();
        cancelAction = config.getBoolean("cancel_action", true);
        config = this.configuration();
        bowCheckEnabled = config.getInt("check_using_bow", 1);
    }

    public void onBowShoot(EntityShootBowEvent event) {
        if (recentShotCount >= 7 && (double) event.getForce() >= 0x1.189374bc6a7fp-2 && bowCheckEnabled >= 0) {
            long elapsed = MillisecondSampler.currentTimeMillis() - lastPacketTime;
            if ((float) elapsed <= event.getForce() * 1075.0F) {
                this.flag("delay.splash", describeViolation(recentShotCount,
                    NumberUtils.round3(event.getForce())), bowCheckEnabled);
                if (cancelAction && (this.violationCount() >= bowCheckEnabled * 2 || recentShotCount >= 19))
                    event.setCancelled(true);
            }
            recentShotCount = 0;
        }
    }
}
