package com.lubesoft.controller;

import com.lubesoft.dao.CustomerDAO;
import com.lubesoft.dao.ServiceHistoryDAO;
import com.lubesoft.dao.VehicleDAO;
import com.lubesoft.model.Customer;
import com.lubesoft.model.ServiceHistory;
import com.lubesoft.model.Vehicle;
import com.lubesoft.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class CustomerController {

    // Customer list
    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, String> colCustName;
    @FXML private TableColumn<Customer, String> colCustPhone;
    @FXML private TableColumn<Customer, String> colCustCompany;
    @FXML private TableColumn<Customer, Double> colCustBalance;

    // Customer form
    @FXML private TextField custNameField;
    @FXML private TextField custPhoneField;
    @FXML private TextField custEmailField;
    @FXML private TextField custCompanyField;
    @FXML private TextField custCreditLimitField;
    @FXML private Label custBalanceLabel;
    @FXML private TextField searchField;

    // Vehicle history
    @FXML private TableView<Vehicle> vehiclesTable;
    @FXML private TableColumn<Vehicle, String> colVehiclePlate;
    @FXML private TableColumn<Vehicle, String> colVehicleMake;
    @FXML private TableColumn<Vehicle, String> colVehicleModel;
    @FXML private TableColumn<Vehicle, Integer> colVehicleYear;

    @FXML private TableView<ServiceHistory> historyTable;
    @FXML private TableColumn<ServiceHistory, String> colHistDate;
    @FXML private TableColumn<ServiceHistory, Integer> colHistMileage;
    @FXML private TableColumn<ServiceHistory, String> colHistOil;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ServiceHistoryDAO serviceHistoryDAO = new ServiceHistoryDAO();

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private Customer selectedCustomer;

    @FXML
    public void initialize() {
        setupTables();
        loadCustomers();

        customersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) onCustomerSelected(newVal);
        });

        vehiclesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) loadServiceHistory(newVal.getId());
        });
    }

    private void setupTables() {
        colCustName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCustPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colCustCompany.setCellValueFactory(new PropertyValueFactory<>("company"));
        colCustBalance.setCellValueFactory(new PropertyValueFactory<>("currentBalance"));
        customersTable.setItems(customers);

        colVehiclePlate.setCellValueFactory(new PropertyValueFactory<>("licensePlate"));
        colVehicleMake.setCellValueFactory(new PropertyValueFactory<>("make"));
        colVehicleModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colVehicleYear.setCellValueFactory(new PropertyValueFactory<>("year"));

        colHistDate.setCellValueFactory(new PropertyValueFactory<>("serviceDate"));
        colHistMileage.setCellValueFactory(new PropertyValueFactory<>("mileage"));
        colHistOil.setCellValueFactory(new PropertyValueFactory<>("oilGrade"));
    }

    private void loadCustomers() {
        new Thread(() -> {
            try {
                List<Customer> list = customerDAO.findAll();
                Platform.runLater(() -> customers.setAll(list));
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadCustomers();
            return;
        }
        new Thread(() -> {
            try {
                List<Customer> results = customerDAO.search(query);
                Platform.runLater(() -> customers.setAll(results));
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSaveCustomer() {
        try {
            Customer c = selectedCustomer != null ? selectedCustomer : new Customer();
            c.setName(custNameField.getText().trim());
            c.setPhone(custPhoneField.getText().trim());
            c.setEmail(custEmailField.getText().trim());
            c.setCompany(custCompanyField.getText().trim());
            c.setCreditLimit(custCreditLimitField.getText().isEmpty() ? 0
                    : Double.parseDouble(custCreditLimitField.getText()));

            customerDAO.save(c);
            AlertUtil.showInfo("Saved", "Customer saved.");
            clearForm();
            loadCustomers();
        } catch (NumberFormatException e) {
            AlertUtil.showError("Invalid Input", "Credit limit must be a number.");
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleNewCustomer() {
        selectedCustomer = null;
        clearForm();
    }

    @FXML
    private void handleDeleteCustomer() {
        if (selectedCustomer == null) return;
        if (!AlertUtil.showConfirm("Delete", "Delete customer " + selectedCustomer.getName() + "?")) return;
        try {
            customerDAO.delete(selectedCustomer.getId());
            loadCustomers();
            clearForm();
        } catch (SQLException e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    private void onCustomerSelected(Customer c) {
        selectedCustomer = c;
        custNameField.setText(c.getName());
        custPhoneField.setText(c.getPhone() != null ? c.getPhone() : "");
        custEmailField.setText(c.getEmail() != null ? c.getEmail() : "");
        custCompanyField.setText(c.getCompany() != null ? c.getCompany() : "");
        custCreditLimitField.setText(String.valueOf(c.getCreditLimit()));
        if (custBalanceLabel != null) custBalanceLabel.setText(String.format("Balance: %.2f", c.getCurrentBalance()));

        new Thread(() -> {
            try {
                List<Vehicle> vehicles = vehicleDAO.findByCustomerId(c.getId());
                Platform.runLater(() -> vehiclesTable.setItems(FXCollections.observableArrayList(vehicles)));
            } catch (SQLException e) {
                // ignore
            }
        }).start();
    }

    private void loadServiceHistory(int vehicleId) {
        new Thread(() -> {
            try {
                List<ServiceHistory> history = serviceHistoryDAO.findByVehicleId(vehicleId);
                Platform.runLater(() -> historyTable.setItems(FXCollections.observableArrayList(history)));
            } catch (SQLException e) {
                // ignore
            }
        }).start();
    }

    private void clearForm() {
        custNameField.clear(); custPhoneField.clear(); custEmailField.clear();
        custCompanyField.clear(); custCreditLimitField.clear();
        if (custBalanceLabel != null) custBalanceLabel.setText("Balance: 0.00");
        vehiclesTable.setItems(FXCollections.observableArrayList());
        historyTable.setItems(FXCollections.observableArrayList());
    }
}
