package com.lubesoft.service;

import com.lubesoft.dao.InvoiceDAO;
import com.lubesoft.dao.ProductDAO;
import com.lubesoft.model.Invoice;
import com.lubesoft.model.InvoiceItem;
import com.lubesoft.model.Product;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * POS workflow service: invoice lifecycle management.
 */
public class POSService {

    private static POSService instance;

    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final ProductDAO productDAO = new ProductDAO();

    private static final AtomicInteger invoiceCounter = new AtomicInteger((int) (System.currentTimeMillis() / 1000) % 100000);
    private static final double TAX_RATE = 0.0; // Set per jurisdiction; configurable

    private POSService() {}

    public static synchronized POSService getInstance() {
        if (instance == null) {
            instance = new POSService();
        }
        return instance;
    }

    /**
     * Creates a new OPEN invoice and returns it.
     */
    public Invoice createInvoice(int technicianId) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(generateInvoiceNumber());
        inv.setTechnicianId(technicianId);
        inv.setStatus("OPEN");
        return invoiceDAO.createInvoice(inv);
    }

    /**
     * Adds a product to an invoice by product ID, checking stock availability.
     * Returns the updated invoice.
     */
    public InvoiceItem addItem(int invoiceId, int productId, double qty) throws SQLException {
        Optional<Product> productOpt = productDAO.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        Product product = productOpt.get();
        if (!product.isBulkOil() && product.getStockQty() < qty) {
            throw new IllegalStateException("Insufficient stock for: " + product.getName());
        }

        InvoiceItem item = new InvoiceItem(
                productId,
                product.getName(),
                qty,
                product.getSellPrice(),
                product.getCostPrice()
        );
        item = invoiceDAO.addItem(invoiceId, item);
        recalculateInvoice(invoiceId);
        return item;
    }

    /**
     * Adds a custom (non-product) line item to an invoice.
     */
    public InvoiceItem addCustomItem(int invoiceId, String description, double qty, double unitPrice) throws SQLException {
        InvoiceItem item = new InvoiceItem(0, description, qty, unitPrice, 0);
        item = invoiceDAO.addItem(invoiceId, item);
        recalculateInvoice(invoiceId);
        return item;
    }

    public void removeItem(int invoiceId, int itemId) throws SQLException {
        invoiceDAO.removeItem(itemId);
        recalculateInvoice(invoiceId);
    }

    public void holdInvoice(int invoiceId) throws SQLException {
        invoiceDAO.updateStatus(invoiceId, "HELD");
    }

    public void resumeInvoice(int invoiceId) throws SQLException {
        invoiceDAO.updateStatus(invoiceId, "WIP");
    }

    public List<Invoice> getHeldInvoices() throws SQLException {
        return invoiceDAO.findByStatus("HELD");
    }

    public List<Invoice> getWipInvoices() throws SQLException {
        return invoiceDAO.findByStatus("WIP");
    }

    /**
     * Completes checkout: validates payment, deducts stock, marks PAID.
     * Uses a single SQL TRANSACTION with ROLLBACK on error.
     */
    public void checkout(int invoiceId, String paymentMethod, double paidAmount,
                         Map<String, Double> splitAmounts) throws SQLException {
        Optional<Invoice> invOpt = invoiceDAO.findById(invoiceId);
        if (invOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }
        Invoice invoice = invOpt.get();
        if (paidAmount < invoice.getTotal() && !"CREDIT".equals(paymentMethod)) {
            throw new IllegalStateException("Insufficient payment amount");
        }

        invoiceDAO.checkout(invoiceId, paymentMethod, paidAmount, invoice.getItems());
    }

    public void voidInvoice(int invoiceId) throws SQLException {
        Optional<Invoice> invOpt = invoiceDAO.findById(invoiceId);
        if (invOpt.isEmpty()) return;
        Invoice invoice = invOpt.get();
        // Only restore stock for PAID invoices
        if ("PAID".equals(invoice.getStatus())) {
            invoiceDAO.voidInvoice(invoiceId, invoice.getItems());
        } else {
            invoiceDAO.updateStatus(invoiceId, "VOID");
        }
    }

    public Optional<Invoice> getInvoice(int invoiceId) throws SQLException {
        return invoiceDAO.findById(invoiceId);
    }

    public void applyDiscount(int invoiceId, double discount) throws SQLException {
        Optional<Invoice> invOpt = invoiceDAO.findById(invoiceId);
        if (invOpt.isEmpty()) return;
        Invoice inv = invOpt.get();
        inv.setDiscount(discount);
        double total = inv.getSubtotal() + inv.getTax() - discount;
        invoiceDAO.updateTotals(invoiceId, inv.getSubtotal(), inv.getTax(), discount, total);
    }

    private void recalculateInvoice(int invoiceId) throws SQLException {
        Optional<Invoice> invOpt = invoiceDAO.findById(invoiceId);
        if (invOpt.isEmpty()) return;
        Invoice inv = invOpt.get();
        inv.recalculate(TAX_RATE);
        invoiceDAO.updateTotals(invoiceId, inv.getSubtotal(), inv.getTax(), inv.getDiscount(), inv.getTotal());
    }

    private String generateInvoiceNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = invoiceCounter.incrementAndGet();
        return "INV-" + date + "-" + String.format("%04d", seq % 10000);
    }
}
