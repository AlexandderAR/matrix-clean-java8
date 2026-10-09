package me.rerere.matrix.util.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import me.rerere.matrix.server.ServerNames;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;

/** Resolves compatibility class names and reflective handles. */
public final class ReflectionLookup {
    private ReflectionLookup() { }

    public static Class<?> loadVersionedClassNames(String legacySuffix, String modernSuffix)
            throws ClassNotFoundException {
        String qualifiedName = ServerNames.versioned(legacySuffix, modernSuffix);
        return Class.forName(ReflectionNameResolver.mapDescriptorName(qualifiedName));
    }

    /** Loads a suffix relative to the running CraftBukkit server package. */
    @NotNull
    public static Class<?> loadClass(@NotNull String craftSuffix) throws ClassNotFoundException {
        String qualifiedName = ServerNames.joinViaInstance(craftSuffix);
        return Class.forName(ReflectionNameResolver.mapDescriptorName(qualifiedName));
    }

    /** Loads a version-aware net.minecraft class suffix. */
    public static Class<?> loadVersionedNmsClass(String suffix) throws ClassNotFoundException {
        String qualifiedName = ServerNames.versionedSince117(suffix);
        return Class.forName(ReflectionNameResolver.mapDescriptorName(qualifiedName));
    }

    private static boolean containsFieldName(String[] fieldNames, String fieldName) {
        for (String candidate : fieldNames) {
            if (Objects.equals(candidate, fieldName)) return true;
        }
        return false;
    }

    private static String missingConstantMessage(String names) {
        return "Cannot find constant with names: " + names;
    }

    /** Returns the first matching declared static value; missing and null values fail. */
    public static Object getStaticConstant(Class<?> declaringClass, String[] fieldNames)
            throws IllegalAccessException {
        Field selected = null;
        for (Field field : declaringClass.getDeclaredFields()) {
            if (containsFieldName(fieldNames, field.getName())) {
                selected = field;
                break;
            }
        }

        Object value = selected == null ? null : selected.get(null);
        if (value != null) return value;
        throw new IllegalStateException(missingConstantMessage(String.join(", ", fieldNames)));
    }

    public static MethodHandle methodHandle(Method method) throws IllegalAccessException {
        if (!method.isAccessible()) method.setAccessible(true);
        return MethodHandles.lookup().unreflect(method);
    }

    public static MethodHandle constructorHandle(Constructor<?> constructor)
            throws IllegalAccessException {
        return MethodHandles.lookup().unreflectConstructor(constructor);
    }

    public static MethodHandle fieldGetter(Field field) throws IllegalAccessException {
        if (!field.isAccessible()) field.setAccessible(true);
        return MethodHandles.lookup().unreflectGetter(field);
    }
}
