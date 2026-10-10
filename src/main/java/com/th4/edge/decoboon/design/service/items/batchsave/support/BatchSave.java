package com.th4.edge.decoboon.design.service.items.batchsave.support;

import com.th4.edge.decoboon.catalog.configure.PricingMethod;
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
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 设计分项批量保存实现。
 *
 * <p>前端「分项与报价」表一次提交整张草稿表：新增/更新行与删除行在同一事务内完成。
 * 先对整批做归属、项目库、区域、工程量校验，全部通过后才落地写入；
 * 任一行不合法整批拒绝，避免出现「只保存了一部分却提示成功」的静默丢行。</p>
 */
@Note("设计分项批量保存")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "status", "version", "area", "layout", "style",
                        "spaces", "items", "updateTime"}),
        @AllowProperties(model = DesignItem.class,
                properties = {"itemId", "space", "type", "category", "catalogItem", "name", "brand", "spec", "unit",
                        "quantity", "quantitySource", "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "pricingMode",
                        "amount", "cost", "formula", "pendingMeasure", "criterion", "craft", "remark", "sort", "images"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "sort", "remark"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "pricingMode",
                        "formula", "criterion", "craft", "active"})
})
@Service("design/items/save")
@Scope(ScopeTypes.PROTOTYPE)
public class BatchSave implements com.th4.edge.decoboon.design.service.items.batchsave.BatchSave {

    @AutoInject
    @NotNull
    private Long designId;

    @AutoInject
    private List<Long> deletedItemIds;

    @AutoInject
    private List<DesignItem> items;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignItemRepository itemRepository;
    private final SpaceRepository spaceRepository;
    private final CatalogItemRepository catalogRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final QuoteCalcContext quoteCalcContext;
    private final DataScopeContext dataScopeContext;

    public BatchSave(DesignContext designContext,
                     DesignItemRepository itemRepository,
                     SpaceRepository spaceRepository,
                     CatalogItemRepository catalogRepository,
                     QuoteLineRepository quoteLineRepository,
                     QuoteCalcContext quoteCalcContext,
                     DataScopeContext dataScopeContext) {
        this.designContext = designContext;
        this.itemRepository = itemRepository;
        this.spaceRepository = spaceRepository;
        this.catalogRepository = catalogRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.quoteCalcContext = quoteCalcContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional
    public Design call(Design ignored) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);

        List<DesignItem> toDelete = prepareDeletions(design);
        List<PreparedRow> toWrite = prepareRows(design);

        // 校验全部通过后才写入，任一行失败时不会留下半批数据。
        for (DesignItem doomed : toDelete) {
            itemRepository.delete(doomed);
        }
        for (PreparedRow row : toWrite) {
            applyCatalog(row.target(), row.catalog());
            row.target().setQuantity(row.quantity());
            row.target().setQuantitySource(row.quantitySource());
            row.target().setRemark(row.remark());
            row.target().setSort(row.sort());
            quoteCalcContext.calculate(row.target());
            designContext.apply(row.target(), design, row.space());
            itemRepository.save(row.target());
        }

        design.setSpaces(spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId()));
        design.setItems(itemRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId()));
        return design;
    }

    private List<DesignItem> prepareDeletions(Design design) {
        List<DesignItem> toDelete = new ArrayList<>();
        if (deletedItemIds == null) {
            return toDelete;
        }
        for (Long itemId : deletedItemIds) {
            if (itemId == null) {
                continue;
            }
            DesignItem doomed = requireOwnedItem(itemId);
            requireSameDesign(design, doomed);
            if (quoteLineRepository.existsByDesignItem_ItemId(itemId)) {
                throw new DefinedException(new DefaultErrorCode("400", "分项已生成报价快照，归档报价后才能删除"));
            }
            toDelete.add(doomed);
        }
        return toDelete;
    }

    private List<PreparedRow> prepareRows(Design design) {
        List<PreparedRow> prepared = new ArrayList<>();
        List<DesignItem> incoming = items == null ? List.of() : items;
        for (DesignItem row : incoming) {
            DesignItem target = row.getItemId() == null ? new DesignItem() : requireOwnedItem(row.getItemId());
            if (target.getItemId() != null) {
                requireSameDesign(design, target);
            }
            BigDecimal quantity = row.getQuantity();
            if (quantity == null || quantity.signum() < 0) {
                throw new DefinedException(new DefaultErrorCode("400", "工程量不能为负数"));
            }
            Long catalogId = row.getCatalogItem() == null ? null : row.getCatalogItem().getItemId();
            if (catalogId == null) {
                throw new DefinedException(new DefaultErrorCode("400",
                        "请从企业报价项目库选择施工、主材、软装、定制、电器或综合项目"));
            }
            CatalogItem catalog = requireCatalog(design, catalogId);
            QuantitySource quantitySource = row.getQuantitySource();
            if (quantitySource == null) {
                quantitySource = target.getQuantitySource();
            }
            if (quantitySource == null) {
                quantitySource = catalog.getFormula() == null || catalog.getFormula().isBlank()
                        ? QuantitySource.Manual : QuantitySource.Formula;
            }
            prepared.add(new PreparedRow(
                    target,
                    row,
                    quantity,
                    row.getRemark(),
                    row.getSort() == null ? 0 : row.getSort(),
                    catalog,
                    requireSpace(design, row.getSpace()),
                    quantitySource));
        }
        return prepared;
    }

    private void requireSameDesign(Design design, DesignItem item) {
        if (item.getDesign() == null || !design.getDesignId().equals(item.getDesign().getDesignId())) {
            throw new DefinedException(new DefaultErrorCode("403", "分项不属于当前方案"));
        }
    }

    private DesignItem requireOwnedItem(Long itemId) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<DesignItem> byId = (root, query, builder) -> builder.equal(root.get("itemId"), itemId);
        return itemRepository.findOne(byId.and(DataScopeSpecification.visible(DesignItem.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "设计分项不存在或无权访问")));
    }

    private CatalogItem requireCatalog(Design design, Long catalogId) {
        CatalogItem item = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料库项目不存在")));
        if (item.getCompany() == null
                || !design.getCompany().getComId().equals(item.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "材料库项目不属于当前企业"));
        }
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new DefinedException(new DefaultErrorCode("400", "该材料库项目已停用"));
        }
        return item;
    }

    private Space requireSpace(Design design, Space requested) {
        Long spaceId = requested == null ? null : requested.getSpaceId();
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
        target.setPricingMode(source.getPricingMode() == null
                ? PricingMethod.MaterialAndManual : source.getPricingMode());
        target.setFormula(source.getFormula());
        target.setCriterion(source.getCriterion());
        target.setCraft(source.getCraft());
        target.setImages(source.getImages() == null ? null : new ArrayList<>(source.getImages()));
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record PreparedRow(
            DesignItem target,
            DesignItem source,
            BigDecimal quantity,
            String remark,
            Integer sort,
            CatalogItem catalog,
            Space space,
            QuantitySource quantitySource) {
    }
}
