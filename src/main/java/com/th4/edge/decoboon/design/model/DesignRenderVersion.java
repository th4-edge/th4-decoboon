package com.th4.edge.decoboon.design.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignRenderStatus;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** AI效果图生成版本。 */
@Note("AI效果图版本")
@Description("记录一次生成任务输出的效果图、模型、请求号和用量")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_design_render_version", indexes = {
        @Index(name = "idx_render_version_company", columnList = "com_id, render_id"),
        @Index(name = "idx_render_version_company_task", columnList = "com_id, generation_id, render_id"),
        @Index(name = "idx_render_version_company_design", columnList = "com_id, design_id, render_id")
})
public class DesignRenderVersion extends AuditModel {

    @Id
    @SnowflakeId
    @Note("效果图版本索引")
    private Long renderId;

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
    @JoinColumn(name = "prompt_id", nullable = false)
    private DesignPromptSnapshot promptSnapshot;

    @Note("版本序号")
    @Column(nullable = false)
    private Integer sequence = 1;

    @Note("版本状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private DesignRenderStatus status = DesignRenderStatus.Ready;

    @Note("效果图地址")
    @Column(length = 1024)
    private String imageUrl;

    @Note("模型名称")
    @Column(length = 128)
    private String modelName;

    @Note("模型请求号")
    @Column(length = 128)
    private String requestId;

    @Note("输入图片数")
    private Integer inputImageCount = 0;

    @Note("输出图片数")
    private Integer outputImageCount = 0;

    @Lob
    @Column(columnDefinition = "longtext")
    @Note("模型用量JSON")
    private String usageJson;

    @Note("模型费用")
    @Column(precision = 12, scale = 4)
    private BigDecimal costAmount;

    @Note("费用币种")
    @Column(length = 8)
    private String costCurrency;

    @Note("开始时间")
    private LocalDateTime startedAt;

    @Note("完成时间")
    private LocalDateTime completedAt;

    public Long getRenderId() { return renderId; }
    public void setRenderId(Long renderId) { this.renderId = renderId; }
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
    public DesignPromptSnapshot getPromptSnapshot() { return promptSnapshot; }
    public void setPromptSnapshot(DesignPromptSnapshot promptSnapshot) { this.promptSnapshot = promptSnapshot; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public DesignRenderStatus getStatus() { return status; }
    public void setStatus(DesignRenderStatus status) { this.status = status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Integer getInputImageCount() { return inputImageCount; }
    public void setInputImageCount(Integer inputImageCount) { this.inputImageCount = inputImageCount; }
    public Integer getOutputImageCount() { return outputImageCount; }
    public void setOutputImageCount(Integer outputImageCount) { this.outputImageCount = outputImageCount; }
    public String getUsageJson() { return usageJson; }
    public void setUsageJson(String usageJson) { this.usageJson = usageJson; }
    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }
    public String getCostCurrency() { return costCurrency; }
    public void setCostCurrency(String costCurrency) { this.costCurrency = costCurrency; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
