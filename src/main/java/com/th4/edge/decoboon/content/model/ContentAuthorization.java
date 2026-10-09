package com.th4.edge.decoboon.content.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationStatus;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationSubject;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 内容素材授权。 */
@Note("内容素材授权")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_content_authorization", indexes = {
        @Index(name = "idx_content_auth_company", columnList = "com_id, auth_id"),
        @Index(name = "idx_content_auth_asset", columnList = "asset_id, purpose, status"),
        @Index(name = "idx_content_auth_expiry", columnList = "com_id, status, valid_to")
})
public class ContentAuthorization extends AuditModel {

    @Id
    @SnowflakeId
    @Note("授权索引")
    private Long authId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @Note("素材")
    private ContentAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", length = 16, nullable = false)
    @Note("授权主体类型")
    private ContentAuthorizationSubject subjectType = ContentAuthorizationSubject.Company;

    @Column(name = "subject_ref_id", nullable = false)
    @Note("授权主体索引")
    private Long subjectRefId;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("授权用途")
    private ContentAuthorizationPurpose purpose = ContentAuthorizationPurpose.Internal;

    @Column(length = 500)
    @Note("渠道，逗号分隔")
    private String channels;

    @Column(length = 255)
    @Note("授权地域")
    private String region;

    @Column(name = "valid_from", nullable = false)
    @Note("生效日期")
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    @Note("失效日期")
    private LocalDate validTo;

    @Column(name = "allow_modify", nullable = false)
    @Note("允许修改")
    private Boolean allowModify = false;

    @Column(name = "allow_ai", nullable = false)
    @Note("允许AI处理")
    private Boolean allowAi = false;

    @Column(name = "allow_ads", nullable = false)
    @Note("允许投流")
    private Boolean allowAds = false;

    @Column(name = "allow_sublicense", nullable = false)
    @Note("允许转授权")
    private Boolean allowSublicense = false;

    @Column(name = "evidence_path", length = 512)
    @Note("授权证据文件路径")
    private String evidencePath;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("授权状态")
    private ContentAuthorizationStatus status = ContentAuthorizationStatus.Pending;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    @Note("撤回人")
    private Employee revokedBy;

    @Column(name = "revoked_at")
    @Note("撤回时间")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 500)
    @Note("撤回原因")
    private String revokeReason;

    @Column(length = 1000)
    @Note("备注")
    private String remark;

    public Long getAuthId() { return authId; }
    public void setAuthId(Long authId) { this.authId = authId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ContentAsset getAsset() { return asset; }
    public void setAsset(ContentAsset asset) { this.asset = asset; }
    public ContentAuthorizationSubject getSubjectType() { return subjectType; }
    public void setSubjectType(ContentAuthorizationSubject subjectType) { this.subjectType = subjectType; }
    public Long getSubjectRefId() { return subjectRefId; }
    public void setSubjectRefId(Long subjectRefId) { this.subjectRefId = subjectRefId; }
    public ContentAuthorizationPurpose getPurpose() { return purpose; }
    public void setPurpose(ContentAuthorizationPurpose purpose) { this.purpose = purpose; }
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Boolean getAllowModify() { return allowModify; }
    public void setAllowModify(Boolean allowModify) { this.allowModify = allowModify; }
    public Boolean getAllowAi() { return allowAi; }
    public void setAllowAi(Boolean allowAi) { this.allowAi = allowAi; }
    public Boolean getAllowAds() { return allowAds; }
    public void setAllowAds(Boolean allowAds) { this.allowAds = allowAds; }
    public Boolean getAllowSublicense() { return allowSublicense; }
    public void setAllowSublicense(Boolean allowSublicense) { this.allowSublicense = allowSublicense; }
    public String getEvidencePath() { return evidencePath; }
    public void setEvidencePath(String evidencePath) { this.evidencePath = evidencePath; }
    public ContentAuthorizationStatus getStatus() { return status; }
    public void setStatus(ContentAuthorizationStatus status) { this.status = status; }
    public Employee getRevokedBy() { return revokedBy; }
    public void setRevokedBy(Employee revokedBy) { this.revokedBy = revokedBy; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
