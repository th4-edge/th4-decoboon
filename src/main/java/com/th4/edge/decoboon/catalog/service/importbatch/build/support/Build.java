package com.th4.edge.decoboon.catalog.service.importbatch.build.support;

import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportFileContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportParser;
import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.decoboon.catalog.model.CatalogImportRow;
import com.th4.edge.decoboon.catalog.repository.CatalogImportBatchRepository;
import com.th4.edge.decoboon.catalog.repository.CatalogImportRowRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.io.StreamFile;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.io.annotation.AutoFile;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报价项目库导入预览实现：上传模板 → 解析 → 校验 → 暂存批次与导入行。
 *
 * <p>预览阶段不写 {@link com.th4.edge.decoboon.catalog.model.CatalogItem}；
 * 同一企业重复上传同一文件（SHA-256 命中有效批次）直接返回原批次，不重复创建。</p>
 */
@Note("报价项目库导入预览")
@Properties(allow = {
        @AllowProperties(model = CatalogImportBatch.class,
                properties = {"batchId", "batchNo", "status", "templateVersion", "validationVersion",
                        "originalFilename", "sourcePath", "contentType", "fileSize", "fileSha256",
                        "duplicatePolicy", "totalRows", "readyRows", "duplicateRows", "suspectedDuplicateRows",
                        "errorRows", "costPendingRows", "importedRows", "skippedRows", "partialImport",
                        "expireAt", "parsedAt", "confirmedAt", "finishedAt", "revokedAt", "message",
                        "createdBy", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("catalog/import-batch/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.catalog.service.importbatch.build.Build {

    private static final List<CatalogImportStatus> ACTIVE_STATUSES = List.of(
            CatalogImportStatus.Uploaded, CatalogImportStatus.Parsed, CatalogImportStatus.PendingConfirm,
            CatalogImportStatus.Importing, CatalogImportStatus.Imported, CatalogImportStatus.PartiallyImported);

    @AutoFile
    private StreamFile file;

    /** 业务侧原始文件名：微信 chooseMessageFile 的临时文件名可能缺少扩展名。 */
    @AutoInject
    private String fileName;

    @User
    private Employee employee;

    private final CatalogImportContext importContext;
    private final CatalogImportFileContext fileContext;
    private final CatalogImportParser parser;
    private final CatalogImportBatchRepository batchRepository;
    private final CatalogImportRowRepository rowRepository;

    public Build(CatalogImportContext importContext,
                 CatalogImportFileContext fileContext,
                 CatalogImportParser parser,
                 CatalogImportBatchRepository batchRepository,
                 CatalogImportRowRepository rowRepository) {
        this.importContext = importContext;
        this.fileContext = fileContext;
        this.parser = parser;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
    }

    @Override
    @Transactional
    public CatalogImportBatch call(CatalogImportBatch request) {
        Employee current = importContext.requireBoss(employee);
        if (file == null || file.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要导入的 .xlsx 或 .csv 模板文件"));
        }
        String originalFilename = fileName == null || fileName.isBlank()
                ? file.getOriginalFilename() : fileName.trim();
        byte[] bytes = bytesOf(file);
        String fingerprint = importContext.fingerprint(bytes);

        // 同一企业重复上传同一文件：返回原预览/结果，不重复创建批次与项目。
        List<CatalogImportBatch> hits = batchRepository
                .findByCompany_ComIdAndFileSha256AndStatusInOrderByBatchIdDesc(
                        current.getCompany().getComId(), fingerprint, ACTIVE_STATUSES);
        if (!hits.isEmpty()) {
            CatalogImportBatch hit = hits.get(0);
            hit.setMessage("该文件已于 " + text(hit.getParsedAt()) + " 上传过（批次 " + hit.getBatchNo()
                    + "），本次返回原批次结果，不会重复创建报价项目");
            return hit;
        }

        CatalogImportParser.ParsedSheet sheet = parser.parse(originalFilename, bytes);
        CatalogImportContext.Plan plan = importContext.plan(current, sheet, false);

        CatalogImportBatch batch = new CatalogImportBatch();
        batch.setCompany(current.getCompany());
        batch.setCreatedBy(current);
        batch.setBatchNo(importContext.nextBatchNo());
        batch.setTemplateVersion(CatalogImportContext.TEMPLATE_VERSION);
        batch.setValidationVersion(CatalogImportContext.VALIDATION_VERSION);
        batch.setOriginalFilename(originalFilename);
        batch.setContentType(file.getContentType());
        batch.setFileSize((long) bytes.length);
        batch.setFileSha256(fingerprint);
        batch.setSourcePath(fileContext.store(current.getCompany().getComId(), fingerprint, originalFilename, bytes));
        batch.setStatus(CatalogImportStatus.PendingConfirm);
        batch.setDuplicatePolicy("Skip");
        batch.setTotalRows(plan.totalRows());
        batch.setReadyRows(plan.readyRows());
        batch.setDuplicateRows(plan.duplicateRows());
        batch.setSuspectedDuplicateRows(plan.suspectedDuplicateRows());
        batch.setErrorRows(plan.errorRows());
        batch.setCostPendingRows(plan.costPendingRows());
        batch.setImportedRows(0);
        batch.setSkippedRows(0);
        batch.setPartialImport(Boolean.FALSE);
        batch.setParsedAt(LocalDateTime.now());
        batch.setExpireAt(importContext.expireAt());
        batch.setMessage(plan.message());
        CatalogImportBatch saved = batchRepository.save(batch);

        for (CatalogImportRow row : plan.rows()) {
            row.setBatch(saved);
        }
        rowRepository.saveAll(plan.rows());
        return saved;
    }

    private byte[] bytesOf(StreamFile streamFile) {
        try {
            return streamFile.getBytes();
        } catch (IOException ex) {
            throw new DefinedException(new DefaultErrorCode("400", "读取上传文件失败，请重新选择文件"));
        }
    }

    private String text(LocalDateTime value) {
        return value == null ? "-" : value.toLocalDate().toString();
    }
}
