package me.rerere.matrix.license;

public final class HardwareAddressFormatter {
    private HardwareAddressFormatter() {}

    public static String format(byte[] address) {
        StringBuilder result = new StringBuilder();
        if (address == null) throw new NullPointerException("\"ARRAYLENGTH npe\" on -1");
        for (int index = 0; index < address.length; index++) {
            String separatorAfterByte = index < address.length - 1 ? "-" : "";
            result.append(String.format("%02X%s", Byte.valueOf(address[index]), separatorAfterByte));
        }
        return result.toString();
    }
}
