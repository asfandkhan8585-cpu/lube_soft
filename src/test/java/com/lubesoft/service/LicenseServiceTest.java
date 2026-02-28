package com.lubesoft.service;

import com.lubesoft.db.DatabaseInitializer;
import com.lubesoft.util.AESUtil;
import com.lubesoft.util.HardwareIdUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LicenseServiceTest {

    private static LicenseService licenseService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.initialize();
        licenseService = LicenseService.getInstance();
    }

    @Test
    @Order(1)
    void testMachineIdIsNotEmpty() {
        String machineId = licenseService.generateMachineId();
        assertNotNull(machineId, "Machine ID should not be null");
        assertFalse(machineId.isBlank(), "Machine ID should not be blank");
    }

    @Test
    @Order(2)
    void testMachineIdIsConsistent() {
        // Should return same ID every time
        String id1 = licenseService.generateMachineId();
        String id2 = licenseService.generateMachineId();
        assertEquals(id1, id2, "Machine ID should be consistent across calls");
    }

    @Test
    @Order(3)
    void testHardwareIdUtilDirectly() {
        String machineId = HardwareIdUtil.getMachineId();
        assertNotNull(machineId);
        assertFalse(machineId.isBlank());
    }

    @Test
    @Order(4)
    void testGenerateAndValidateLicenseKey() {
        String machineId = licenseService.generateMachineId();
        String licenseKey = licenseService.generateLicenseKeyForMachine(machineId);

        assertNotNull(licenseKey, "Generated license key should not be null");
        assertFalse(licenseKey.isBlank(), "Generated license key should not be blank");

        boolean valid = licenseService.validateLicenseKey(licenseKey);
        assertTrue(valid, "Generated license key should be valid for this machine");
    }

    @Test
    @Order(5)
    void testInvalidLicenseKeyRejected() {
        boolean valid = licenseService.validateLicenseKey("invalid-key-12345");
        assertFalse(valid, "Invalid key should be rejected");
    }

    @Test
    @Order(6)
    void testAesEncryptDecrypt() throws Exception {
        String salt = "TestSalt#2024";
        String plaintext = "Hello, LubeSoft!";
        String encrypted = AESUtil.encrypt(plaintext, salt);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted, "Encrypted text should differ from plaintext");

        String decrypted = AESUtil.decrypt(encrypted, salt);
        assertEquals(plaintext, decrypted, "Decrypted text should match original");
    }

    @Test
    @Order(7)
    void testTrialDaysRemainingIsNonNegative() {
        long daysRemaining = licenseService.getTrialDaysRemaining();
        assertTrue(daysRemaining >= 0, "Trial days remaining should be >= 0");
    }

    @Test
    @Order(8)
    void testInstallDateIsPersisted() {
        String date1 = licenseService.getOrCreateInstallDate();
        String date2 = licenseService.getOrCreateInstallDate();
        assertEquals(date1, date2, "Install date should be consistent");
    }
}
