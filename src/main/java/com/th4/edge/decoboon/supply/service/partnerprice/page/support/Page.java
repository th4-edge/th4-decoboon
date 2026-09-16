package com.th4.edge.decoboon.supply.service.partnerprice.page.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 合作方价格分页。 */
@Note("合作方价格分页")
@Properties(allow = {
        @AllowProperties(model = PartnerPrice.class,
                properties = {"partnerPriceId", "partner", "item", "purchasePrice", "laborPrice",
                        "minimumOrder", "leadTimeDays", "settlementDays", "effectiveFrom", "effectiveTo",
                        "active", "remark", "createTime"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "type", "name"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "brand", "spec"})
})
@Service("partner-price/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.supply.service.partnerprice.page.Page {
    @User private Employee employee;
    @AutoInject private Long partnerId;
    @AutoInject private Long itemId;
    private final TenantContext tenantContext;
    private final PartnerPriceRepository priceRepository;
    public Page(TenantContext tenantContext, PartnerPriceRepository priceRepository) {
        this.tenantContext = tenantContext;
        this.priceRepository = priceRepository;
    }
    @Override
    public org.springframework.data.domain.Page<PartnerPrice> call(PartnerPrice request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
        return new PageImpl<>(priceRepository.findByCompany_ComIdOrderByPartnerPriceIdDesc(current.getCompany().getComId())
                .stream()
                .filter(item -> partnerId == null || item.getPartner().getPartnerId().equals(partnerId))
                .filter(item -> itemId == null || item.getItem().getItemId().equals(itemId))
                .toList());
    }
}
