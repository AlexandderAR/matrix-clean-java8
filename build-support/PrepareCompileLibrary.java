import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Repairs compiler metadata only. The runtime dependency remains byte-for-byte unchanged. */
public final class PrepareCompileLibrary {
    private static String[] strings;
    private static int removed;

    public static void main(String[] args) throws IOException {
        Path output = Paths.get(args[1]);
        Files.createDirectories(output.getParent());
        int changed = 0;
        try (ZipFile source = new ZipFile(args[0]);
             ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(output))) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                byte[] bytes;
                try (InputStream input = source.getInputStream(entry)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] block = new byte[8192];
                    int count;
                    while ((count = input.read(block)) != -1) buffer.write(block, 0, count);
                    bytes = buffer.toByteArray();
                }
                if (entry.getName().endsWith(".class")) {
                    byte[] prepared = prepare(bytes);
                    if (!Arrays.equals(bytes, prepared)) changed++;
                    bytes = prepared;
                }
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTime(0);
                target.putNextEntry(copy);
                target.write(bytes);
                target.closeEntry();
            }
        }
        System.out.println("Compile-only enum metadata: " + changed + " classes, " + removed + " attributes removed.");
    }

    private static byte[] prepare(byte[] bytes) throws IOException {
        ByteArrayInputStream raw = new ByteArrayInputStream(bytes);
        DataInputStream input = new DataInputStream(raw);
        if (input.readInt() != 0xCAFEBABE) throw new IOException("Invalid class file");
        input.readUnsignedShort(); input.readUnsignedShort();
        int count = input.readUnsignedShort();
        strings = new String[count];
        for (int i = 1; i < count; i++) {
            int tag = input.readUnsignedByte();
            switch (tag) {
                case 1: strings[i] = input.readUTF(); break;
                case 3: case 4: input.readInt(); break;
                case 5: case 6: input.readLong(); i++; break;
                case 7: case 8: case 16: case 19: case 20: input.readUnsignedShort(); break;
                case 9: case 10: case 11: case 12: case 17: case 18: input.readInt(); break;
                case 15: input.readUnsignedByte(); input.readUnsignedShort(); break;
                default: throw new IOException("Unknown constant pool tag: " + tag);
            }
        }
        boolean isEnum = (input.readUnsignedShort() & 0x4000) != 0;
        if (!isEnum) return bytes;
        input.readUnsignedShort(); input.readUnsignedShort();
        int interfaces = input.readUnsignedShort();
        for (int i = 0; i < interfaces; i++) input.readUnsignedShort();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytes.length);
        DataOutputStream output = new DataOutputStream(buffer);
        output.write(bytes, 0, bytes.length - raw.available());
        copyMembers(input, output, false);
        copyMembers(input, output, true);
        // Class-level attributes, including signatures and bootstrap data, are untouched.
        output.write(bytes, bytes.length - raw.available(), raw.available());
        return buffer.toByteArray();
    }

    private static void copyMembers(DataInputStream input, DataOutputStream output, boolean methods)
            throws IOException {
        int members = input.readUnsignedShort();
        output.writeShort(members);
        for (int i = 0; i < members; i++) {
            int access = input.readUnsignedShort();
            int name = input.readUnsignedShort();
            int descriptor = input.readUnsignedShort();
            output.writeShort(access); output.writeShort(name); output.writeShort(descriptor);
            int count = input.readUnsignedShort();
            int kept = 0;
            ByteArrayOutputStream attributes = new ByteArrayOutputStream();
            DataOutputStream destination = new DataOutputStream(attributes);
            for (int j = 0; j < count; j++) {
                int attributeName = input.readUnsignedShort();
                int length = input.readInt();
                if (length < 0) throw new IOException("Oversized attribute");
                byte[] value = new byte[length];
                input.readFully(value);
                String attribute = strings[attributeName];
                boolean parameterAnnotation = "RuntimeVisibleParameterAnnotations".equals(attribute)
                        || "RuntimeInvisibleParameterAnnotations".equals(attribute);
                if (methods && "<init>".equals(strings[name]) && parameterAnnotation) {
                    removed++;
                    continue;
                }
                kept++;
                destination.writeShort(attributeName); destination.writeInt(length); destination.write(value);
            }
            output.writeShort(kept); output.write(attributes.toByteArray());
        }
    }
}
