package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 管理层人员自动考核报表。 */
@Note("人员绩效报表")
public class PerformanceReport {

    private LocalDate month;
    private Integer designerCount = 0;
    private Integer marketerCount = 0;
    private Integer pmCount = 0;
    private BigDecimal averageDesignerConversionRate = BigDecimal.ZERO;
    private BigDecimal averageMarketerSignRate = BigDecimal.ZERO;
    private BigDecimal averagePmOnTimeDeliveryRate = BigDecimal.ZERO;
    private BigDecimal averagePmAcceptancePassRate = BigDecimal.ZERO;
    private List<EmployeePerformance> performances = List.of();

    /** 门店维度范围标注（`DEC-067` BOSS-STORE-03）：全部门店视图带未归属条数，单店恒 0。 */
    private StoreScopeInfo storeScope;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public Integer getDesignerCount() { return designerCount; }
    public void setDesignerCount(Integer designerCount) { this.designerCount = designerCount; }
    public Integer getMarketerCount() { return marketerCount; }
    public void setMarketerCount(Integer marketerCount) { this.marketerCount = marketerCount; }
    public Integer getPmCount() { return pmCount; }
    public void setPmCount(Integer pmCount) { this.pmCount = pmCount; }
    public BigDecimal getAverageDesignerConversionRate() { return averageDesignerConversionRate; }
    public void setAverageDesignerConversionRate(BigDecimal averageDesignerConversionRate) { this.averageDesignerConversionRate = averageDesignerConversionRate; }
    public BigDecimal getAverageMarketerSignRate() { return averageMarketerSignRate; }
    public void setAverageMarketerSignRate(BigDecimal averageMarketerSignRate) { this.averageMarketerSignRate = averageMarketerSignRate; }
    public BigDecimal getAveragePmOnTimeDeliveryRate() { return averagePmOnTimeDeliveryRate; }
    public void setAveragePmOnTimeDeliveryRate(BigDecimal averagePmOnTimeDeliveryRate) { this.averagePmOnTimeDeliveryRate = averagePmOnTimeDeliveryRate; }
    public BigDecimal getAveragePmAcceptancePassRate() { return averagePmAcceptancePassRate; }
    public void setAveragePmAcceptancePassRate(BigDecimal averagePmAcceptancePassRate) { this.averagePmAcceptancePassRate = averagePmAcceptancePassRate; }
    public List<EmployeePerformance> getPerformances() { return performances; }
    public void setPerformances(List<EmployeePerformance> performances) { this.performances = performances; }

    public StoreScopeInfo getStoreScope() { return storeScope; }
    public void setStoreScope(StoreScopeInfo storeScope) { this.storeScope = storeScope; }
}
