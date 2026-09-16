package com.th4.edge.decoboon.supply.service.partner.update.support;

import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.decoboon.finance.configure.SettlementMode;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 更新合作方。 */
@Note("更新合作方")
@Properties(allow = @AllowProperties(model = Partner.class,
        properties = {"partnerId", "type", "name", "contactName", "telNo", "taxNo", "addr",
                "settlementTerms", "settlementMode", "settlementDays", "cutoffDay", "bankName", "bankAccount", "status", "remark", "createTime"}))
@Service("partner/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.supply.service.partner.update.Update {
    @User private Employee employee;
    @AutoInject private Long partnerId;
    @AutoInject private String name;
    @AutoInject private String contactName;
    @AutoInject private String telNo;
    @AutoInject private String taxNo;
    @AutoInject private String addr;
    @AutoInject private String settlementTerms;
    @AutoInject private SettlementMode settlementMode;
    @AutoInject private Integer settlementDays;
    @AutoInject private Integer cutoffDay;
    @AutoInject private String bankName;
    @AutoInject private String bankAccount;
    @AutoInject private PartnerStatus status;
    @AutoInject private String remark;
    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;

    public Update(TenantContext tenantContext, PartnerRepository partnerRepository) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
    }

    @Override
    @Transactional
    public Partner call(Partner request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.ProjectSupport);
        Partner partner = partnerRepository.findById(partnerId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合作方不存在")));
        if (name != null && !name.isBlank()) partner.setName(name.trim());
        if (contactName != null) partner.setContactName(contactName);
        if (telNo != null) partner.setTelNo(telNo);
        if (taxNo != null) partner.setTaxNo(taxNo);
        if (addr != null) partner.setAddr(addr);
        if (settlementTerms != null) partner.setSettlementTerms(settlementTerms);
        if (settlementMode != null) partner.setSettlementMode(settlementMode);
        if (settlementDays != null) partner.setSettlementDays(settlementDays);
        if (cutoffDay != null) partner.setCutoffDay(cutoffDay);
        if (bankName != null) partner.setBankName(bankName);
        if (bankAccount != null) partner.setBankAccount(bankAccount);
        if (status != null) partner.setStatus(status);
        if (remark != null) partner.setRemark(remark);
        return partnerRepository.save(partner);
    }
}
