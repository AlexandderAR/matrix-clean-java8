package me.rerere.matrix.callbacks;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.compat.Cancelable;
import me.rerere.matrix.player.PlayerCheckContext;

/** Periodically runs active checks for a player's context. */
public final class CancelableAction implements Function0<Object> {
    private Cancelable task;
    @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull
    private final PlayerCheckContext context;

    public CancelableAction(
            @me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull PlayerCheckContext context) {
        this.context = context;
    }

    public void runChecks() {
        context.processActions();
        context.updateGameMode();
        CheckRegistry checkRegistry = context.checks();
        Player player = currentPlayer(context);
        World world = player.getWorld();
        List<CheckRuntimeState> checks = checkRegistry.checks();
        Iterator<CheckRuntimeState> iterator = checks.iterator();
        while (iterator.hasNext()) {
            CheckRuntimeState check = iterator.next();
            if (!check.checkType().isEnabled()) continue;
            if (context.shouldBypass(check.checkType())) continue;
            if (!check.checkType().isEnabledIn(world)) continue;
            try {
                check.onTick();
            } catch (Exception failure) {
                failure.printStackTrace();
                Logger logger = Matrix.Companion.matrixInstance().getLogger();
                String message = errorDescription(
                        check.checkType().name(), failure.getClass().getName());
                logger.warning(message);
            }
        }
    }

    public void schedule() {
        task = FoliaSupport.runAtFixedRateSyncForPlayer$default(
                FoliaSupport.INSTANCE,
                currentPlayer(context),
                this,
                null,
                1L,
                1L,
                4,
                null);
    }

    public void cancel() {
        Cancelable scheduledTask = task;
        if (scheduledTask == null) {
            Intrinsics.throwUninitializedPropertyAccessException("task");
        }
        scheduledTask.cancel();
    }

    private static String errorDescription(String checkName, String failureType) {
        return new StringBuilder("Error happened! check: ").append(checkName)
                .append(" type: ").append(failureType).toString();
    }

    private static Player currentPlayer(PlayerCheckContext context) {
        try {
            return context.getPlayer();
        } catch (Throwable failure) {
            return CancelableAction.<RuntimeException, Player>rethrow(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R rethrow(Throwable failure) throws T {
        throw (T) failure;
    }

    @Override
    public Object invoke() {
        runChecks();
        return Unit.INSTANCE;
    }
}
