package com.lubesoft.service;

import com.lubesoft.dao.InventoryDAO;
import com.lubesoft.dao.ProductDAO;
import com.lubesoft.model.InventoryTransaction;
import com.lubesoft.model.Product;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Inventory management: stock adjustments and purchase order receipt.
 */
public class InventoryService {

    private static InventoryService instance;

    private final ProductDAO productDAO = new ProductDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    private InventoryService() {}

    public static synchronized InventoryService getInstance() {
        if (instance == null) {
            instance = new InventoryService();
        }
        return instance;
    }

    public List<Product> getAllProducts() throws SQLException {
        return productDAO.findAll();
    }

    public List<Product> getLowStockProducts() throws SQLException {
        return productDAO.findLowStock();
    }

    public Product saveProduct(Product product) throws SQLException {
        return productDAO.save(product);
    }

    /**
     * Records a stock adjustment and updates product quantity.
     */
    public void adjustStock(int productId, double qtyChange, String reason) throws SQLException {
        Optional<Product> productOpt = productDAO.findById(productId);
        if (productOpt.isEmpty()) throw new IllegalArgumentException("Product not found: " + productId);

        Product product = productOpt.get();
        double newQty = product.getStockQty() + qtyChange;
        if (newQty < 0) throw new IllegalStateException("Stock cannot go below zero");

        product.setStockQty(newQty);
        productDAO.save(product);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProductId(productId);
        tx.setType("ADJUSTMENT");
        tx.setQtyChange(qtyChange);
        tx.setNotes(reason);
        inventoryDAO.record(tx);
    }

    /**
     * Receives purchased stock (from a PO).
     */
    public void receiveStock(int productId, double qtyReceived, double unitCost, int poId) throws SQLException {
        Optional<Product> productOpt = productDAO.findById(productId);
        if (productOpt.isEmpty()) throw new IllegalArgumentException("Product not found: " + productId);

        Product product = productOpt.get();
        product.setStockQty(product.getStockQty() + qtyReceived);
        product.setCostPrice(unitCost); // Update cost price on receipt
        productDAO.save(product);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProductId(productId);
        tx.setType("PURCHASE");
        tx.setQtyChange(qtyReceived);
        tx.setReferenceId(poId);
        tx.setNotes("PO #" + poId);
        inventoryDAO.record(tx);
    }

    public List<InventoryTransaction> getTransactions(int productId) throws SQLException {
        return inventoryDAO.findByProduct(productId);
    }

    public Optional<Product> findByBarcode(String barcode) throws SQLException {
        return productDAO.findByBarcode(barcode);
    }

    public List<Product> searchProducts(String query) throws SQLException {
        return productDAO.search(query);
    }
}
