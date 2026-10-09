package me.rerere.matrix.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.util.MacAddressFormatter;

/** Selects a hardware address and site-local addresses from network interfaces. */
public final class NetworkInput {
    private NetworkInput() {
    }

    public static String selectedMacString() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        Iterator<NetworkInterface> iterator = Collections.list(interfaces).iterator();
        NetworkInterface selected = null;
        while (iterator.hasNext()) {
            NetworkInterface candidate = iterator.next();
            if (candidate.getHardwareAddress() != null) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return "";
        }
        return MacAddressFormatter.format(selected.getHardwareAddress());
    }

    public static String selectedSiteLocalAddresses() throws SocketException {
        String addresses = "";
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
            while (interfaceAddresses.hasMoreElements()) {
                InetAddress address = interfaceAddresses.nextElement();
                if (address.isSiteLocalAddress()) {
                    addresses = MatrixApiResponseMessages.concatenate(
                            addresses, address.getHostAddress());
                }
            }
        }
        return addresses;
    }
}
