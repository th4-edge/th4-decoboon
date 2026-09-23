package com.th4.edge.decoboon.plan.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.plan.configure.PaymentTriggerType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
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

import java.math.BigDecimal;

/** 企业付款计划节点。 */
@Note("企业付款计划节点")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_company_payment_node", indexes = {
        @Index(name = "idx_company_payment_node_company", columnList = "com_id, node_id"),
        @Index(name = "idx_company_payment_node_plan", columnList = "plan_id, sort")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_payment_node_code", columnNames = {"plan_id", "code"})
})
public class CompanyPaymentNode extends AuditModel {

    @Id
    @SnowflakeId
    @Note("节点索引")
    private Long nodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @Note("付款计划模板")
    private CompanyPaymentPlan plan;

    @Column(length = 64, nullable = false)
    @Note("节点编码")
    private String code;

    @Column(length = 128, nullable = false)
    @Note("节点名称")
    private String name;

    @Column(precision = 10, scale = 4, nullable = false)
    @Note("收款比例")
    private BigDecimal ratio = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("应收类型")
    private ReceivableType type = ReceivableType.Progress;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("触发类型")
    private PaymentTriggerType triggerType = PaymentTriggerType.ContractSigned;

    @Column(length = 64)
    @Note("触发阶段编码")
    private String triggerStageCode;

    @Column(nullable = false)
    @Note("偏移天数")
    private Integer offsetDays = 0;

    @Column(nullable = false)
    @Note("排序")
    private Integer sort = 0;

    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CompanyPaymentPlan getPlan() { return plan; }
    public void setPlan(CompanyPaymentPlan plan) { this.plan = plan; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
    public ReceivableType getType() { return type; }
    public void setType(ReceivableType type) { this.type = type; }
    public PaymentTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(PaymentTriggerType triggerType) { this.triggerType = triggerType; }
    public String getTriggerStageCode() { return triggerStageCode; }
    public void setTriggerStageCode(String triggerStageCode) { this.triggerStageCode = triggerStageCode; }
    public Integer getOffsetDays() { return offsetDays; }
    public void setOffsetDays(Integer offsetDays) { this.offsetDays = offsetDays; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
