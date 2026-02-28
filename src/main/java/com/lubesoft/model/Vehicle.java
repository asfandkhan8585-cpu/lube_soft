package com.lubesoft.model;

public class Vehicle {
    private int id;
    private int customerId;
    private String licensePlate;
    private String vin;
    private String make;
    private String model;
    private int year;
    private int mileage;
    private String oilGrade;
    private String notes;

    public Vehicle() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    public String getOilGrade() { return oilGrade; }
    public void setOilGrade(String oilGrade) { this.oilGrade = oilGrade; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return licensePlate + " - " + year + " " + make + " " + model;
    }
}
