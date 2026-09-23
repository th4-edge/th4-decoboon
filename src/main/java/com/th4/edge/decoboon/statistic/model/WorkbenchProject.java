package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 企业负责人首页需要介入的风险项目。 */
@Note("首页风险项目")
public class WorkbenchProject {

    private Long projectId;
    private String no;
    private String name;
    private ProjectStatus status;
    private Integer progress;
    private String managerName;
    private BusinessAlertLevel riskLevel;
    private Integer riskCount;
    private BusinessAlertType mainRiskType;
    private String riskReason;
    private String nextAction;
    private BigDecimal amount;
    private LocalDate dueDate;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public BusinessAlertLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(BusinessAlertLevel riskLevel) { this.riskLevel = riskLevel; }
    public Integer getRiskCount() { return riskCount; }
    public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    public BusinessAlertType getMainRiskType() { return mainRiskType; }
    public void setMainRiskType(BusinessAlertType mainRiskType) { this.mainRiskType = mainRiskType; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
