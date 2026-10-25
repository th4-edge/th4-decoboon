package com.th4.edge.decoboon.catalog.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.configure.CatalogRevisionType;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 报价项目不可变修订审计。 */
@Note("报价项目修订审计")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "catalog_item_revision", indexes = {
        @Index(name = "idx_catalog_revision_company_item", columnList = "com_id, item_id, revision"),
        @Index(name = "idx_catalog_revision_company_time", columnList = "com_id, changed_at")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_catalog_revision_item_revision", columnNames = {"com_id", "item_id", "revision"}))
public class CatalogItemRevision extends AuditModel {

    @Id
    @SnowflakeId
    @Note("修订索引")
    private Long revisionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(length = 64)
    private String itemCode;

    @Column(nullable = false)
    private Integer revision;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 16, nullable = false)
    private CatalogRevisionType changeType;

    @Lob
    @Column(name = "snapshot_json", columnDefinition = "longtext", nullable = false)
    private String snapshotJson;

    @Column(name = "snapshot_hash", length = 64, nullable = false)
    private String snapshotHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private Employee changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public CatalogRevisionType getChangeType() { return changeType; }
    public void setChangeType(CatalogRevisionType changeType) { this.changeType = changeType; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public String getSnapshotHash() { return snapshotHash; }
    public void setSnapshotHash(String snapshotHash) { this.snapshotHash = snapshotHash; }
    public Employee getChangedBy() { return changedBy; }
    public void setChangedBy(Employee changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
