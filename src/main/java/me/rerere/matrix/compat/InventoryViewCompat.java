package me.rerere.matrix.compat;

import java.lang.reflect.Method;
import me.rerere.matrix.inventory.InventoryResolver;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.util.reflect.ReflectionNameResolver;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** Preserves the lazy reflection needed for InventoryView API changes. */
public final class InventoryViewCompat implements InventoryResolver {
    private final Object delegator;
    public static final InventoryViewReflection COMPANION = new InventoryViewReflection(null);
    private static final Lazy<Class<?>> VIEW_CLASS = LazyKt.lazy(InventoryViewCompat::loadViewClass);
    private static final Lazy<Method> TOP_METHOD = LazyKt.lazy(InventoryViewCompat::loadTopMethod);
    private static final Lazy<Method> BOTTOM_METHOD = LazyKt.lazy(InventoryViewCompat::loadBottomMethod);
    private static final Lazy<Method> CLICK_VIEW_METHOD = LazyKt.lazy(InventoryViewCompat::loadClickViewMethod);

    private InventoryViewCompat(Object delegator) {
        this.delegator = delegator;
    }

    public static InventoryResolver fromClickEvent(InventoryClickEvent event) {
        try {
            Object view = CLICK_VIEW_METHOD.getValue().invoke(event);
            return wrap(identity(view));
        } catch (ReflectiveOperationException failure) {
            throw propagate(failure);
        }
    }

    private static Method loadClickViewMethod() {
        Class<?> eventClass = InventoryClickEvent.class;
        String methodName = ReflectionNameResolver.mapMethodName(
                "getView", eventClass, new Class<?>[0]);
        try {
            return eventClass.getMethod(methodName);
        } catch (NoSuchMethodException failure) {
            throw propagate(failure);
        }
    }

    public static InventoryViewCompat wrap(Object delegator) {
        return new InventoryViewCompat(delegator);
    }

    public Object getDelegator() {
        return delegator;
    }

    public static Object identity(Object value) {
        return value;
    }

    public static boolean equalDelegators(Object first, Object second) {
        return Intrinsics.areEqual(first, second);
    }

    public static boolean isSameView(Object expectedDelegator, Object candidate) {
        if (!(candidate instanceof InventoryViewCompat)) return false;
        return equalDelegators(expectedDelegator,
                ((InventoryViewCompat) candidate).getDelegator());
    }

    @Override public boolean equals(Object candidate) {
        return isSameView(delegator, candidate);
    }

    public static int hashDelegator(Object delegator) {
        return delegator.hashCode();
    }

    @Override public int hashCode() {
        return hashDelegator(delegator);
    }

    public String describe() {
        return "InventoryViewCompatImpl(delegator=" + delegator + ")";
    }

    @Override public String toString() {
        return describe();
    }

    @Override public Inventory getBottomInventory() {
        return invokeBottomInventory(delegator);
    }

    @Override public Inventory getTopInventory() {
        return invokeTopInventory(delegator);
    }

    public static Inventory invokeTopInventory(Object inventoryView) {
        try {
            Object inventory = InventoryViewReflection.resolveTopInventoryMethod(COMPANION)
                    .invoke(inventoryView, new Object[0]);
            Intrinsics.checkNotNull(inventory, "null cannot be cast to non-null type org.bukkit.inventory.Inventory");
            return (Inventory) inventory;
        } catch (Throwable failure) { throw propagate(failure); }
    }

    public static Inventory invokeBottomInventory(Object inventoryView) {
        try {
            Object inventory = InventoryViewReflection.resolveBottomInventoryMethod(COMPANION)
                    .invoke(inventoryView, new Object[0]);
            Intrinsics.checkNotNull(inventory, "null cannot be cast to non-null type org.bukkit.inventory.Inventory");
            return (Inventory) inventory;
        } catch (Throwable failure) { throw propagate(failure); }
    }

    public static Lazy<Class<?>> viewClassLazy() { return VIEW_CLASS; }
    public static Lazy<Method> topMethodLazy() { return TOP_METHOD; }
    public static Lazy<Method> bottomMethodLazy() { return BOTTOM_METHOD; }

    private static Class<?> loadViewClass() {
        try { return Class.forName("org.bukkit.inventory.InventoryView"); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    private static Method loadTopMethod() { return loadMethod("getTopInventory"); }
    private static Method loadBottomMethod() { return loadMethod("getBottomInventory"); }
    private static Method loadMethod(String name) {
        Class<?> viewClass = InventoryViewReflection.resolveViewClass(COMPANION);
        Class<?>[] parameterTypes = new Class<?>[0];
        String mappedName = ReflectionNameResolver.mapMethodName(name, viewClass, parameterTypes);
        try { return viewClass.getDeclaredMethod(mappedName, parameterTypes); }
        catch (Throwable failure) { throw propagate(failure); }
    }
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException propagate(Throwable failure) throws T { throw (T) failure; }
}
