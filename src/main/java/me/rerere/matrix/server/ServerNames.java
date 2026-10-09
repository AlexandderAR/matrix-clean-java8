package me.rerere.matrix.server;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.Matrix;

/** Resolves Minecraft package names for legacy and modern server versions. */
public class ServerNames {
    public static final ServerNames INSTANCE = new ServerNames();
    private static final me.rerere.matrix.thirdparty.kotlin.Lazy<String> packageComponentLazy =
            me.rerere.matrix.thirdparty.kotlin.LazyKt.lazy(ServerNames::computePackageComponent);
    private static final me.rerere.matrix.thirdparty.kotlin.Lazy<String> serverPackageNameLazy =
            me.rerere.matrix.thirdparty.kotlin.LazyKt.lazy(ServerNames::serverPackage);

    private ServerNames() { }

    private static String buildDottedName(String left, String right) {
        return new StringBuilder(left).append(".").append(right).toString();
    }

    public static final String joinViaInstance(String suffix) {
        String base = INSTANCE.getServerPackageName();
        return buildDottedName(base, suffix);
    }

    private static final String serverPackage() {
        return org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
    }

    private final String getLazyPackageComponent() {
        return packageComponentLazy.getValue();
    }

    private final String getServerPackageName() {
        return serverPackageNameLazy.getValue();
    }

    private static String netMinecraftName(String suffix) {
        return "net.minecraft." + suffix;
    }

    private static String nmsPackageName(String version, String name) {
        return "net.minecraft.server." + version + "." + name;
    }

    public static final String packageComponent() {
        return INSTANCE.getLazyPackageComponent();
    }

    private static final String computePackageComponent() {
        String dottedPackageName = serverPackage().replace('.', ',');
        String[] packageComponents = dottedPackageName.split(",", -1);
        int componentCount = packageComponents.length;
        while (componentCount > 0 && packageComponents[componentCount - 1].length() == 0) --componentCount;
        return componentCount > 3 ? packageComponents[3] : null;
    }

    public static final String versioned(String legacySuffix, String modernSuffix) {
        ServerVersion version = Matrix.getServerVersion();
        if (version.compareTo(ServerVersion.V_1_17) < 0) {
            String component = INSTANCE.getLazyPackageComponent();
            return nmsPackageName(component, legacySuffix);
        }
        return netMinecraftName(modernSuffix);
    }

    public static final String versionedSince117(String suffix) {
        ServerVersion version = Matrix.getServerVersion();
        if (version.compareTo(ServerVersion.V_1_17) < 0) {
            String component = INSTANCE.getLazyPackageComponent();
            return nmsPackageName(component, suffix);
        }
        return netMinecraftName(suffix);
    }
}
