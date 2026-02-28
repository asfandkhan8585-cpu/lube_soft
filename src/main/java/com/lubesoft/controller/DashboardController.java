package com.lubesoft.controller;

import com.lubesoft.dao.InvoiceDAO;
import com.lubesoft.dao.ProductDAO;
import com.lubesoft.model.Invoice;
import com.lubesoft.model.Product;
import com.lubesoft.service.AuthService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML private Label todaySalesLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label pendingCreditsLabel;
    @FXML private Label welcomeLabel;

    @FXML private TableView<Invoice> recentTransactionsTable;
    @FXML private TableColumn<Invoice, String> colInvoiceNum;
    @FXML private TableColumn<Invoice, String> colStatus;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, Double> colTotal;

    @FXML private TableView<Product> lowStockTable;
    @FXML private TableColumn<Product, String> colProductName;
    @FXML private TableColumn<Product, Double> colStockQty;
    @FXML private TableColumn<Product, Double> colMinStock;

    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final ProductDAO productDAO = new ProductDAO();

    @FXML
    public void initialize() {
        var user = AuthService.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        }

        setupTables();
        loadDataAsync();
    }

    private void setupTables() {
        colInvoiceNum.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("completedAt"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStockQty.setCellValueFactory(new PropertyValueFactory<>("stockQty"));
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
    }

    private void loadDataAsync() {
        new Thread(() -> {
            try {
                double todayRevenue = invoiceDAO.getTodayRevenue();
                List<Invoice> recent = invoiceDAO.findRecentPaid(10);
                List<Product> lowStock = productDAO.findLowStock();

                Platform.runLater(() -> {
                    todaySalesLabel.setText(String.format("%.2f", todayRevenue));
                    lowStockLabel.setText(String.valueOf(lowStock.size()));
                    pendingCreditsLabel.setText("0"); // Placeholder

                    recentTransactionsTable.setItems(FXCollections.observableArrayList(recent));
                    lowStockTable.setItems(FXCollections.observableArrayList(lowStock));
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    todaySalesLabel.setText("Error");
                });
            }
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadDataAsync();
    }
}
