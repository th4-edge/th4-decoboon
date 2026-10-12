package com.th4.edge.decoboon.design.service.schemes.info.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.context.PropertyRegionSyncContext;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.decoboon.property.repository.PropertyRegionRepository;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/** 设计方案详情实现。 */
@Note("设计方案详情")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "customer", "owner", "version", "style", "status", "area",
                        "layout", "description", "coverUrl", "cadUrl", "cadPreviewUrl", "cadConvertStatus", "originalPlanUrl", "demolitionUrl",
                        "masonryUrl", "optimizationUrl", "sitePhotos", "surveyPhotos", "originalPlanImages", "spaces", "items",
                        "requirements", "createTime", "updateTime", "property"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "houseRegionId", "inheritedArea", "inheritedPerimeter", "houseValueStale",
                        "sort", "remark", "images"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "category", "brand", "series", "spec", "name", "unit", "formula",
                        "materialPrice", "manualPrice", "costPrice", "pricingMode", "craft", "criterion"}),
        @AllowProperties(model = DesignItem.class,
                properties = {"itemId", "space", "type", "category", "catalogItem", "name", "brand",
                        "spec", "unit", "quantity", "quantitySource", "materialPrice", "manualPrice", "costPrice", "pricingMode",
                        "amount", "cost", "formula", "pendingMeasure", "formulaRevisionNeeded", "criterion", "craft",
                        "remark", "sort", "images"}),
        @AllowProperties(model = Requirement.class,
                properties = {"requirementId", "type", "spaceName", "kind", "category", "priority", "status",
                        "title", "detail", "response", "source", "confirmedAt"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/scheme/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.design.service.schemes.info.Info {

    @AutoInject
    @NotNull
    private Long designId;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final SpaceRepository spaceRepository;
    private final DesignItemRepository itemRepository;
    private final RequirementRepository requirementRepository;
    private final DesignTemplateFormulaContext formulaContext;
    private final PropertyRegionRepository regionRepository;
    private final PropertyRegionSyncContext regionSyncContext;

    public Info(DesignContext designContext,
                SpaceRepository spaceRepository,
                DesignItemRepository itemRepository,
                RequirementRepository requirementRepository,
                DesignTemplateFormulaContext formulaContext,
                PropertyRegionRepository regionRepository,
                PropertyRegionSyncContext regionSyncContext) {
        this.designContext = designContext;
        this.spaceRepository = spaceRepository;
        this.itemRepository = itemRepository;
        this.requirementRepository = requirementRepository;
        this.formulaContext = formulaContext;
        this.regionRepository = regionRepository;
        this.regionSyncContext = regionSyncContext;
    }

    @Override
    public Design call(Design request) {
        Design design = designContext.requireReadableDesign(designId, employee);
        design.setSpaces(spaceRepository.findByDesign_DesignIdOrderBySortAsc(designId));
        List<DesignItem> items = itemRepository.findByDesign_DesignIdOrderBySortAsc(designId);
        // GAP-042：历史中文公式分项无法按专业变量重算，服务端投影「公式待修订」供设计师识别与修复。
        for (DesignItem item : items) {
            item.setFormulaRevisionNeeded(!formulaContext.isSupportedFormula(item.getFormula()));
        }
        design.setItems(items);
        design.setRequirements(requirementRepository.findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(
                design.getCompany().getComId(), design.getCustomer().getCustId()));
        // DEC-064：房屋区域实测值变更后，方案区域投影「房屋实测值已变更」，
        // 由设计师复核或显式同步（非草稿方案不自动改金额，只提示）。
        applyHouseValueStale(design);
        return design;
    }

    private void applyHouseValueStale(Design design) {
        if (design.getProperty() == null || design.getSpaces() == null || design.getSpaces().isEmpty()) {
            return;
        }
        Long comId = design.getCompany() == null ? null : design.getCompany().getComId();
        if (comId == null) {
            return;
        }
        java.util.Map<Long, PropertyRegion> regions = new java.util.HashMap<>();
        for (PropertyRegion region : regionRepository
                .findByCompany_ComIdAndProperty_PropertyIdOrderBySortAscRegionIdAsc(
                        comId, design.getProperty().getPropertyId())) {
            regions.put(region.getRegionId(), region);
        }
        for (com.th4.edge.decoboon.design.model.Space space : design.getSpaces()) {
            PropertyRegion region = space.getHouseRegionId() == null ? null : regions.get(space.getHouseRegionId());
            space.setHouseValueStale(regionSyncContext.isStale(space, region));
        }
    }
}
