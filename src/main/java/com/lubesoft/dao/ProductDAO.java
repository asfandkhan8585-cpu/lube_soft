package com.lubesoft.dao;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    public Optional<Product> findById(int id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findByBarcode(String barcode) throws SQLException {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findBySku(String sku) throws SQLException {
        String sql = "SELECT * FROM products WHERE sku = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Product> search(String query) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE name LIKE ? OR sku LIKE ? OR barcode LIKE ? OR category LIKE ? ORDER BY name";
        String pattern = "%" + query + "%";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Product> findLowStock() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE stock_qty <= min_stock ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Product save(Product p) throws SQLException {
        if (p.getId() == 0) {
            return insert(p);
        } else {
            update(p);
            return p;
        }
    }

    private Product insert(Product p) throws SQLException {
        String sql = "INSERT INTO products(sku,name,category,unit,sell_price,cost_price,stock_qty,min_stock,max_stock,barcode,is_bulk_oil) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getUnit());
            ps.setDouble(5, p.getSellPrice());
            ps.setDouble(6, p.getCostPrice());
            ps.setDouble(7, p.getStockQty());
            ps.setDouble(8, p.getMinStock());
            ps.setDouble(9, p.getMaxStock());
            ps.setString(10, p.getBarcode());
            ps.setInt(11, p.isBulkOil() ? 1 : 0);
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        }
        return p;
    }

    private void update(Product p) throws SQLException {
        String sql = "UPDATE products SET sku=?,name=?,category=?,unit=?,sell_price=?,cost_price=?,stock_qty=?,min_stock=?,max_stock=?,barcode=?,is_bulk_oil=? WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getUnit());
            ps.setDouble(5, p.getSellPrice());
            ps.setDouble(6, p.getCostPrice());
            ps.setDouble(7, p.getStockQty());
            ps.setDouble(8, p.getMinStock());
            ps.setDouble(9, p.getMaxStock());
            ps.setString(10, p.getBarcode());
            ps.setInt(11, p.isBulkOil() ? 1 : 0);
            ps.setInt(12, p.getId());
            ps.executeUpdate();
        }
    }

    /** Update stock quantity directly (use InventoryDAO for tracked transactions). */
    public void updateStock(Connection conn, int productId, double newQty) throws SQLException {
        String sql = "UPDATE products SET stock_qty=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newQty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setUnit(rs.getString("unit"));
        p.setSellPrice(rs.getDouble("sell_price"));
        p.setCostPrice(rs.getDouble("cost_price"));
        p.setStockQty(rs.getDouble("stock_qty"));
        p.setMinStock(rs.getDouble("min_stock"));
        p.setMaxStock(rs.getDouble("max_stock"));
        p.setBarcode(rs.getString("barcode"));
        p.setBulkOil(rs.getInt("is_bulk_oil") == 1);
        return p;
    }
}
