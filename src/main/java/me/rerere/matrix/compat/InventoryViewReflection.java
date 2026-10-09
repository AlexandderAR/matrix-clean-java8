package me.rerere.matrix.compat;

import java.lang.reflect.Method;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;

public final class InventoryViewReflection {
    private InventoryViewReflection() { }
    public InventoryViewReflection(DefaultConstructorMarker marker) { this(); }
    private Class<?> getViewClass() {
        return (Class<?>) InventoryViewCompat.viewClassLazy().getValue();
    }
    private Method getTopInventoryMethod() {
        return (Method) InventoryViewCompat.topMethodLazy().getValue();
    }
    private Method getBottomInventoryMethod() {
        return (Method) InventoryViewCompat.bottomMethodLazy().getValue();
    }
    public static Method resolveBottomInventoryMethod(InventoryViewReflection reflection) {
        return reflection.getBottomInventoryMethod();
    }
    public static Method resolveTopInventoryMethod(InventoryViewReflection reflection) {
        return reflection.getTopInventoryMethod();
    }
    public static Class<?> resolveViewClass(InventoryViewReflection reflection) {
        return reflection.getViewClass();
    }
}
