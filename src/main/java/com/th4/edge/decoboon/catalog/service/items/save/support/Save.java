package com.th4.edge.decoboon.catalog.service.items.save.support;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.CatalogCodeContext;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 家装材料库新增实现。 */
@Note("家装材料库新增")
@Properties(allow = @AllowProperties(model = CatalogItem.class,
        properties = {"itemId", "type", "code", "category", "brand", "series", "spec", "name", "unit",
                "pricingMode", "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "formula", "craft", "criterion",
                "description", "images", "maintainer", "active", "sort", "sourceType", "createTime"}))
@Service("catalog/item/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.catalog.service.items.save.Save {

    @AutoInject @NotNull private ItemType type;
    @AutoInject private String code;
    @AutoInject @NotEmpty private String category;
    @AutoInject private String brand;
    @AutoInject private String series;
    @AutoInject private String spec;
    @AutoInject @NotEmpty private String name;
    @AutoInject @NotNull private Unit unit;
    @AutoInject @NotNull private PricingMethod pricingMode;
    @AutoInject private BigDecimal materialPrice;
    @AutoInject private BigDecimal manualPrice;
    @AutoInject private BigDecimal fixedTotalPrice;
    @AutoInject private BigDecimal costPrice;
    @AutoInject private String craft;
    @AutoInject private String criterion;
    @AutoInject private String description;
    @AutoInject private List<String> images;
    @AutoInject private Integer sort;
    @AutoInject private Integer durationDays;
    @AutoInject private BigDecimal lossRate;
    @AutoInject private BigDecimal minimumOrder;
    @AutoInject private Integer leadTimeDays;
    @AutoInject private Integer warrantyMonths;
    @AutoInject private String board;
    @AutoInject private String countertop;

    @User private Employee employee;

    private final CatalogItemRepository repository;
    private final CatalogCostContext costContext;
    private final PinyinContext pinyinContext;
    private final CatalogCodeContext codeContext;
    private final CatalogContext context;
    private final CatalogDetailContext detailContext;

    public Save(CatalogItemRepository repository, CatalogCostContext costContext, PinyinContext pinyinContext, CatalogCodeContext codeContext,
                CatalogContext context, CatalogDetailContext detailContext) {
        this.repository = repository;
        this.costContext = costContext;
        this.pinyinContext = pinyinContext;
        this.codeContext = codeContext;
        this.context = context;
        this.detailContext = detailContext;
    }

    @Override
    public CatalogItem call(CatalogItem request) {
        Employee current = context.requirePriceMaintainer(employee);
        CatalogItem item = new CatalogItem();
        item.setCompany(current.getCompany());
        item.setMaintainer(current);
        apply(item, current.getCompany().getComId());
        applyDetails(item);
        CatalogItem saved = repository.save(item);
        detailContext.sync(saved);
        return costContext.mask(saved, current);
    }

    private void apply(CatalogItem item, Long comId) {
        item.setType(type);
        // 前端可不传编号：为空时由后端按「类型前缀 + 企业内序号」自动生成；传了则尊重企业自定义命名
        item.setCode(code == null || code.isBlank()
                ? codeContext.nextCode(comId, type)
                : code.trim());
        item.setCategory(category);
        item.setBrand(brand);
        item.setSeries(series);
        item.setSpec(spec);
        item.setName(name);
        item.setNameInitials(pinyinContext.initials(name));
        item.setUnit(unit);
        item.setPricingMode(pricingMode);
        item.setMaterialPrice(zero(materialPrice));
        item.setManualPrice(zero(manualPrice));
        item.setFixedTotalPrice(fixedTotalPrice);
        // 成本空白保持 null（成本待维护），0 表示明确零成本
        item.setCostPrice(costPrice);
        item.setSourceType(com.th4.edge.decoboon.catalog.configure.CatalogSourceType.Manual);
        item.setFormula(context.formula(pricingMode));
        item.setCraft(craft);
        item.setCriterion(criterion);
        item.setDescription(description);
        item.setImages(images);
        item.setSort(sort == null ? 0 : sort);
        item.setActive(Boolean.TRUE);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 类型特有明细字段：仅传输，落库由 CatalogDetailContext 完成。 */
    private void applyDetails(CatalogItem item) {
        item.setDurationDays(durationDays);
        item.setLossRate(lossRate);
        item.setMinimumOrder(minimumOrder);
        item.setLeadTimeDays(leadTimeDays);
        item.setWarrantyMonths(warrantyMonths);
        item.setBoard(board);
        item.setCountertop(countertop);
    }
}
