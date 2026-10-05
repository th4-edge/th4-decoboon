package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverPaymentDirection;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 剩余收付款计划：期初应收/应付的时间分解。
 *
 * <p>不是新增应收应付；计划合计不得超过对应期初余额，不足部分视为未排期。</p>
 */
@Note("剩余收付款计划")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "carryover_payment_plan", indexes = {
        @Index(name = "idx_carryover_payment_item", columnList = "com_id, item_id, direction, plan_id"),
        @Index(name = "idx_carryover_payment_company", columnList = "com_id, plan_id")
})
public class CarryoverPaymentPlan extends AuditModel {

    @Id
    @SnowflakeId
    @Note("计划索引")
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @Note("所属承接项目")
    private CarryoverProject item;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("计划方向")
    private CarryoverPaymentDirection direction;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    @Note("计划金额")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "planned_date")
    @Note("计划日期；为空表示未排期")
    private LocalDate plannedDate;

    @Column(length = 128)
    @Note("往来对象")
    private String counterparty;

    @Column(length = 500)
    @Note("备注")
    private String remark;

    @Column(nullable = false)
    @Note("排序")
    private Integer sort = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @Note("创建人")
    private Employee createdBy;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverProject getItem() { return item; }
    public void setItem(CarryoverProject item) { this.item = item; }
    public CarryoverPaymentDirection getDirection() { return direction; }
    public void setDirection(CarryoverPaymentDirection direction) { this.direction = direction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
