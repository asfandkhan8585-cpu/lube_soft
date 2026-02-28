package com.lubesoft.db;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseManagerTest {

    @Test
    @Order(1)
    void testGetConnectionReturnsValidConnection() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
        }
    }

    @Test
    @Order(2)
    void testWalModeIsEnabled() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             var st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA journal_mode")) {
            assertTrue(rs.next());
            assertEquals("wal", rs.getString(1).toLowerCase());
        }
    }

    @Test
    @Order(3)
    void testInitializerCreatesAllTables() throws SQLException {
        DatabaseInitializer.initialize();

        Set<String> expectedTables = Set.of(
                "users", "customers", "vehicles", "products",
                "invoices", "invoice_items", "service_history",
                "dvi_checklist", "inventory_transactions",
                "purchase_orders", "po_items", "time_entries", "license_info"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        for (String table : expectedTables) {
            assertTrue(actualTables.contains(table), "Table should exist: " + table);
        }
    }

    @Test
    @Order(4)
    void testAdminUserSeeded() throws SQLException {
        DatabaseInitializer.initialize();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             var ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username='admin' AND role='ADMIN'");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "Admin user should exist");
        }
    }

    @Test
    @Order(5)
    void testSingletonPattern() {
        DatabaseManager a = DatabaseManager.getInstance();
        DatabaseManager b = DatabaseManager.getInstance();
        assertSame(a, b, "DatabaseManager should be a singleton");
    }
}
