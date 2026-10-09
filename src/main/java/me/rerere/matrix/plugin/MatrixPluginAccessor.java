package me.rerere.matrix.plugin;

import me.rerere.matrix.Matrix;

/** Reads the plugin instance from Matrix's static holder. */
public final class MatrixPluginAccessor {
    private MatrixPluginAccessor() {}

    public static Matrix getMatrix() {
        me.rerere.matrix.config.MatrixConfigBridge holder = Matrix.Companion;
        if (holder == null) {
            throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        }
        return holder.matrixInstance();
    }
}
