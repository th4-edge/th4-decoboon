package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.util.List;

/** 企业经营预警汇总。 */
@Note("经营预警报表")
public class BusinessAlertReport {

    private LocalDate asOf;
    private Integer totalCount = 0;
    private Integer highCount = 0;
    private Integer mediumCount = 0;
    private Integer lowCount = 0;
    private List<BusinessAlert> overdueReceivables = List.of();
    private List<BusinessAlert> suspensionRisks = List.of();
    private List<BusinessAlert> materialOverIssues = List.of();
    private List<BusinessAlert> changeAnomalies = List.of();

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getHighCount() { return highCount; }
    public void setHighCount(Integer highCount) { this.highCount = highCount; }
    public Integer getMediumCount() { return mediumCount; }
    public void setMediumCount(Integer mediumCount) { this.mediumCount = mediumCount; }
    public Integer getLowCount() { return lowCount; }
    public void setLowCount(Integer lowCount) { this.lowCount = lowCount; }
    public List<BusinessAlert> getOverdueReceivables() { return overdueReceivables; }
    public void setOverdueReceivables(List<BusinessAlert> overdueReceivables) { this.overdueReceivables = overdueReceivables; }
    public List<BusinessAlert> getSuspensionRisks() { return suspensionRisks; }
    public void setSuspensionRisks(List<BusinessAlert> suspensionRisks) { this.suspensionRisks = suspensionRisks; }
    public List<BusinessAlert> getMaterialOverIssues() { return materialOverIssues; }
    public void setMaterialOverIssues(List<BusinessAlert> materialOverIssues) { this.materialOverIssues = materialOverIssues; }
    public List<BusinessAlert> getChangeAnomalies() { return changeAnomalies; }
    public void setChangeAnomalies(List<BusinessAlert> changeAnomalies) { this.changeAnomalies = changeAnomalies; }
}
