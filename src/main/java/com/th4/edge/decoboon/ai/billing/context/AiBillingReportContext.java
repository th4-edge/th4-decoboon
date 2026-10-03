package com.th4.edge.decoboon.ai.billing.context;

import com.th4.edge.decoboon.ai.billing.configure.AiAccountStatus;
import com.th4.edge.decoboon.ai.billing.model.AiAccountReport;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceAccountRepository;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceLedgerRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AI 点数账户与账本只读摘要。仅 BOSS 可读。 */
@Component
public class AiBillingReportContext {

    private final TenantContext tenantContext;
    private final AiResourceAccountRepository accountRepository;
    private final AiResourceLedgerRepository ledgerRepository;
    private final boolean formalEnabled;

    public AiBillingReportContext(TenantContext tenantContext,
                                  AiResourceAccountRepository accountRepository,
                                  AiResourceLedgerRepository ledgerRepository,
                                  @Value("${ai.billing.formal-enabled:false}") boolean formalEnabled) {
        this.tenantContext = tenantContext;
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.formalEnabled = formalEnabled;
    }

    public AiAccountReport report(Employee principal) {
        Employee current = tenantContext.requireBoss(principal);
        Long companyId = current.getCompany().getComId();
        AiAccountReport report = new AiAccountReport();
        report.setFormalEnabled(formalEnabled);
        accountRepository.findByCompany_ComId(companyId).ifPresent(account -> {
            report.setTotalGranted(nvl(account.getTotalGranted()));
            report.setAvailableUnits(nvl(account.getAvailableUnits()));
            report.setReservedUnits(nvl(account.getReservedUnits()));
            report.setConsumedUnits(nvl(account.getConsumedUnits()));
            report.setRefundedUnits(nvl(account.getRefundedUnits()));
            report.setAdjustedUnits(nvl(account.getAdjustedUnits()));
            report.setBalanceVersion(nvl(account.getBalanceVersion()));
            report.setStatus(account.getStatus() == null ? AiAccountStatus.Active : account.getStatus());
        });
        report.setRecentLedgers(ledgerRepository.findTop20ByCompany_ComIdOrderByLedgerIdDesc(companyId));
        return report;
    }

    private long nvl(Long value) { return value == null ? 0L : value; }
}
