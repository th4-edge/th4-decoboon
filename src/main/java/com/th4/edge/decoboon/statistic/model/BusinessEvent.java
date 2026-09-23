package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.statistic.configure.BusinessEventCategory;
import com.th4.edge.decoboon.statistic.configure.BusinessEventLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessEventType;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 企业负责人经营动态事件。 */
@Note("经营动态事件")
public class BusinessEvent {

    private String eventId;
    private BusinessEventCategory category;
    private BusinessEventType type;
    private BusinessEventLevel level;
    private String title;
    private String content;
    private LocalDateTime occurredAt;
    private BigDecimal amount;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Long projectId;
    private String projectName;
    private Long customerId;
    private String customerName;
    private String ownerName;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BusinessEventCategory getCategory() { return category; }
    public void setCategory(BusinessEventCategory category) { this.category = category; }
    public BusinessEventType getType() { return type; }
    public void setType(BusinessEventType type) { this.type = type; }
    public BusinessEventLevel getLevel() { return level; }
    public void setLevel(BusinessEventLevel level) { this.level = level; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
