package com.th4.edge.decoboon.quote.service.workbook.importer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    private int total;
    private int valid;
    private int failed;
    private final List<Row> rows = new ArrayList<>();

    public static class Row {
        private int excelRow;
        private Long itemId;
        private String region;
        private String name;
        private BigDecimal quantity;
        private String quantityFormula;
        private BigDecimal materialPrice;
        private BigDecimal manualPrice;
        private boolean importable;
        private final List<String> errors = new ArrayList<>();

        public int getExcelRow() { return excelRow; }
        public void setExcelRow(int excelRow) { this.excelRow = excelRow; }
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public String getQuantityFormula() { return quantityFormula; }
        public void setQuantityFormula(String quantityFormula) { this.quantityFormula = quantityFormula; }
        public BigDecimal getMaterialPrice() { return materialPrice; }
        public void setMaterialPrice(BigDecimal materialPrice) { this.materialPrice = materialPrice; }
        public BigDecimal getManualPrice() { return manualPrice; }
        public void setManualPrice(BigDecimal manualPrice) { this.manualPrice = manualPrice; }
        public boolean isImportable() { return importable; }
        public void setImportable(boolean importable) { this.importable = importable; }
        public List<String> getErrors() { return errors; }
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getValid() { return valid; }
    public void setValid(int valid) { this.valid = valid; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public List<Row> getRows() { return rows; }
}
