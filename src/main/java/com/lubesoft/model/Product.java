package com.lubesoft.model;

public class Product {
    private int id;
    private String sku;
    private String name;
    private String category;
    private String unit;
    private double sellPrice;
    private double costPrice;
    private double stockQty;
    private double minStock;
    private double maxStock;
    private String barcode;
    private boolean isBulkOil;

    public Product() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public double getStockQty() { return stockQty; }
    public void setStockQty(double stockQty) { this.stockQty = stockQty; }

    public double getMinStock() { return minStock; }
    public void setMinStock(double minStock) { this.minStock = minStock; }

    public double getMaxStock() { return maxStock; }
    public void setMaxStock(double maxStock) { this.maxStock = maxStock; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public boolean isBulkOil() { return isBulkOil; }
    public void setBulkOil(boolean bulkOil) { isBulkOil = bulkOil; }

    public boolean isLowStock() { return stockQty <= minStock; }

    @Override
    public String toString() { return name + " (SKU: " + sku + ")"; }
}
