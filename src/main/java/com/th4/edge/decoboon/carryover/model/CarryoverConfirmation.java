package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverConfirmationRole;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
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

/** 承接项目分角色确认记录。 */
@Note("承接项目确认记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "carryover_confirmation", indexes = {
        @Index(name = "idx_carryover_confirmation_item", columnList = "com_id, item_id, required_role")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_carryover_confirmation_role_version",
                columnNames = {"com_id", "item_id", "required_role", "confirmed_version"})
})
public class CarryoverConfirmation extends AuditModel {

    @Note("确认记录索引")
    @Id
    @SnowflakeId
    private Long confirmationId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属承接项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private CarryoverProject item;

    @Note("需要确认的业务角色")
    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", length = 16, nullable = false)
    private CarryoverConfirmationRole requiredRole;

    @Note("实际确认人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", nullable = false)
    private Employee confirmedBy;

    @Note("是否代确认")
    @Column(nullable = false)
    private Boolean proxy = Boolean.FALSE;

    @Note("确认的草稿版本")
    @Column(name = "confirmed_version", nullable = false)
    private Integer confirmedVersion;

    @Note("确认时间")
    @Column(nullable = false)
    private LocalDateTime confirmedAt;

    @Note("确认摘要")
    @Column(length = 500)
    private String summary;

    public Long getConfirmationId() { return confirmationId; }
    public void setConfirmationId(Long confirmationId) { this.confirmationId = confirmationId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverProject getItem() { return item; }
    public void setItem(CarryoverProject item) { this.item = item; }
    public CarryoverConfirmationRole getRequiredRole() { return requiredRole; }
    public void setRequiredRole(CarryoverConfirmationRole requiredRole) { this.requiredRole = requiredRole; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    public Boolean getProxy() { return proxy; }
    public void setProxy(Boolean proxy) { this.proxy = proxy; }
    public Integer getConfirmedVersion() { return confirmedVersion; }
    public void setConfirmedVersion(Integer confirmedVersion) { this.confirmedVersion = confirmedVersion; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
