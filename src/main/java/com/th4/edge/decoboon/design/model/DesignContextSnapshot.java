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

/** AI生成使用的业务上下文不可变快照。 */
@Note("AI设计上下文快照")
@Description("保存本次生成使用的客户、画像、房屋、量房、区域和诉求结构化数据")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_design_context_snapshot", indexes = {
        @Index(name = "idx_context_snapshot_company", columnList = "com_id, snapshot_id"),
        @Index(name = "idx_context_snapshot_company_task", columnList = "com_id, generation_id, snapshot_id"),
        @Index(name = "idx_context_snapshot_company_design", columnList = "com_id, design_id, snapshot_id")
})
public class DesignContextSnapshot extends AuditModel {

    @Id
    @SnowflakeId
    @Note("上下文快照索引")
    private Long snapshotId;

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

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    @Note("结构化上下文JSON")
    private String contextJson;

    @Column(length = 64, nullable = false)
    @Note("来源摘要")
    private String sourceDigest;

    @Lob
    @Column(columnDefinition = "longtext")
    @Note("缺失字段JSON")
    private String missingFieldsJson;

    @Lob
    @Column(columnDefinition = "longtext")
    @Note("冲突标记JSON")
    private String conflictFlagsJson;

    @Column(length = 16, nullable = false)
    @Note("置信度")
    private String confidence;

    @Note("采集时间")
    private LocalDateTime capturedAt;

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
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
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getSourceDigest() { return sourceDigest; }
    public void setSourceDigest(String sourceDigest) { this.sourceDigest = sourceDigest; }
    public String getMissingFieldsJson() { return missingFieldsJson; }
    public void setMissingFieldsJson(String missingFieldsJson) { this.missingFieldsJson = missingFieldsJson; }
    public String getConflictFlagsJson() { return conflictFlagsJson; }
    public void setConflictFlagsJson(String conflictFlagsJson) { this.conflictFlagsJson = conflictFlagsJson; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}
