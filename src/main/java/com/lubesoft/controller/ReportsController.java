package com.lubesoft.controller;

import com.lubesoft.model.Invoice;
import com.lubesoft.service.ReportService;
import com.lubesoft.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportsController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private TabPane reportTabs;

    // Sales tab
    @FXML private TableView<Invoice> salesTable;
    @FXML private TableColumn<Invoice, String> colSalesInv;
    @FXML private TableColumn<Invoice, String> colSalesDate;
    @FXML private TableColumn<Invoice, String> colSalesPayment;
    @FXML private TableColumn<Invoice, Double> colSalesTotal;
    @FXML private Label salesTotalLabel;

    private final ReportService reportService = ReportService.getInstance();

    @FXML
    public void initialize() {
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());

        setupSalesTable();
        loadSalesReport();
    }

    private void setupSalesTable() {
        colSalesInv.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        colSalesDate.setCellValueFactory(new PropertyValueFactory<>("completedAt"));
        colSalesPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colSalesTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
    }

    @FXML
    private void handleRunReport() {
        String activeTab = reportTabs.getSelectionModel().getSelectedItem().getText();
        switch (activeTab) {
            case "Sales"       -> loadSalesReport();
            case "Credit Aging" -> loadCreditAging();
            case "Inv. Leakage" -> loadLeakageReport();
            case "Productivity" -> loadProductivityReport();
        }
    }

    private void loadSalesReport() {
        String from = getFromDate();
        String to = getToDate();
        new Thread(() -> {
            try {
                List<Invoice> invoices = reportService.getSalesReport(from, to);
                double total = reportService.getSalesTotal(from, to);
                Platform.runLater(() -> {
                    salesTable.setItems(FXCollections.observableArrayList(invoices));
                    if (salesTotalLabel != null) salesTotalLabel.setText("Total: " + String.format("%.2f", total));
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    private void loadCreditAging() {
        new Thread(() -> {
            try {
                var aging = reportService.getCreditAging();
                Platform.runLater(() -> {
                    StringBuilder sb = new StringBuilder("Credit Aging Summary:\n");
                    aging.forEach((bucket, customers) ->
                            sb.append(bucket).append(" days: ").append(customers.size()).append(" customer(s)\n"));
                    AlertUtil.showInfo("Credit Aging", sb.toString());
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    private void loadLeakageReport() {
        new Thread(() -> {
            try {
                var leakage = reportService.getInventoryLeakage(getFromDate(), getToDate());
                Platform.runLater(() -> {
                    StringBuilder sb = new StringBuilder("Inventory Leakage:\n");
                    for (var row : leakage) {
                        sb.append(row.get("name")).append(": sold=").append(row.get("sold"))
                          .append(", waste=").append(row.get("waste")).append("\n");
                    }
                    AlertUtil.showInfo("Inventory Leakage", sb.toString());
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    private void loadProductivityReport() {
        new Thread(() -> {
            try {
                var productivity = reportService.getEmployeeProductivity(getFromDate(), getToDate());
                Platform.runLater(() -> {
                    StringBuilder sb = new StringBuilder("Employee Productivity:\n");
                    for (var row : productivity) {
                        sb.append(row.get("username")).append(": ")
                          .append(row.get("invoice_count")).append(" invoices, ")
                          .append(String.format("%.1f", row.get("hours_worked"))).append(" hrs\n");
                    }
                    AlertUtil.showInfo("Employee Productivity", sb.toString());
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("sales_report.pdf");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        new Thread(() -> {
            try {
                List<Invoice> invoices = reportService.getSalesReport(getFromDate(), getToDate());
                reportService.exportSalesToPdf(invoices, file.getAbsolutePath());
                Platform.runLater(() -> AlertUtil.showInfo("Exported", "Sales report exported to PDF."));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Export Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleExportExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        chooser.setInitialFileName("sales_report.xlsx");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        new Thread(() -> {
            try {
                List<Invoice> invoices = reportService.getSalesReport(getFromDate(), getToDate());
                reportService.exportSalesToExcel(invoices, file.getAbsolutePath());
                Platform.runLater(() -> AlertUtil.showInfo("Exported", "Sales report exported to Excel."));
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Export Error", e.getMessage()));
            }
        }).start();
    }

    private String getFromDate() {
        return fromDatePicker.getValue() != null ? fromDatePicker.getValue().toString() : LocalDate.now().toString();
    }

    private String getToDate() {
        return toDatePicker.getValue() != null ? toDatePicker.getValue().toString() : LocalDate.now().toString();
    }
}
