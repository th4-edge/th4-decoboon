package com.th4.edge.decoboon.catalog.service.items.delete.support;

import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.CatalogImportReferenceContext;
import com.th4.edge.decoboon.catalog.model.CatalogImportRow;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
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

import java.util.List;

/**
 * 报价项目库删除实现。
 *
 * <p>复用现有维护权限（企业负责人/店长/财务），企业只取登录员工所属企业。
 * 任意下游业务引用（设计分项、套餐、报价、任务、材料计划、库存、库存流水、供应商供货、
 * 供应商采购价）存在时禁止硬删除并建议“停用”，保护历史事实与快照。
 * 导入批次行关联采用<b>受控解除</b>：清空项目关联并保留批次行审计信息，不触发外键异常。</p>
 */
@Note("报价项目库删除")
@Properties(allow = @AllowProperties(model = CatalogItem.class,
        properties = {"itemId", "type", "code", "category", "brand", "series", "spec", "name", "unit",
                "pricingMode", "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "formula",
                "active", "sort", "sourceType"}))
@Service("catalog/item/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.catalog.service.items.delete.Delete {

    @AutoInject
    @NotNull
    private Long itemId;

    @User
    private Employee employee;

    private final CatalogContext context;
    private final CatalogItemRepository repository;
    private final CatalogDetailContext detailContext;
    private final CatalogImportReferenceContext referenceContext;
    private final CatalogImportRowRepository importRowRepository;

    public Delete(CatalogContext context,
                  CatalogItemRepository repository,
                  CatalogDetailContext detailContext,
                  CatalogImportReferenceContext referenceContext,
                  CatalogImportRowRepository importRowRepository) {
        this.context = context;
        this.repository = repository;
        this.detailContext = detailContext;
        this.referenceContext = referenceContext;
        this.importRowRepository = importRowRepository;
    }

    @Override
    @Transactional
    public CatalogItem call(CatalogItem request) {
        Employee current = context.requireMaintainer(employee);
        CatalogItem item = repository.findByItemIdAndCompany_ComId(itemId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价项目不存在或无权访问")));

        String reference = referenceContext.findReference(item);
        if (reference != null) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该项目已被" + reference + "引用，无法删除；如不再使用，可改为停用"));
        }

        // 导入批次行：解除关联并保留批次审计（不阻断删除、不触发外键异常）
        List<CatalogImportRow> importRows = importRowRepository.findByItem_ItemId(itemId);
        for (CatalogImportRow row : importRows) {
            row.setItem(null);
            row.setErrorCode("DELETED");
            row.setErrorReason("项目已被手工删除，批次审计保留（原项目：" + item.getName() + "）");
        }
        if (!importRows.isEmpty()) {
            importRowRepository.saveAll(importRows);
        }

        detailContext.remove(item);
        repository.delete(item);
        return item;
    }
}
