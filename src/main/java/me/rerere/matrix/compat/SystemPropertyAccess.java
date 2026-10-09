package me.rerere.matrix.compat;

import me.rerere.matrix.geometry.AxisAlignedBox;

/** Applies the native encoding compatibility fallback used by Matrix. */
public class SystemPropertyAccess {
    public static String getProperty(String name) {
        boolean compatibilityMode = AxisAlignedBox.isMode();
        if (compatibilityMode) return name;
        if (name.equals("native.encoding")) {
            String nativeEncoding = System.getProperty(name);
            return nativeEncoding == null ? System.getProperty("file.encoding") : nativeEncoding;
        }
        return System.getProperty(name);
    }
}
