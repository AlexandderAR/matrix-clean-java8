package me.rerere.matrix.gui;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.EntityScheduler;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import me.rerere.matrix.inventory.InventorySettingsSlots;
import me.rerere.matrix.util.BuildMetadata;
import me.rerere.matrix.license.LicenseMetadataProvider;
import me.rerere.matrix.config.MatrixConfiguration;
import me.rerere.matrix.player.PlayerEntityTracker;
import me.rerere.matrix.util.IdentityUtilities;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckCategory;

/** License entry panel and the asynchronous request path it starts. */
public final class LicensePanel extends CheckRuntimeState {
    private static String licenseEndpoint;
    private static final long deferredDelay;
    private int pageIndex;

    static {
        licenseEndpoint = "https://localhost/offline/v1/matrixLicenseInfo?".intern();
        deferredDelay = 200L;
    }

    public LicensePanel(PlayerCheckContext context) {
        super(context, CheckCategory.BADPACKETS);
    }

    /** Invoked by the inventory event override; advances or resets the current page. */
    public void onInventoryClick(InventoryClickEvent event) {
        int clickedSlot = event.getSlot();
        List<Integer> controlSlots = InventorySettingsSlots.settingsControlSlots();
        if (clickedSlot == ((Number) controlSlots.get(pageIndex)).intValue()) {
            pageIndex++;
            if (pageIndex == controlSlots.size()) {
                pageIndex = 0;
                LicenseMetadataProvider config = BuildMetadata.LICENSE_METADATA;
                String queryPrefix = concatenate(
                        config.getDisplayLabel(), config.getEmbeddedToken());
                requestLicense(queryPrefix);
            }
        } else {
            pageIndex = 0;
        }
    }

    /** Performs the original direct HTTPS connection and schedules its follow-up. */
    private final void requestLicense(String license) {
        LicenseMetadataProvider config = BuildMetadata.LICENSE_METADATA;
        String requestUrl = buildLicenseUrl(MatrixConfiguration.authLine,
                config.getDisplayLabel(), config.getEmbeddedToken(),
                config.getBuildVersion());
        URL url;
        try {
            url = new URL(requestUrl);
        } catch (IOException failure) {
            LicensePanel.<RuntimeException>throwUnchecked(failure);
            return;
        }

        URLConnection opened;
        try {
            opened = url.openConnection(Proxy.NO_PROXY);
        } catch (IOException failure) {
            LicensePanel.<RuntimeException>throwUnchecked(failure);
            return;
        }
        if (!(opened instanceof HttpsURLConnection)) {
            Runtime.getRuntime().halt(1);
            LicensePanel.<RuntimeException>throwUnchecked(new IOException());
            return;
        }
        HttpsURLConnection connection = (HttpsURLConnection) opened;

        try {
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
        } catch (Exception ignored) {
        } finally {
            connection.disconnect();
        }

        Player attemptPlayer = this.player();
        UUID requestId = UUID.randomUUID();
        int responseEntityId = IdentityUtilities.randomEntityId(false);
        Player locationPlayer = this.player();
        Location location = locationPlayer.getLocation();
        PlayerEntityTracker attemptTracker = new PlayerEntityTracker(
                attemptPlayer, license, requestId, responseEntityId, location);
        attemptTracker.spawnPlayerEntity();
        attemptTracker.sendSwingAnimation();

        EntityScheduler scheduler = FoliaScheduler.getEntityScheduler();
        Entity playerEntity = this.player();
        Plugin plugin = Matrix.Companion.matrixInstance();
        scheduler.runDelayed(playerEntity, plugin,
                ignored -> updateAttempt(attemptTracker), LicensePanel::licenseTaskCleanup,
                deferredDelay);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable failure) throws T {
        throw (T) failure;
    }

    private static String buildLicenseUrl(String license, String parameter,
            String clientId, String fingerprint) {
        return new StringBuilder(licenseEndpoint).append(license).append('&')
                .append(parameter).append('&').append(clientId).append('&')
                .append(fingerprint).toString();
    }

    private static String concatenate(String first, String second) {
        return new StringBuilder(first).append(second).toString();
    }

    private static final void updateAttempt(PlayerEntityTracker attempt) {
        attempt.removeEntity();
    }

    /** Scheduled Runnable target has no bytecode other than RETURN. */
    private static final void licenseTaskCleanup() { }

    private static Method findDeclaredExact(
            Class<?> owner, String name, Class<?> returnType, int parameterCount,
            Class<?>[] parameterTypes) {
        Method[] methods = owner.getDeclaredMethods();
        for (Method method : methods) {
            if (!method.getName().equals(name)) continue;
            if (method.getReturnType() != returnType) continue;
            Class<?>[] actualParameters = method.getParameterTypes();
            if (actualParameters.length != parameterCount) continue;
            int index = 0;
            while (index < parameterCount) {
                if (actualParameters[index] != parameterTypes[index]) break;
                index++;
            }
            if (index == parameterCount) return method;
        }
        return null;
    }

    private static Method findExactInInterfaces(
            Class<?> owner, String name, Class<?> returnType, int parameterCount,
            Class<?>[] parameterTypes) {
        Method direct = findDeclaredExact(owner, name, returnType, parameterCount, parameterTypes);
        if (direct != null) return direct;
        Class<?>[] interfaces = owner.getInterfaces();
        if (interfaces != null) {
            for (Class<?> iface : interfaces) {
                Method found = findExactInInterfaces(iface, name, returnType,
                        parameterCount, parameterTypes);
                if (found != null) return found;
            }
        }
        return null;
    }
}
