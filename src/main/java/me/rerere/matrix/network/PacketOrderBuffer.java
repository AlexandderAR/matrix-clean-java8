package me.rerere.matrix.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.util.MutableValue;
import me.rerere.matrix.network.packet.PacketSnapshot;
import me.rerere.matrix.Matrix;

/** Fixed-capacity packet-order history, with the plugin jar's original MD5 helpers. */
public final class PacketOrderBuffer {
    private final int capacity;
    private PacketSnapshot[] entries;

    public PacketOrderBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("(PacketOrder) capacity can not be zero or negative!");
        }
        this.capacity = capacity;
        this.entries = new PacketSnapshot[capacity];
    }

    public void addPacket(PacketTypeCommon packetType, long packetOrder, MutableValue metadata) {
        PacketSnapshot entry = new PacketSnapshot(packetType, packetOrder, metadata);
        for (int index = 0; index < this.capacity - 1; index++) {
            if (index == 0) {
                this.entries[0] = null;
            }
            this.entries[index] = this.entries[index + 1];
        }
        this.entries[this.capacity - 1] = entry;
    }

    public int capacity() {
        return this.capacity;
    }

    public boolean hasAnyPacket() {
        return this.entries[0] != null;
    }

    /** Returns null for indices at or above capacity; negative indices retain the original array failure. */
    public PacketSnapshot packetAt(int index) {
        if (index < this.capacity) {
            return this.entries[index];
        }
        return null;
    }

    public static String fileMd5Hex(File file)
            throws IOException, NoSuchAlgorithmException {
        byte[] digest = fileMd5Digest(file);
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) {
            hex.append(Integer.toString((value & 255) + 256, 16).substring(1));
        }
        return hex.toString();
    }

    private static byte[] fileMd5Digest(File file)
            throws IOException, NoSuchAlgorithmException {
        FileInputStream input = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        MessageDigest digest = MessageDigest.getInstance("MD5");
        int count;
        do {
            count = input.read(buffer);
            if (count > 0) {
                digest.update(buffer, 0, count);
            }
        } while (count != -1);
        input.close();

        return digest.digest();
    }

    public static String pluginFileMd5() {
        try {
            Class<?> pluginLoaderClass = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
            Field pluginFileField = pluginLoaderClass.getDeclaredField("file");
            pluginFileField.setAccessible(true);
            File pluginFile = (File) pluginFileField.get(Matrix.pluginClassLoader);
            return fileMd5Hex(pluginFile);
        } catch (Exception failure) {
            failure.printStackTrace();
            return "???";
        }
    }
}
