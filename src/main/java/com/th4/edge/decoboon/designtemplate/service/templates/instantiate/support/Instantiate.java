package com.th4.edge.decoboon.designtemplate.service.templates.instantiate.support;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.configure.SpaceValueOrigin;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设计模版实例化。
 *
 * <p>前置：客户与房屋已建立、模版已发布且在可见范围内（BR-040 §7）。
 * 幂等：同一房屋 + 同一模版版本只生成一套区域，重复提交直接返回既有设计方案。</p>
 *
 * <p>白名单：只放行设计方案的展示字段；<b>不返回模版分项的任何价格字段</b>。</p>
 */
@Note("设计模版实例化")
@Description("按已发布模版生成设计方案的区域骨架（幂等）")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout",
                        "description", "spaces", "items", "designTemplateVersion", "createTime", "owner", "store", "team"}),
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "type", "name", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId",
                        "sort", "remark"}),
        @AllowProperties(model = DesignItem.class,
                properties = {"itemId", "type", "category", "name", "brand", "spec", "unit", "quantity",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "pricingMode", "amount",
                        "formula", "quantityFormula", "pendingMeasure", "criterion", "craft", "remark", "sort"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"})
})
@Service("design/template/instantiate")
@Scope(ScopeTypes.PROTOTYPE)
public class Instantiate implements com.th4.edge.decoboon.designtemplate.service.templates.instantiate.Instantiate {

    @AutoInject
    private Long templateId;

    @AutoInject
    private Long versionId;

    @AutoInject
    private Long propertyId;

    @AutoInject
    private String name;

    @User
    private Employee employee;

    private final DesignTemplateRepository templateRepository;
    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateRoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final DesignRepository designRepository;
    private final SpaceRepository spaceRepository;
    private final DesignItemRepository designItemRepository;
    private final DesignTemplateItemRepository templateItemRepository;
    private final DesignTemplateFormulaContext formulaContext;
    private final DesignTemplateAccessContext accessContext;
    private final MeasureRepository measureRepository;
    private final MeasureRegionRepository measureRegionRepository;

    public Instantiate(DesignTemplateRepository templateRepository,
                       DesignTemplateVersionRepository versionRepository,
                       DesignTemplateRoomRepository roomRepository,
                       PropertyRepository propertyRepository,
                       DesignRepository designRepository,
                       SpaceRepository spaceRepository,
                       DesignItemRepository designItemRepository,
                       DesignTemplateItemRepository templateItemRepository,
                       DesignTemplateFormulaContext formulaContext,
                       DesignTemplateAccessContext accessContext,
                       MeasureRepository measureRepository,
                       MeasureRegionRepository measureRegionRepository) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.roomRepository = roomRepository;
        this.propertyRepository = propertyRepository;
        this.designRepository = designRepository;
        this.spaceRepository = spaceRepository;
        this.designItemRepository = designItemRepository;
        this.templateItemRepository = templateItemRepository;
        this.formulaContext = formulaContext;
        this.accessContext = accessContext;
        this.measureRepository = measureRepository;
        this.measureRegionRepository = measureRegionRepository;
    }

    @Override
    @Transactional
    public Design call(Design request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();

        if (templateId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "templateId不能为空"));
        }
        if (propertyId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "propertyId不能为空"));
        }
        DesignTemplate template = templateRepository.findByTemplateIdAndCompany_ComId(templateId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版不存在")));
        if (!accessContext.visible(me, template)) {
            throw new DefinedException(new DefaultErrorCode("403", "无权使用该模版"));
        }
        if (template.getStatus() == DesignTemplate.Status.Disabled) {
            throw new DefinedException(new DefaultErrorCode("400", "模版已停用，不能实例化"));
        }
        DesignTemplateVersion version = versionId == null ? template.getCurrentVersion()
                : versionRepository.findByVersionIdAndCompany_ComId(versionId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版版本不存在")));
        if (version == null) {
            throw new DefinedException(new DefaultErrorCode("400", "模版没有可实例化的版本"));
        }
        if (version.getStatus() != DesignTemplate.Status.Published) {
            throw new DefinedException(new DefaultErrorCode("400", "只有已发布的模版版本可以实例化"));
        }

        Property property = propertyRepository.findByPropertyIdAndCompany_ComId(propertyId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋不存在")));

        // 幂等：同房屋 + 同模版版本只生成一次
        Design existing = designRepository
                .findFirstByCompany_ComIdAndProperty_PropertyIdAndDesignTemplateVersion_VersionId(
                        comId, propertyId, version.getVersionId())
                .orElse(null);
        if (existing != null) {
            existing.setSpaces(spaceRepository.findByDesign_DesignIdOrderBySortAsc(existing.getDesignId()));
            existing.setItems(designItemRepository.findByDesign_DesignIdOrderBySortAsc(existing.getDesignId()));
            return existing;
        }

        List<DesignTemplateRoom> rooms = roomRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), comId);
        if (rooms.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "模版没有区域，无法实例化"));
        }

        Design design = new Design();
        design.setNo("D" + System.currentTimeMillis());
        design.setName(name == null || name.isBlank() ? version.getName() : name.trim());
        design.setVersion(1);
        design.setStyle(version.getTags());
        design.setStatus(DesignStatus.Draft);
        design.setCompany(me.getCompany());
        design.setStore(me.getStore());
        design.setTeam(me.getTeam());
        design.setOwner(me);
        design.setCustomer(property.getCustomer());
        design.setProperty(property);
        design.setDesignTemplateVersion(version);
        design.setArea(BigDecimal.ZERO);
        Design saved = designRepository.save(design);

        // DEC-066：原始区域挂在量房单上（量房完成后冻结），实例化时从**最近一次已完成的量房**继承，
        // 作为设计方案的结构起点（一次性复制，之后不联动）。匹配歧义（同类型同名多条）不静默择一。
        Measure latestMeasure = measureRepository.findByPropertyOrderByMeasureTimeDesc(property).stream()
                .filter(item -> item.getStatus() == MeasureStatus.Completed)
                .findFirst()
                .orElse(null);
        List<MeasureRegion> measureRegions = latestMeasure == null ? List.of()
                : measureRegionRepository.findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(
                        comId, latestMeasure.getMeasureId());
        List<Space> spaces = new ArrayList<>();
        Map<String, Space> spaceByRoomKey = new HashMap<>();
        for (DesignTemplateRoom room : rooms) {
            Space space = new Space();
            space.setCompany(me.getCompany());
            space.setStore(me.getStore());
            space.setTeam(me.getTeam());
            space.setOwner(me);
            space.setDesign(saved);
            space.setProperty(property);
            space.setType(room.getRoomType() == null ? SpaceType.Bedroom : room.getRoomType());
            space.setName(room.getName());
            space.setLength(room.getLength());
            space.setWidth(room.getWidth());
            space.setHeight(room.getHeight());
            space.setSort(room.getSortNo() == null ? 0 : room.getSortNo());
            space.setRemark(room.getRemark());
            // BR-040 §4 / DEC-062（EV-213）：长宽是量房快照，绝不据此推算面积/周长。
            // DEC-064：房屋档案已有实测值则继承（可作工程量基数）；无实测值时用模版参考值打底
            //（来源标记 TemplateReference 且 areaSource=Estimated，不进入工程量与金额）；都没有则待补。
            applyInheritedMeasured(space, room, measureRegions);
            spaces.add(space);
            spaceByRoomKey.put(room.getRoomKey(), space);
        }
        spaceRepository.saveAll(spaces);

        // 方案面积为区域实测面积合计（DEC-062）：实例化阶段尚无实测值，保持 0（前端显示「面积待补充」），
        // 设计师录入区域实测面积后由 design/space/save → DesignFormulaRecalcContext 重算。
        saved.setSpaces(spaces);

        // 分项：名称/单位/价格读 catalogItem；工程量按区域专业变量服务端求值（初算，最终以报价主链为准）
        List<DesignTemplateItem> templateItems = templateItemRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), comId);
        List<DesignItem> items = new ArrayList<>();
        for (DesignTemplateItem templateItem : templateItems) {
            CatalogItem catalogItem = templateItem.getCatalogItem();
            if (catalogItem == null) continue;
            DesignTemplateRoom room = templateItem.getRoomKey() == null ? null
                    : rooms.stream().filter(r -> r.getRoomKey().equals(templateItem.getRoomKey())).findFirst().orElse(null);

            BigDecimal quantity;
            boolean pendingMeasure = false;
            String templateFormula = templateItem.getQuantityFormula();
            if (templateFormula != null && !templateFormula.isBlank()) {
                try {
                    quantity = room != null
                            // DEC-064：按本区域继承到的实测值求值；未继承到（待补/仅参考值）则抛「未就绪」落待补。
                            ? formulaContext.evaluate(room, templateFormula,
                                    measuredArea(spaceByRoomKey.get(room.getRoomKey())),
                                    measuredPerimeter(spaceByRoomKey.get(room.getRoomKey())))
                            : formulaContext.evaluateWith(templateFormula, wholeHouseVariables(rooms, spaces));
                } catch (DefinedException exception) {
                    // BR-040 §7 / 2026-10-10 用户裁决：尺寸未填不再整单失败；
                    // 用 quantity=0 + formula 前缀标记“待补尺寸”，与“0=赠送”并存。
                    if (exception.getErrorCode() != null
                            && "TEMPLATE_SIZE_MISSING".equals(exception.getErrorCode().getCode())) {
                        quantity = BigDecimal.ZERO;
                        pendingMeasure = true;
                    } else {
                        throw exception;
                    }
                }
            } else {
                quantity = templateItem.getDefaultQuantity() == null ? BigDecimal.ZERO : templateItem.getDefaultQuantity();
            }

            DesignItem item = new DesignItem();
            item.setCompany(me.getCompany());
            item.setStore(me.getStore());
            item.setTeam(me.getTeam());
            item.setOwner(me);
            item.setDesign(saved);
            item.setSpace(room == null ? null : spaceByRoomKey.get(room.getRoomKey()));
            item.setProperty(property);
            item.setType(catalogItem.getType());
            item.setCategory(catalogItem.getCategory());
            item.setCatalogItem(catalogItem);
            item.setName(catalogItem.getName());
            item.setBrand(catalogItem.getBrand());
            item.setSpec(catalogItem.getSpec());
            item.setUnit(catalogItem.getUnit());
            item.setPricingMode(catalogItem.getPricingMode());
            item.setMaterialPrice(catalogItem.getMaterialPrice());
            item.setManualPrice(catalogItem.getManualPrice());
            item.setFixedTotalPrice(catalogItem.getFixedTotalPrice());
            // formula 是报价项目库的计价说明快照；模版专业变量只进入 quantityFormula。
            item.setFormula(catalogItem.getFormula());
            item.setQuantityFormula(pendingMeasure
                    ? DesignTemplateFormulaContext.PENDING_MEASURE_MARKER + "|"
                            + (templateFormula == null ? "" : templateFormula)
                    : templateFormula);
            item.setCriterion(catalogItem.getCriterion());
            item.setCraft(catalogItem.getCraft());
            item.setRemark(templateItem.getRemark());
            item.setSort(templateItem.getSortNo() == null ? 0 : templateItem.getSortNo());
            item.setQuantity(quantity);
            item.setQuantitySource(templateFormula == null || templateFormula.isBlank()
                    ? com.th4.edge.decoboon.design.configure.QuantitySource.Manual
                    : com.th4.edge.decoboon.design.configure.QuantitySource.Formula);
            // 待补项不按固定总价等计价方式出金额；先落 0，等设计师调整或按“0=赠送”处理。
            item.setAmount(pendingMeasure ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amountOf(item));
            items.add(item);
        }
        if (!items.isEmpty()) {
            designItemRepository.saveAll(items);
        }
        // 方案面积 = 区域实测面积合计（只算 Measured；参考值与未录入不参与，DEC-062 / DEC-064）
        saved.setArea(spaces.stream()
                .filter(space -> space.getAreaSource() == AreaSource.Measured)
                .map(Space::getArea)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        saved.setItems(items);
        return designRepository.save(saved);
    }

    /**
     * 全屋共享分项的变量。
     *
     * <p>{@code COUNT(room)} 恒可用；{@code A/P/WP} 只在**全部区域都继承到实测值**时聚合
     * （与前端 {@code variablesOfWholeHouse} 同口径）。任一区域未就绪则整项按「未就绪」处理，
     * 由 {@code design/space/save} 触发的重算在设计师补齐实测值后再求值。</p>
     */
    private Map<String, BigDecimal> wholeHouseVariables(List<DesignTemplateRoom> rooms, List<Space> spaces) {
        Map<String, BigDecimal> whole = new HashMap<>();
        whole.put("N", BigDecimal.ONE);
        whole.put(DesignTemplateFormulaContext.ROOM_COUNT_KEY, BigDecimal.valueOf(rooms.size()));
        if (spaces.isEmpty()) {
            return whole;
        }
        boolean areaReady = true;
        boolean perimeterReady = true;
        boolean wallsReady = true;
        BigDecimal area = BigDecimal.ZERO;
        BigDecimal perimeter = BigDecimal.ZERO;
        BigDecimal walls = BigDecimal.ZERO;
        for (Space space : spaces) {
            BigDecimal measured = measuredArea(space);
            BigDecimal measuredRound = measuredPerimeter(space);
            if (measured == null) {
                areaReady = false;
            } else {
                area = area.add(measured);
            }
            if (measuredRound == null) {
                perimeterReady = false;
            } else {
                perimeter = perimeter.add(measuredRound);
            }
            if (measuredRound == null || space.getHeight() == null) {
                wallsReady = false;
            } else {
                walls = walls.add(measuredRound.multiply(space.getHeight()));
            }
        }
        if (areaReady) {
            whole.put("A", area);
            whole.put("CP", area);
        }
        if (perimeterReady) {
            whole.put("P", perimeter);
        }
        if (wallsReady) {
            whole.put("WP", walls);
        }
        return whole;
    }

    /** DEC-066 继承：量房原始区域实测值优先（起点），其次模版参考值打底（待实测确认，不进金额）。 */
    private void applyInheritedMeasured(Space space, DesignTemplateRoom room, List<MeasureRegion> measureRegions) {
        MeasureRegion region = matchMeasureRegion(measureRegions, room, space.getType(), space.getName());
        if (region != null) {
            space.setSourceMeasureRegionId(region.getRegionId());
            if (region.getArea() != null || region.getPerimeter() != null) {
                space.setArea(region.getArea());
                space.setPerimeter(region.getPerimeter());
                space.setAreaSource(region.getAreaSource() == null ? AreaSource.Estimated : region.getAreaSource());
                space.setValueOrigin(SpaceValueOrigin.Measure);
                return;
            }
        }
        if (room.getReferenceArea() != null || room.getReferencePerimeter() != null) {
            space.setArea(room.getReferenceArea());
            space.setPerimeter(room.getReferencePerimeter());
            // 参考值不承载房屋实测事实：标 Estimated 使其无法作为工程量基数（DesignFormulaRecalcContext 只接受 Measured）
            space.setAreaSource(AreaSource.Estimated);
            space.setValueOrigin(SpaceValueOrigin.TemplateReference);
        }
    }

    /**
     * 量房原始区域匹配（DEC-066）：先按区域键精确匹配，再按「区域类型 + 标准化名称」；
     * 同名多条视为歧义并返回 {@code null}（不静默择一）。
     */
    private MeasureRegion matchMeasureRegion(List<MeasureRegion> regions, DesignTemplateRoom room,
                                             SpaceType type, String name) {
        if (regions.isEmpty()) {
            return null;
        }
        if (room.getRoomKey() != null && !room.getRoomKey().isBlank()) {
            for (MeasureRegion region : regions) {
                if (region.getRoomKey() != null && room.getRoomKey().equalsIgnoreCase(region.getRoomKey())) {
                    return region;
                }
            }
        }
        String normalized = normalizeName(name);
        MeasureRegion matched = null;
        for (MeasureRegion region : regions) {
            if (region.getRoomType() == type && normalizeName(region.getName()).equals(normalized)) {
                if (matched != null) {
                    return null;
                }
                matched = region;
            }
        }
        return matched;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "");
    }

    /** 可作为工程量基数的实测面积：只有 Measured 来源可用（DEC-062）。 */
    private BigDecimal measuredArea(Space space) {
        return space != null && space.getAreaSource() == AreaSource.Measured ? space.getArea() : null;
    }

    /** 可作为工程量基数的实测周长：口径同 {@link #measuredArea(Space)}。 */
    private BigDecimal measuredPerimeter(Space space) {
        return space != null && space.getAreaSource() == AreaSource.Measured ? space.getPerimeter() : null;
    }

    /** 分项金额初算：按计价方式对销售侧单价求值（成本与利润不在此处产生）。 */
    private BigDecimal amountOf(DesignItem item) {
        BigDecimal quantity = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
        BigDecimal material = item.getMaterialPrice() == null ? BigDecimal.ZERO : item.getMaterialPrice();
        BigDecimal manual = item.getManualPrice() == null ? BigDecimal.ZERO : item.getManualPrice();
        PricingMethod mode = item.getPricingMode() == null ? PricingMethod.MaterialAndManual : item.getPricingMode();
        BigDecimal amount = switch (mode) {
            case FixedTotal -> item.getFixedTotalPrice() == null
                    ? material.add(manual).multiply(quantity) : item.getFixedTotalPrice();
            case MaterialOnly -> material.multiply(quantity);
            case ManualOnly -> manual.multiply(quantity);
            case MaterialAndManual -> material.add(manual).multiply(quantity);
        };
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
