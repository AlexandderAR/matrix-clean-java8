import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

/** Builds a compile-only Spigot namespace view using the SDK's own Tiny mappings. */
public final class DeriveSpigotCompileSdk {
    private DeriveSpigotCompileSdk() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: <paper-sdk.jar> <mapping-entry> <spigot-sdk.jar>");
        }
        JarFile input = new JarFile(args[0]);
        Map<String, String> remappings = readMappings(input, args[1]);
        if (remappings.isEmpty()) throw new IllegalStateException("SDK mapping contained no class mappings");

        JarOutputStream output = new JarOutputStream(new java.io.FileOutputStream(args[2]));
        try {
            java.util.Enumeration<JarEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (isInvalidatedSignature(name)) continue;
                byte[] bytes;
                try (java.io.InputStream stream = input.getInputStream(entry)) {
                    bytes = stream.readAllBytes();
                }
                String outputName = name;
                if (name.endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    ClassVisitor remapper = new ClassRemapper(writer, new SimpleRemapper(remappings));
                    reader.accept(remapper, 0);
                    bytes = writer.toByteArray();
                    outputName = new ClassReader(bytes).getClassName() + ".class";
                }
                ZipEntry copied = new ZipEntry(outputName);
                copied.setTime(entry.getTime());
                output.putNextEntry(copied);
                output.write(bytes);
                output.closeEntry();
            }
        } finally {
            output.close();
            input.close();
        }
    }

    private static Map<String, String> readMappings(JarFile jar, String entryName) throws Exception {
        JarEntry entry = jar.getJarEntry(entryName);
        if (entry == null) throw new IllegalStateException("SDK is missing official mapping entry " + entryName);
        Map<String, String> mappings = new HashMap<String, String>();
        String owner = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                jar.getInputStream(entry), java.nio.charset.StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.startsWith("tiny\t2\t0\tmojang+yarn\tspigot")) {
                throw new IllegalStateException("Unexpected Tiny namespaces in " + entryName + ": " + header);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t", -1);
                if (parts.length >= 3 && "c".equals(parts[0])) {
                    owner = parts[1];
                    mappings.put(owner, parts[2]);
                } else if (owner != null && parts.length >= 5 && "".equals(parts[0])) {
                    String descriptor = parts[2];
                    if ("m".equals(parts[1])) {
                        mappings.put(owner + "." + parts[3] + descriptor, parts[4]);
                    } else if ("f".equals(parts[1])) {
                        mappings.put(owner + "." + parts[3], parts[4]);
                    }
                }
            }
        }
        return mappings;
    }

    private static boolean isInvalidatedSignature(String name) {
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        return name.startsWith("META-INF/") && (upper.endsWith(".SF") || upper.endsWith(".RSA")
                || upper.endsWith(".DSA") || upper.equals("META-INF/INDEX.LIST"));
    }
}
