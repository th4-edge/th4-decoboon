package com.th4.edge.decoboon.payroll.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;

/** 薪酬工作台聚合投影。 */
@Note("薪酬工作台")
public class PayrollWorkbenchReport {

    private String period;
    private Integer pendingAttendanceCount = 0;
    private Integer draftSheetCount = 0;
    private Integer pendingSheetCount = 0;
    private Integer confirmedSheetCount = 0;
    private Integer partialPaidSheetCount = 0;
    private Integer paidSheetCount = 0;
    @Unit("元")
    private BigDecimal pendingEmployeePayableAmount = BigDecimal.ZERO;
    @Unit("元")
    private BigDecimal releasedUnpaidCommissionAmount = BigDecimal.ZERO;
    private Integer pendingPayslipCount = 0;
    private Integer publishedPayslipCount = 0;
    private Boolean currentPeriodHasSheet = Boolean.FALSE;
    private Long latestSheetId;
    private String latestSheetPeriod;
    private PayrollSheetStatus latestSheetStatus;
    @Unit("元")
    private BigDecimal latestSheetTotalNetAmount = BigDecimal.ZERO;

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Integer getPendingAttendanceCount() { return pendingAttendanceCount; }
    public void setPendingAttendanceCount(Integer pendingAttendanceCount) { this.pendingAttendanceCount = pendingAttendanceCount; }
    public Integer getDraftSheetCount() { return draftSheetCount; }
    public void setDraftSheetCount(Integer draftSheetCount) { this.draftSheetCount = draftSheetCount; }
    public Integer getPendingSheetCount() { return pendingSheetCount; }
    public void setPendingSheetCount(Integer pendingSheetCount) { this.pendingSheetCount = pendingSheetCount; }
    public Integer getConfirmedSheetCount() { return confirmedSheetCount; }
    public void setConfirmedSheetCount(Integer confirmedSheetCount) { this.confirmedSheetCount = confirmedSheetCount; }
    public Integer getPartialPaidSheetCount() { return partialPaidSheetCount; }
    public void setPartialPaidSheetCount(Integer partialPaidSheetCount) { this.partialPaidSheetCount = partialPaidSheetCount; }
    public Integer getPaidSheetCount() { return paidSheetCount; }
    public void setPaidSheetCount(Integer paidSheetCount) { this.paidSheetCount = paidSheetCount; }
    public BigDecimal getPendingEmployeePayableAmount() { return pendingEmployeePayableAmount; }
    public void setPendingEmployeePayableAmount(BigDecimal pendingEmployeePayableAmount) { this.pendingEmployeePayableAmount = pendingEmployeePayableAmount; }
    public BigDecimal getReleasedUnpaidCommissionAmount() { return releasedUnpaidCommissionAmount; }
    public void setReleasedUnpaidCommissionAmount(BigDecimal releasedUnpaidCommissionAmount) { this.releasedUnpaidCommissionAmount = releasedUnpaidCommissionAmount; }
    public Integer getPendingPayslipCount() { return pendingPayslipCount; }
    public void setPendingPayslipCount(Integer pendingPayslipCount) { this.pendingPayslipCount = pendingPayslipCount; }
    public Integer getPublishedPayslipCount() { return publishedPayslipCount; }
    public void setPublishedPayslipCount(Integer publishedPayslipCount) { this.publishedPayslipCount = publishedPayslipCount; }
    public Boolean getCurrentPeriodHasSheet() { return currentPeriodHasSheet; }
    public void setCurrentPeriodHasSheet(Boolean currentPeriodHasSheet) { this.currentPeriodHasSheet = currentPeriodHasSheet; }
    public Long getLatestSheetId() { return latestSheetId; }
    public void setLatestSheetId(Long latestSheetId) { this.latestSheetId = latestSheetId; }
    public String getLatestSheetPeriod() { return latestSheetPeriod; }
    public void setLatestSheetPeriod(String latestSheetPeriod) { this.latestSheetPeriod = latestSheetPeriod; }
    public PayrollSheetStatus getLatestSheetStatus() { return latestSheetStatus; }
    public void setLatestSheetStatus(PayrollSheetStatus latestSheetStatus) { this.latestSheetStatus = latestSheetStatus; }
    public BigDecimal getLatestSheetTotalNetAmount() { return latestSheetTotalNetAmount; }
    public void setLatestSheetTotalNetAmount(BigDecimal latestSheetTotalNetAmount) { this.latestSheetTotalNetAmount = latestSheetTotalNetAmount; }
}
