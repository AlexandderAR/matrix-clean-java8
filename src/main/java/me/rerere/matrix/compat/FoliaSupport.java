package me.rerere.matrix.compat;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import com.google.common.collect.Sets;
import com.google.common.collect.Maps;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;

/** Provides compatible scheduling operations for Bukkit and Folia servers. */
public final class FoliaSupport {
    public static final FoliaSupport INSTANCE = new FoliaSupport();

    private static final Lazy<Boolean> IS_FOLIA = LazyKt.lazy(FoliaSupport::detectFolia);
    private static final Lazy<MethodHandle> ENTITY_SCHEDULER = LazyKt.lazy(FoliaSupport::entitySchedulerMethod);
    private static final Lazy<MethodHandle> ASYNC_SCHEDULER = LazyKt.lazy(FoliaSupport::asyncSchedulerMethod);
    private static final Lazy<MethodHandle> GLOBAL_REGION_SCHEDULER = LazyKt.lazy(FoliaSupport::globalRegionSchedulerMethod);
    private static final Lazy<MethodHandle> TELEPORT_ASYNC = LazyKt.lazy(FoliaSupport::teleportAsyncMethod);
    private static final Lazy<MethodHandle> REGION_OWNERSHIP = LazyKt.lazy(FoliaSupport::regionOwnershipMethod);

    private FoliaSupport() {}

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable absentOrUnavailable) {
            return false;
        }
    }

    public boolean isUsingFolia() { return IS_FOLIA.getValue(); }
    private MethodHandle entitySchedulerMethodHandle() { return ENTITY_SCHEDULER.getValue(); }
    private MethodHandle asyncSchedulerMethodHandle() { return ASYNC_SCHEDULER.getValue(); }
    private MethodHandle globalRegionSchedulerMethodHandle() { return GLOBAL_REGION_SCHEDULER.getValue(); }
    private MethodHandle teleportAsyncMethodHandle() { return TELEPORT_ASYNC.getValue(); }
    private MethodHandle regionOwnershipMethodHandle() { return REGION_OWNERSHIP.getValue(); }

    private Object entityScheduler(Player player) {
        try {
            Object scheduler = INSTANCE.entitySchedulerMethodHandle().invoke(player);
            return me.rerere.matrix.compat.EntitySchedulerProxy.constructorImpl(scheduler);
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, Object>sneaky(failure);
        }
    }

    private Object asyncScheduler() {
        try {
            Object scheduler = (Object) INSTANCE.asyncSchedulerMethodHandle().invoke();
            return me.rerere.matrix.compat.AsyncSchedulerProxy.constructValue(scheduler);
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, Object>sneaky(failure);
        }
    }

    private Object globalRegionScheduler() {
        try {
            Object scheduler = (Object) INSTANCE.globalRegionSchedulerMethodHandle().invoke();
            return me.rerere.matrix.compat.GlobalRegionSchedulerProxy.constructor(scheduler);
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, Object>sneaky(failure);
        }
    }

    private static void runAsyncTaskBody(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) { task.invoke(); }
    private static void runAsyncLaterTaskBody(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) { task.invoke(); }
    private static void runAsyncTimerTaskBody(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) { task.invoke(); }
    private static void runGlobalTaskBody(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) { task.invoke(); }
    private static void runDelayedGlobalTaskBody(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) { task.invoke(); }
    private static me.rerere.matrix.thirdparty.kotlin.Unit runPlayerTaskBody(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) {
        task.invoke();
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }
    private static me.rerere.matrix.thirdparty.kotlin.Unit runDelayedPlayerTaskBody(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) {
        task.invoke();
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }
    private static me.rerere.matrix.thirdparty.kotlin.Unit runFixedRateFinishedBody() { return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; }
    private static me.rerere.matrix.thirdparty.kotlin.Unit runPlayerFinishedBody() { return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; }
    private static me.rerere.matrix.thirdparty.kotlin.Unit runDelayedPlayerFinishedBody() { return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; }

    private static MethodHandle entitySchedulerMethod() {
        return findHandle("org.bukkit.entity.Entity", "getScheduler");
    }

    private static MethodHandle asyncSchedulerMethod() {
        return findHandle("org.bukkit.Bukkit", "getAsyncScheduler");
    }

    private static MethodHandle globalRegionSchedulerMethod() {
        return findHandle("org.bukkit.Bukkit", "getGlobalRegionScheduler");
    }

    private static MethodHandle teleportAsyncMethod() {
        try {
            for (Method method : Class.forName("org.bukkit.entity.Entity").getDeclaredMethods()) {
                if (method.getName().equals("teleportAsync") && method.getParameterCount() == 1) {
                    return me.rerere.matrix.util.reflect.ReflectionLookup.methodHandle(method);
                }
            }
            throw new java.util.NoSuchElementException("Array contains no element matching the predicate.");
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, MethodHandle>sneaky(failure);
        }
    }

    private static MethodHandle regionOwnershipMethod() {
        return findHandle("org.bukkit.Bukkit", "isOwnedByCurrentRegion", Location.class);
    }

    private static MethodHandle findHandle(String owner, String name, Class<?>... parameterTypes) {
        try {
            Method method = Class.forName(owner).getDeclaredMethod(name, parameterTypes);
            return me.rerere.matrix.util.reflect.ReflectionLookup.methodHandle(method);
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, MethodHandle>sneaky(failure);
        }
    }

    public static Entity nearbyEntityById(Player player, int entityId) {
        World world = player.getWorld();
        Location location = player.getLocation();
        Collection<Entity> nearby = world.getNearbyEntities(location, 8.0, 8.0, 8.0);
        for (Entity entity : nearby) {
            if (entity.getEntityId() == entityId) return entity;
        }
        return null;
    }

    public static boolean isLocationOwnedByCurrentRegion(Location location) {
        if (!INSTANCE.isUsingFolia()) return true;
        try {
            return (boolean) INSTANCE.regionOwnershipMethodHandle().invoke(location);
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, Boolean>sneaky(failure);
        }
    }

    public CompletableFuture<?> teleportAsync(Entity entity, Location location) {
        try {
            return (CompletableFuture<?>) INSTANCE.teleportAsyncMethodHandle().invoke(entity, location);
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, CompletableFuture<?>>sneaky(failure);
        }
    }

    public Set<Object> newHashSet() {
        return INSTANCE.isUsingFolia() ? Sets.newConcurrentHashSet() : new HashSet<>();
    }

    public Map<Object, Object> newHashMap() {
        return INSTANCE.isUsingFolia() ? Maps.newConcurrentMap() : new HashMap<>();
    }

    private interface ThrowableSupplier<T> {
        T get() throws Throwable;
    }

    private static <T> T invokeScheduler(ThrowableSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable failure) {
            return FoliaSupport.<RuntimeException, T>sneaky(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T sneaky(Throwable failure) throws E {
        throw (E) failure;
    }

    private static org.bukkit.plugin.Plugin plugin() {
        return me.rerere.matrix.Matrix.Companion.matrixInstance();
    }

    public void runTaskAsync(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) {
        if (!isUsingFolia()) Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> task.invoke());
        else {
            Object scheduler = asyncScheduler();
            Object callback = new me.rerere.matrix.compat.AsyncTaskCallback(task);
            Object scheduled = invokeScheduler(() -> me.rerere.matrix.compat.AsyncSchedulerProxy.runNow(scheduler, (me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<?, ?>) callback));
            me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
        }
    }

    public void runTaskAsyncLater(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task, long delay) {
        if (!isUsingFolia()) Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), () -> task.invoke(), delay);
        else {
            Object scheduler = asyncScheduler();
            Object callback = new me.rerere.matrix.compat.AsyncLaterTaskCallback(task);
            Object scheduled = invokeScheduler(() -> me.rerere.matrix.compat.AsyncSchedulerProxy.runDelayed(scheduler, (me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<?, ?>) callback, delay));
            me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
        }
    }

    public void runTaskTimerAsync(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task, long initialDelay, long period) {
        if (!isUsingFolia()) Bukkit.getScheduler().runTaskTimerAsynchronously(plugin(), () -> task.invoke(), initialDelay, period);
        else {
            Object scheduler = asyncScheduler();
            Object callback = new me.rerere.matrix.compat.AsyncTimerTaskCallback(task);
            Object scheduled = invokeScheduler(() -> me.rerere.matrix.compat.AsyncSchedulerProxy.runAtFixedRate(scheduler, (me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<?, ?>) callback, initialDelay, period));
            me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
        }
    }

    public void runAtGlobalScheduler(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) {
        if (!isUsingFolia()) Bukkit.getScheduler().runTask(plugin(), () -> task.invoke());
        else {
            Object scheduled = me.rerere.matrix.compat.GlobalRegionSchedulerProxy.run(globalRegionScheduler(), task);
            me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
        }
    }

    public void runDelayedGlobal(me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task, long delay) {
        if (!isUsingFolia()) Bukkit.getScheduler().runTaskLater(plugin(), () -> task.invoke(), delay);
        else {
            Object scheduled = me.rerere.matrix.compat.GlobalRegionSchedulerProxy.runDelayed(globalRegionScheduler(), task, delay);
            me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
        }
    }

    public void runSyncForPlayer(Player player, me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task, me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> retiredCallback) {
        if (!isUsingFolia()) {
            org.bukkit.scheduler.BukkitRunnable runnable = (org.bukkit.scheduler.BukkitRunnable)
                    new me.rerere.matrix.compat.PlayerTask(player, task, retiredCallback);
            runnable.runTask(plugin());
        } else {
            Object scheduler = entityScheduler(player);
            me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<me.rerere.matrix.thirdparty.kotlin.Unit> wrapped = () -> { task.invoke(); return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; };
            Object scheduled = invokeScheduler(() -> me.rerere.matrix.compat.EntitySchedulerProxy.run(scheduler, wrapped, retiredCallback));
            me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
        }
    }

    public static void runSyncForPlayer$default(FoliaSupport instance, Player player, me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task,
            me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> retiredCallback, int mask, Object marker) {
        if ((mask & 4) != 0) retiredCallback = () -> { runPlayerFinishedBody(); return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; };
        instance.runSyncForPlayer(player, task, retiredCallback);
    }

    public void runSyncForPlayer(Player player, me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task) {
        runSyncForPlayer$default(this, player, task, null, 4, null);
    }

    public void runDelayedSyncForPlayer(Player player, me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task, long delay) {
        if (!isUsingFolia()) {
            org.bukkit.scheduler.BukkitRunnable runnable = (org.bukkit.scheduler.BukkitRunnable)
                    new me.rerere.matrix.compat.DelayedPlayerTask(player, task);
            runnable.runTaskLater(plugin(), delay);
        } else {
            Object scheduler = entityScheduler(player);
            me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<me.rerere.matrix.thirdparty.kotlin.Unit> wrapped = () -> { task.invoke(); return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; };
            me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<me.rerere.matrix.thirdparty.kotlin.Unit> retired = () -> { runDelayedPlayerFinishedBody(); return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; };
            Object scheduled = invokeScheduler(() -> me.rerere.matrix.compat.EntitySchedulerProxy.runDelayed(scheduler, wrapped, retired, delay));
            me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
        }
    }

    public me.rerere.matrix.compat.Cancelable runAtFixedRateSyncForPlayer(Player player,
            me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task, me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> retiredCallback, long initialDelay, long period) {
        if (!isUsingFolia()) {
            me.rerere.matrix.compat.FixedRatePlayerTask runnable =
                    new me.rerere.matrix.compat.FixedRatePlayerTask(player, task, retiredCallback);
            runnable.runTaskTimer(plugin(), initialDelay, period);
            return new me.rerere.matrix.compat.FixedRateCancelable(runnable);
        }
        Object scheduler = entityScheduler(player);
        Object callback = new me.rerere.matrix.compat.FixedRateTaskCallback(task);
        Object scheduled = invokeScheduler(() -> me.rerere.matrix.compat.EntitySchedulerProxy.runAtFixedRate(scheduler, (me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<?, ?>) callback, retiredCallback, initialDelay, period));
        return me.rerere.matrix.compat.ScheduledTaskProxy.boxImpl(scheduled);
    }

    public static me.rerere.matrix.compat.Cancelable runAtFixedRateSyncForPlayer$default(FoliaSupport instance, Player player,
            me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> task, me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0<?> retiredCallback, long initialDelay,
            long period, int mask, Object marker) {
        if ((mask & 4) != 0) retiredCallback = () -> { runFixedRateFinishedBody(); return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE; };
        return instance.runAtFixedRateSyncForPlayer(player, task, retiredCallback, initialDelay, period);
    }

    public MethodHandle getGetGlobalRegionSchedulerFn() { return globalRegionSchedulerMethodHandle(); }
}
