package com.lubesoft.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HexFormat;

/**
 * Reads a stable hardware identifier for license binding.
 * Primary: /sys/class/dmi/id/product_uuid (Linux)
 * Fallback: SHA-256 of (MAC address + hostname)
 */
public class HardwareIdUtil {

    private static final String DMI_UUID_PATH = "/sys/class/dmi/id/product_uuid";

    public static String getMachineId() {
        // Try DMI product UUID (Linux)
        try {
            Path uuidPath = Path.of(DMI_UUID_PATH);
            if (Files.exists(uuidPath)) {
                String uuid = Files.readString(uuidPath).trim();
                if (!uuid.isEmpty()) {
                    return uuid.toLowerCase();
                }
            }
        } catch (Exception ignored) {
            // Fall through to MAC-based fallback
        }

        // Fallback: hash of MAC + hostname
        return macAndHostFallback();
    }

    private static String macAndHostFallback() {
        try {
            StringBuilder sb = new StringBuilder();

            // Collect MAC addresses from all non-loopback interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (!ni.isLoopback() && ni.isUp()) {
                        byte[] mac = ni.getHardwareAddress();
                        if (mac != null) {
                            for (byte b : mac) {
                                sb.append(String.format("%02x", b));
                            }
                            break; // Use first valid MAC
                        }
                    }
                }
            }

            // Append hostname
            sb.append(InetAddress.getLocalHost().getHostName());

            // Hash the combined string
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // Last resort: use a fixed string (no hardware binding)
            return "unknown-machine-id";
        }
    }
}
