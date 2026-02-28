package com.lubesoft.service;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.LicenseInfo;
import com.lubesoft.util.AESUtil;
import com.lubesoft.util.HardwareIdUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Handles offline AES-256 licensing with a 7-day trial period.
 */
public class LicenseService {

    private static LicenseService instance;

    private static final String SECRET_SALT = "LubeSoft2024#SecretSalt";
    private static final int TRIAL_DAYS = 7;
    private static final String SYSTEM_DAT_PATH =
            System.getProperty("user.home") + File.separator + ".lubesoft" + File.separator + "system.dat";

    private LicenseService() {}

    public static synchronized LicenseService getInstance() {
        if (instance == null) {
            instance = new LicenseService();
        }
        return instance;
    }

    /**
     * Reads or creates the system.dat file that records the install timestamp.
     * Returns the install date string.
     */
    public String getOrCreateInstallDate() {
        Path path = Path.of(SYSTEM_DAT_PATH);
        if (Files.exists(path)) {
            try {
                String encrypted = Files.readString(path).trim();
                return AESUtil.decrypt(encrypted, SECRET_SALT);
            } catch (Exception e) {
                // Corrupted file - recreate
            }
        }

        // Create new system.dat with current timestamp
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try {
            Files.createDirectories(path.getParent());
            String encrypted = AESUtil.encrypt(now, SECRET_SALT);
            Files.writeString(path, encrypted);
        } catch (Exception e) {
            // Ignore write errors; use current time
        }
        return now;
    }

    public boolean isActivated() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT is_activated FROM license_info LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt("is_activated") == 1;
            }
        } catch (SQLException e) {
            // DB not ready
        }
        return false;
    }

    public boolean isTrialExpired() {
        String installDate = getOrCreateInstallDate();
        try {
            LocalDateTime install = LocalDateTime.parse(installDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long daysSince = ChronoUnit.DAYS.between(install, LocalDateTime.now());
            return daysSince > TRIAL_DAYS;
        } catch (Exception e) {
            return false; // If we can't parse, treat as not expired
        }
    }

    public long getTrialDaysRemaining() {
        String installDate = getOrCreateInstallDate();
        try {
            LocalDateTime install = LocalDateTime.parse(installDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long daysSince = ChronoUnit.DAYS.between(install, LocalDateTime.now());
            return Math.max(0, TRIAL_DAYS - daysSince);
        } catch (Exception e) {
            return TRIAL_DAYS;
        }
    }

    public String generateMachineId() {
        return HardwareIdUtil.getMachineId();
    }

    /**
     * Validates the license key by AES-decrypting it and checking it contains the machine ID.
     */
    public boolean validateLicenseKey(String licenseKey) {
        try {
            String decrypted = AESUtil.decrypt(licenseKey, SECRET_SALT);
            String machineId = generateMachineId();
            return decrypted.contains(machineId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Activates the license: validates the key then persists activation to DB.
     */
    public boolean activate(String licenseKey) {
        if (!validateLicenseKey(licenseKey)) {
            return false;
        }
        String machineId = generateMachineId();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE license_info SET machine_id=?, license_key=?, is_activated=1, activated_at=datetime('now')")) {
            ps.setString(1, machineId);
            ps.setString(2, licenseKey);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Generates a license key for a given machine ID (admin/dev tool).
     */
    public String generateLicenseKeyForMachine(String machineId) {
        try {
            String payload = machineId + "|" + SECRET_SALT;
            return AESUtil.encrypt(payload, SECRET_SALT);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate license key", e);
        }
    }

    public LicenseInfo getLicenseInfo() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM license_info LIMIT 1")) {
            if (rs.next()) {
                LicenseInfo li = new LicenseInfo();
                li.setId(rs.getInt("id"));
                li.setMachineId(rs.getString("machine_id"));
                li.setLicenseKey(rs.getString("license_key"));
                li.setActivated(rs.getInt("is_activated") == 1);
                li.setInstallDate(rs.getString("install_date"));
                li.setActivatedAt(rs.getString("activated_at"));
                return li;
            }
        } catch (SQLException e) {
            // ignore
        }
        return new LicenseInfo();
    }
}
