package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 剩余收付款计划汇总：期初余额、已排期、未排期。 */
@Note("剩余收付款计划汇总")
public class CarryoverPaymentPlanReport {

    private Long itemId;
    @Unit("元") private BigDecimal receivableOutstanding = BigDecimal.ZERO;
    @Unit("元") private BigDecimal payableOutstanding = BigDecimal.ZERO;
    @Unit("元") private BigDecimal plannedReceivable = BigDecimal.ZERO;
    @Unit("元") private BigDecimal plannedPayable = BigDecimal.ZERO;
    @Unit("元") private BigDecimal unplannedReceivable = BigDecimal.ZERO;
    @Unit("元") private BigDecimal unplannedPayable = BigDecimal.ZERO;
    private List<CarryoverPaymentPlan> plans = new ArrayList<>();

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public BigDecimal getReceivableOutstanding() { return receivableOutstanding; }
    public void setReceivableOutstanding(BigDecimal receivableOutstanding) { this.receivableOutstanding = receivableOutstanding; }
    public BigDecimal getPayableOutstanding() { return payableOutstanding; }
    public void setPayableOutstanding(BigDecimal payableOutstanding) { this.payableOutstanding = payableOutstanding; }
    public BigDecimal getPlannedReceivable() { return plannedReceivable; }
    public void setPlannedReceivable(BigDecimal plannedReceivable) { this.plannedReceivable = plannedReceivable; }
    public BigDecimal getPlannedPayable() { return plannedPayable; }
    public void setPlannedPayable(BigDecimal plannedPayable) { this.plannedPayable = plannedPayable; }
    public BigDecimal getUnplannedReceivable() { return unplannedReceivable; }
    public void setUnplannedReceivable(BigDecimal unplannedReceivable) { this.unplannedReceivable = unplannedReceivable; }
    public BigDecimal getUnplannedPayable() { return unplannedPayable; }
    public void setUnplannedPayable(BigDecimal unplannedPayable) { this.unplannedPayable = unplannedPayable; }
    public List<CarryoverPaymentPlan> getPlans() { return plans; }
    public void setPlans(List<CarryoverPaymentPlan> plans) { this.plans = plans == null ? new ArrayList<>() : plans; }
}
