package com.th4.edge.decoboon.catalog.service.importrow.page.support;

import com.th4.edge.decoboon.catalog.configure.CatalogImportRowStatus;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.context.CatalogImportContext;
import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.decoboon.catalog.model.CatalogImportRow;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogImportBatchRepository;
import com.th4.edge.decoboon.catalog.repository.CatalogImportRowRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 导入行分页：可导入摘要、重复项、疑似重复与错误清单。 */
@Note("报价项目库导入行分页")
@Properties(allow = {
        @AllowProperties(model = CatalogImportRow.class,
                properties = {"rowId", "rowNo", "status", "errorField", "errorCode", "errorReason",
                        "itemName", "itemType", "unit", "pricingMode", "saleAmount", "costPending",
                        "item", "createTime"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "code", "name", "type", "category", "brand", "series", "spec", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "active"}),
        @AllowProperties(model = ItemType.class, properties = {"name", "value", "key"}),
        @AllowProperties(model = Unit.class, properties = {"name", "value", "key"}),
        @AllowProperties(model = PricingMethod.class, properties = {"name", "value", "key"})
})
@Service("catalog/import-row/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.catalog.service.importrow.page.Page {

    @AutoInject @NotNull private Long batchId;
    @AutoInject private CatalogImportRowStatus status;
    @AutoInject private Integer current;
    @AutoInject private Integer size;

    @User private Employee employee;

    private final CatalogImportContext importContext;
    private final CatalogImportBatchRepository batchRepository;
    private final CatalogImportRowRepository rowRepository;

    public Page(CatalogImportContext importContext,
                CatalogImportBatchRepository batchRepository,
                CatalogImportRowRepository rowRepository) {
        this.importContext = importContext;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CatalogImportRow> call(CatalogImportRow request) {
        Employee currentEmployee = importContext.requireMaintainer(employee);
        CatalogImportBatch batch = batchRepository
                .findByBatchIdAndCompany_ComId(batchId, currentEmployee.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "导入批次不存在或无权访问")));
        List<CatalogImportRow> rows = rowRepository.findByBatch_BatchIdOrderByRowNoAsc(batch.getBatchId()).stream()
                .filter(row -> status == null || row.getStatus() == status)
                .toList();
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(pageIndex * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return new PageImpl<>(rows.subList(from, to), PageRequest.of(pageIndex, pageSize), rows.size());
    }

    /** 供响应白名单使用：确保 BigDecimal 类型不参与裁剪。 */
    private BigDecimal unused() {
        return null;
    }
}
