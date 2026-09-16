package com.th4.edge.decoboon.supply.service.statement.report.support;

import com.th4.edge.decoboon.finance.repository.PayableRepository;
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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 供应商对账与付款计划汇总。 */
@Note("供应商对账")
@Properties(allow = @AllowProperties(model = Partner.class,
        properties = {"partnerId", "type", "name", "contactName", "telNo", "settlementTerms",
                "payableAmount", "paidAmount", "outstandingAmount"}))
@Service("partner/statement/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.supply.service.statement.report.Report {
    @User private Employee employee;
    @AutoInject @NotNull private Long partnerId;
    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;
    private final PayableRepository payableRepository;

    public Report(TenantContext tenantContext, PartnerRepository partnerRepository,
                  PayableRepository payableRepository) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
        this.payableRepository = payableRepository;
    }

    @Override
    public Partner call(Partner request) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        Partner partner = partnerRepository.findById(partnerId)
                .filter(item -> item.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合作方不存在")));
        var payables = payableRepository.findByCompany_ComIdAndPayeePartner_PartnerIdOrderByDueDateAsc(comId, partnerId);
        BigDecimal payable = payables.stream().map(item -> value(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = payables.stream().map(item -> value(item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        partner.setPayableAmount(payable);
        partner.setPaidAmount(paid);
        partner.setOutstandingAmount(payable.subtract(paid));
        return partner;
    }

    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
