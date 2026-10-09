package me.rerere.matrix.network;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Kotlin inline helper retained for callers that need a reified PacketWrapper cast. */
public final class PacketBytes {
    private PacketBytes() {
    }

    public static Object unwrapPacketWrapper(PacketWrapper wrapper) {
        Intrinsics.reifiedOperationMarker(2, "T");
        return wrapper;
    }
}
