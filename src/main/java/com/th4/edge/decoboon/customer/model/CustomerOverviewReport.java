package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.util.List;

/** 企业负责人客户经营聚合结果。 */
@Note("企业负责人客户经营聚合")
public class CustomerOverviewReport {
    private LocalDate asOf;
    private LocalDate month;
    private List<CustomerMetricCard> metrics = List.of();
    private List<CustomerStageCount> stageCounts = List.of();
    private List<CustomerOwnerGroup> ownerGroups = List.of();
    private List<Customer> customers = List.of();
    private Integer current = 1;
    private Integer size = 20;
    private Long total = 0L;
    private Boolean hasMore = Boolean.FALSE;

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public List<CustomerMetricCard> getMetrics() { return metrics; }
    public void setMetrics(List<CustomerMetricCard> metrics) { this.metrics = metrics == null ? List.of() : List.copyOf(metrics); }
    public List<CustomerStageCount> getStageCounts() { return stageCounts; }
    public void setStageCounts(List<CustomerStageCount> stageCounts) { this.stageCounts = stageCounts == null ? List.of() : List.copyOf(stageCounts); }
    public List<CustomerOwnerGroup> getOwnerGroups() { return ownerGroups; }
    public void setOwnerGroups(List<CustomerOwnerGroup> ownerGroups) {
        this.ownerGroups = ownerGroups == null ? List.of() : List.copyOf(ownerGroups);
    }
    public List<Customer> getCustomers() { return customers; }
    public void setCustomers(List<Customer> customers) { this.customers = customers == null ? List.of() : List.copyOf(customers); }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Boolean getHasMore() { return hasMore; }
    public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }
}
