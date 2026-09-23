package com.th4.edge.decoboon.plan.model;

import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.plan.configure.PaymentTriggerType;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/** 平台付款计划节点基线。 */
@Note("付款计划节点基线")
public class PaymentPlanNodeTemplate {
    private String code;
    private String name;
    private BigDecimal ratio;
    private ReceivableType type = ReceivableType.Other;
    private PaymentTriggerType triggerType = PaymentTriggerType.ContractSigned;
    private String triggerStageCode;
    private Integer offsetDays = 0;
    private Integer sort = 0;

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
