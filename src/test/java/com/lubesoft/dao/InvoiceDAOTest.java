package com.lubesoft.dao;

import com.lubesoft.db.DatabaseInitializer;
import com.lubesoft.db.DatabaseManager;
import com.lubesoft.model.Invoice;
import com.lubesoft.model.InvoiceItem;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceDAOTest {

    private static InvoiceDAO invoiceDAO;
    private static int testInvoiceId;
    // Use timestamp suffix to avoid UNIQUE constraint conflicts across test runs
    private static final String INV_NUM = "TEST-INV-" + System.currentTimeMillis();
    private static final String VOID_INV_NUM = "TEST-VOID-" + System.currentTimeMillis();

    @BeforeAll
    static void setup() throws SQLException {
        DatabaseInitializer.initialize();
        invoiceDAO = new InvoiceDAO();
        // Remove any leftover test invoices from previous runs with the SAME timestamp (none, fresh run)
        // No cleanup needed for unique timestamps.
    }

    @Test
    @Order(1)
    void testCreateInvoice() throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(INV_NUM);
        inv.setStatus("OPEN");
        inv.setSubtotal(0);
        inv.setTax(0);
        inv.setDiscount(0);
        inv.setTotal(0);

        Invoice created = invoiceDAO.createInvoice(inv);
        assertTrue(created.getId() > 0, "Invoice should have a generated ID");
        testInvoiceId = created.getId();
    }

    @Test
    @Order(2)
    void testFindById() throws SQLException {
        Assumptions.assumeTrue(testInvoiceId > 0, "Skipping: invoice not created");
        Optional<Invoice> found = invoiceDAO.findById(testInvoiceId);
        assertTrue(found.isPresent(), "Invoice should be found by ID");
        assertEquals(INV_NUM, found.get().getInvoiceNumber());
        assertEquals("OPEN", found.get().getStatus());
    }

    @Test
    @Order(3)
    void testAddItem() throws SQLException {
        Assumptions.assumeTrue(testInvoiceId > 0, "Skipping: invoice not created");
        InvoiceItem item = new InvoiceItem(0, "Oil Change Service", 1, 49.99, 15.00);
        InvoiceItem saved = invoiceDAO.addItem(testInvoiceId, item);
        assertTrue(saved.getId() > 0, "Item should have a generated ID");
        assertEquals("Oil Change Service", saved.getDescription());
    }

    @Test
    @Order(4)
    void testFindItemsForInvoice() throws SQLException {
        Assumptions.assumeTrue(testInvoiceId > 0, "Skipping: invoice not created");
        List<InvoiceItem> items = invoiceDAO.findItems(testInvoiceId);
        assertFalse(items.isEmpty(), "Invoice should have at least one item");
    }

    @Test
    @Order(5)
    void testUpdateStatus() throws SQLException {
        Assumptions.assumeTrue(testInvoiceId > 0, "Skipping: invoice not created");
        invoiceDAO.updateStatus(testInvoiceId, "HELD");
        Optional<Invoice> updated = invoiceDAO.findById(testInvoiceId);
        assertTrue(updated.isPresent());
        assertEquals("HELD", updated.get().getStatus());
    }

    @Test
    @Order(6)
    void testFindByStatus() throws SQLException {
        Assumptions.assumeTrue(testInvoiceId > 0, "Skipping: invoice not created");
        List<Invoice> held = invoiceDAO.findByStatus("HELD");
        assertTrue(held.stream().anyMatch(i -> i.getId() == testInvoiceId),
                "Invoice should appear in HELD list");
    }

    @Test
    @Order(7)
    void testCheckoutTransaction() throws SQLException {
        Assumptions.assumeTrue(testInvoiceId > 0, "Skipping: invoice not created");
        invoiceDAO.updateStatus(testInvoiceId, "WIP");
        invoiceDAO.updateTotals(testInvoiceId, 49.99, 0, 0, 49.99);

        List<InvoiceItem> items = invoiceDAO.findItems(testInvoiceId);
        // product_id=0 items skip stock deduction
        invoiceDAO.checkout(testInvoiceId, "CASH", 49.99, items);

        Optional<Invoice> paid = invoiceDAO.findById(testInvoiceId);
        assertTrue(paid.isPresent());
        assertEquals("PAID", paid.get().getStatus());
        assertEquals("CASH", paid.get().getPaymentMethod());
    }

    @Test
    @Order(8)
    void testTodayRevenue() throws SQLException {
        double revenue = invoiceDAO.getTodayRevenue();
        assertTrue(revenue >= 49.99, "Today's revenue should include the test invoice");
    }

    @Test
    @Order(9)
    void testVoidInvoice() throws SQLException {
        Invoice inv2 = new Invoice();
        inv2.setInvoiceNumber(VOID_INV_NUM);
        inv2.setStatus("OPEN");
        inv2.setTotal(0);
        Invoice created = invoiceDAO.createInvoice(inv2);
        assertTrue(created.getId() > 0);

        invoiceDAO.voidInvoice(created.getId(), List.of());
        Optional<Invoice> voided = invoiceDAO.findById(created.getId());
        assertTrue(voided.isPresent());
        assertEquals("VOID", voided.get().getStatus());
    }
}

