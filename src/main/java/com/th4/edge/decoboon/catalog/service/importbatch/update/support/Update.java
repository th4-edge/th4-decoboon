package com.th4.edge.decoboon.catalog.service.importbatch.update.support;

import com.th4.edge.decoboon.catalog.configure.CatalogImportRowStatus;
import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportReferenceContext;
import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.decoboon.catalog.model.CatalogImportRow;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.catalog.repository.CatalogImportBatchRepository;
import com.th4.edge.decoboon.catalog.repository.CatalogImportRowRepository;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导入批次撤销实现（PRD §9）。
 *
 * <p>仅已导入/部分导入批次可撤销；必须在导入完成后 24 小时内；本批新增项目任意一条
 * 被设计分项、套餐、报价、任务、材料计划、库存、供货价引用时整批撤销失败，不做部分删除。</p>
 */
@Note("报价项目库导入批次撤销")
@Properties(allow = {
        @AllowProperties(model = CatalogImportBatch.class,
                properties = {"batchId", "batchNo", "status", "templateVersion", "originalFilename",
                        "totalRows", "readyRows", "duplicateRows", "suspectedDuplicateRows", "errorRows",
                        "costPendingRows", "importedRows", "skippedRows", "partialImport",
                        "parsedAt", "confirmedAt", "finishedAt", "revokedAt", "message", "createdBy", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("catalog/import-batch/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.catalog.service.importbatch.update.Update {

    @AutoInject @NotNull private Long batchId;
    @AutoInject private CatalogImportStatus status;

    @User private Employee employee;

    /** 撤销窗口（小时），P0 冻结 24 小时，可由配置覆盖。 */
    @Value("${decoboon.catalog.import.revoke-window-hours:24}")
    private long revokeWindowHours = 24;

    private final CatalogImportContext importContext;
    private final CatalogImportBatchRepository batchRepository;
    private final CatalogImportRowRepository rowRepository;
    private final CatalogItemRepository itemRepository;
    private final CatalogDetailContext detailContext;
    private final CatalogImportReferenceContext referenceContext;
    private final DistributedLock distributedLock;

    public Update(CatalogImportContext importContext,
                  CatalogImportBatchRepository batchRepository,
                  CatalogImportRowRepository rowRepository,
                  CatalogItemRepository itemRepository,
                  CatalogDetailContext detailContext,
                  CatalogImportReferenceContext referenceContext,
                DistributedLock distributedLock) {
        this.importContext = importContext;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.itemRepository = itemRepository;
        this.detailContext = detailContext;
        this.referenceContext = referenceContext;
        this.distributedLock = distributedLock;
    }

    @Override
    @Transactional
    public CatalogImportBatch call(CatalogImportBatch request) {
        Employee current = importContext.requireBoss(employee);
        String lockKey = "catalog-import-batch:" + current.getCompany().getComId() + ":" + batchId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "导入批次正在处理中，请稍后重试"));
        }
        try {
        CatalogImportBatch batch = batchRepository.findByBatchId(batchId)
                .filter(item -> item.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "导入批次不存在或无权访问")));
        if (status != CatalogImportStatus.Revoked) {
            throw new DefinedException(new DefaultErrorCode("400", "当前仅支持撤销已导入批次"));
        }
        // 幂等：已撤销批次重复撤销返回同一结果。
        if (batch.getStatus() == CatalogImportStatus.Revoked) {
            return batch;
        }
        if (!batch.getStatus().imported()) {
            throw new DefinedException(new DefaultErrorCode("400", "只有已导入或部分导入的批次可以撤销"));
        }
        LocalDateTime finishedAt = batch.getFinishedAt() == null ? batch.getConfirmedAt() : batch.getFinishedAt();
        LocalDateTime deadline = (finishedAt == null ? LocalDateTime.now() : finishedAt).plusHours(revokeWindowHours);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "已超过 " + revokeWindowHours + " 小时撤销窗口，不能撤销本次导入"));
        }

        List<CatalogImportRow> importedRows = rowRepository.findByBatch_BatchIdAndStatusInOrderByRowNoAsc(
                batch.getBatchId(), List.of(CatalogImportRowStatus.Imported));
        for (CatalogImportRow row : importedRows) {
            CatalogItem item = row.getItem();
            if (item == null) {
                continue;
            }
            String reference = referenceContext.findReference(item);
            if (reference != null) {
                throw new DefinedException(new DefaultErrorCode("400",
                        "项目「" + item.getName() + "」已被" + reference + "引用，整批不能撤销"));
            }
        }
        for (CatalogImportRow row : importedRows) {
            CatalogItem item = row.getItem();
            if (item == null) {
                continue;
            }
            detailContext.remove(item);
            itemRepository.delete(item);
            row.setItem(null);
            row.setStatus(CatalogImportRowStatus.Skipped);
            row.setErrorCode("REVOKED");
            row.setErrorReason("批次已撤销，项目已删除");
        }
        rowRepository.saveAll(importedRows);

        batch.setStatus(CatalogImportStatus.Revoked);
        batch.setRevokedAt(LocalDateTime.now());
        batch.setMessage("批次已撤销：删除本批新增项目 " + batch.getImportedRows() + " 条，原文件可重新上传为新批次");
        return batchRepository.save(batch);
        } finally {
            distributedLock.unlock(lockKey);
        }
    }
}
