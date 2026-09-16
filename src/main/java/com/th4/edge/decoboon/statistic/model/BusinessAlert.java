package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 可下钻到业务对象的经营预警。 */
@Note("经营预警")
public class BusinessAlert {

    private BusinessAlertType type;
    private BusinessAlertLevel level;
    private String title;
    private String message;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Project project;
    private Employee owner;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal rate = BigDecimal.ZERO;
    private LocalDate dueDate;

    public BusinessAlertType getType() { return type; }
    public void setType(BusinessAlertType type) { this.type = type; }
    public BusinessAlertLevel getLevel() { return level; }
    public void setLevel(BusinessAlertLevel level) { this.level = level; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
