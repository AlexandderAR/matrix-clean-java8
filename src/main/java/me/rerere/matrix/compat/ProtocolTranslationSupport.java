package me.rerere.matrix.compat;

import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.protocolsupport.ProtocolSupportUtil;
import me.rerere.matrix.thirdparty.io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;

public class ProtocolTranslationSupport {
    private static boolean translationAvailable;
    public ProtocolTranslationSupport() { }
    public static void initialize() {
        boolean protocolSupportAvailable = ProtocolSupportUtil.isAvailable();
        translationAvailable = protocolSupportAvailable || ViaVersionUtil.isAvailable();
    }

    public static boolean isAvailable() {
        return translationAvailable;
    }

}
