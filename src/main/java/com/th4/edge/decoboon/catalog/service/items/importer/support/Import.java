package com.th4.edge.decoboon.catalog.service.items.importer.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.context.CatalogCodeContext;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.CatalogRevisionContext;
import com.th4.edge.decoboon.catalog.configure.CatalogRevisionType;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.catalog.service.items.importer.ImportResult;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/** 报价项目库批量导入实现。 */
@Note("报价项目库批量导入")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.decoboon.catalog.service.items.importer.ImportResult.class,
                properties = {"total", "success", "failed", "failures"}),
        @AllowProperties(model = com.th4.edge.decoboon.catalog.service.items.importer.ImportResult.Failure.class,
                properties = {"index", "name", "reason"})
})
@Service("catalog/item/import")
@Scope(ScopeTypes.PROTOTYPE)
public class Import implements com.th4.edge.decoboon.catalog.service.items.importer.Import {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @AutoInject private List<CatalogItem> items;
    @AutoInject private String filename;

    @User private Employee employee;

    private final CatalogItemRepository repository;
    private final CatalogCostContext costContext;
    private final PinyinContext pinyinContext;
    private final CatalogCodeContext codeContext;
    private final CatalogContext context;
    private final CatalogDetailContext detailContext;
    private final CatalogRevisionContext revisionContext;

    public Import(CatalogItemRepository repository, CatalogCostContext costContext, PinyinContext pinyinContext,
                  CatalogCodeContext codeContext, CatalogContext context, CatalogDetailContext detailContext,
                  CatalogRevisionContext revisionContext) {
        this.repository = repository;
        this.costContext = costContext;
        this.pinyinContext = pinyinContext;
        this.codeContext = codeContext;
        this.context = context;
        this.detailContext = detailContext;
        this.revisionContext = revisionContext;
    }

    @Override
    public ImportResult call(List<CatalogItem> request) {
        Employee current = context.requireMaintainer(employee);
        List<CatalogItem> data = resolveData();
        ImportResult result = new ImportResult();
        result.setTotal(data.size());

        int sortBase = 0;
        for (int i = 0; i < data.size(); i++) {
            CatalogItem source = data.get(i);
            try {
                CatalogItem item = buildItem(source, current, sortBase + i);
                item.setRevision(1);
                CatalogItem saved = repository.save(item);
                detailContext.sync(saved);
                revisionContext.record(saved, current, CatalogRevisionType.Create);
                result.incrementSuccess();
            } catch (Exception ex) {
                result.addFailure(i + 1, source.getName() != null ? source.getName() : "(无名称)", ex.getMessage());
            }
        }
        return result;
    }

    private List<CatalogItem> resolveData() {
        if (filename != null && !filename.isBlank()) {
            try (InputStream in = new ClassPathResource(filename).getInputStream()) {
                return MAPPER.readValue(in, new TypeReference<List<CatalogItem>>() {});
            } catch (Exception ex) {
                throw new RuntimeException("读取导入文件失败: " + filename + " - " + ex.getMessage(), ex);
            }
        }
        if (items != null && !items.isEmpty()) {
            return items;
        }
        throw new RuntimeException("请提供要导入的数据或选择导入文件");
    }

    private CatalogItem buildItem(CatalogItem source, Employee current, int sort) {
        CatalogItem item = new CatalogItem();
        item.setCompany(current.getCompany());
        item.setMaintainer(current);
        item.setType(source.getType() != null ? source.getType() : ItemType.Construction);
        item.setCode(source.getCode());
        item.setCategory(source.getCategory() != null && !source.getCategory().isBlank() ? source.getCategory() : "其他");
        item.setBrand(source.getBrand());
        item.setSeries(source.getSeries());
        item.setSpec(source.getSpec());
        item.setName(source.getName());
        item.setNameInitials(pinyinContext.initials(source.getName()));
        item.setUnit(source.getUnit() != null ? source.getUnit() : Unit.Item);
        item.setPricingMode(source.getPricingMode() != null ? source.getPricingMode() : PricingMethod.MaterialAndManual);
        item.setMaterialPrice(zero(source.getMaterialPrice()));
        item.setManualPrice(zero(source.getManualPrice()));
        // 成本空白保持 null（成本待维护），0 表示明确零成本。
        item.setCostPrice(source.getCostPrice());
        item.setFormula(context.formula(item.getPricingMode()));
        item.setCraft(source.getCraft());
        item.setCriterion(source.getCriterion());
        item.setDescription(source.getDescription());
        item.setImages(source.getImages());
        item.setSort(sort);
        item.setActive(Boolean.TRUE);
        // 类型特有明细
        item.setDurationDays(source.getDurationDays());
        item.setLossRate(source.getLossRate());
        item.setMinimumOrder(source.getMinimumOrder());
        item.setLeadTimeDays(source.getLeadTimeDays());
        item.setWarrantyMonths(source.getWarrantyMonths());
        item.setBoard(source.getBoard());
        item.setCountertop(source.getCountertop());
        return item;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
