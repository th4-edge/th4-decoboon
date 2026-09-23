package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/** 企业负责人增减项日期窗口。 */
@Note("企业负责人增减项日期窗口")
public class BossContractChangePeriod {

    private String key;
    private String label;
    private Integer createdCount;
    private Integer confirmedCount;
    private BigDecimal confirmedAmount;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getCreatedCount() { return createdCount; }
    public void setCreatedCount(Integer createdCount) { this.createdCount = createdCount; }
    public Integer getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(Integer confirmedCount) { this.confirmedCount = confirmedCount; }
    public BigDecimal getConfirmedAmount() { return confirmedAmount; }
    public void setConfirmedAmount(BigDecimal confirmedAmount) { this.confirmedAmount = confirmedAmount; }
}
