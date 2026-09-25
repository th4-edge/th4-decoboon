package com.th4.edge.decoboon.catalog.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 报价项目库批量导入批次（PRD §11.1）。
 *
 * <p>预览阶段只写批次与导入行，不写 {@link CatalogItem}；确认导入后事务写入正式项目，
 * 并通过 {@link CatalogImportRow#getItem()} 记录每个新增项目的来源，用于撤销与追溯。</p>
 */
@Note("报价项目库导入批次")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_import_batch", indexes = {
        @Index(name = "idx_catalog_import_batch_company", columnList = "com_id, batch_id"),
        @Index(name = "idx_catalog_import_batch_company_status", columnList = "com_id, status, batch_id"),
        @Index(name = "idx_catalog_import_batch_company_file", columnList = "com_id, file_sha256, batch_id")
})
public class CatalogImportBatch extends AbstractModel {

    @Note("批次索引")
    @Id
    @SnowflakeId
    private Long batchId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("操作人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Employee createdBy;

    @Note("批次号")
    @Column(length = 48, nullable = false)
    private String batchNo;

    @Note("模板版本")
    @Column(length = 24, nullable = false)
    private String templateVersion;

    @Note("校验版本（预览与确认必须一致）")
    @Column(length = 24, nullable = false)
    private String validationVersion;

    @Note("原始文件名")
    @Column(length = 256)
    private String originalFilename;

    @Note("上传文件对象键")
    @Column(length = 512, nullable = false)
    private String sourcePath;

    @Note("文件类型")
    @Column(length = 128)
    private String contentType;

    @Note("文件大小（字节）")
    private Long fileSize;

    @Note("文件指纹 SHA-256")
    @Column(length = 64, nullable = false)
    private String fileSha256;

    @Note("批次状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private CatalogImportStatus status = CatalogImportStatus.Uploaded;

    @Note("疑似重复策略（Skip 跳过 / Import 按新增导入）")
    @Column(length = 16, nullable = false)
    private String duplicatePolicy = "Skip";

    @Note("有效数据行数")
    @Column(nullable = false)
    private Integer totalRows = 0;

    @Note("可导入行数")
    @Column(nullable = false)
    private Integer readyRows = 0;

    @Note("重复跳过行数")
    @Column(nullable = false)
    private Integer duplicateRows = 0;

    @Note("疑似重复行数")
    @Column(nullable = false)
    private Integer suspectedDuplicateRows = 0;

    @Note("错误行数")
    @Column(nullable = false)
    private Integer errorRows = 0;

    @Note("成本待维护行数")
    @Column(nullable = false)
    private Integer costPendingRows = 0;

    @Note("实际新增项目数")
    @Column(nullable = false)
    private Integer importedRows = 0;

    @Note("被跳过行数（部分导入时的错误行与疑似重复行）")
    @Column(nullable = false)
    private Integer skippedRows = 0;

    @Note("是否经二次确认仅导入合格行")
    @Column(nullable = false)
    private Boolean partialImport = Boolean.FALSE;

    @Note("预览过期时间")
    private LocalDateTime expireAt;

    @Note("解析完成时间")
    private LocalDateTime parsedAt;

    @Note("确认导入时间")
    private LocalDateTime confirmedAt;

    @Note("导入结束时间")
    private LocalDateTime finishedAt;

    @Note("撤销时间")
    private LocalDateTime revokedAt;

    @Note("批次提示（重复文件、疑似重复处理、失败原因）")
    @Lob
    private String message;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getValidationVersion() { return validationVersion; }
    public void setValidationVersion(String validationVersion) { this.validationVersion = validationVersion; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileSha256() { return fileSha256; }
    public void setFileSha256(String fileSha256) { this.fileSha256 = fileSha256; }
    public CatalogImportStatus getStatus() { return status; }
    public void setStatus(CatalogImportStatus status) { this.status = status; }
    public String getDuplicatePolicy() { return duplicatePolicy; }
    public void setDuplicatePolicy(String duplicatePolicy) { this.duplicatePolicy = duplicatePolicy; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getReadyRows() { return readyRows; }
    public void setReadyRows(Integer readyRows) { this.readyRows = readyRows; }
    public Integer getDuplicateRows() { return duplicateRows; }
    public void setDuplicateRows(Integer duplicateRows) { this.duplicateRows = duplicateRows; }
    public Integer getSuspectedDuplicateRows() { return suspectedDuplicateRows; }
    public void setSuspectedDuplicateRows(Integer suspectedDuplicateRows) { this.suspectedDuplicateRows = suspectedDuplicateRows; }
    public Integer getErrorRows() { return errorRows; }
    public void setErrorRows(Integer errorRows) { this.errorRows = errorRows; }
    public Integer getCostPendingRows() { return costPendingRows; }
    public void setCostPendingRows(Integer costPendingRows) { this.costPendingRows = costPendingRows; }
    public Integer getImportedRows() { return importedRows; }
    public void setImportedRows(Integer importedRows) { this.importedRows = importedRows; }
    public Integer getSkippedRows() { return skippedRows; }
    public void setSkippedRows(Integer skippedRows) { this.skippedRows = skippedRows; }
    public Boolean getPartialImport() { return partialImport; }
    public void setPartialImport(Boolean partialImport) { this.partialImport = partialImport; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
