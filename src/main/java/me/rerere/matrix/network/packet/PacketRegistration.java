package me.rerere.matrix.network.packet;

import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.RecentElements;

/** Packet registration state and its registration callback. */
public final class PacketRegistration {
    private static final String registrationLabelSuffix;
    private final RecentElements<PacketWrapperSnapshot> registry;

    public PacketRegistration(int capacity) { registry = new RecentElements<>(capacity); }

    public final void registerIfAccepted(
            PacketWrapper packet,
            Function1 shouldRegister,
            Function1 onAccepted) {
        final boolean accepted;
        try {
            accepted = ((Boolean) shouldRegister.invoke(packet)).booleanValue();
        } catch (LicenseResponseException failure) {
            throw failure;
        }
        if (!accepted) return;

        PacketWrapperSnapshot entry = new PacketWrapperSnapshot(
                packet, System.currentTimeMillis());
        registry.add(entry);
        if (registry.isAtCapacity()) {
            onAccepted.invoke(CollectionsKt.toList(
                    (Iterable<?>) registry));
        }
    }

    private static String registrationLabel(String name, long timestamp) {
        return new StringBuilder(name).append("(").append(timestamp)
                .append(registrationLabelSuffix).toString();
    }

    public final void clear() { registry.clear(); }
    public final RecentElements<PacketWrapperSnapshot> registry() { return registry; }

    static {
        registrationLabelSuffix = ") ";
    }
}
