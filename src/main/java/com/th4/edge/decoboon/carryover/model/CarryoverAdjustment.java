package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.carryover.configure.CarryoverAdjustmentType;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
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
import java.math.BigDecimal;

/** 接管项目锁定后的调整留痕。 */
@Note("承接调整记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "carryover_adjustment", indexes = {
        @Index(name = "idx_carryover_adjustment_item", columnList = "com_id, item_id, adjustment_id")
})
public class CarryoverAdjustment extends AuditModel {

    @Note("调整索引")
    @Id
    @SnowflakeId
    private Long adjustmentId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属承接项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private CarryoverProject item;

    @Note("调整类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CarryoverAdjustmentType type;

    @Note("调整前金额")
    @Unit("元")
    @Column(name = "before_amount", precision = 14, scale = 2)
    private BigDecimal beforeAmount;

    @Note("调整差额")
    @Unit("元")
    @Column(name = "delta_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal deltaAmount;

    @Note("调整后金额")
    @Unit("元")
    @Column(name = "after_amount", precision = 14, scale = 2)
    private BigDecimal afterAmount;

    @Note("调整原因")
    @Column(length = 500, nullable = false)
    private String reason;

    @Note("操作人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Employee createdBy;

    public Long getAdjustmentId() { return adjustmentId; }
    public void setAdjustmentId(Long adjustmentId) { this.adjustmentId = adjustmentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CarryoverProject getItem() { return item; }
    public void setItem(CarryoverProject item) { this.item = item; }
    public CarryoverAdjustmentType getType() { return type; }
    public void setType(CarryoverAdjustmentType type) { this.type = type; }
    public BigDecimal getBeforeAmount() { return beforeAmount; }
    public void setBeforeAmount(BigDecimal beforeAmount) { this.beforeAmount = beforeAmount; }
    public BigDecimal getDeltaAmount() { return deltaAmount; }
    public void setDeltaAmount(BigDecimal deltaAmount) { this.deltaAmount = deltaAmount; }
    public BigDecimal getAfterAmount() { return afterAmount; }
    public void setAfterAmount(BigDecimal afterAmount) { this.afterAmount = afterAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
