package com.lubesoft.controller;

import com.lubesoft.dao.VehicleDAO;
import com.lubesoft.model.Invoice;
import com.lubesoft.model.InvoiceItem;
import com.lubesoft.model.Product;
import com.lubesoft.model.Vehicle;
import com.lubesoft.service.AuthService;
import com.lubesoft.service.InventoryService;
import com.lubesoft.service.POSService;
import com.lubesoft.util.AlertUtil;
import com.lubesoft.util.PrintUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class POSController {

    @FXML private TextField searchField;
    @FXML private TextField barcodeField;
    @FXML private TextField plateField;
    @FXML private Label vehicleLabel;
    @FXML private Label invoiceNumberLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private TextField discountField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextField amountPaidField;

    @FXML private TableView<InvoiceItem> itemsTable;
    @FXML private TableColumn<InvoiceItem, String> colItemDesc;
    @FXML private TableColumn<InvoiceItem, Double> colItemQty;
    @FXML private TableColumn<InvoiceItem, Double> colItemPrice;
    @FXML private TableColumn<InvoiceItem, Double> colItemTotal;

    @FXML private ListView<String> wipList;
    @FXML private TableView<Product> productSearchTable;
    @FXML private TableColumn<Product, String> colProdName;
    @FXML private TableColumn<Product, String> colProdSku;
    @FXML private TableColumn<Product, Double> colProdPrice;
    @FXML private TableColumn<Product, Double> colProdStock;

    // DVI checklist
    @FXML private ComboBox<String> airFilterCombo;
    @FXML private ComboBox<String> wipersCombo;
    @FXML private ComboBox<String> fluidLevelsCombo;
    @FXML private ComboBox<String> tiresCombo;

    private Invoice currentInvoice;
    private final ObservableList<InvoiceItem> invoiceItems = FXCollections.observableArrayList();
    private final POSService posService = POSService.getInstance();
    private final InventoryService inventoryService = InventoryService.getInstance();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

    @FXML
    public void initialize() {
        setupTables();
        setupCombos();
        newInvoice();
    }

    private void setupTables() {
        colItemDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colItemQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colItemPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colItemTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        itemsTable.setItems(invoiceItems);

        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colProdPrice.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        colProdStock.setCellValueFactory(new PropertyValueFactory<>("stockQty"));

        // Double-click product to add to invoice
        productSearchTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Product selected = productSearchTable.getSelectionModel().getSelectedItem();
                if (selected != null) addProductToInvoice(selected, 1);
            }
        });
    }

    private void setupCombos() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList("CASH", "CREDIT", "DIGITAL", "SPLIT"));
        paymentMethodCombo.setValue("CASH");

        String[] conditions = {"OK", "WARNING", "CRITICAL", "N/A"};
        if (airFilterCombo != null) airFilterCombo.setItems(FXCollections.observableArrayList(conditions));
        if (wipersCombo != null) wipersCombo.setItems(FXCollections.observableArrayList(conditions));
        if (fluidLevelsCombo != null) fluidLevelsCombo.setItems(FXCollections.observableArrayList(conditions));
        if (tiresCombo != null) tiresCombo.setItems(FXCollections.observableArrayList(conditions));
    }

    @FXML
    private void newInvoice() {
        new Thread(() -> {
            try {
                var user = AuthService.getCurrentUser();
                int techId = user != null ? user.getId() : 0;
                Invoice inv = posService.createInvoice(techId);
                Platform.runLater(() -> {
                    currentInvoice = inv;
                    invoiceItems.clear();
                    invoiceNumberLabel.setText(inv.getInvoiceNumber());
                    vehicleLabel.setText("No vehicle selected");
                    updateTotals();
                    refreshWipList();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", "Could not create invoice: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleBarcodeSearch() {
        String barcode = barcodeField.getText().trim();
        if (barcode.isEmpty()) return;

        new Thread(() -> {
            try {
                Optional<Product> productOpt = inventoryService.findByBarcode(barcode);
                Platform.runLater(() -> {
                    if (productOpt.isPresent()) {
                        addProductToInvoice(productOpt.get(), 1);
                    } else {
                        AlertUtil.showWarning("Not Found", "No product with barcode: " + barcode);
                    }
                    barcodeField.clear();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleProductSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        new Thread(() -> {
            try {
                List<Product> results = inventoryService.searchProducts(query);
                Platform.runLater(() -> productSearchTable.setItems(FXCollections.observableArrayList(results)));
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handlePlateLookup() {
        String plate = plateField.getText().trim();
        if (plate.isEmpty()) return;

        new Thread(() -> {
            try {
                Optional<Vehicle> vehicleOpt = vehicleDAO.findByLicensePlate(plate);
                Platform.runLater(() -> {
                    if (vehicleOpt.isPresent()) {
                        Vehicle v = vehicleOpt.get();
                        vehicleLabel.setText(v.getYear() + " " + v.getMake() + " " + v.getModel() + " (" + v.getLicensePlate() + ")");
                        if (currentInvoice != null) currentInvoice.setVehicleId(v.getId());
                    } else {
                        AlertUtil.showWarning("Not Found", "No vehicle with plate: " + plate);
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    private void addProductToInvoice(Product product, double qty) {
        if (currentInvoice == null) {
            AlertUtil.showError("Error", "No active invoice.");
            return;
        }
        new Thread(() -> {
            try {
                InvoiceItem item = posService.addItem(currentInvoice.getId(), product.getId(), qty);
                Platform.runLater(() -> {
                    invoiceItems.add(item);
                    updateTotals();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRemoveItem() {
        InvoiceItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null || currentInvoice == null) return;

        new Thread(() -> {
            try {
                posService.removeItem(currentInvoice.getId(), selected.getId());
                Platform.runLater(() -> {
                    invoiceItems.remove(selected);
                    updateTotals();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleHold() {
        if (currentInvoice == null || invoiceItems.isEmpty()) return;
        new Thread(() -> {
            try {
                posService.holdInvoice(currentInvoice.getId());
                Platform.runLater(() -> {
                    refreshWipList();
                    newInvoice();
                    AlertUtil.showInfo("Held", "Invoice " + currentInvoice.getInvoiceNumber() + " is on hold.");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCheckout() {
        if (currentInvoice == null || invoiceItems.isEmpty()) {
            AlertUtil.showWarning("Empty Invoice", "Add items before checkout.");
            return;
        }

        String paymentMethod = paymentMethodCombo.getValue();
        double paid;
        try {
            paid = amountPaidField.getText().isEmpty() ? currentInvoice.getTotal()
                    : Double.parseDouble(amountPaidField.getText());
        } catch (NumberFormatException e) {
            AlertUtil.showError("Invalid Amount", "Enter a valid payment amount.");
            return;
        }

        // Apply discount
        try {
            if (!discountField.getText().isEmpty()) {
                double discount = Double.parseDouble(discountField.getText());
                posService.applyDiscount(currentInvoice.getId(), discount);
                refreshInvoiceFromDb();
            }
        } catch (Exception e) {
            AlertUtil.showError("Discount Error", e.getMessage());
            return;
        }

        final double finalPaid = paid;
        new Thread(() -> {
            try {
                posService.checkout(currentInvoice.getId(), paymentMethod, finalPaid, null);
                Platform.runLater(() -> {
                    AlertUtil.showInfo("Paid", "Invoice " + currentInvoice.getInvoiceNumber() + " completed.");
                    newInvoice();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Checkout Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleVoidInvoice() {
        if (currentInvoice == null) return;
        if (!AlertUtil.showConfirm("Void Invoice", "Are you sure you want to void this invoice?")) return;

        new Thread(() -> {
            try {
                posService.voidInvoice(currentInvoice.getId());
                Platform.runLater(() -> {
                    AlertUtil.showInfo("Voided", "Invoice voided.");
                    newInvoice();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handlePrintInvoice() {
        if (currentInvoice == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Invoice PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName(currentInvoice.getInvoiceNumber() + ".pdf");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        currentInvoice.setItems(invoiceItems);
        try {
            PrintUtil.exportInvoiceToPdf(currentInvoice, file.getAbsolutePath());
            AlertUtil.showInfo("Exported", "Invoice exported to PDF.");
        } catch (Exception e) {
            AlertUtil.showError("Export Error", e.getMessage());
        }
    }

    private void handleResumeFromWip(String invoiceNumAndId) {
        // Parse the invoice ID from the string "INV-... (id=X)"
        try {
            int start = invoiceNumAndId.lastIndexOf("(id=") + 4;
            int end = invoiceNumAndId.lastIndexOf(")");
            if (start > 4 && end > start) {
                int invoiceId = Integer.parseInt(invoiceNumAndId.substring(start, end));
                new Thread(() -> {
                    try {
                        posService.resumeInvoice(invoiceId);
                        Optional<Invoice> invOpt = posService.getInvoice(invoiceId);
                        Platform.runLater(() -> {
                            invOpt.ifPresent(inv -> {
                                currentInvoice = inv;
                                invoiceItems.setAll(inv.getItems());
                                invoiceNumberLabel.setText(inv.getInvoiceNumber());
                                updateTotals();
                                refreshWipList();
                            });
                        });
                    } catch (SQLException e) {
                        Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
                    }
                }).start();
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError("Error", "Could not parse invoice ID.");
        }
    }

    private void refreshWipList() {
        new Thread(() -> {
            try {
                List<Invoice> held = posService.getHeldInvoices();
                List<Invoice> wip = posService.getWipInvoices();
                held.addAll(wip);
                Platform.runLater(() -> {
                    ObservableList<String> items = FXCollections.observableArrayList();
                    for (Invoice inv : held) {
                        items.add(inv.getInvoiceNumber() + " [" + inv.getStatus() + "] (id=" + inv.getId() + ")");
                    }
                    wipList.setItems(items);
                });
            } catch (SQLException e) {
                // Ignore
            }
        }).start();
    }

    @FXML
    private void handleResumeSelected() {
        String selected = wipList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            handleResumeFromWip(selected);
        }
    }

    private void refreshInvoiceFromDb() throws SQLException {
        if (currentInvoice == null) return;
        Optional<Invoice> invOpt = posService.getInvoice(currentInvoice.getId());
        invOpt.ifPresent(inv -> {
            currentInvoice = inv;
            invoiceItems.setAll(inv.getItems());
            updateTotals();
        });
    }

    private void updateTotals() {
        if (currentInvoice == null) return;
        double subtotal = invoiceItems.stream().mapToDouble(InvoiceItem::getTotal).sum();
        subtotalLabel.setText(String.format("%.2f", subtotal));
        taxLabel.setText(String.format("%.2f", currentInvoice.getTax()));
        totalLabel.setText(String.format("%.2f", currentInvoice.getTotal()));
    }
}
