package me.rerere.matrix.license;

import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.crypto.RuntimePrefaceBootstrap;
import me.rerere.matrix.license.LicenseRuntimeBridge;
import me.rerere.matrix.network.PrefaceFieldDecoder;
import me.rerere.matrix.thirdparty.kotlin.Result;

/** Runs the selected class-loader prologue before the remaining startup gates. */
public final class LicenseClassLoaderPrologue {
    private static final ClassLoaderActions PRODUCTION = new DefaultActions();

    private LicenseClassLoaderPrologue() {
        super();
    }

    public static void runSelected(Object target, ClassLoaderActions actions) throws ClassNotFoundException {
        Class<?> targetClass = actions.getObjectClass(target);
        ClassLoader classLoader = actions.getClassLoader(targetClass);
        if (classLoader == null) {
            throw new UnresolvedNativePathError("JNI FatalError(classloader == null) at 0x67bef");
        }
        Class<?> swClass = actions.loadSwClass(classLoader);
        actions.getResultCompanion();
        if (!actions.isSwInstance(target, swClass)) {
            throw new UnresolvedNativePathError("native ClassCastException branch at 0x68104");
        }
    }

    public static void selectedBeforeJmx(LicenseRuntimeBridge target)
            throws ClassNotFoundException {
        runSelected(target, PRODUCTION);
    }

    public static PrefaceFieldDecoder.Fields selectedBeforeToken(
            LicenseRuntimeBridge target,
            PrefaceRuntimeContext prologue)
            throws ClassNotFoundException {
        selectedBeforeJmx(target);
        return RuntimePrefaceBootstrap.afterNativePrologue(prologue);
    }

    public interface ClassLoaderActions {
        Class<?> getObjectClass(Object target);

        ClassLoader getClassLoader(Class<?> targetClass);

        Class<?> loadSwClass(ClassLoader classLoader) throws ClassNotFoundException;

        Object getResultCompanion();

        boolean isSwInstance(Object target, Class<?> swClass);
    }

    public static final class UnresolvedNativePathError extends Error {
        public UnresolvedNativePathError(String message) {
            super(message);
        }
    }

    private static final class DefaultActions implements ClassLoaderActions {
        @Override
        public Class<?> getObjectClass(Object target) {
            return target.getClass();
        }

        @Override
        public ClassLoader getClassLoader(Class<?> targetClass) {
            return targetClass.getClassLoader();
        }

        @Override
        public Class<?> loadSwClass(ClassLoader classLoader) throws ClassNotFoundException {
            return classLoader.loadClass("me.rerere.matrix.license.LicenseRuntimeBridge");
        }

        @Override
        public Object getResultCompanion() {
            return Result.Companion;
        }

        @Override
        public boolean isSwInstance(Object target, Class<?> swClass) {
            return swClass.isInstance(target);
        }
    }
}
