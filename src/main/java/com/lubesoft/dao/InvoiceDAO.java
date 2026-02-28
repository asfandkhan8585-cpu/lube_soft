package com.lubesoft.dao;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.Invoice;
import com.lubesoft.model.InvoiceItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceDAO {

    public Invoice createInvoice(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices(invoice_number,customer_id,vehicle_id,technician_id,status,subtotal,tax,discount,total,paid_amount,notes) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoice.getInvoiceNumber());
            setNullableInt(ps, 2, invoice.getCustomerId());
            setNullableInt(ps, 3, invoice.getVehicleId());
            setNullableInt(ps, 4, invoice.getTechnicianId());
            ps.setString(5, invoice.getStatus() != null ? invoice.getStatus() : "OPEN");
            ps.setDouble(6, invoice.getSubtotal());
            ps.setDouble(7, invoice.getTax());
            ps.setDouble(8, invoice.getDiscount());
            ps.setDouble(9, invoice.getTotal());
            ps.setDouble(10, invoice.getPaidAmount());
            ps.setString(11, invoice.getNotes());
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) invoice.setId(keys.getInt(1));
            }
        }
        return invoice;
    }

    public Optional<Invoice> findById(int id) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Invoice inv = mapRow(rs);
                    inv.setItems(findItems(id));
                    return Optional.of(inv);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Invoice> findByInvoiceNumber(String number) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE invoice_number = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Invoice inv = mapRow(rs);
                    inv.setItems(findItems(inv.getId()));
                    return Optional.of(inv);
                }
            }
        }
        return Optional.empty();
    }

    public List<Invoice> findByStatus(String status) throws SQLException {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Invoice> findByDateRange(String from, String to) throws SQLException {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE date(created_at) BETWEEN date(?) AND date(?) ORDER BY created_at DESC";
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

    public List<Invoice> findRecentPaid(int limit) throws SQLException {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE status='PAID' ORDER BY completed_at DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void updateStatus(int invoiceId, String status) throws SQLException {
        String sql = "UPDATE invoices SET status=? WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, invoiceId);
            ps.executeUpdate();
        }
    }

    public void updateTotals(int invoiceId, double subtotal, double tax, double discount, double total) throws SQLException {
        String sql = "UPDATE invoices SET subtotal=?,tax=?,discount=?,total=? WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, subtotal);
            ps.setDouble(2, tax);
            ps.setDouble(3, discount);
            ps.setDouble(4, total);
            ps.setInt(5, invoiceId);
            ps.executeUpdate();
        }
    }

    public InvoiceItem addItem(int invoiceId, InvoiceItem item) throws SQLException {
        String sql = "INSERT INTO invoice_items(invoice_id,product_id,description,qty,unit_price,cost_price,total) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            setNullableInt(ps, 2, item.getProductId());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getQty());
            ps.setDouble(5, item.getUnitPrice());
            ps.setDouble(6, item.getCostPrice());
            ps.setDouble(7, item.getTotal());
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
        item.setInvoiceId(invoiceId);
        return item;
    }

    public void removeItem(int itemId) throws SQLException {
        String sql = "DELETE FROM invoice_items WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }

    public void removeAllItems(int invoiceId) throws SQLException {
        String sql = "DELETE FROM invoice_items WHERE invoice_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.executeUpdate();
        }
    }

    public List<InvoiceItem> findItems(int invoiceId) throws SQLException {
        List<InvoiceItem> list = new ArrayList<>();
        String sql = "SELECT * FROM invoice_items WHERE invoice_id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapItemRow(rs));
            }
        }
        return list;
    }

    /**
     * Checkout with a single SQL TRANSACTION: deduct stock, record inventory
     * transactions, update invoice to PAID, record service history if vehicle present.
     */
    public void checkout(int invoiceId, String paymentMethod, double paidAmount,
                         List<InvoiceItem> items) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update invoice to PAID
                String updateInv = "UPDATE invoices SET status='PAID', payment_method=?, paid_amount=?, completed_at=datetime('now') WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateInv)) {
                    ps.setString(1, paymentMethod);
                    ps.setDouble(2, paidAmount);
                    ps.setInt(3, invoiceId);
                    ps.executeUpdate();
                }

                // Deduct stock and record inventory transactions for each item
                for (InvoiceItem item : items) {
                    if (item.getProductId() > 0) {
                        // Deduct stock
                        String deductStock = "UPDATE products SET stock_qty = stock_qty - ? WHERE id=?";
                        try (PreparedStatement ps = conn.prepareStatement(deductStock)) {
                            ps.setDouble(1, item.getQty());
                            ps.setInt(2, item.getProductId());
                            ps.executeUpdate();
                        }
                        // Record inventory transaction
                        String invTx = "INSERT INTO inventory_transactions(product_id,type,qty_change,reference_id,notes) VALUES(?,'SALE',?,?,?)";
                        try (PreparedStatement ps = conn.prepareStatement(invTx)) {
                            ps.setInt(1, item.getProductId());
                            ps.setDouble(2, -item.getQty());
                            ps.setInt(3, invoiceId);
                            ps.setString(4, "Invoice #" + invoiceId);
                            ps.executeUpdate();
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void voidInvoice(int invoiceId, List<InvoiceItem> items) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Restore stock for each item
                for (InvoiceItem item : items) {
                    if (item.getProductId() > 0) {
                        String restoreStock = "UPDATE products SET stock_qty = stock_qty + ? WHERE id=?";
                        try (PreparedStatement ps = conn.prepareStatement(restoreStock)) {
                            ps.setDouble(1, item.getQty());
                            ps.setInt(2, item.getProductId());
                            ps.executeUpdate();
                        }
                        String invTx = "INSERT INTO inventory_transactions(product_id,type,qty_change,reference_id,notes) VALUES(?,'ADJUSTMENT',?,?,?)";
                        try (PreparedStatement ps = conn.prepareStatement(invTx)) {
                            ps.setInt(1, item.getProductId());
                            ps.setDouble(2, item.getQty());
                            ps.setInt(3, invoiceId);
                            ps.setString(4, "Void Invoice #" + invoiceId);
                            ps.executeUpdate();
                        }
                    }
                }
                // Mark invoice void
                String voidSql = "UPDATE invoices SET status='VOID' WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(voidSql)) {
                    ps.setInt(1, invoiceId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Returns today's total revenue from PAID invoices. */
    public double getTodayRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total),0) FROM invoices WHERE status='PAID' AND date(completed_at)=date('now')";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, int value) throws SQLException {
        if (value == 0) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getInt("id"));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        inv.setCustomerId(rs.getInt("customer_id"));
        inv.setVehicleId(rs.getInt("vehicle_id"));
        inv.setTechnicianId(rs.getInt("technician_id"));
        inv.setStatus(rs.getString("status"));
        inv.setPaymentMethod(rs.getString("payment_method"));
        inv.setSubtotal(rs.getDouble("subtotal"));
        inv.setTax(rs.getDouble("tax"));
        inv.setDiscount(rs.getDouble("discount"));
        inv.setTotal(rs.getDouble("total"));
        inv.setPaidAmount(rs.getDouble("paid_amount"));
        inv.setCreatedAt(rs.getString("created_at"));
        inv.setCompletedAt(rs.getString("completed_at"));
        inv.setNotes(rs.getString("notes"));
        return inv;
    }

    private InvoiceItem mapItemRow(ResultSet rs) throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.setId(rs.getInt("id"));
        item.setInvoiceId(rs.getInt("invoice_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setDescription(rs.getString("description"));
        item.setQty(rs.getDouble("qty"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        item.setCostPrice(rs.getDouble("cost_price"));
        item.setTotal(rs.getDouble("total"));
        return item;
    }
}
