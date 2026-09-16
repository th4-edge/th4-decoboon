package com.th4.edge.decoboon.customer.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

/** 业主同频邀请。 */
@Note("业主邀请")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "cust_customer_invite", indexes = {
        @Index(name = "idx_customer_invite_company", columnList = "com_id, invite_id"),
        @Index(name = "idx_customer_invite_company_project", columnList = "com_id, project_id, invite_id")
})
public class CustomerInvite extends AuditModel {

    @Id
    @SnowflakeId
    @Note("邀请索引")
    private Long inviteId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("关联项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("显式关联客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Note("邀请人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private Employee inviter;

    @Note("接受邀请的微信身份")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private Credentials acceptedBy;

    @Note("撤销人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private Employee cancelledBy;

    @Note("受邀人名称")
    @Column(length = 32, nullable = false)
    private String customerName;

    @Note("受邀手机号")
    @Column(length = 20)
    private String telNo;

    @Note("一次性邀请 token")
    @Column(length = 64, nullable = false, unique = true)
    private String token;

    @Note("邀请状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private InviteStatus status = InviteStatus.Pending;

    @Note("过期时间")
    private LocalDateTime expireTime;

    @Note("接受时间")
    private LocalDateTime acceptedAt;

    @Note("撤销时间")
    private LocalDateTime cancelledAt;

    @Note("脱敏手机号；仅用于公开查询响应")
    @Transient
    private String maskedTelNo;

    public Long getInviteId() { return inviteId; }
    public void setInviteId(Long inviteId) { this.inviteId = inviteId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Employee getInviter() { return inviter; }
    public void setInviter(Employee inviter) { this.inviter = inviter; }
    public Credentials getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(Credentials acceptedBy) { this.acceptedBy = acceptedBy; }
    public Employee getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Employee cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public InviteStatus getStatus() { return status; }
    public void setStatus(InviteStatus status) { this.status = status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getMaskedTelNo() { return maskedTelNo; }
    public void setMaskedTelNo(String maskedTelNo) { this.maskedTelNo = maskedTelNo; }
}
