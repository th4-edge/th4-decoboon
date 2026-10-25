package com.th4.edge.decoboon.catalog.service.items.update.support;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.context.CatalogContext;
import com.th4.edge.decoboon.catalog.context.CatalogDetailContext;
import com.th4.edge.decoboon.catalog.context.PinyinContext;
import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.catalog.context.CatalogMutationContext;
import com.th4.edge.decoboon.catalog.context.CatalogRevisionContext;
import com.th4.edge.decoboon.catalog.configure.CatalogRevisionType;
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
@Properties(allow = {
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "code", "category", "brand", "series", "spec", "name", "unit",
                        "pricingMode", "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "formula", "craft", "criterion",
                        "description", "images", "maintainer", "active", "sort", "revision", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
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
    @AutoInject private BigDecimal fixedTotalPrice;
    @AutoInject private BigDecimal costPrice;
    @AutoInject private String craft;
    @AutoInject private String criterion;
    @AutoInject private String description;
    @AutoInject private List<String> images;
    @AutoInject private Boolean active;
    @AutoInject private Integer sort;
    @AutoInject private Integer revision;
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
    private final CatalogContext context;
    private final CatalogDetailContext detailContext;
    private final CatalogRevisionContext revisionContext;
    private final CatalogMutationContext mutationContext;

    public Update(CatalogItemRepository repository, CatalogCostContext costContext, PinyinContext pinyinContext, CatalogContext context, CatalogDetailContext detailContext,
                  CatalogRevisionContext revisionContext, CatalogMutationContext mutationContext) {
        this.repository = repository;
        this.costContext = costContext;
        this.pinyinContext = pinyinContext;
        this.context = context;
        this.detailContext = detailContext;
        this.revisionContext = revisionContext;
        this.mutationContext = mutationContext;
    }

    @Override
    public CatalogItem call(CatalogItem request) {
        Employee current = context.requirePriceMaintainer(employee);
        return mutationContext.execute(current.getCompany().getComId(), itemId, () -> updateLocked(current));
    }

    private CatalogItem updateLocked(Employee current) {
        CatalogItem item = repository.findByItemIdAndCompany_ComId(itemId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料库项目不存在")));
        if (revision != null && !revision.equals(item.getRevision())) {
            throw new DefinedException(new DefaultErrorCode("409", "报价项目已被其他人更新，请刷新后重试"));
        }
        detailContext.hydrate(item);
        String before = revisionContext.fingerprint(item);
        boolean wasActive = Boolean.TRUE.equals(item.getActive());
        if (item.getType() != type) clearDetails(item);
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
        item.setFixedTotalPrice(fixedTotalPrice);
        item.setCostPrice(costPrice);
        item.setFormula(context.formula(pricingMode));
        item.setCraft(craft);
        item.setCriterion(criterion);
        item.setDescription(description);
        item.setImages(images);
        item.setActive(active == null || active);
        item.setSort(sort == null ? 0 : sort);
        item.setMaintainer(current);
        applyDetails(item);
        CatalogItem saved = repository.save(item);
        detailContext.sync(saved);
        if (!before.equals(revisionContext.fingerprint(saved))) {
            revisionContext.nextRevision(saved);
            CatalogRevisionType changeType = wasActive && !Boolean.TRUE.equals(saved.getActive())
                    ? CatalogRevisionType.Disable
                    : !wasActive && Boolean.TRUE.equals(saved.getActive())
                            ? CatalogRevisionType.Enable : CatalogRevisionType.Update;
            revisionContext.record(saved, current, changeType);
        }
        return costContext.mask(saved, current);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 类型特有明细字段：仅传输，落库由 CatalogDetailContext 完成。 */
    private void applyDetails(CatalogItem item) {
        if (durationDays != null) item.setDurationDays(durationDays);
        if (lossRate != null) item.setLossRate(lossRate);
        if (minimumOrder != null) item.setMinimumOrder(minimumOrder);
        if (leadTimeDays != null) item.setLeadTimeDays(leadTimeDays);
        if (warrantyMonths != null) item.setWarrantyMonths(warrantyMonths);
        if (board != null) item.setBoard(board);
        if (countertop != null) item.setCountertop(countertop);
    }

    private void clearDetails(CatalogItem item) {
        item.setDurationDays(null);
        item.setLossRate(null);
        item.setMinimumOrder(null);
        item.setLeadTimeDays(null);
        item.setWarrantyMonths(null);
        item.setBoard(null);
        item.setCountertop(null);
    }
}
