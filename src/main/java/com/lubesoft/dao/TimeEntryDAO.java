package com.lubesoft.dao;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.TimeEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimeEntryDAO {

    public TimeEntry clockIn(int userId, String notes) throws SQLException {
        String sql = "INSERT INTO time_entries(user_id,clock_in,notes) VALUES(?,datetime('now'),?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, notes);
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Clock-in failed");
    }

    public void clockOut(int entryId) throws SQLException {
        String sql = "UPDATE time_entries SET clock_out=datetime('now') WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entryId);
            ps.executeUpdate();
        }
    }

    public Optional<TimeEntry> findActiveEntry(int userId) throws SQLException {
        String sql = "SELECT * FROM time_entries WHERE user_id=? AND clock_out IS NULL ORDER BY clock_in DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<TimeEntry> findById(int id) throws SQLException {
        String sql = "SELECT * FROM time_entries WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<TimeEntry> findByUser(int userId) throws SQLException {
        List<TimeEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM time_entries WHERE user_id=? ORDER BY clock_in DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<TimeEntry> findByDateRange(String from, String to) throws SQLException {
        List<TimeEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM time_entries WHERE date(clock_in) BETWEEN date(?) AND date(?) ORDER BY clock_in DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from);
            ps.setString(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private TimeEntry mapRow(ResultSet rs) throws SQLException {
        TimeEntry te = new TimeEntry();
        te.setId(rs.getInt("id"));
        te.setUserId(rs.getInt("user_id"));
        te.setClockIn(rs.getString("clock_in"));
        te.setClockOut(rs.getString("clock_out"));
        te.setNotes(rs.getString("notes"));
        return te;
    }
}
