package com.lubesoft.db;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates all application tables and seeds initial data on first run.
 */
public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            createTables(conn);
            seedAdminUser(conn);
            seedLicenseInfo(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('ADMIN','MANAGER','TECHNICIAN')),
                    active INTEGER NOT NULL DEFAULT 1
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    company TEXT,
                    credit_limit REAL NOT NULL DEFAULT 0,
                    current_balance REAL NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS vehicles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER REFERENCES customers(id),
                    license_plate TEXT UNIQUE,
                    vin TEXT,
                    make TEXT,
                    model TEXT,
                    year INTEGER,
                    mileage INTEGER,
                    oil_grade TEXT,
                    notes TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sku TEXT UNIQUE,
                    name TEXT NOT NULL,
                    category TEXT,
                    unit TEXT,
                    sell_price REAL NOT NULL DEFAULT 0,
                    cost_price REAL NOT NULL DEFAULT 0,
                    stock_qty REAL NOT NULL DEFAULT 0,
                    min_stock REAL NOT NULL DEFAULT 0,
                    max_stock REAL NOT NULL DEFAULT 0,
                    barcode TEXT UNIQUE,
                    is_bulk_oil INTEGER NOT NULL DEFAULT 0
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS invoices (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_number TEXT NOT NULL UNIQUE,
                    customer_id INTEGER REFERENCES customers(id),
                    vehicle_id INTEGER REFERENCES vehicles(id),
                    technician_id INTEGER REFERENCES users(id),
                    status TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','WIP','HELD','PAID','VOID')),
                    payment_method TEXT CHECK(payment_method IN ('CASH','CREDIT','DIGITAL','SPLIT')),
                    subtotal REAL NOT NULL DEFAULT 0,
                    tax REAL NOT NULL DEFAULT 0,
                    discount REAL NOT NULL DEFAULT 0,
                    total REAL NOT NULL DEFAULT 0,
                    paid_amount REAL NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    completed_at TEXT,
                    notes TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS invoice_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_id INTEGER NOT NULL REFERENCES invoices(id),
                    product_id INTEGER REFERENCES products(id),
                    description TEXT,
                    qty REAL NOT NULL DEFAULT 1,
                    unit_price REAL NOT NULL DEFAULT 0,
                    cost_price REAL NOT NULL DEFAULT 0,
                    total REAL NOT NULL DEFAULT 0
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS service_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    vehicle_id INTEGER NOT NULL REFERENCES vehicles(id),
                    invoice_id INTEGER REFERENCES invoices(id),
                    mileage INTEGER,
                    oil_grade TEXT,
                    service_date TEXT NOT NULL DEFAULT (date('now')),
                    notes TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS dvi_checklist (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_id INTEGER NOT NULL REFERENCES invoices(id),
                    air_filter_status TEXT,
                    wipers_status TEXT,
                    fluid_levels_status TEXT,
                    tire_status TEXT,
                    notes TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL REFERENCES products(id),
                    type TEXT NOT NULL CHECK(type IN ('SALE','PURCHASE','ADJUSTMENT','WASTE')),
                    qty_change REAL NOT NULL,
                    reference_id INTEGER,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    notes TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    status TEXT NOT NULL DEFAULT 'DRAFT',
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    notes TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS po_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    po_id INTEGER NOT NULL REFERENCES purchase_orders(id),
                    product_id INTEGER NOT NULL REFERENCES products(id),
                    qty_ordered REAL NOT NULL DEFAULT 0,
                    unit_cost REAL NOT NULL DEFAULT 0,
                    received_qty REAL NOT NULL DEFAULT 0
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS time_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    clock_in TEXT NOT NULL,
                    clock_out TEXT,
                    notes TEXT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS license_info (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    machine_id TEXT,
                    license_key TEXT,
                    is_activated INTEGER NOT NULL DEFAULT 0,
                    install_date TEXT NOT NULL DEFAULT (datetime('now')),
                    activated_at TEXT
                )""");
        }
    }

    private static void seedAdminUser(Connection conn) throws SQLException {
        // Only insert if no users exist
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.getInt(1) == 0) {
                String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users(username, password_hash, role, active) VALUES(?,?,?,1)")) {
                    ps.setString(1, "admin");
                    ps.setString(2, hash);
                    ps.setString(3, "ADMIN");
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void seedLicenseInfo(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM license_info")) {
            if (rs.getInt(1) == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO license_info(machine_id, is_activated, install_date) VALUES(?,0,datetime('now'))")) {
                    ps.setString(1, "");
                    ps.executeUpdate();
                }
            }
        }
    }
}
