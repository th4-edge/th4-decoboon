package com.th4.edge.decoboon.catalog.service.importbatch.save.support;

import com.th4.edge.decoboon.catalog.configure.CatalogImportRowStatus;
import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.decoboon.catalog.configure.CatalogSourceType;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogCodeContext;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportFileContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportParser;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报价项目库导入确认实现：单事务写入正式项目与类型明细。
 *
 * <p>幂等：批次已导入时重复确认直接返回同一批次，不重复创建项目；
 * 存在错误行时默认整批不导入，只有 partialImport=true（页面二次确认）才仅导入合格行。</p>
 */
@Note("报价项目库导入确认")
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
@Service("catalog/import-batch/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.catalog.service.importbatch.save.Save {

    @AutoInject @NotNull private Long batchId;
    @AutoInject private Boolean partialImport;
    @AutoInject private String duplicatePolicy;

    @User private Employee employee;

    private final CatalogImportContext importContext;
    private final CatalogImportFileContext fileContext;
    private final CatalogImportParser parser;
    private final CatalogImportBatchRepository batchRepository;
    private final CatalogImportRowRepository rowRepository;
    private final CatalogItemRepository itemRepository;
    private final CatalogCodeContext codeContext;
    private final PinyinContext pinyinContext;
    private final CatalogContext catalogContext;
    private final CatalogDetailContext detailContext;
    private final DistributedLock distributedLock;

    public Save(CatalogImportContext importContext,
                CatalogImportFileContext fileContext,
                CatalogImportParser parser,
                CatalogImportBatchRepository batchRepository,
                CatalogImportRowRepository rowRepository,
                CatalogItemRepository itemRepository,
                CatalogCodeContext codeContext,
                PinyinContext pinyinContext,
                CatalogContext catalogContext,
                CatalogDetailContext detailContext,
                DistributedLock distributedLock) {
        this.importContext = importContext;
        this.fileContext = fileContext;
        this.parser = parser;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.itemRepository = itemRepository;
        this.codeContext = codeContext;
        this.pinyinContext = pinyinContext;
        this.catalogContext = catalogContext;
        this.detailContext = detailContext;
        this.distributedLock = distributedLock;
    }

    @Override
    @Transactional
    public CatalogImportBatch call(CatalogImportBatch request) {
        Employee current = importContext.requireMaintainer(employee);
        String lockKey = "catalog-import-batch:" + current.getCompany().getComId() + ":" + batchId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "导入批次正在处理中，请稍后重试"));
        }
        try {
        CatalogImportBatch batch = batchRepository.findByBatchId(batchId)
                .filter(item -> item.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "导入批次不存在或无权访问")));

        // 幂等：已导入批次重复确认返回同一结果。
        if (batch.getStatus().imported()) {
            return batch;
        }
        if (batch.getStatus() == CatalogImportStatus.Revoked) {
            throw new DefinedException(new DefaultErrorCode("400", "批次已撤销，不能再次确认导入"));
        }
        if (batch.getStatus() != CatalogImportStatus.PendingConfirm) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "批次当前状态不能确认导入：" + batch.getStatus().getName()));
        }
        LocalDateTime now = LocalDateTime.now();
        if (batch.getExpireAt() != null && batch.getExpireAt().isBefore(now)) {
            batch.setStatus(CatalogImportStatus.Expired);
            batch.setMessage("预览已过期（超过 " + CatalogImportContext.EXPIRE_HOURS + " 小时），请重新上传模板文件");
            batchRepository.save(batch);
            throw new DefinedException(new DefaultErrorCode("400", "预览已过期，请重新上传模板文件"));
        }
        if (!CatalogImportContext.VALIDATION_VERSION.equals(batch.getValidationVersion())) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "校验规则已更新（预览 " + batch.getValidationVersion() + "），请重新上传模板文件"));
        }

        byte[] bytes = fileContext.read(batch.getSourcePath());
        if (!importContext.fingerprint(bytes).equalsIgnoreCase(batch.getFileSha256())) {
            throw new DefinedException(new DefaultErrorCode("400", "导入文件已变化，请重新上传模板文件"));
        }
        boolean importSuspected = "Import".equalsIgnoreCase(duplicatePolicy);
        CatalogImportParser.ParsedSheet sheet = parser.parse(batch.getOriginalFilename(), bytes);
        CatalogImportContext.Plan plan = importContext.plan(current, sheet, importSuspected);
        if (plan.errorRows() > 0 && !Boolean.TRUE.equals(partialImport)) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "存在 " + plan.errorRows() + " 行错误，不能整批导入；请修正后重新上传，或二次确认仅导入合格行"));
        }

        batch.setStatus(CatalogImportStatus.Importing);
        batch.setDuplicatePolicy(importSuspected ? "Import" : "Skip");
        batch.setPartialImport(Boolean.TRUE.equals(partialImport) && plan.errorRows() > 0);
        batch.setConfirmedAt(now);
        batchRepository.saveAndFlush(batch);

        Map<Integer, CatalogImportRow> persisted = new HashMap<>();
        for (CatalogImportRow row : rowRepository.findByBatch_BatchIdOrderByRowNoAsc(batch.getBatchId())) {
            persisted.put(row.getRowNo(), row);
        }
        Map<ItemType, Integer> sortCursor = new EnumMap<>(ItemType.class);
        int readyIndex = 0;
        int imported = 0;
        for (CatalogImportRow planned : plan.rows()) {
            CatalogImportRow row = persisted.get(planned.getRowNo());
            if (row == null) {
                continue;
            }
            if (planned.getStatus() == CatalogImportRowStatus.Ready) {
                CatalogItem seed = plan.readyItems().get(readyIndex++);
                int sort = sortCursor.computeIfAbsent(seed.getType(),
                        type -> (int) itemRepository.countByCompany_ComIdAndType(current.getCompany().getComId(), type));
                CatalogItem saved = persist(seed, current, batch, sort);
                sortCursor.put(seed.getType(), sort + 1);
                row.setStatus(CatalogImportRowStatus.Imported);
                row.setItem(saved);
                row.setErrorField(null);
                row.setErrorCode(null);
                row.setErrorReason(null);
                imported++;
            } else {
                row.setStatus(planned.getStatus());
                row.setErrorField(planned.getErrorField());
                row.setErrorCode(planned.getErrorCode());
                row.setErrorReason(planned.getErrorReason());
            }
        }
        rowRepository.saveAll(persisted.values());

        batch.setTotalRows(plan.totalRows());
        batch.setReadyRows(plan.readyRows());
        batch.setDuplicateRows(plan.duplicateRows());
        batch.setSuspectedDuplicateRows(plan.suspectedDuplicateRows());
        batch.setErrorRows(plan.errorRows());
        batch.setCostPendingRows(plan.costPendingRows());
        batch.setImportedRows(imported);
        batch.setSkippedRows(plan.errorRows() + (importSuspected ? 0 : plan.suspectedDuplicateRows()));
        batch.setStatus(plan.errorRows() > 0 ? CatalogImportStatus.PartiallyImported : CatalogImportStatus.Imported);
        batch.setFinishedAt(LocalDateTime.now());
        batch.setMessage(buildMessage(batch));
        return batchRepository.save(batch);
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private CatalogItem persist(CatalogItem seed, Employee current, CatalogImportBatch batch, int sort) {
        seed.setImportBatch(batch);
        seed.setSourceType(CatalogSourceType.Import);
        if (seed.getCode() == null || seed.getCode().isBlank()) {
            seed.setCode(codeContext.nextCode(current.getCompany().getComId(), seed.getType()));
        }
        seed.setNameInitials(pinyinContext.initials(seed.getName()));
        seed.setFormula(catalogContext.formula(seed.getPricingMode()));
        seed.setSort(sort);
        CatalogItem saved = itemRepository.save(seed);
        detailContext.sync(saved);
        return saved;
    }

    private String buildMessage(CatalogImportBatch batch) {
        StringBuilder builder = new StringBuilder();
        builder.append("批次 ").append(batch.getBatchNo()).append("：新增 ").append(batch.getImportedRows())
                .append("，重复跳过 ").append(batch.getDuplicateRows());
        if (batch.getSuspectedDuplicateRows() != null && batch.getSuspectedDuplicateRows() > 0) {
            builder.append("，疑似重复").append("Import".equals(batch.getDuplicatePolicy()) ? "按新增 " : "跳过 ")
                    .append(batch.getSuspectedDuplicateRows());
        }
        builder.append("，错误 ").append(batch.getErrorRows());
        builder.append("，成本待维护 ").append(batch.getCostPendingRows());
        return builder.toString();
    }
}
