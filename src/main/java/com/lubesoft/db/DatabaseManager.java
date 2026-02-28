package com.lubesoft.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database manager. Opens the SQLite database at ~/.lubesoft/lubesoft.db
 * and configures WAL mode for better concurrency.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".lubesoft";
    private static final String DB_PATH = DB_DIR + File.separator + "lubesoft.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private DatabaseManager() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Returns a new connection each call; callers must close it (try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Enable WAL mode and foreign keys
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys=ON");
        }
        return conn;
    }

    public String getDbPath() {
        return DB_PATH;
    }
}
