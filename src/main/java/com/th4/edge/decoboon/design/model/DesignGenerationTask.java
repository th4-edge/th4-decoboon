package com.th4.edge.decoboon.design.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignGenerationMode;
import com.th4.edge.decoboon.design.configure.DesignGenerationStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

/** 一次AI设计方案生成任务。 */
@Note("AI设计生成任务")
@Description("记录设计上下文、Prompt快照、模型结果和版本状态")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_design_generation_task", indexes = {
        @Index(name = "idx_generation_company", columnList = "com_id, generation_id"),
        @Index(name = "idx_generation_company_store", columnList = "com_id, store_id, generation_id"),
        @Index(name = "idx_generation_company_team", columnList = "com_id, team_id, generation_id"),
        @Index(name = "idx_generation_company_owner", columnList = "com_id, emp_id, generation_id"),
        @Index(name = "idx_generation_company_design", columnList = "com_id, design_id, generation_id")
})
public class DesignGenerationTask extends AuditModel {

    @Id
    @SnowflakeId
    @Note("生成任务索引")
    private Long generationId;

    @Note("任务编号")
    @Keyword
    @Column(length = 64, nullable = false, unique = true)
    private String no;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("负责设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee owner;

    @Note("客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Note("所属房屋")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Note("设计方案")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @Note("生成模式")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private DesignGenerationMode mode = DesignGenerationMode.Concept;

    @Note("任务状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private DesignGenerationStatus status = DesignGenerationStatus.ContextBuilding;

    @Note("任务版本")
    @Column(nullable = false)
    private Integer version = 1;

    @Note("来源摘要")
    @Column(length = 64)
    private String sourceDigest;

    @Note("模型名称")
    @Column(length = 128)
    private String modelName;

    @Note("失败编码")
    @Column(length = 64)
    private String failureCode;

    @Note("失败说明")
    @Column(length = 512)
    private String failureMessage;

    @Note("构建时间")
    private LocalDateTime builtAt;

    @Note("完成时间")
    private LocalDateTime completedAt;

    @Transient
    @Note("上下文快照")
    private DesignContextSnapshot contextSnapshot;

    @Transient
    @Note("Prompt快照")
    private DesignPromptSnapshot promptSnapshot;

    public Long getGenerationId() { return generationId; }
    public void setGenerationId(Long generationId) { this.generationId = generationId; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
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
    public DesignGenerationMode getMode() { return mode; }
    public void setMode(DesignGenerationMode mode) { this.mode = mode; }
    public DesignGenerationStatus getStatus() { return status; }
    public void setStatus(DesignGenerationStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getSourceDigest() { return sourceDigest; }
    public void setSourceDigest(String sourceDigest) { this.sourceDigest = sourceDigest; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public LocalDateTime getBuiltAt() { return builtAt; }
    public void setBuiltAt(LocalDateTime builtAt) { this.builtAt = builtAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public DesignContextSnapshot getContextSnapshot() { return contextSnapshot; }
    public void setContextSnapshot(DesignContextSnapshot contextSnapshot) { this.contextSnapshot = contextSnapshot; }
    public DesignPromptSnapshot getPromptSnapshot() { return promptSnapshot; }
    public void setPromptSnapshot(DesignPromptSnapshot promptSnapshot) { this.promptSnapshot = promptSnapshot; }
}
