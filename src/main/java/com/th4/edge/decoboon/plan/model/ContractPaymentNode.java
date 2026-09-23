package com.th4.edge.decoboon.plan.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.plan.configure.PaymentTriggerType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 合同级付款节点快照。 */
@Note("合同付款节点快照")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_contract_payment_node", indexes = {
        @Index(name = "idx_contract_payment_node_company", columnList = "com_id, node_id"),
        @Index(name = "idx_contract_payment_node_plan", columnList = "plan_id, sort")
})
public class ContractPaymentNode extends AuditModel {
    @Id @SnowflakeId @Note("付款节点索引") private Long nodeId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false) @Note("付款计划") private ContractPaymentPlan plan;
    @Column(length = 64, nullable = false) @Note("节点编码") private String code;
    @Column(length = 128, nullable = false) @Note("节点名称") private String name;
    @Column(precision = 8, scale = 4, nullable = false) @Note("付款比例") private BigDecimal ratio = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 14, scale = 2, nullable = false) @Note("节点金额") private BigDecimal amount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("应收类型") private ReceivableType type = ReceivableType.Progress;
    @Enumerated(EnumType.STRING) @Column(length = 24, nullable = false) @Note("触发类型") private PaymentTriggerType triggerType = PaymentTriggerType.ContractSigned;
    @Column(length = 64) @Note("触发阶段编码") private String triggerStageCode;
    @Column(length = 128) @Note("触发阶段名称") private String triggerStageName;
    @Column(nullable = false) @Note("相对触发日偏移天数") private Integer offsetDays = 0;
    @Column(nullable = false) @Note("约定日期") private LocalDate dueDate;
    @Column(nullable = false) @Note("排序") private Integer sort = 0;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receivable_id") @Note("生成的应收") private Receivable receivable;

    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ContractPaymentPlan getPlan() { return plan; }
    public void setPlan(ContractPaymentPlan plan) { this.plan = plan; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public ReceivableType getType() { return type; }
    public void setType(ReceivableType type) { this.type = type; }
    public PaymentTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(PaymentTriggerType triggerType) { this.triggerType = triggerType; }
    public String getTriggerStageCode() { return triggerStageCode; }
    public void setTriggerStageCode(String triggerStageCode) { this.triggerStageCode = triggerStageCode; }
    public String getTriggerStageName() { return triggerStageName; }
    public void setTriggerStageName(String triggerStageName) { this.triggerStageName = triggerStageName; }
    public Integer getOffsetDays() { return offsetDays; }
    public void setOffsetDays(Integer offsetDays) { this.offsetDays = offsetDays; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Receivable getReceivable() { return receivable; }
    public void setReceivable(Receivable receivable) { this.receivable = receivable; }
}
