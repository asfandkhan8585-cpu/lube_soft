package com.lubesoft.dao;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleDAO {

    public Optional<Vehicle> findById(int id) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Vehicle> findByLicensePlate(String plate) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE license_plate = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plate.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Vehicle> findByCustomerId(int customerId) throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM vehicles WHERE customer_id = ? ORDER BY year DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Vehicle save(Vehicle v) throws SQLException {
        if (v.getId() == 0) {
            return insert(v);
        } else {
            update(v);
            return v;
        }
    }

    private Vehicle insert(Vehicle v) throws SQLException {
        String sql = "INSERT INTO vehicles(customer_id,license_plate,vin,make,model,year,mileage,oil_grade,notes) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, v.getCustomerId());
            ps.setString(2, v.getLicensePlate() != null ? v.getLicensePlate().toUpperCase().trim() : null);
            ps.setString(3, v.getVin());
            ps.setString(4, v.getMake());
            ps.setString(5, v.getModel());
            ps.setInt(6, v.getYear());
            ps.setInt(7, v.getMileage());
            ps.setString(8, v.getOilGrade());
            ps.setString(9, v.getNotes());
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) v.setId(keys.getInt(1));
            }
        }
        return v;
    }

    private void update(Vehicle v) throws SQLException {
        String sql = "UPDATE vehicles SET customer_id=?,license_plate=?,vin=?,make=?,model=?,year=?,mileage=?,oil_grade=?,notes=? WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, v.getCustomerId());
            ps.setString(2, v.getLicensePlate() != null ? v.getLicensePlate().toUpperCase().trim() : null);
            ps.setString(3, v.getVin());
            ps.setString(4, v.getMake());
            ps.setString(5, v.getModel());
            ps.setInt(6, v.getYear());
            ps.setInt(7, v.getMileage());
            ps.setString(8, v.getOilGrade());
            ps.setString(9, v.getNotes());
            ps.setInt(10, v.getId());
            ps.executeUpdate();
        }
    }

    public void updateMileage(int vehicleId, int mileage) throws SQLException {
        String sql = "UPDATE vehicles SET mileage=? WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mileage);
            ps.setInt(2, vehicleId);
            ps.executeUpdate();
        }
    }

    private Vehicle mapRow(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("id"));
        v.setCustomerId(rs.getInt("customer_id"));
        v.setLicensePlate(rs.getString("license_plate"));
        v.setVin(rs.getString("vin"));
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setYear(rs.getInt("year"));
        v.setMileage(rs.getInt("mileage"));
        v.setOilGrade(rs.getString("oil_grade"));
        v.setNotes(rs.getString("notes"));
        return v;
    }
}
