package com.lubesoft.model;

public class InventoryTransaction {
    private int id;
    private int productId;
    private String type; // SALE, PURCHASE, ADJUSTMENT, WASTE
    private double qtyChange;
    private int referenceId;
    private String createdAt;
    private String notes;

    public InventoryTransaction() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getQtyChange() { return qtyChange; }
    public void setQtyChange(double qtyChange) { this.qtyChange = qtyChange; }

    public int getReferenceId() { return referenceId; }
    public void setReferenceId(int referenceId) { this.referenceId = referenceId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
