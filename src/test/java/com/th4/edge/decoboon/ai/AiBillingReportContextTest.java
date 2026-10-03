package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.billing.context.AiBillingReportContext;
import com.th4.edge.decoboon.ai.billing.model.AiAccountReport;
import com.th4.edge.decoboon.ai.billing.model.AiResourceAccount;
import com.th4.edge.decoboon.ai.billing.model.AiResourceLedger;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceAccountRepository;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceLedgerRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiBillingReportContextTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final AiResourceAccountRepository accountRepository = mock(AiResourceAccountRepository.class);
    private final AiResourceLedgerRepository ledgerRepository = mock(AiResourceLedgerRepository.class);
    private final AiBillingReportContext context = new AiBillingReportContext(
            tenantContext, accountRepository, ledgerRepository, false);

    private Company company;
    private Employee boss;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setComId(41L);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
    }

    @Test
    void bossShouldReadAccountSummary() {
        when(tenantContext.requireBoss(boss)).thenReturn(boss);
        AiResourceAccount account = new AiResourceAccount();
        account.setCompany(company);
        account.setTotalGranted(100L);
        account.setAvailableUnits(80L);
        account.setReservedUnits(5L);
        account.setConsumedUnits(20L);
        account.setRefundedUnits(5L);
        account.setBalanceVersion(6L);
        AiResourceLedger ledger = new AiResourceLedger();
        ledger.setLedgerId(9L);
        when(accountRepository.findByCompany_ComId(41L)).thenReturn(Optional.of(account));
        when(ledgerRepository.findTop20ByCompany_ComIdOrderByLedgerIdDesc(41L)).thenReturn(List.of(ledger));

        AiAccountReport report = context.report(boss);

        assertThat(report.getTotalGranted()).isEqualTo(100L);
        assertThat(report.getAvailableUnits()).isEqualTo(80L);
        assertThat(report.getReservedUnits()).isEqualTo(5L);
        assertThat(report.getConsumedUnits()).isEqualTo(20L);
        assertThat(report.getFormalEnabled()).isFalse();
        assertThat(report.getRecentLedgers()).hasSize(1);
    }

    @Test
    void nonBossShouldBeRejectedByTenantContext() {
        Employee finance = new Employee();
        finance.setRole(EmployeeRole.Finance);
        when(tenantContext.requireBoss(finance)).thenThrow(new DefinedException(
                new com.th4.edge.framework.exception.DefaultErrorCode("403", "无权")));

        assertThatThrownBy(() -> context.report(finance)).isInstanceOf(DefinedException.class);
    }
}
