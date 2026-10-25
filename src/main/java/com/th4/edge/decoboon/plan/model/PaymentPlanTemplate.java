package com.th4.edge.decoboon.plan.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.decoboon.plan.configure.PlanTemplateSource;
import com.th4.edge.decoboon.plan.configure.PlanTemplateStatus;

import java.util.List;

/** 平台付款计划模板基线。 */
@Note("付款计划模板基线")
public class PaymentPlanTemplate {
    private Long templateId;
    private String code;
    private Integer version = 1;
    private String name;
    private String description;
    private Integer sort = 0;
    private PlanTemplateSource source = PlanTemplateSource.Baseline;
    private String sourceBaselineCode;
    private Integer sourceBaselineVersion;
    private PlanTemplateStatus status = PlanTemplateStatus.Enabled;
    private Boolean defaultPlan = Boolean.FALSE;
    private Boolean currentEnabled = Boolean.FALSE;
    private List<PaymentPlanNodeTemplate> nodes = List.of();

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public PlanTemplateSource getSource() { return source; }
    public void setSource(PlanTemplateSource source) { this.source = source; }
    public String getSourceBaselineCode() { return sourceBaselineCode; }
    public void setSourceBaselineCode(String sourceBaselineCode) { this.sourceBaselineCode = sourceBaselineCode; }
    public Integer getSourceBaselineVersion() { return sourceBaselineVersion; }
    public void setSourceBaselineVersion(Integer sourceBaselineVersion) { this.sourceBaselineVersion = sourceBaselineVersion; }
    public PlanTemplateStatus getStatus() { return status; }
    public void setStatus(PlanTemplateStatus status) { this.status = status; }
    public Boolean getDefaultPlan() { return defaultPlan; }
    public void setDefaultPlan(Boolean defaultPlan) { this.defaultPlan = defaultPlan; }
    public Boolean getCurrentEnabled() { return currentEnabled; }
    public void setCurrentEnabled(Boolean currentEnabled) { this.currentEnabled = currentEnabled; }
    public List<PaymentPlanNodeTemplate> getNodes() { return nodes; }
    public void setNodes(List<PaymentPlanNodeTemplate> nodes) { this.nodes = nodes == null ? List.of() : List.copyOf(nodes); }
}
