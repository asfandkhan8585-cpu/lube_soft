package com.lubesoft.dao;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDAO {

    public Optional<Customer> findById(int id) throws SQLException {
        String sql = "SELECT * FROM customers WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Customer> search(String query) throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? OR company LIKE ? ORDER BY name";
        String pattern = "%" + query + "%";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Customer save(Customer customer) throws SQLException {
        if (customer.getId() == 0) {
            return insert(customer);
        } else {
            update(customer);
            return customer;
        }
    }

    private Customer insert(Customer c) throws SQLException {
        String sql = "INSERT INTO customers(name,phone,email,company,credit_limit,current_balance) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getCompany());
            ps.setDouble(5, c.getCreditLimit());
            ps.setDouble(6, c.getCurrentBalance());
            ps.executeUpdate();
            try (var _gkStmt = conn.createStatement(); ResultSet keys = _gkStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
        }
        return c;
    }

    private void update(Customer c) throws SQLException {
        String sql = "UPDATE customers SET name=?,phone=?,email=?,company=?,credit_limit=?,current_balance=? WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getCompany());
            ps.setDouble(5, c.getCreditLimit());
            ps.setDouble(6, c.getCurrentBalance());
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Returns customers with outstanding balance (credit usage). */
    public List<Customer> findWithBalance() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE current_balance > 0 ORDER BY current_balance DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setCompany(rs.getString("company"));
        c.setCreditLimit(rs.getDouble("credit_limit"));
        c.setCurrentBalance(rs.getDouble("current_balance"));
        c.setCreatedAt(rs.getString("created_at"));
        return c;
    }
}
