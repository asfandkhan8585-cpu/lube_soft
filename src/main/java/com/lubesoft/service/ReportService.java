package com.lubesoft.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.lubesoft.dao.CustomerDAO;
import com.lubesoft.dao.InvoiceDAO;
import com.lubesoft.dao.ProductDAO;
import com.lubesoft.dao.TimeEntryDAO;
import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.Customer;
import com.lubesoft.model.Invoice;
import com.lubesoft.model.Product;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates business reports: sales, credit aging, inventory leakage, employee productivity.
 */
public class ReportService {

    private static ReportService instance;

    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final TimeEntryDAO timeEntryDAO = new TimeEntryDAO();

    private ReportService() {}

    public static synchronized ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }

    // ---------- Data retrieval methods ----------

    public List<Invoice> getSalesReport(String fromDate, String toDate) throws SQLException {
        return invoiceDAO.findByDateRange(fromDate, toDate);
    }

    public double getSalesTotal(String fromDate, String toDate) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total),0) FROM invoices WHERE status='PAID' AND date(completed_at) BETWEEN date(?) AND date(?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    /**
     * Returns customers with credit balances categorized by age (30/60/90 days).
     * Map keys: "0-30", "31-60", "61-90", "90+"
     */
    public Map<String, List<Customer>> getCreditAging() throws SQLException {
        Map<String, List<Customer>> aging = new LinkedHashMap<>();
        aging.put("0-30", new ArrayList<>());
        aging.put("31-60", new ArrayList<>());
        aging.put("61-90", new ArrayList<>());
        aging.put("90+", new ArrayList<>());

        String sql = """
            SELECT c.*, julianday('now') - julianday(MIN(i.created_at)) as days_old
            FROM customers c
            JOIN invoices i ON i.customer_id = c.id AND i.payment_method = 'CREDIT' AND i.status = 'PAID'
            WHERE c.current_balance > 0
            GROUP BY c.id
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Customer c = mapCustomerRow(rs);
                int days = rs.getInt("days_old");
                if (days <= 30) aging.get("0-30").add(c);
                else if (days <= 60) aging.get("31-60").add(c);
                else if (days <= 90) aging.get("61-90").add(c);
                else aging.get("90+").add(c);
            }
        }
        return aging;
    }

    /**
     * Inventory leakage: products sold vs. inventory recorded differences.
     */
    public List<Map<String, Object>> getInventoryLeakage(String fromDate, String toDate) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = """
            SELECT p.id, p.name, p.sku,
                   COALESCE(SUM(CASE WHEN it.type='SALE' THEN ABS(it.qty_change) ELSE 0 END),0) as sold,
                   COALESCE(SUM(CASE WHEN it.type='WASTE' THEN it.qty_change ELSE 0 END),0) as waste,
                   p.stock_qty
            FROM products p
            LEFT JOIN inventory_transactions it ON it.product_id = p.id
                AND date(it.created_at) BETWEEN date(?) AND date(?)
            GROUP BY p.id
            HAVING waste > 0 OR sold > 0
            ORDER BY waste DESC
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("name", rs.getString("name"));
                    row.put("sku", rs.getString("sku"));
                    row.put("sold", rs.getDouble("sold"));
                    row.put("waste", rs.getDouble("waste"));
                    row.put("stock_qty", rs.getDouble("stock_qty"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    /**
     * Employee productivity: number of invoices processed per technician in a date range.
     */
    public List<Map<String, Object>> getEmployeeProductivity(String fromDate, String toDate) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username, COUNT(i.id) as invoice_count,
                   COALESCE(SUM(i.total),0) as total_revenue,
                   COALESCE(SUM(te.total_hours),0) as hours_worked
            FROM users u
            LEFT JOIN invoices i ON i.technician_id = u.id
                AND i.status='PAID'
                AND date(i.completed_at) BETWEEN date(?) AND date(?)
            LEFT JOIN (
                SELECT user_id,
                       SUM(CAST((julianday(COALESCE(clock_out, datetime('now'))) - julianday(clock_in)) * 24 AS REAL)) as total_hours
                FROM time_entries
                WHERE date(clock_in) BETWEEN date(?) AND date(?)
                GROUP BY user_id
            ) te ON te.user_id = u.id
            WHERE u.active=1
            GROUP BY u.id
            ORDER BY invoice_count DESC
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            ps.setString(3, fromDate);
            ps.setString(4, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("username", rs.getString("username"));
                    row.put("invoice_count", rs.getInt("invoice_count"));
                    row.put("total_revenue", rs.getDouble("total_revenue"));
                    row.put("hours_worked", rs.getDouble("hours_worked"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    // ---------- PDF Export ----------

    public void exportSalesToPdf(List<Invoice> invoices, String filePath) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font cellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9);

        doc.add(new Paragraph("Sales Report", titleFont));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        for (String h : new String[]{"Invoice#", "Date", "Status", "Payment", "Total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        double grandTotal = 0;
        for (Invoice inv : invoices) {
            table.addCell(new Phrase(inv.getInvoiceNumber(), cellFont));
            table.addCell(new Phrase(inv.getCreatedAt(), cellFont));
            table.addCell(new Phrase(inv.getStatus(), cellFont));
            table.addCell(new Phrase(inv.getPaymentMethod() != null ? inv.getPaymentMethod() : "", cellFont));
            table.addCell(new Phrase(String.format("%.2f", inv.getTotal()), cellFont));
            grandTotal += inv.getTotal();
        }

        doc.add(table);
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Grand Total: " + String.format("%.2f", grandTotal), headerFont));
        doc.close();
    }

    // ---------- Excel Export ----------

    public void exportSalesToExcel(List<Invoice> invoices, String filePath) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sales Report");
            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            Row header = sheet.createRow(0);
            String[] cols = {"Invoice#", "Date", "Status", "Payment", "Subtotal", "Tax", "Discount", "Total"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(inv.getInvoiceNumber());
                row.createCell(1).setCellValue(inv.getCreatedAt());
                row.createCell(2).setCellValue(inv.getStatus());
                row.createCell(3).setCellValue(inv.getPaymentMethod() != null ? inv.getPaymentMethod() : "");
                row.createCell(4).setCellValue(inv.getSubtotal());
                row.createCell(5).setCellValue(inv.getTax());
                row.createCell(6).setCellValue(inv.getDiscount());
                row.createCell(7).setCellValue(inv.getTotal());
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
    }

    private Customer mapCustomerRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setCompany(rs.getString("company"));
        c.setCreditLimit(rs.getDouble("credit_limit"));
        c.setCurrentBalance(rs.getDouble("current_balance"));
        return c;
    }
}
