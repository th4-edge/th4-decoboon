package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.statistic.configure.ActivationStatus;
import com.th4.edge.decoboon.statistic.configure.ChurnRiskLevel;
import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 当前企业用户成功与流失风险只读投影。 */
@Note("用户成功与流失风险")
public class SuccessProgramReport {

    private String companyName;
    private LocalDate startDate;
    private long daysSinceCreated;
    private ActivationStatus activationStatus = ActivationStatus.NotActivated;
    private int activationScore;
    private int roleActivationRate;
    private ChurnRiskLevel riskLevel = ChurnRiskLevel.Low;
    private String nextAction;
    private String nextActionLabel;
    private String nextActionPath;
    private List<String> riskReasons = new ArrayList<>();
    private List<SuccessMilestone> milestones = new ArrayList<>();
    private List<RoleActivation> roleActivations = new ArrayList<>();
    private LocalDateTime refreshedAt;

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public long getDaysSinceCreated() { return daysSinceCreated; }
    public void setDaysSinceCreated(long daysSinceCreated) { this.daysSinceCreated = daysSinceCreated; }
    public ActivationStatus getActivationStatus() { return activationStatus; }
    public void setActivationStatus(ActivationStatus activationStatus) { this.activationStatus = activationStatus; }
    public int getActivationScore() { return activationScore; }
    public void setActivationScore(int activationScore) { this.activationScore = activationScore; }
    public int getRoleActivationRate() { return roleActivationRate; }
    public void setRoleActivationRate(int roleActivationRate) { this.roleActivationRate = roleActivationRate; }
    public ChurnRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ChurnRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getNextActionLabel() { return nextActionLabel; }
    public void setNextActionLabel(String nextActionLabel) { this.nextActionLabel = nextActionLabel; }
    public String getNextActionPath() { return nextActionPath; }
    public void setNextActionPath(String nextActionPath) { this.nextActionPath = nextActionPath; }
    public List<String> getRiskReasons() { return riskReasons; }
    public void setRiskReasons(List<String> riskReasons) { this.riskReasons = riskReasons; }
    public List<SuccessMilestone> getMilestones() { return milestones; }
    public void setMilestones(List<SuccessMilestone> milestones) { this.milestones = milestones; }
    public List<RoleActivation> getRoleActivations() { return roleActivations; }
    public void setRoleActivations(List<RoleActivation> roleActivations) { this.roleActivations = roleActivations; }
    public LocalDateTime getRefreshedAt() { return refreshedAt; }
    public void setRefreshedAt(LocalDateTime refreshedAt) { this.refreshedAt = refreshedAt; }
}
