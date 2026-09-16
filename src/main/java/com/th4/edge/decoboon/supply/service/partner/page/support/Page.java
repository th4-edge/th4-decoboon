package com.th4.edge.decoboon.supply.service.partner.page.support;

import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 合作方分页。 */
@Note("合作方分页")
@Properties(allow = @AllowProperties(model = Partner.class,
        properties = {"partnerId", "type", "name", "contactName", "telNo", "taxNo", "addr",
                "settlementTerms", "bankName", "bankAccount", "status", "remark", "createTime"}))
@Service("partner/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.supply.service.partner.page.Page {
    @User private Employee employee;
    @AutoInject private PartnerType type;
    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;

    public Page(TenantContext tenantContext, PartnerRepository partnerRepository) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
    }

    @Override
    public org.springframework.data.domain.Page<Partner> call(Partner request) {
        Employee current = tenantContext.requireEmployee(employee);
        return new PageImpl<>(partnerRepository.findByCompany_ComIdOrderByPartnerIdDesc(current.getCompany().getComId())
                .stream().filter(item -> type == null || item.getType() == type).toList());
    }
}
