package com.th4.edge.decoboon.catalog.service.importbatch.info.support;

import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.decoboon.catalog.repository.CatalogImportBatchRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 报价项目库导入批次详情（结果页刷新与过期识别）。 */
@Note("报价项目库导入批次详情")
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
@Service("catalog/import-batch/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.catalog.service.importbatch.info.Info {

    @AutoInject
    @NotNull
    private Long batchId;

    @User
    private Employee employee;

    private final CatalogImportContext importContext;
    private final CatalogImportBatchRepository batchRepository;

    public Info(CatalogImportContext importContext, CatalogImportBatchRepository batchRepository) {
        this.importContext = importContext;
        this.batchRepository = batchRepository;
    }

    @Override
    @Transactional
    public CatalogImportBatch call(CatalogImportBatch request) {
        Employee current = importContext.requireBoss(employee);
        CatalogImportBatch batch = batchRepository
                .findByBatchIdAndCompany_ComId(batchId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "导入批次不存在或无权访问")));
        if (batch.getStatus() == CatalogImportStatus.PendingConfirm
                && batch.getExpireAt() != null && batch.getExpireAt().isBefore(LocalDateTime.now())) {
            batch.setStatus(CatalogImportStatus.Expired);
            batch.setMessage("预览已过期（超过 " + CatalogImportContext.EXPIRE_HOURS + " 小时），请重新上传模板文件");
            batch = batchRepository.save(batch);
        }
        return batch;
    }
}
