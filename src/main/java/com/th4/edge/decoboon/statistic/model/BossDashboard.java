package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 企业负责人经营驾驶舱四联看板。 */
@Note("企业负责人经营驾驶舱")
public class BossDashboard {

    private LocalDate month;
    private DashboardMetric signedAmount = new DashboardMetric();
    private DashboardMetric receivedAmount = new DashboardMetric();
    private DashboardMetric grossProfit = new DashboardMetric();
    private DashboardMetric activeProjectCount = new DashboardMetric();

    /** 门店维度范围标注（`DEC-067` BOSS-STORE-03）：全部门店视图带未归属条数，单店恒 0。 */
    private StoreScopeInfo storeScope;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public DashboardMetric getSignedAmount() { return signedAmount; }
    public void setSignedAmount(DashboardMetric signedAmount) { this.signedAmount = signedAmount; }
    public DashboardMetric getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(DashboardMetric receivedAmount) { this.receivedAmount = receivedAmount; }
    public DashboardMetric getGrossProfit() { return grossProfit; }
    public void setGrossProfit(DashboardMetric grossProfit) { this.grossProfit = grossProfit; }
    public DashboardMetric getActiveProjectCount() { return activeProjectCount; }
    public void setActiveProjectCount(DashboardMetric activeProjectCount) { this.activeProjectCount = activeProjectCount; }

    public StoreScopeInfo getStoreScope() { return storeScope; }
    public void setStoreScope(StoreScopeInfo storeScope) { this.storeScope = storeScope; }
}
