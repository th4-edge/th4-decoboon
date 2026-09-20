package com.th4.edge.decoboon.catalog.service.items.importer;

import java.util.ArrayList;
import java.util.List;

/** 批量导入结果。 */
public class ImportResult {

    private int total;
    private int success;
    private int failed;
    private final List<Failure> failures = new ArrayList<>();

    public static class Failure {
        private final int index;
        private final String name;
        private final String reason;

        public Failure(int index, String name, String reason) {
            this.index = index;
            this.name = name;
            this.reason = reason;
        }

        public int getIndex() { return index; }
        public String getName() { return name; }
        public String getReason() { return reason; }
    }

    public void incrementSuccess() { success++; }
    public void addFailure(int index, String name, String reason) {
        failed++;
        failures.add(new Failure(index, name, reason));
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSuccess() { return success; }
    public int getFailed() { return failed; }
    public List<Failure> getFailures() { return failures; }
}
