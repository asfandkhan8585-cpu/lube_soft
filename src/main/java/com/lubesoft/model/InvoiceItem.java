package com.lubesoft.model;

public class InvoiceItem {
    private int id;
    private int invoiceId;
    private int productId;
    private String description;
    private double qty;
    private double unitPrice;
    private double costPrice;
    private double total;

    public InvoiceItem() {}

    public InvoiceItem(int productId, String description, double qty, double unitPrice, double costPrice) {
        this.productId = productId;
        this.description = description;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.costPrice = costPrice;
        this.total = qty * unitPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getQty() { return qty; }
    public void setQty(double qty) {
        this.qty = qty;
        this.total = qty * unitPrice;
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.total = qty * unitPrice;
    }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
