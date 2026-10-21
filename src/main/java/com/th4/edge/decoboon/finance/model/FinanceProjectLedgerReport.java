package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 项目账本筛选结果汇总。 */
@Note("项目账本汇总")
public class FinanceProjectLedgerReport {

    @Note("匹配项目数")
    private Integer projectCount = 0;

    @Note("在跑项目数")
    private Integer activeProjectCount = 0;

    @Unit("元")
    @Note("项目合同金额合计")
    private BigDecimal contractAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("累计回款金额合计")
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("应收金额合计")
    private BigDecimal receivableAmount = BigDecimal.ZERO;

    @Unit("百分比")
    @Note("回款率")
    private BigDecimal collectionRate = BigDecimal.ZERO;

    @Note("成本是否全部完整")
    private Boolean costComplete = Boolean.TRUE;

    @Note("成本不完整项目数")
    private Integer costIncompleteProjectCount = 0;

    @Note("数据截止时间")
    private LocalDateTime asOf;

    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getActiveProjectCount() { return activeProjectCount; }
    public void setActiveProjectCount(Integer activeProjectCount) { this.activeProjectCount = activeProjectCount; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(BigDecimal receivableAmount) { this.receivableAmount = receivableAmount; }
    public BigDecimal getCollectionRate() { return collectionRate; }
    public void setCollectionRate(BigDecimal collectionRate) { this.collectionRate = collectionRate; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public Integer getCostIncompleteProjectCount() { return costIncompleteProjectCount; }
    public void setCostIncompleteProjectCount(Integer costIncompleteProjectCount) { this.costIncompleteProjectCount = costIncompleteProjectCount; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
}
