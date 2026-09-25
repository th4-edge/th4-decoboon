package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.ArrayList;
import java.util.List;

/** 客户批量手机号解析预览与导入结果。 */
@Note("客户批量手机号解析结果")
public class CustomerBatchPreview {

    private int total;
    private int validCount;
    private int duplicateCount;
    private int invalidCount;
    private int createdCount;
    private List<Item> items = new ArrayList<>();

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getValidCount() { return validCount; }
    public void setValidCount(int validCount) { this.validCount = validCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
    public int getInvalidCount() { return invalidCount; }
    public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }
    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    /** 单条解析结果。 */
    @Note("客户批量手机号解析明细")
    public static class Item {
        private String input;
        private String phone;
        private String status;
        private String reason;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
