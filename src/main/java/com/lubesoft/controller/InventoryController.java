package com.lubesoft.controller;

import com.lubesoft.model.Product;
import com.lubesoft.service.InventoryService;
import com.lubesoft.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class InventoryController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colSellPrice;
    @FXML private TableColumn<Product, Double> colStock;
    @FXML private TableColumn<Product, Double> colMinStock;

    // Product form
    @FXML private TextField skuField;
    @FXML private TextField nameField;
    @FXML private TextField categoryField;
    @FXML private TextField unitField;
    @FXML private TextField sellPriceField;
    @FXML private TextField costPriceField;
    @FXML private TextField stockField;
    @FXML private TextField minStockField;
    @FXML private TextField maxStockField;
    @FXML private TextField barcodeField;
    @FXML private CheckBox isBulkOilCheck;
    @FXML private TextField searchField;

    private final InventoryService inventoryService = InventoryService.getInstance();
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private Product selectedProduct;

    @FXML
    public void initialize() {
        setupTable();
        loadProducts();

        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) populateForm(newVal);
        });
    }

    private void setupTable() {
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colSellPrice.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQty"));
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        productsTable.setItems(products);

        // Highlight low stock rows in red
        productsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (!empty && product != null && product.isLowStock()) {
                    setStyle("-fx-background-color: #ffe0e0;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadProducts() {
        new Thread(() -> {
            try {
                List<Product> list = inventoryService.getAllProducts();
                Platform.runLater(() -> products.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Failed to load products: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadProducts();
            return;
        }
        new Thread(() -> {
            try {
                List<Product> results = inventoryService.searchProducts(query);
                Platform.runLater(() -> products.setAll(results));
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSave() {
        try {
            Product p = selectedProduct != null ? selectedProduct : new Product();
            p.setSku(skuField.getText().trim());
            p.setName(nameField.getText().trim());
            p.setCategory(categoryField.getText().trim());
            p.setUnit(unitField.getText().trim());
            p.setSellPrice(Double.parseDouble(sellPriceField.getText()));
            p.setCostPrice(Double.parseDouble(costPriceField.getText()));
            p.setStockQty(Double.parseDouble(stockField.getText()));
            p.setMinStock(Double.parseDouble(minStockField.getText()));
            p.setMaxStock(Double.parseDouble(maxStockField.getText()));
            p.setBarcode(barcodeField.getText().trim());
            if (isBulkOilCheck != null) p.setBulkOil(isBulkOilCheck.isSelected());

            inventoryService.saveProduct(p);
            AlertUtil.showInfo("Saved", "Product saved successfully.");
            clearForm();
            loadProducts();
        } catch (NumberFormatException e) {
            AlertUtil.showError("Invalid Input", "Please enter valid numeric values for prices and stock.");
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleNew() {
        selectedProduct = null;
        clearForm();
    }

    @FXML
    private void handleAdjustStock() {
        if (selectedProduct == null) {
            AlertUtil.showWarning("No Selection", "Select a product first.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Adjust Stock");
        dialog.setHeaderText("Product: " + selectedProduct.getName());
        dialog.setContentText("Enter qty change (+/-): ");
        dialog.showAndWait().ifPresent(val -> {
            try {
                double change = Double.parseDouble(val);
                inventoryService.adjustStock(selectedProduct.getId(), change, "Manual adjustment");
                AlertUtil.showInfo("Done", "Stock adjusted.");
                loadProducts();
            } catch (NumberFormatException e) {
                AlertUtil.showError("Error", "Invalid number.");
            } catch (Exception e) {
                AlertUtil.showError("Error", e.getMessage());
            }
        });
    }

    private void populateForm(Product p) {
        selectedProduct = p;
        skuField.setText(p.getSku() != null ? p.getSku() : "");
        nameField.setText(p.getName());
        categoryField.setText(p.getCategory() != null ? p.getCategory() : "");
        unitField.setText(p.getUnit() != null ? p.getUnit() : "");
        sellPriceField.setText(String.valueOf(p.getSellPrice()));
        costPriceField.setText(String.valueOf(p.getCostPrice()));
        stockField.setText(String.valueOf(p.getStockQty()));
        minStockField.setText(String.valueOf(p.getMinStock()));
        maxStockField.setText(String.valueOf(p.getMaxStock()));
        barcodeField.setText(p.getBarcode() != null ? p.getBarcode() : "");
        if (isBulkOilCheck != null) isBulkOilCheck.setSelected(p.isBulkOil());
    }

    private void clearForm() {
        skuField.clear(); nameField.clear(); categoryField.clear(); unitField.clear();
        sellPriceField.clear(); costPriceField.clear(); stockField.clear();
        minStockField.clear(); maxStockField.clear(); barcodeField.clear();
        if (isBulkOilCheck != null) isBulkOilCheck.setSelected(false);
    }
}
