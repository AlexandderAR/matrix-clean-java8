package me.rerere.matrix.util;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.condition.ConditionalCommands;
import me.rerere.matrix.compat.CompatibilityManager;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.commands.MatrixCommandDispatcher;
import me.rerere.matrix.network.PlayerPacketListener;
import me.rerere.matrix.network.packet.TransactionPacketListener;
import me.rerere.matrix.network.PlayerMetadataPacketListener;
import me.rerere.matrix.listeners.BlockWorldListener;
import me.rerere.matrix.network.GlideMetadataListener;
import me.rerere.matrix.listeners.LoginCooldownListener;
import me.rerere.matrix.inference.InferenceTask;
import me.rerere.matrix.config.MatrixConfigBridge;


/** Descriptor-specific typed accessors for Matrix-owned services. */
public final class TypedMatrixMembers {
    private static final String NULL_MESSAGE = "\"INVOKEVIRTUAL Object npe\" on -1";
    private TypedMatrixMembers() {}

    private static Matrix requireMatrix(Matrix matrix) {
        if (matrix == null) throw new NullPointerException(NULL_MESSAGE);
        return matrix;
    }

    private static MatrixConfigBridge requireMatrixConfigBridge() {
        MatrixConfigBridge singleton = Matrix.Companion;
        if (singleton == null) throw new NullPointerException(NULL_MESSAGE);
        return singleton;
    }

    private static MillisecondSampler requireSampler(MillisecondSampler sampler) {
        if (sampler == null) throw new NullPointerException("\"INVOKEVIRTUAL Void npe\" on -1");
        return sampler;
    }

    public static MatrixCommandDispatcher getMatrixCommand(Matrix matrix) { return requireMatrix(matrix).getMatrixCommand(); }
    public static ConditionalCommands getConditionalCommands(Matrix matrix) { return requireMatrix(matrix).getConditionalCommands(); }
    public static PlayerPacketListener getPacketListener(Matrix matrix) { return requireMatrix(matrix).getPacketListener(); }
    public static TransactionPacketListener getPingPongListener(Matrix matrix) { return requireMatrix(matrix).getPingPongListener(); }
    public static PlayerMetadataPacketListener getFilters(Matrix matrix) { return requireMatrix(matrix).getFilters(); }
    public static BlockWorldListener getCheckListener(Matrix matrix) { return requireMatrix(matrix).getCheckListener(); }
    public static GlideMetadataListener getElytraListener(Matrix matrix) { return requireMatrix(matrix).getElytraListener(); }
    public static LoginCooldownListener getRelogManager(Matrix matrix) { return requireMatrix(matrix).getRelogManager(); }
    public static CompatibilityManager getCompatibilityManager(Matrix matrix) { return requireMatrix(matrix).getCompatibilityManager(); }
    public static InferenceTask getInferenceTask(Matrix matrix) { return requireMatrix(matrix).getInferenceTask(); }
    public static ServerVersion serverVersion() { return requireMatrixConfigBridge().serverVersion(); }
    public static void startMillisecondSampler() { requireSampler(MillisecondSampler.INSTANCE).start(); }
}
