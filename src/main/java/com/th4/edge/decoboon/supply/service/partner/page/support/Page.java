package com.th4.edge.decoboon.supply.service.partner.page.support;

import com.th4.edge.decoboon.finance.repository.PayableRepository;
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

import java.math.BigDecimal;

/** 合作方分页。 */
@Note("合作方分页")
@Properties(allow = @AllowProperties(model = Partner.class,
        properties = {"partnerId", "type", "name", "contactName", "telNo", "taxNo", "addr",
                "settlementTerms", "settlementMode", "settlementDays", "cutoffDay", "bankName", "bankAccount", "status", "remark", "brands", "createTime",
                "payableAmount", "paidAmount", "outstandingAmount"}))
@Service("partner/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.supply.service.partner.page.Page {
    @User private Employee employee;
    @AutoInject private PartnerType type;
    @AutoInject private String brand;
    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;
    private final PayableRepository payableRepository;

    public Page(TenantContext tenantContext, PartnerRepository partnerRepository,
                PayableRepository payableRepository) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
        this.payableRepository = payableRepository;
    }

    @Override
    public org.springframework.data.domain.Page<Partner> call(Partner request) {
        Employee current = tenantContext.requireEmployee(employee);
        return new PageImpl<>(partnerRepository.findByCompany_ComIdOrderByPartnerIdDesc(current.getCompany().getComId())
                .stream()
                .filter(item -> type == null || item.getType() == type)
                .filter(item -> brand == null || brand.isBlank()
                        || item.getBrands().stream().anyMatch(value -> value.equalsIgnoreCase(brand.trim())))
                .map(this::applyPayableSummary)
                .toList());
    }

    private Partner applyPayableSummary(Partner partner) {
        var payables = payableRepository.findByCompany_ComIdAndPayeePartner_PartnerIdOrderByDueDateAsc(
                partner.getCompany().getComId(), partner.getPartnerId());
        BigDecimal payable = payables.stream().map(item -> value(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = payables.stream().map(item -> value(item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        partner.setPayableAmount(payable);
        partner.setPaidAmount(paid);
        partner.setOutstandingAmount(payable.subtract(paid));
        return partner;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
