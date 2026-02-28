package com.lubesoft.model;

import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    private int id;
    private String status;
    private String createdAt;
    private String notes;
    private List<POItem> items = new ArrayList<>();

    public PurchaseOrder() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<POItem> getItems() { return items; }
    public void setItems(List<POItem> items) { this.items = items; }

    public static class POItem {
        private int id;
        private int poId;
        private int productId;
        private double qtyOrdered;
        private double unitCost;
        private double receivedQty;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getPoId() { return poId; }
        public void setPoId(int poId) { this.poId = poId; }

        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }

        public double getQtyOrdered() { return qtyOrdered; }
        public void setQtyOrdered(double qtyOrdered) { this.qtyOrdered = qtyOrdered; }

        public double getUnitCost() { return unitCost; }
        public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

        public double getReceivedQty() { return receivedQty; }
        public void setReceivedQty(double receivedQty) { this.receivedQty = receivedQty; }
    }
}
