package com.th4.edge.decoboon.design.service.items.save.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.design.configure.QuantitySource;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * 设计分项保存实现。
 *
 * <p>设计师只能选择企业统一材料库；施工/主材老数据允许通过原主数据关联保存，
 * 但名称、规格、价格和公式均由主数据覆盖，不接受前端手工改价。</p>
 */
@Note("设计分项保存")
@Properties(allow = {
        @AllowProperties(model = DesignItem.class,
                properties = {"itemId", "design", "space", "type", "category", "catalogItem",
                        "name", "brand", "spec", "unit", "quantity", "quantitySource", "materialPrice", "manualPrice",
                        "costPrice", "pricingMode", "amount", "cost", "formula", "pendingMeasure", "criterion", "craft", "remark", "sort",
                        "images", "createTime", "updateTime"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "status", "area", "layout"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit", "formula"})
})
@Service("design/item/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.design.service.items.save.Save {

    @AutoInject @NotNull private Long designId;
    @AutoInject private Long itemId;
    @AutoInject private Long spaceId;
    @AutoInject private Long catalogItemId;
    @AutoInject @NotNull private BigDecimal quantity;
    @AutoInject private String remark;
    @AutoInject private Integer sort;
    @User private Employee employee;

    private final DesignContext designContext;
    private final DesignItemRepository itemRepository;
    private final SpaceRepository spaceRepository;
    private final CatalogItemRepository catalogRepository;
    private final QuoteCalcContext quoteCalcContext;
    private final DataScopeContext dataScopeContext;

    public Save(DesignContext designContext,
                DesignItemRepository itemRepository,
                SpaceRepository spaceRepository,
                CatalogItemRepository catalogRepository,
                QuoteCalcContext quoteCalcContext,
                DataScopeContext dataScopeContext) {
        this.designContext = designContext;
        this.itemRepository = itemRepository;
        this.spaceRepository = spaceRepository;
        this.catalogRepository = catalogRepository;
        this.quoteCalcContext = quoteCalcContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public DesignItem call(DesignItem request) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);
        DesignItem target = itemId == null ? new DesignItem() : requireItem(itemId, employee);
        Space space = requireSpace(design, spaceId);

        if (catalogItemId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请从企业报价项目库选择施工、主材、软装、定制、电器或综合项目"));
        }
        applyCatalog(target, requireCatalog(design, catalogItemId));

        target.setQuantity(quantity);
        target.setQuantitySource(QuantitySource.Manual);
        target.setRemark(remark);
        target.setSort(sort == null ? 0 : sort);
        quoteCalcContext.calculate(target);
        designContext.apply(target, design, space);
        return itemRepository.save(target);
    }

    private CatalogItem requireCatalog(Design design, Long catalogId) {
        CatalogItem item = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料库项目不存在")));
        if (!design.getCompany().getComId().equals(item.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "材料库项目不属于当前企业"));
        }
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new DefinedException(new DefaultErrorCode("400", "该材料库项目已停用"));
        }
        return item;
    }

    private void applyCatalog(DesignItem target, CatalogItem source) {
        target.setCatalogItem(source);
        target.setType(source.getType());
        target.setCategory(source.getCategory());
        target.setName(source.getName());
        target.setBrand(source.getBrand());
        target.setSpec(source.getSpec());
        target.setUnit(source.getUnit());
        target.setMaterialPrice(zero(source.getMaterialPrice()));
        target.setManualPrice(zero(source.getManualPrice()));
        target.setFixedTotalPrice(source.getFixedTotalPrice());
        // 成本空白保持 null（成本待维护），不得落成 0
        target.setCostPrice(source.getCostPrice());
        target.setPricingMode(source.getPricingMode() == null ? PricingMethod.MaterialAndManual : source.getPricingMode());
        target.setFormula(source.getFormula());
        target.setCriterion(source.getCriterion());
        target.setCraft(source.getCraft());
        target.setImages(source.getImages() == null ? null : new ArrayList<>(source.getImages()));
    }

    private DesignItem requireItem(Long itemId, Employee employee) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<DesignItem> byId = (root, query, builder) -> builder.equal(root.get("itemId"), itemId);
        return itemRepository.findOne(byId.and(DataScopeSpecification.visible(DesignItem.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "设计分项不存在或无权访问")));
    }

    private Space requireSpace(Design design, Long spaceId) {
        if (spaceId == null) {
            return null;
        }
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "方案区域不存在")));
        if (!design.getDesignId().equals(space.getDesign().getDesignId())
                || !design.getCompany().getComId().equals(space.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "方案区域不属于当前方案"));
        }
        return space;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
