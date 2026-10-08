package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerDormantReason;
import com.th4.edge.decoboon.customer.configure.CustomerStatusChangeType;
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

/** 客户休眠/恢复状态变更日志，不可承载或修改业务金额。 */
@Note("客户状态变更日志")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "cust_customer_status_log", indexes = {
        @Index(name = "idx_customer_status_log_customer", columnList = "com_id, cust_id, log_id"),
        @Index(name = "idx_customer_status_log_status", columnList = "com_id, to_status, log_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_status_log_request", columnNames = {"com_id", "request_id"})
})
public class CustomerStatusLog extends AuditModel {

    @Id
    @SnowflakeId
    @Note("状态日志索引")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_id", nullable = false)
    @Note("客户")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 16)
    @Note("原状态")
    private CustStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 16, nullable = false)
    @Note("新状态")
    private CustStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 24, nullable = false)
    @Note("变更类型")
    private CustomerStatusChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 32)
    @Note("原因编码")
    private CustomerDormantReason reasonCode;

    @Column(name = "reason_text", length = 200)
    @Note("原因说明")
    private String reasonText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    @Note("操作人")
    private Employee operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_role", length = 24, nullable = false)
    @Note("操作时角色")
    private EmployeeRole operatorRole;

    @Column(name = "request_id", length = 96)
    @Note("幂等请求号")
    private String requestId;

    @Column(name = "changed_at", nullable = false)
    @Note("变更时间")
    private LocalDateTime changedAt = LocalDateTime.now();

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public CustStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(CustStatus fromStatus) { this.fromStatus = fromStatus; }
    public CustStatus getToStatus() { return toStatus; }
    public void setToStatus(CustStatus toStatus) { this.toStatus = toStatus; }
    public CustomerStatusChangeType getChangeType() { return changeType; }
    public void setChangeType(CustomerStatusChangeType changeType) { this.changeType = changeType; }
    public CustomerDormantReason getReasonCode() { return reasonCode; }
    public void setReasonCode(CustomerDormantReason reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    public Employee getOperator() { return operator; }
    public void setOperator(Employee operator) { this.operator = operator; }
    public EmployeeRole getOperatorRole() { return operatorRole; }
    public void setOperatorRole(EmployeeRole operatorRole) { this.operatorRole = operatorRole; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
