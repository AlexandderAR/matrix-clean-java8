package me.rerere.matrix.player;

import java.util.Map;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.network.packet.TransactionPingQueue;

/** Invokes the stored session action for each Matrix session entry. */
public final class SessionRefreshTask implements Runnable {
    public final Matrix matrix;

    public SessionRefreshTask(Matrix matrix) {
        this.matrix = matrix;
    }

    @Override
    public final void run() {
        Map<?, PlayerCheckContext> sessions = ((PlayerSessionRegistry) matrix.getPlayerDataManager())
                .sessions();
        for (Map.Entry<?, PlayerCheckContext> entry : sessions.entrySet()) {
            java.util.UUID playerId = (java.util.UUID) entry.getKey();
            PlayerCheckContext session = entry.getValue();
            TransactionPingQueue transactionQueue = session.transactionQueue();
            transactionQueue.flushPendingWork();
        }
    }
}
