package me.rerere.matrix.checks.combat;

import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.checks.SecurityMode;

public final class HitBoxCancelWayResolver {
    public static final HitBoxCancelWayResolver INSTANCE = new HitBoxCancelWayResolver();

    private HitBoxCancelWayResolver() {
    }

    public HitBoxCancelWayResolver(DefaultConstructorMarker marker) {
        this();
    }


    public SecurityMode resolve(String requestedWay) {
        for (SecurityMode candidate : SecurityMode.entries()) {
            if (candidate.name().equalsIgnoreCase(requestedWay)) {
                return candidate;
            }
        }

        MatrixConfigBridge logger = Matrix.Companion;
        logger.logMessage("(HitBox) Unknown cancel way: " + requestedWay);
        logger.logMessage("(HitBox) Use default cancel way: Silent");
        return SecurityMode.SILENT;
    }


}
