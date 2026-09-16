package com.th4.edge.tenant.billing.model;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.billing.configure.LedgerBizType;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 企业余额流水。
 */
@Note("余额流水")
@Description("套餐发放、续费到账、人工调整的审计记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_balance_ledger")
public class BalanceLedger extends AbstractModel {

    @Note("流水索引")
    @Id
    @SnowflakeId
    private Long ledgerId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("关联订单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Note("业务类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "biz_type", length = 16, nullable = false)
    private LedgerBizType bizType;

    @Note("变动天数")
    @Unit("天")
    @Column(nullable = false)
    private Integer changeDays;

    @Note("变动后余额")
    @Unit("天")
    @Column(nullable = false)
    private Integer balanceAfter;

    @Note("备注")
    @Keyword
    @Column(length = 256)
    private String remark;

    @Note("创建时间")
    @Column(name = "create_time")
    private LocalDateTime createTime;

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public LedgerBizType getBizType() {
        return bizType;
    }

    public void setBizType(LedgerBizType bizType) {
        this.bizType = bizType;
    }

    public Integer getChangeDays() {
        return changeDays;
    }

    public void setChangeDays(Integer changeDays) {
        this.changeDays = changeDays;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
