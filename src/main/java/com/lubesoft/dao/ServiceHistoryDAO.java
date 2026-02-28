package com.lubesoft.dao;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.ServiceHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceHistoryDAO {

    public ServiceHistory save(ServiceHistory sh) throws SQLException {
        String sql = "INSERT INTO service_history(vehicle_id,invoice_id,mileage,oil_grade,service_date,notes) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sh.getVehicleId());
            if (sh.getInvoiceId() > 0) {
                ps.setInt(2, sh.getInvoiceId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, sh.getMileage());
            ps.setString(4, sh.getOilGrade());
            ps.setString(5, sh.getServiceDate() != null ? sh.getServiceDate() : "date('now')");
            ps.setString(6, sh.getNotes());
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) sh.setId(keys.getInt(1));
            }
        }
        return sh;
    }

    public List<ServiceHistory> findByVehicleId(int vehicleId) throws SQLException {
        List<ServiceHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM service_history WHERE vehicle_id=? ORDER BY service_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private ServiceHistory mapRow(ResultSet rs) throws SQLException {
        ServiceHistory sh = new ServiceHistory();
        sh.setId(rs.getInt("id"));
        sh.setVehicleId(rs.getInt("vehicle_id"));
        sh.setInvoiceId(rs.getInt("invoice_id"));
        sh.setMileage(rs.getInt("mileage"));
        sh.setOilGrade(rs.getString("oil_grade"));
        sh.setServiceDate(rs.getString("service_date"));
        sh.setNotes(rs.getString("notes"));
        return sh;
    }
}
