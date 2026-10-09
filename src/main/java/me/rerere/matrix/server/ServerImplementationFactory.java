package me.rerere.matrix.server;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.api.exception.UnknownMinecraftVersionException;
import me.rerere.matrix.world.WorldAccess;

/** Selects the server-specific implementation from Matrix's observed version table. */
public final class ServerImplementationFactory {
    public WorldAccess visitor;

    public final void setVisitor(WorldAccess visitor) {
        this.visitor = visitor;
    }

    private static String unsupportedVersionMessage(String release) {
        return new StringBuilder("Unsupported Minecraft Server Version: ").append(release).toString();
    }

    public final void initialize() {
        initializeFromServerVersion();
    }

    private void initializeFromServerVersion() {
        ServerVersion version = Matrix.Companion.serverVersion();
        int exactVersionCase = switchCase(version);
        WorldAccess selected;

        if (exactVersionCase == 1) {
            selected = createWorldAccess("me.rerere.matrix.world.compatibility.LegacyCraftBukkit18BlockAccess");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_12, ServerVersion.V_1_12_2)) {
            selected = createWorldAccess("me.rerere.matrix.world.BukkitWorldQueries");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_13, ServerVersion.V_1_13_1)) {
            selected = createWorldAccess("me.rerere.matrix.compat.LegacyCraftBukkit113BlockAccess");
        } else if (exactVersionCase == 2) {
            selected = createWorldAccess("me.rerere.matrix.world.V113R2WorldAdapter");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_14, ServerVersion.V_1_14_4)) {
            selected = createWorldAccess("me.rerere.matrix.world.nms.v1_14.Legacy114WorldAccess");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_15, ServerVersion.V_1_15_2)) {
            selected = createWorldAccess("me.rerere.matrix.compat.bukkit.Bukkit115Access");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_16, ServerVersion.V_1_16_1)) {
            selected = createWorldAccess("me.rerere.matrix.world.Legacy116WorldAdapter");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_16_2, ServerVersion.V_1_16_3)) {
            selected = createWorldAccess("me.rerere.matrix.world.Legacy116R2WorldAdapter");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_16_4, ServerVersion.V_1_16_5)) {
            selected = createWorldAccess("me.rerere.matrix.world.V116R3WorldAdapter");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_17, ServerVersion.V_1_17_1)) {
            selected = createWorldAccess("me.rerere.matrix.world.LegacyReflectiveWorldAdapter");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_18, ServerVersion.V_1_18_1)) {
            selected = createWorldAccess("me.rerere.matrix.world.Bukkit18WorldAccess");
        } else if (exactVersionCase == 3) {
            selected = createWorldAccess("me.rerere.matrix.platform.bukkit.v1_18_R2.CraftBukkit118R2Access");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_19, ServerVersion.V_1_19_2)) {
            selected = createWorldAccess("me.rerere.matrix.compat.nms.V119R1NmsAccess");
        } else if (exactVersionCase == 4) {
            selected = createWorldAccess("me.rerere.matrix.server.nms.v1_19_R2.ServerImplementation119R2");
        } else if (exactVersionCase == 5) {
            selected = createWorldAccess("me.rerere.matrix.world.V119R3WorldAdapter");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_20, ServerVersion.V_1_20_1)) {
            selected = createWorldAccess("me.rerere.matrix.world.V120R1WorldAdapter");
        } else if (exactVersionCase == 6) {
            selected = createWorldAccess("me.rerere.matrix.world.CraftPhysicsAccess");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_20_3, ServerVersion.V_1_20_4)) {
            selected = createWorldAccess("me.rerere.matrix.world.V120R3WorldAdapter");
        } else if (exactVersionCase == 7 || exactVersionCase == 8) {
            String packageVersion = ServerNames.packageComponent();
            selected = "v1_20_R4".equals(packageVersion)
                    ? createWorldAccess("me.rerere.matrix.world.NmsWorldAccess")
                    : createWorldAccess("me.rerere.matrix.world.BukkitNmsBridge");
        } else if (isWithinVersionRange(version, ServerVersion.V_1_21, ServerVersion.V_1_21_11)) {
            selected = createWorldAccess("me.rerere.matrix.world.NmsMethodAccess");
        } else if (isWithinVersionRange(version, ServerVersion.V_26_1, ServerVersion.V_26_3)) {
            selected = createWorldAccess("me.rerere.matrix.world.ReflectiveWorldAdapter");
        } else {
            String release = version.getReleaseName();
            throw new UnknownMinecraftVersionException(unsupportedVersionMessage(release));
        }

        this.visitor = selected;
        if (version == ServerVersion.V_1_8_8 ||
                isWithinVersionRange(version, ServerVersion.V_1_13, ServerVersion.V_1_15_2)) {
            Matrix.Companion.matrixInstance().getLogger()
                    .warning("You are using an unpopular Minecraft version. This may cause issues.");
            Matrix.Companion.matrixInstance().getLogger()
                    .warning("Matrix might drop support for this version in the future.");
            Matrix.Companion.matrixInstance().getLogger()
                    .warning("Please update to a supported version. (1.17+)");
        }
    }

    public final WorldAccess visitor() {
        WorldAccess value = this.visitor;
        if (value != null) return value;
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics
                .throwUninitializedPropertyAccessException("visitor");
        return null;
    }

    public ServerImplementationFactory() { }

    /** Creates the already-selected adapter without coupling main compilation to its SDK. */
    private static WorldAccess createWorldAccess(String className) {
        try {
            Class<?> adapterClass = Class.forName(className, true,
                    ServerImplementationFactory.class.getClassLoader());
            Object adapter = adapterClass.getConstructor().newInstance();
            return WorldAccess.class.cast(adapter);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw propagateConstructorFailure(failure.getCause());
        } catch (ReflectiveOperationException | SecurityException failure) {
            throw new IllegalStateException("Unable to construct selected server adapter: " + className, failure);
        }
    }

    private static RuntimeException propagateConstructorFailure(Throwable failure) {
        ServerImplementationFactory.<RuntimeException>throwUnchecked(failure);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <Failure extends Throwable> void throwUnchecked(Throwable failure) throws Failure {
        throw (Failure) failure;
    }


    private static boolean isWithinVersionRange(ServerVersion version, ServerVersion minimum,
                                                ServerVersion maximum) {
        return version.compareTo(maximum) <= 0 && version.compareTo(minimum) >= 0;
    }

    private static int switchCase(ServerVersion version) {
        switch (version) {
            case V_1_8_8: return 1;
            case V_1_13_2: return 2;
            case V_1_18_2: return 3;
            case V_1_19_3: return 4;
            case V_1_19_4: return 5;
            case V_1_20_2: return 6;
            case V_1_20_5: return 7;
            case V_1_20_6: return 8;
            default: return 0;
        }
    }
}
