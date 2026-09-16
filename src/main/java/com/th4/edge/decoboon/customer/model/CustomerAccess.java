package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
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

/**
 * 微信身份与客户、项目之间的显式业主授权。
 *
 * <p>同一微信身份可授权多个项目，同一项目也可显式授权多个业主；授权不以姓名或手机号模糊匹配。</p>
 */
@Note("业主项目授权")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "cust_customer_access",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_access_credential_project",
                columnNames = {"cre_id", "project_id"}),
        indexes = {
                @Index(name = "idx_customer_access_company", columnList = "com_id, access_id"),
                @Index(name = "idx_customer_access_company_project", columnList = "com_id, project_id, access_id"),
                @Index(name = "idx_customer_access_company_customer", columnList = "com_id, cust_id, access_id"),
                @Index(name = "idx_customer_access_company_credential", columnList = "com_id, cre_id, access_id")
        })
public class CustomerAccess extends AuditModel {

    @Note("授权索引")
    @Id
    @SnowflakeId
    private Long accessId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("微信身份")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cre_id", nullable = false)
    private Credentials credential;

    @Note("显式关联客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_id", nullable = false)
    private Customer customer;

    @Note("显式关联项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("授权来源邀请")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_id")
    private CustomerInvite invite;

    @Note("授权展示名称")
    @Column(length = 32, nullable = false)
    private String displayName;

    @Note("授权状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CustomerAccessStatus status = CustomerAccessStatus.Active;

    @Note("授权时间")
    private LocalDateTime grantedAt;

    @Note("撤销时间")
    private LocalDateTime revokedAt;

    @Note("撤销人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private Employee revokedBy;

    public Long getAccessId() { return accessId; }
    public void setAccessId(Long accessId) { this.accessId = accessId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Credentials getCredential() { return credential; }
    public void setCredential(Credentials credential) { this.credential = credential; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public CustomerInvite getInvite() { return invite; }
    public void setInvite(CustomerInvite invite) { this.invite = invite; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public CustomerAccessStatus getStatus() { return status; }
    public void setStatus(CustomerAccessStatus status) { this.status = status; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public Employee getRevokedBy() { return revokedBy; }
    public void setRevokedBy(Employee revokedBy) { this.revokedBy = revokedBy; }
}
