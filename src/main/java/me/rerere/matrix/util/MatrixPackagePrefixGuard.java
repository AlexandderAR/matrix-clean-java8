package me.rerere.matrix.util;

/** Checks whether a class name begins with Matrix's package prefix. */
public final class MatrixPackagePrefixGuard {
    private static final String PACKAGE_PREFIX = "me.rerere.matrix";

    public MatrixPackagePrefixGuard() {
    }

    public static boolean startsWithMatrixPackage(String value) {
        return value.startsWith(PACKAGE_PREFIX);
    }
}
