package com.th4.edge.decoboon.supply.service.materialsupplier.page.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.model.MaterialSupplier;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.MaterialSupplierRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 材料-供应商供应关系分页。 */
@Note("材料-供应商供应关系分页")
@Properties(allow = {
        @AllowProperties(model = MaterialSupplier.class,
                properties = {"materialSupplierId", "materialKey", "materialName", "brand", "spec", "model",
                        "unit", "purchasePrice", "minimumOrder", "leadTimeDays", "settlementDays",
                        "effectiveFrom", "effectiveTo", "active", "remark", "createTime", "partner", "catalogItem"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "type", "name", "contactName", "telNo", "status",
                        "settlementMode", "settlementDays", "cutoffDay"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "brand", "series", "spec"})
})
@Service("supply/material-supplier/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.supply.service.materialsupplier.page.Page {

    @User private Employee employee;
    @AutoInject private String materialKey;
    @AutoInject private Long partnerId;

    private final TenantContext tenantContext;
    private final MaterialSupplierRepository repository;

    public Page(TenantContext tenantContext, MaterialSupplierRepository repository) {
        this.tenantContext = tenantContext;
        this.repository = repository;
    }

    @Override
    public java.util.List<MaterialSupplier> call(MaterialSupplier request) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        var rows = partnerId == null
                ? repository.findByCompany_ComIdOrderByMaterialSupplierIdDesc(comId)
                : repository.findByCompany_ComIdAndPartner_PartnerIdOrderByMaterialSupplierIdDesc(comId, partnerId);
        if (materialKey != null && !materialKey.isBlank()) {
            rows = rows.stream()
                    .filter(item -> materialKey.trim().equalsIgnoreCase(item.getMaterialKey()))
                    .toList();
        }
        return new PageImpl<>(rows).getContent();
    }
}
