package me.rerere.matrix.commands;

import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.AbstractCommandDescriptor;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.util.BuildMetadata;
import me.rerere.matrix.cloud.CloudServiceClient;
import me.rerere.matrix.config.BooleanOptionRouter;
import me.rerere.matrix.thirdparty.kotlin.Result$Failure;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

public final class UpdateCommand extends AbstractCommandDescriptor {
    private static boolean shutdownAfterUpdate;
    private final List<?> completions = null;
    public static final BooleanOptionRouter COMPANION = new BooleanOptionRouter();

    public UpdateCommand() {
        super("update", "", "Update Matrix to latest version", "matrix.update");
    }

    public List<?> getTabCompletions() {
        return completions;
    }

    public void execute(CommandSender sender, String[] unusedArguments) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("\u00a7cThis command can only be executed by console");
            return;
        }
        FoliaSupport.INSTANCE.runTaskAsync(UpdateCommand::performUpdate);
    }

    private static Unit performUpdate() {
        CloudServiceClient updater = Matrix.Companion.matrixInstance().getRuntimeServices();
        String token = BuildMetadata.LICENSE_METADATA.getAuthorizationToken();
        Function1<String, Unit> progress = UpdateCommand::logProgress;
        Object result = updater.downloadLatestProduct(1, token, progress);
        Throwable failure = result instanceof Result$Failure ? ((Result$Failure) result).exception : null;
        if (failure != null) {
            failure.printStackTrace();
            Logger logger = Matrix.Companion.matrixInstance().getLogger();
            String message = failure.getMessage();
            if (message == null) {
                message = failure.getClass().getSimpleName();
            }
            logger.warning(formatProgressMessage(message));
        }
        if (!(result instanceof Result$Failure)) {
            Unit ignoredValue = (Unit) result;
            Matrix.Companion.matrixInstance().getLogger()
                    .info("[Update] Success, please restart your server");
            if (shutdownAfterUpdate) {
                Bukkit.shutdown();
                System.exit(0);
                throw new RuntimeException("System.exit returned normally, while it was supposed to halt JVM.");
            }
        }
        return Unit.INSTANCE;
    }

    private static Unit logProgress(String message) {
        Logger logger = Matrix.Companion.matrixInstance().getLogger();
        logger.info(formatProgressMessage(message));
        return Unit.INSTANCE;
    }

    private static String formatProgressMessage(String message) {
        return new StringBuilder("[Update] ").append(message).toString();
    }

    public static void setShutdownAfterUpdate(boolean value) { shutdownAfterUpdate = value; }

    public static boolean isShutdownAfterUpdate() { return shutdownAfterUpdate; }

}
