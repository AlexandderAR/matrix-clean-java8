package me.rerere.matrix.license;

import java.util.logging.Logger;
import me.rerere.matrix.Matrix;


public final class LicenseLoggerAccess {
    public static Logger matrixLogger() { return Matrix.Companion.matrixInstance().getLogger(); }
}
