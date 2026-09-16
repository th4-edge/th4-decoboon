package com.th4.edge.decoboon.catalog.service.items.update.support;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 家装材料库更新实现。 */
@Note("家装材料库更新")
@Properties(allow = @AllowProperties(model = CatalogItem.class,
        properties = {"itemId", "type", "code", "category", "brand", "series", "spec", "name", "unit",
                "pricingMode", "materialPrice", "manualPrice", "costPrice", "formula", "craft", "criterion",
                "description", "images", "maintainer", "active", "sort", "updateTime"}))
@Service("catalog/item/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.catalog.service.items.update.Update {

    @AutoInject @NotNull private Long itemId;
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
    @AutoInject private BigDecimal costPrice;
    @AutoInject private String craft;
    @AutoInject private String criterion;
    @AutoInject private String description;
    @AutoInject private List<String> images;
    @AutoInject private Boolean active;
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
    private final PinyinContext pinyinContext;
    private final CatalogContext context;
    private final CatalogDetailContext detailContext;

    public Update(CatalogItemRepository repository, PinyinContext pinyinContext, CatalogContext context, CatalogDetailContext detailContext) {
        this.repository = repository;
        this.pinyinContext = pinyinContext;
        this.context = context;
        this.detailContext = detailContext;
    }

    @Override
    public CatalogItem call(CatalogItem request) {
        Employee current = context.requireMaintainer(employee);
        CatalogItem item = repository.findByItemIdAndCompany_ComId(itemId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料库项目不存在")));
        item.setType(type);
        item.setCode(code);
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
        item.setCostPrice(zero(costPrice));
        item.setFormula(context.formula(pricingMode));
        item.setCraft(craft);
        item.setCriterion(criterion);
        item.setDescription(description);
        item.setImages(images);
        item.setActive(active == null || active);
        item.setSort(sort == null ? 0 : sort);
        item.setMaintainer(current);
        CatalogItem saved = repository.save(item);
        detailContext.sync(saved);
        return saved;
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
