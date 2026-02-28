package com.lubesoft.dao;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.InventoryTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    public InventoryTransaction record(InventoryTransaction tx) throws SQLException {
        String sql = "INSERT INTO inventory_transactions(product_id,type,qty_change,reference_id,notes) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tx.getProductId());
            ps.setString(2, tx.getType());
            ps.setDouble(3, tx.getQtyChange());
            if (tx.getReferenceId() > 0) {
                ps.setInt(4, tx.getReferenceId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, tx.getNotes());
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) tx.setId(keys.getInt(1));
            }
        }
        return tx;
    }

    public List<InventoryTransaction> findByProduct(int productId) throws SQLException {
        List<InventoryTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory_transactions WHERE product_id=? ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<InventoryTransaction> findByDateRange(String from, String to) throws SQLException {
        List<InventoryTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory_transactions WHERE date(created_at) BETWEEN date(?) AND date(?) ORDER BY created_at DESC";
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

    private InventoryTransaction mapRow(ResultSet rs) throws SQLException {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setId(rs.getInt("id"));
        tx.setProductId(rs.getInt("product_id"));
        tx.setType(rs.getString("type"));
        tx.setQtyChange(rs.getDouble("qty_change"));
        tx.setReferenceId(rs.getInt("reference_id"));
        tx.setCreatedAt(rs.getString("created_at"));
        tx.setNotes(rs.getString("notes"));
        return tx;
    }
}
