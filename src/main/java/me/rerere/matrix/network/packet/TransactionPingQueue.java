package me.rerere.matrix.network.packet;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import com.google.common.collect.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.model.CheckState;
import me.rerere.matrix.network.packet.RandomTransactionIdPool;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerPingAccessor;
import me.rerere.matrix.runtime.MatrixTasks;
import me.rerere.matrix.util.PingManagerSupport;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.ConnectionState;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.User;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function2;
import me.rerere.matrix.thirdparty.kotlin.time.Duration;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;
import me.rerere.matrix.thirdparty.kotlin.time.TimeSource$Monotonic;
import me.rerere.matrix.thirdparty.kotlin.time.TimeSource$Monotonic$ValueTimeMark;

/** Per-player ping transaction queues and their timeout callbacks. */
public final class TransactionPingQueue {
    private final ConcurrentLinkedQueue<Integer> recentPingIds;
    private final AtomicReference<CheckState> pendingWork;
    private final ConcurrentLinkedQueue<Integer> trackedTransactions;
    private final ConcurrentMap<Integer,Object> transactionMarks;
    private long completedTransactionCount;
    private final ConcurrentLinkedQueue<Integer> waitingTransactionIds;
    private long transactionTimeoutMark;
    private boolean notificationsEnabled;
    private final ConcurrentLinkedQueue<Integer> transactionQueue;
    private final RandomTransactionIdPool clock;
    private final ArrayList<Integer> skippedTransactionIds;
    private long timeoutMark;
    private final ConcurrentMap<Integer,Queue<Function0<?>>> transactionCallbacks;
    private final PlayerCheckContext playerData;
    private static boolean globallyEnabled;

    static {
        setGloballyEnabled(true);
    }

    public TransactionPingQueue(PlayerCheckContext data) {
        playerData=data;
        clock=new RandomTransactionIdPool();
        transactionTimeoutMark=markNow();
        pendingWork=new AtomicReference<>(null);
        timeoutMark=markNow();
        trackedTransactions=Queues.newConcurrentLinkedQueue();
        transactionCallbacks=Maps.newConcurrentMap();
        transactionMarks=Maps.newConcurrentMap();
        transactionQueue=Queues.newConcurrentLinkedQueue();
        recentPingIds=Queues.newConcurrentLinkedQueue();
        waitingTransactionIds=Queues.newConcurrentLinkedQueue();
        skippedTransactionIds=Lists.newArrayList();
    }

    private static String formatQueueState(Object first, Object second, String recent,
                                           boolean folia, ServerVersion version, int protocol) {
        return new StringBuilder("queue: ").append(first).append(" ~ ").append(second)
          .append(", recent pong ids: ").append(recent).append(", folia: ").append(folia)
          .append(", server version: ").append(version).append('#').append(protocol).toString();
    }
    private static String formatPingReceived(ChatColor c, String name, int id, int size) {
        return new StringBuilder().append((Object)c).append("[↓] Ping Received from ").append(name)
          .append(": #").append(id).append(", waiting_size: ").append(size).toString();
    }
    private static String formatPingSent(ChatColor c, String name, int id, int size) {
        return new StringBuilder().append((Object)c).append("[↑] Ping Sent to ").append(name)
          .append(": #").append(id).append(", waiting_size: ").append(size).toString();
    }
    private static String formatWaitingQueueWarning(String name, int size, long done,
                                                    String version, int protocol) {
        return new StringBuilder(name).append("'s waiting transaction queue is too long(queue: ").append(size)
          .append(", processed: ").append(done).append(", ver: ").append(version).append('#').append(protocol).append(')').toString();
    }
    private static String formatSkippedTransactions(String name, int n, Object first, Object last) {
        return new StringBuilder(name).append("'s skipped ").append(n).append(" transactions (")
          .append(first).append(" ~ ").append(last).append(')').toString();
    }
    private static String formatPlayerPingState(String name, String address, int ping,
                                                String version, int protocol) {
        return new StringBuilder(name).append(" -> ping: ").append(address).append('/').append(ping)
          .append(", version: ").append(version).append('#').append(protocol).toString();
    }
    private static String formatInvalidTransactionId(int id) {
        return new StringBuilder("invalid transaction id: ").append(id).toString();
    }
    private static String formatSendQueueWarning(String playerName, String clientReleaseName) {
        return new StringBuilder("Gonna send transaction queue is too long, maybe you have some bugs(")
          .append(playerName).append(", ").append(clientReleaseName).append(')').toString();
    }
    private static String formatTransactionMapWarning(String name, int size) {
        return new StringBuilder(name).append("'s transaction tasks map is too long(").append(size)
          .append("), maybe you have some bugs").toString();
    }
    public final void flushPendingWork() {
        CheckState pending = pendingWork.getAndSet(null);
        if (pending != null) {
            transmit(pending.secondaryValue());
        }
    }

    public final void receiveTransaction(int id) {
        Integer transactionId = Integer.valueOf(id);
        trackedTransactions.add(transactionId);
        if (trackedTransactions.size() > 20) {
            trackedTransactions.poll();
        }
        if (!recentPingIds.contains(transactionId)) {
            return;
        }
        if (notificationsEnabled) {
            Bukkit.broadcast(formatPingReceived(ChatColor.BLUE,
                    playerData.playerName(), id, recentPingIds.size()), "matrix.notify");
        }
        Integer completed;
        while ((completed=recentPingIds.poll())!=null) {
            completedTransactionCount++;
            waitingTransactionIds.add(completed);
            while (waitingTransactionIds.size()>20) {
                if (waitingTransactionIds.poll()==null) break;
            }
            Object completedAt=transactionMarks.remove(completed);
            if(completedAt!=null) playerData.updateLatency(unboxTimeMark(completedAt));
            Queue<Function0<?>> callbacks=transactionCallbacks.remove(completed);
            if(callbacks!=null) PingManagerSupport.invokeQueuedAction(callbacks);
            if(completed.intValue()==id) break;
            skippedTransactionIds.add(completed);
        }
        if(!skippedTransactionIds.isEmpty()
                && !PingManagerSupport.isWarningDisabled()) {
            MatrixTasks.logger().warning(
                    formatSkippedTransactions(playerData.playerName(),
                    skippedTransactionIds.size(),skippedTransactionIds.get(0),skippedTransactionIds.get(skippedTransactionIds.size()-1)));
        }
        skippedTransactionIds.clear();
    }

    private final void transmit(int id) {
        if (!PingManagerSupport.fitsSignedShort(id)) {
            throw new IllegalStateException(formatInvalidTransactionId(id));
        }
        User user = playerData.packetUser();
        if (user.getConnectionState() != ConnectionState.PLAY) {
            return;
        }
        transactionQueue.add(Integer.valueOf(id));
        playerData.sendPing((int) (short) id);
        timeoutMark = markNow();
    }

    public final PlayerCheckContext coordinator() { return playerData; }

    public final void onQueuedPing(int id) {
        Integer transactionId = Integer.valueOf(id);
        boolean inverseMode = AxisAlignedBox.isInverseMode();
        boolean removed = transactionQueue.remove(transactionId);
        if (inverseMode && removed) {
            recentPingIds.add(transactionId);
            transactionMarks.put(transactionId, boxTimeMark(markNow()));
            if (notificationsEnabled && inverseMode) {
                Bukkit.broadcast(formatPingSent(ChatColor.GREEN, playerData.playerName(), id,
                        recentPingIds.size()), "matrix.notify");
            }
        }
        if (transactionQueue.size() > 1000 && !PingManagerSupport.isWarningDisabled()) {
            MatrixTasks.logger().warning(
                    formatSendQueueWarning(playerData.playerName(),
                            playerData.clientVersion().getReleaseName()));
            schedulePlayerCallback(getPlayerUnchecked(playerData),
                    () -> kickForSendQueueTimeout(this));
        }
    }

    public final CheckState currentWork() {
        CheckState pending = pendingWork.get();
        if (pending != null) {
            return pending;
        }
        CheckState created = createTimedWork();
        pendingWork.compareAndSet(null, created);
        return currentWork();
    }

    private final CheckState createTimedWork() {
        int firstId = clock.nextId();
        int secondId = clock.nextId();
        CheckState work = new CheckState(firstId, secondId);
        transmit(work.primaryValue());
        return work;
    }

    public final CheckState workWithCallbacks(Function0<?> first,Function0<?> second) {
        CheckState work=currentWork();
        registerCallback(work.primaryValue(),first);
        registerCallback(work.secondaryValue(),second);
        return work;
    }

    private final void registerCallback(int id, Function0<?> callback) {
        Integer transactionId = Integer.valueOf(id);
        if (waitingTransactionIds.contains(transactionId)) {
            callback.invoke();
            return;
        }
        Queue<Function0<?>> callbacks = transactionCallbacks.compute(transactionId,
                (key, current) -> (Queue<Function0<?>>) appendCallback(callback, key, current));
        if (!waitingTransactionIds.contains(transactionId) || callbacks == null) {
            return;
        }
        if (transactionCallbacks.remove(transactionId, callbacks)) {
            PingManagerSupport.invokeQueuedAction(callbacks);
        }
    }

    private static Queue<?> appendCallback(Function0<?> callback,Integer key,Queue<?> current) {
        @SuppressWarnings("unchecked") Queue<Object> callbacks=current==null
                ?(Queue<Object>)new ConcurrentLinkedQueue<>():(Queue<Object>)current;
        callbacks.add(callback); return callbacks;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static Queue<?> invokeQueueMerge(Function2 fn,Object key,Object value) {
        return (Queue<?>)fn.invoke(key,value);
    }

    private final void onTick() {
        if (elapsedOver(timeoutMark, 100, DurationUnit.MILLISECONDS)) {
            sendNextTransaction();
        }
        checkQueues();
    }

    private final void sendNextTransaction() {
        int id = clock.nextId();
        transmit(id);
    }

    private static Unit kickForTransactionTaskTimeout(TransactionPingQueue q) {
        getPlayerUnchecked(q.playerData).kickPlayer("transaction timeout (too many transactionTasks"); return Unit.INSTANCE;
    }
    private static Unit kickForSendQueueTimeout(TransactionPingQueue q) {
        getPlayerUnchecked(q.playerData).kickPlayer("Transaction timeout (for gonnaSendTransactionQueue)"); return Unit.INSTANCE;
    }
    private static Unit kickForWaitingQueueTimeout(TransactionPingQueue q) {
        getPlayerUnchecked(q.playerData).kickPlayer("Transaction timeout (for waitingTransactionQueue)"); return Unit.INSTANCE;
    }
    private static Player getPlayerUnchecked(PlayerCheckContext context) {
        try { return context.getPlayer(); }
        catch (Throwable failure) { return throwUnchecked(failure); }
    }

    private final void checkQueues() {
        if(PingManagerSupport.isWarningDisabled())return;
        if(recentPingIds.size()>1500 && elapsedOver(transactionTimeoutMark,1,DurationUnit.SECONDS)) {
            transactionTimeoutMark=markNow();
            ServerVersion serverVersion=me.rerere.matrix.Matrix.Companion.serverVersion();
            boolean folia=FoliaSupport.INSTANCE.isUsingFolia();
            Logger logger=MatrixTasks.logger();
            logger.warning(formatWaitingQueueWarning(playerData.playerName(),recentPingIds.size(),
                    completedTransactionCount,playerData.clientVersion().getReleaseName(),
                    playerData.protocolVersion()));
            logger.warning(formatQueueState(firstOrNull(recentPingIds),lastOrNull(recentPingIds),
                    joinValues(trackedTransactions),folia,serverVersion,serverVersion.getProtocolVersion()));
            schedulePlayerCallback(getPlayerUnchecked(playerData),()->kickForWaitingQueueTimeout(this));
        }
        if(transactionCallbacks.size()>5000 && elapsedOver(transactionTimeoutMark,1,DurationUnit.SECONDS)) {
            transactionTimeoutMark=markNow();
            Logger logger=MatrixTasks.logger();
            logger.warning(formatTransactionMapWarning(playerData.playerName(),transactionCallbacks.size()));
            String duration=durationToString(playerData.previousLatency());
            int ping=PlayerPingAccessor.readHandlePing(getPlayerUnchecked(playerData));
            logger.warning(formatPlayerPingState(playerData.playerName(),duration,ping,
                    playerData.clientVersion().getReleaseName(),playerData.protocolVersion()));
            schedulePlayerCallback(getPlayerUnchecked(playerData),()->kickForTransactionTaskTimeout(this));
        }
    }

    private static void schedulePlayerCallback(Player player,Function0<?> callback) {
        FoliaSupport.runSyncForPlayer$default(
                FoliaSupport.INSTANCE,player,callback,null,4,null);
    }
    private static Object firstOrNull(Iterable<?> values) {
        Iterator<?> it=values.iterator(); return it.hasNext()?it.next():null;
    }
    private static Object lastOrNull(Iterable<?> values) {
        Iterator<?> it=values.iterator(); Object last=null; while(it.hasNext())last=it.next(); return last;
    }
    private static String joinValues(Iterable<?> values) {
        StringBuilder out=new StringBuilder(); Iterator<?> it=values.iterator();
        while(it.hasNext()) { if(out.length()>0)out.append(", "); Object value=it.next(); out.append(value==null?"null":value.toString()); }
        return out.toString();
    }

    private static long markNow() {
        try {
            Method method=TimeSource$Monotonic.class.getMethod("markNow-z9LOYto");
            return ((Long)method.invoke(TimeSource$Monotonic.INSTANCE)).longValue();
        } catch(InvocationTargetException failure) { return throwUnchecked(failure.getCause()); }
        catch(ReflectiveOperationException failure) { throw new LinkageError("Monotonic markNow ABI unavailable",failure); }
    }
    private static Object boxTimeMark(long value) {
        return invokeKotlinValue(TimeSource$Monotonic$ValueTimeMark.class,"box-impl",new Class<?>[]{long.class},null,Long.valueOf(value));
    }
    private static long unboxTimeMark(Object value) {
        return ((Long)invokeKotlinValue(value.getClass(),"unbox-impl",new Class<?>[0],value)).longValue();
    }
    private static boolean elapsedOver(long mark,int amount,DurationUnit unit) {
        long elapsed=((Long)invokeKotlinValue(TimeSource$Monotonic$ValueTimeMark.class,"elapsedNow-UwyO8pc",
                new Class<?>[]{long.class},null,Long.valueOf(mark))).longValue();
        Object durationCompanion=Duration.Companion;
        long limit=DurationKt.toDuration(amount,unit);
        int comparison=((Integer)invokeKotlinValue(Duration.class,"compareTo-LRDsOJo",
                new Class<?>[]{long.class,long.class},null,Long.valueOf(elapsed),Long.valueOf(limit))).intValue();
        return comparison>0;
    }
    private static String durationToString(long value) {
        return (String)invokeKotlinValue(Duration.class,"toString-impl",new Class<?>[]{long.class},null,Long.valueOf(value));
    }
    private static Object invokeKotlinValue(Class<?> owner,String methodName,Class<?>[] parameterTypes,Object receiver,Object... args) {
        try { return owner.getDeclaredMethod(methodName,parameterTypes).invoke(receiver,args); }
        catch(InvocationTargetException failure) { return throwUnchecked(failure.getCause()); }
        catch(ReflectiveOperationException failure) {
            throw new LinkageError("Kotlin value-class ABI unavailable: "+owner.getName()+"."+methodName,failure);
        }
    }
    @SuppressWarnings("unchecked")
    private static <T,E extends Throwable>T throwUnchecked(Throwable failure)throws E { throw (E)failure; }

    public final void setNotifications(boolean enabled) {
        notificationsEnabled=enabled;
    }
    public final boolean notificationsEnabled() { return notificationsEnabled; }

    public final void periodicMaintenance() { onTick(); }

    public static void setGloballyEnabled(boolean value) { globallyEnabled=value; }
    public static boolean isGloballyEnabled() { return globallyEnabled; }
    public static boolean isGloballyDisabled() {
        return !isGloballyEnabled();
    }
}


