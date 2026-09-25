package com.th4.edge.decoboon.catalog.service.importbatch.page.support;

import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.decoboon.catalog.repository.CatalogImportBatchRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 最近导入记录分页（默认 10 条）。 */
@Note("报价项目库导入批次分页")
@Properties(allow = {
        @AllowProperties(model = CatalogImportBatch.class,
                properties = {"batchId", "batchNo", "status", "templateVersion", "originalFilename",
                        "totalRows", "readyRows", "duplicateRows", "suspectedDuplicateRows", "errorRows",
                        "costPendingRows", "importedRows", "skippedRows", "partialImport",
                        "parsedAt", "confirmedAt", "finishedAt", "revokedAt", "message", "createdBy", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("catalog/import-batch/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.catalog.service.importbatch.page.Page {

    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private CatalogImportStatus status;

    @User private Employee employee;

    private final CatalogImportContext importContext;
    private final CatalogImportBatchRepository batchRepository;

    public Page(CatalogImportContext importContext, CatalogImportBatchRepository batchRepository) {
        this.importContext = importContext;
        this.batchRepository = batchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CatalogImportBatch> call(CatalogImportBatch request) {
        Employee currentEmployee = importContext.requireBoss(employee);
        List<CatalogImportBatch> all = batchRepository
                .findByCompany_ComIdOrderByBatchIdDesc(currentEmployee.getCompany().getComId()).stream()
                .filter(batch -> status == null || batch.getStatus() == status)
                .toList();
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);
        int from = Math.min(pageIndex * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(pageIndex, pageSize), all.size());
    }
}
