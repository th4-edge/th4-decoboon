package com.th4.edge.decoboon.supply.service.materialsupplier.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.project.context.MaterialPlanContext;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.model.MaterialSupplier;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.MaterialSupplierRepository;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 维护材料-供应商供应关系：同一材料同一供应商唯一，重复保存按主键更新。 */
@Note("材料-供应商供应关系保存")
@Properties(allow = {
        @AllowProperties(model = MaterialSupplier.class,
                properties = {"materialSupplierId", "materialKey", "materialName", "brand", "spec", "model",
                        "unit", "purchasePrice", "minimumOrder", "leadTimeDays", "settlementDays",
                        "effectiveFrom", "effectiveTo", "active", "remark", "createTime", "partner", "catalogItem"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "type", "name", "contactName", "telNo", "status"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "brand", "series", "spec"})
})
@Service("supply/material-supplier/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.supply.service.materialsupplier.save.Save {

    @User private Employee employee;
    @AutoInject @NotNull private Long partnerId;
    @AutoInject private Long catalogItemId;
    @AutoInject @Length(max = 128) private String materialName;
    @AutoInject @Length(max = 128) private String brand;
    @AutoInject @Length(max = 256) private String spec;
    @AutoInject @Length(max = 128) private String model;
    @AutoInject private Unit unit;
    @AutoInject private BigDecimal purchasePrice;
    @AutoInject private BigDecimal minimumOrder;
    @AutoInject private Integer leadTimeDays;
    @AutoInject private Integer settlementDays;
    @AutoInject private LocalDate effectiveFrom;
    @AutoInject private LocalDate effectiveTo;
    @AutoInject @Length(max = 500) private String remark;

    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final MaterialSupplierRepository repository;
    private final MaterialPlanContext materialPlanContext;
    private final ModelIdentityContext modelIdentityContext;

    public Save(TenantContext tenantContext,
                PartnerRepository partnerRepository,
                CatalogItemRepository catalogItemRepository,
                MaterialSupplierRepository repository,
                MaterialPlanContext materialPlanContext,
                ModelIdentityContext modelIdentityContext) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.repository = repository;
        this.materialPlanContext = materialPlanContext;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public MaterialSupplier call(MaterialSupplier request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
        Long comId = current.getCompany().getComId();
        Partner partner = partnerRepository.findById(partnerId)
                .filter(item -> item.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "供应商不存在或无权访问")));
        if (partner.getStatus() != PartnerStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("400", "供应商已停用，不能维护供应关系"));
        }
        CatalogItem catalogItem = catalogItemId == null ? null : catalogItemRepository.findById(catalogItemId)
                .filter(item -> item.getCompany() == null || item.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价项目不存在")));

        String key = materialPlanContext.materialKey(catalogItemId,
                blankToNull(brand), blankToNull(spec), blankToNull(model), unit);
        if (key == null || key.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请至少填写品牌、规格或型号，以确定材料"));
        }
        MaterialSupplier target = repository
                .findFirstByCompany_ComIdAndMaterialKeyAndPartner_PartnerId(comId, key, partnerId)
                .orElseGet(MaterialSupplier::new);
        target.setCompany(current.getCompany());
        target.setPartner(partner);
        target.setMaterialKey(key);
        target.setCatalogItem(catalogItem);
        target.setMaterialName(blankToNull(materialName));
        target.setBrand(blankToNull(brand));
        target.setSpec(blankToNull(spec));
        target.setModel(blankToNull(model));
        target.setUnit(unit);
        target.setPurchasePrice(purchasePrice == null ? BigDecimal.ZERO : purchasePrice);
        target.setMinimumOrder(minimumOrder == null ? BigDecimal.ONE : minimumOrder);
        target.setLeadTimeDays(leadTimeDays == null ? 0 : leadTimeDays);
        target.setSettlementDays(settlementDays);
        target.setEffectiveFrom(effectiveFrom);
        target.setEffectiveTo(effectiveTo);
        target.setActive(Boolean.TRUE);
        target.setRemark(remark);
        target.setCreatedBy(current);
        return repository.save(modelIdentityContext.prepare(target));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
