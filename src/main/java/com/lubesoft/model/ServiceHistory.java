package com.lubesoft.model;

public class ServiceHistory {
    private int id;
    private int vehicleId;
    private int invoiceId;
    private int mileage;
    private String oilGrade;
    private String serviceDate;
    private String notes;

    public ServiceHistory() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    public String getOilGrade() { return oilGrade; }
    public void setOilGrade(String oilGrade) { this.oilGrade = oilGrade; }

    public String getServiceDate() { return serviceDate; }
    public void setServiceDate(String serviceDate) { this.serviceDate = serviceDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
