package com.th4.edge.decoboon.supply.service.materialsupplier.match.support;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.project.context.MaterialPlanContext;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.MaterialSupplier;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.MaterialSupplierRepository;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按下单材料匹配可用供应商与参考价。
 *
 * <p>DEC-MP-03/DEC-MP-08：只使用结构化材料标识匹配，不使用选材自由文本。
 * 命中顺序：①企业维护的材料-供应商供应关系；②供应商品牌匹配（参考价取 PartnerPrice）。</p>
 */
@Note("材料供应商匹配")
@Properties(allow = {
        @AllowProperties(model = MaterialSupplier.class,
                properties = {"materialSupplierId", "materialKey", "materialName", "brand", "spec", "model",
                        "unit", "purchasePrice", "minimumOrder", "leadTimeDays", "settlementDays",
                        "effectiveFrom", "effectiveTo", "active", "remark", "partner", "catalogItem"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "type", "name", "contactName", "telNo", "status",
                        "settlementMode", "settlementDays", "cutoffDay"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "brand", "series", "spec"})
})
@Service("supply/material-supplier/match")
@Scope(ScopeTypes.PROTOTYPE)
public class Match implements com.th4.edge.decoboon.supply.service.materialsupplier.match.Match {

    @User private Employee employee;
    @AutoInject private String materialKey;
    @AutoInject private Long catalogItemId;
    @AutoInject private String brand;
    @AutoInject private String spec;
    @AutoInject private String model;
    @AutoInject private Unit unit;

    private final TenantContext tenantContext;
    private final MaterialSupplierRepository materialSupplierRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerPriceRepository partnerPriceRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final MaterialPlanContext materialPlanContext;

    public Match(TenantContext tenantContext,
                 MaterialSupplierRepository materialSupplierRepository,
                 PartnerRepository partnerRepository,
                 PartnerPriceRepository partnerPriceRepository,
                 CatalogItemRepository catalogItemRepository,
                 MaterialPlanContext materialPlanContext) {
        this.tenantContext = tenantContext;
        this.materialSupplierRepository = materialSupplierRepository;
        this.partnerRepository = partnerRepository;
        this.partnerPriceRepository = partnerPriceRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.materialPlanContext = materialPlanContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialSupplier> call(MaterialSupplier request) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        CatalogItem catalogItem = catalogItemId == null ? null : catalogItemRepository.findById(catalogItemId)
                .filter(item -> item.getCompany() == null || item.getCompany().getComId().equals(comId))
                .orElse(null);
        String key = blankToNull(materialKey);
        if (key == null) {
            key = materialPlanContext.materialKey(catalogItemId,
                    blankToNull(brand), blankToNull(spec), blankToNull(model), unit);
        }
        List<MaterialSupplier> matched = new ArrayList<>();
        if (key != null && !key.isBlank()) {
            matched.addAll(materialSupplierRepository
                    .findByCompany_ComIdAndMaterialKeyAndActiveTrueOrderByPurchasePriceAsc(comId, key.trim()));
        }
        if (!matched.isEmpty()) {
            return matched;
        }
        return brandFallback(comId, catalogItem);
    }

    /** 品牌兜底：品牌匹配的启用供应商，参考价取 PartnerPrice 当前有效采购价。 */
    private List<MaterialSupplier> brandFallback(Long comId, CatalogItem catalogItem) {
        String wanted = blankToNull(brand);
        if (wanted == null) {
            return List.of();
        }
        Map<Long, MaterialSupplier> result = new LinkedHashMap<>();
        for (Partner partner : partnerRepository.findByCompany_ComIdAndTypeAndStatusOrderByName(
                comId, PartnerType.Supplier, PartnerStatus.Active)) {
            if (partner.getBrands() == null || partner.getBrands().stream()
                    .noneMatch(item -> item != null && item.equalsIgnoreCase(wanted))) {
                continue;
            }
            MaterialSupplier transientRow = new MaterialSupplier();
            transientRow.setCompany(partner.getCompany());
            transientRow.setPartner(partner);
            transientRow.setBrand(wanted);
            transientRow.setSpec(blankToNull(spec));
            transientRow.setModel(blankToNull(model));
            transientRow.setUnit(unit);
            transientRow.setCatalogItem(catalogItem);
            transientRow.setMaterialName(catalogItem == null ? null : catalogItem.getName());
            transientRow.setPurchasePrice(referencePrice(partner, catalogItem));
            transientRow.setMinimumOrder(BigDecimal.ONE);
            transientRow.setLeadTimeDays(0);
            transientRow.setSettlementDays(partner.getSettlementDays());
            transientRow.setActive(Boolean.TRUE);
            transientRow.setMaterialKey(materialPlanContext.materialKey(
                    catalogItem == null ? null : catalogItem.getItemId(),
                    wanted, blankToNull(spec), blankToNull(model), unit));
            result.putIfAbsent(partner.getPartnerId(), transientRow);
        }
        return new ArrayList<>(result.values());
    }

    private BigDecimal referencePrice(Partner partner, CatalogItem catalogItem) {
        if (catalogItem == null) {
            return BigDecimal.ZERO;
        }
        PartnerPrice price = partnerPriceRepository
                .findFirstByPartnerAndItemAndActiveTrueOrderByPartnerPriceIdDesc(partner, catalogItem);
        return price == null || price.getPurchasePrice() == null ? BigDecimal.ZERO : price.getPurchasePrice();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
