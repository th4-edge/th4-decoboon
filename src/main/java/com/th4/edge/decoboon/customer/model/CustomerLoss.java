package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerLossReason;
import com.th4.edge.decoboon.customer.configure.CustomerLossScope;
import com.th4.edge.decoboon.customer.configure.CustomerLossStage;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** 客户签约前流失事实。记录本轮经营机会的结束原因，不删除或回写历史业务对象。 */
@Note("客户流失记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "cust_customer_loss", indexes = {
        @Index(name = "idx_customer_loss_company", columnList = "com_id, loss_id"),
        @Index(name = "idx_customer_loss_customer_active", columnList = "com_id, cust_id, restored_at, loss_id"),
        @Index(name = "idx_customer_loss_property_active", columnList = "com_id, property_id, restored_at, loss_id"),
        @Index(name = "idx_customer_loss_stage_reason", columnList = "com_id, loss_stage, reason_code, lost_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_loss_request", columnNames = {"com_id", "request_id"})
})
public class CustomerLoss extends AuditModel {

    @Id
    @SnowflakeId
    @Note("流失记录索引")
    private Long lossId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_id", nullable = false)
    @Note("客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    @Note("流失房屋；客户整体流失时为空")
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(name = "loss_scope", length = 16, nullable = false)
    @Note("流失范围")
    private CustomerLossScope scope = CustomerLossScope.Customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "loss_stage", length = 24, nullable = false)
    @Note("流失阶段")
    private CustomerLossStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 16)
    @Note("流失前客户状态")
    private CustStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 32, nullable = false)
    @Note("流失原因")
    private CustomerLossReason reasonCode;

    @Column(name = "reason_text", length = 500)
    @Note("流失原因说明")
    private String reasonText;

    @Column(name = "competitor_name", length = 100)
    @Note("竞争对手名称")
    private String competitorName;

    @Column(name = "lost_at", nullable = false)
    @Note("流失时间")
    private LocalDateTime lostAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_by", nullable = false)
    @Note("流失操作人")
    private Employee lostBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_role", length = 24, nullable = false)
    @Note("操作时角色")
    private EmployeeRole operatorRole;

    @Column(name = "request_id", length = 96, nullable = false)
    @Note("幂等请求号")
    private String requestId;

    @Column(name = "restored_at")
    @Note("恢复时间")
    private LocalDateTime restoredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restored_by")
    @Note("恢复操作人")
    private Employee restoredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "restore_status", length = 16)
    @Note("恢复后的客户状态")
    private CustStatus restoreStatus;

    @Column(name = "restore_note", length = 500)
    @Note("恢复说明")
    private String restoreNote;

    @Column(name = "restore_request_id", length = 96)
    @Note("恢复幂等请求号")
    private String restoreRequestId;

    public Long getLossId() { return lossId; }
    public void setLossId(Long lossId) { this.lossId = lossId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public CustomerLossScope getScope() { return scope; }
    public void setScope(CustomerLossScope scope) { this.scope = scope; }
    public CustomerLossStage getStage() { return stage; }
    public void setStage(CustomerLossStage stage) { this.stage = stage; }
    public CustStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(CustStatus fromStatus) { this.fromStatus = fromStatus; }
    public CustomerLossReason getReasonCode() { return reasonCode; }
    public void setReasonCode(CustomerLossReason reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    public String getCompetitorName() { return competitorName; }
    public void setCompetitorName(String competitorName) { this.competitorName = competitorName; }
    public LocalDateTime getLostAt() { return lostAt; }
    public void setLostAt(LocalDateTime lostAt) { this.lostAt = lostAt; }
    public Employee getLostBy() { return lostBy; }
    public void setLostBy(Employee lostBy) { this.lostBy = lostBy; }
    public EmployeeRole getOperatorRole() { return operatorRole; }
    public void setOperatorRole(EmployeeRole operatorRole) { this.operatorRole = operatorRole; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getRestoredAt() { return restoredAt; }
    public void setRestoredAt(LocalDateTime restoredAt) { this.restoredAt = restoredAt; }
    public Employee getRestoredBy() { return restoredBy; }
    public void setRestoredBy(Employee restoredBy) { this.restoredBy = restoredBy; }
    public CustStatus getRestoreStatus() { return restoreStatus; }
    public void setRestoreStatus(CustStatus restoreStatus) { this.restoreStatus = restoreStatus; }
    public String getRestoreNote() { return restoreNote; }
    public void setRestoreNote(String restoreNote) { this.restoreNote = restoreNote; }
    public String getRestoreRequestId() { return restoreRequestId; }
    public void setRestoreRequestId(String restoreRequestId) { this.restoreRequestId = restoreRequestId; }
}
