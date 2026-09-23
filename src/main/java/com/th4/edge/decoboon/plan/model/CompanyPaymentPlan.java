package com.th4.edge.decoboon.plan.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.plan.configure.PlanTemplateStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;

import java.time.LocalDateTime;
import java.util.List;

/** 企业自定义付款计划模板。 */
@Note("企业付款计划模板")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_company_payment_plan", indexes = {
        @Index(name = "idx_company_payment_plan_company", columnList = "com_id, template_id"),
        @Index(name = "idx_company_payment_plan_status", columnList = "com_id, status, is_default")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_payment_plan_code", columnNames = {"com_id", "code"})
})
public class CompanyPaymentPlan extends AuditModel {

    @Id
    @SnowflakeId
    @Note("模板索引")
    private Long templateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @Column(length = 64, nullable = false)
    @Note("模板编码")
    private String code;

    @Column(nullable = false)
    @Note("版本")
    private Integer version = 1;

    @Column(length = 128, nullable = false)
    @Note("模板名称")
    private String name;

    @Column(length = 512)
    @Note("模板说明")
    private String description;

    @Column(nullable = false)
    @Note("排序")
    private Integer sort = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("模板状态")
    private PlanTemplateStatus status = PlanTemplateStatus.Draft;

    @Column(name = "is_default", nullable = false)
    @Note("是否默认")
    private Boolean defaultPlan = Boolean.FALSE;

    @Column(length = 64)
    @Note("来源基线编码")
    private String sourceBaselineCode;

    @Note("来源基线版本")
    private Integer sourceBaselineVersion;

    @Note("发布时间")
    private LocalDateTime publishedAt;

    @Transient
    @Note("付款节点")
    private List<CompanyPaymentNode> nodes = List.of();

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
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
    public PlanTemplateStatus getStatus() { return status; }
    public void setStatus(PlanTemplateStatus status) { this.status = status; }
    public Boolean getDefaultPlan() { return defaultPlan; }
    public void setDefaultPlan(Boolean defaultPlan) { this.defaultPlan = defaultPlan; }
    public String getSourceBaselineCode() { return sourceBaselineCode; }
    public void setSourceBaselineCode(String sourceBaselineCode) { this.sourceBaselineCode = sourceBaselineCode; }
    public Integer getSourceBaselineVersion() { return sourceBaselineVersion; }
    public void setSourceBaselineVersion(Integer sourceBaselineVersion) { this.sourceBaselineVersion = sourceBaselineVersion; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public List<CompanyPaymentNode> getNodes() { return nodes; }
    public void setNodes(List<CompanyPaymentNode> nodes) { this.nodes = nodes == null ? List.of() : List.copyOf(nodes); }
}
