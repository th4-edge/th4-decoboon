package com.th4.edge.decoboon.supply.service.partner.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 新增合作方。 */
@Note("新增合作方")
@Properties(allow = @AllowProperties(model = Partner.class,
        properties = {"partnerId", "type", "name", "contactName", "telNo", "taxNo", "addr",
                "settlementTerms", "bankName", "bankAccount", "status", "remark", "createTime"}))
@Service("partner/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.supply.service.partner.save.Save {

    @User private Employee employee;
    @AutoInject private PartnerType type;
    @AutoInject @NotEmpty @Length(max = 128) private String name;
    @AutoInject @Length(max = 32) private String contactName;
    @AutoInject @Length(max = 20) private String telNo;
    @AutoInject @Length(max = 64) private String taxNo;
    @AutoInject @Length(max = 256) private String addr;
    @AutoInject @Length(max = 64) private String settlementTerms;
    @AutoInject @Length(max = 64) private String bankName;
    @AutoInject @Length(max = 64) private String bankAccount;
    @AutoInject @Length(max = 500) private String remark;

    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;
    private final ModelIdentityContext modelIdentityContext;

    public Save(TenantContext tenantContext, PartnerRepository partnerRepository,
                ModelIdentityContext modelIdentityContext) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public Partner call(Partner request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss,
                EmployeeRole.ProjectSupport);
        request.setCompany(current.getCompany());
        request.setType(type == null ? PartnerType.Supplier : type);
        request.setName(name.trim());
        request.setContactName(contactName);
        request.setTelNo(telNo);
        request.setTaxNo(taxNo);
        request.setAddr(addr);
        request.setSettlementTerms(settlementTerms);
        request.setBankName(bankName);
        request.setBankAccount(bankAccount);
        request.setStatus(PartnerStatus.Active);
        request.setRemark(remark);
        request.setCreatedBy(current);
        return partnerRepository.save(modelIdentityContext.prepare(request));
    }
}
