package com.th4.edge.decoboon.design.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** AI生成的Prompt和结构化文档内容快照。 */
@Note("AI设计Prompt快照")
@Description("保存视觉、谈单和方案书三类输出及模型参数")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_design_prompt_snapshot", indexes = {
        @Index(name = "idx_prompt_snapshot_company", columnList = "com_id, prompt_id"),
        @Index(name = "idx_prompt_snapshot_company_task", columnList = "com_id, generation_id, prompt_id"),
        @Index(name = "idx_prompt_snapshot_company_design", columnList = "com_id, design_id, prompt_id")
})
public class DesignPromptSnapshot extends AuditModel {

    @Id
    @SnowflakeId
    @Note("Prompt快照索引")
    private Long promptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private DesignGenerationTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private DesignContextSnapshot contextSnapshot;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    @Note("视觉Prompt")
    private String visualPrompt;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    @Note("谈单Prompt")
    private String talkPrompt;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    @Note("方案书结构化内容JSON")
    private String documentBriefJson;

    @Column(length = 32, nullable = false)
    @Note("模板版本")
    private String templateVersion;

    @Column(length = 128)
    @Note("模型名称")
    private String modelName;

    @Lob
    @Column(columnDefinition = "longtext")
    @Note("模型参数JSON")
    private String modelParametersJson;

    @Note("渲染时间")
    private LocalDateTime renderedAt;

    public Long getPromptId() { return promptId; }
    public void setPromptId(Long promptId) { this.promptId = promptId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public Design getDesign() { return design; }
    public void setDesign(Design design) { this.design = design; }
    public DesignGenerationTask getTask() { return task; }
    public void setTask(DesignGenerationTask task) { this.task = task; }
    public DesignContextSnapshot getContextSnapshot() { return contextSnapshot; }
    public void setContextSnapshot(DesignContextSnapshot contextSnapshot) { this.contextSnapshot = contextSnapshot; }
    public String getVisualPrompt() { return visualPrompt; }
    public void setVisualPrompt(String visualPrompt) { this.visualPrompt = visualPrompt; }
    public String getTalkPrompt() { return talkPrompt; }
    public void setTalkPrompt(String talkPrompt) { this.talkPrompt = talkPrompt; }
    public String getDocumentBriefJson() { return documentBriefJson; }
    public void setDocumentBriefJson(String documentBriefJson) { this.documentBriefJson = documentBriefJson; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelParametersJson() { return modelParametersJson; }
    public void setModelParametersJson(String modelParametersJson) { this.modelParametersJson = modelParametersJson; }
    public LocalDateTime getRenderedAt() { return renderedAt; }
    public void setRenderedAt(LocalDateTime renderedAt) { this.renderedAt = renderedAt; }
}
