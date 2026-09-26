package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.billing.configure.AiLedgerType;
import com.th4.edge.decoboon.ai.billing.configure.AiRateCardStatus;
import com.th4.edge.decoboon.ai.billing.context.AiBillingContext;
import com.th4.edge.decoboon.ai.billing.model.AiRateCardVersion;
import com.th4.edge.decoboon.ai.billing.model.AiResourceAccount;
import com.th4.edge.decoboon.ai.billing.model.AiResourceLedger;
import com.th4.edge.decoboon.ai.billing.repository.AiRateCardVersionRepository;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceAccountRepository;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceLedgerRepository;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiBillingContextTest {

    private static final long COM_ID = 41L;

    private final AiResourceAccountRepository accountRepository = mock(AiResourceAccountRepository.class);
    private final AiResourceLedgerRepository ledgerRepository = mock(AiResourceLedgerRepository.class);
    private final AiRateCardVersionRepository rateCardRepository = mock(AiRateCardVersionRepository.class);
    private final List<AiResourceLedger> ledgers = new ArrayList<>();

    private Company company;
    private Employee boss;
    private AiUsageCall call;
    private AiResourceAccount account;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setComId(COM_ID);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Store store = new Store();
        store.setStoreId(10L);
        store.setName("总店");
        store.setCompany(company);
        call = new AiUsageCall();
        call.setCallId(1001L);
        call.setCompany(company);
        call.setAllocationStore(store);
        call.setAllocationType(AiStoreAllocation.BusinessStore);
        call.setRateCardVersion("test-rate-v1");

        account = new AiResourceAccount();
        account.setAccountId(1L);
        account.setCompany(company);
        when(accountRepository.lockByCompanyId(COM_ID)).thenAnswer(invocation -> Optional.of(account));
        when(accountRepository.save(any(AiResourceAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any(AiResourceLedger.class))).thenAnswer(invocation -> {
            AiResourceLedger ledger = invocation.getArgument(0);
            ledger.setLedgerId((long) ledgers.size() + 1);
            ledgers.add(ledger);
            return ledger;
        });
        when(ledgerRepository.findByCompany_ComIdAndCall_CallIdAndType(anyLong(), anyLong(), any(AiLedgerType.class)))
                .thenAnswer(invocation -> {
                    Long callId = invocation.getArgument(1);
                    AiLedgerType type = invocation.getArgument(2);
                    return ledgers.stream().filter(item -> item.getCall() != null
                            && callId.equals(item.getCall().getCallId()) && item.getType() == type).toList();
                });
    }

    @Test
    void reserveSettleAndRefundShouldKeepAuditableBalance() {
        AiBillingContext context = new AiBillingContext(accountRepository, ledgerRepository, rateCardRepository, true);
        context.grant(boss, company, 100L, "测试发放");
        context.reserve(boss, call, 40L);
        context.settle(boss, call, 30L);
        context.refund(boss, call, 10L, "质量返还");

        assertThat(account.getTotalGranted()).isEqualTo(100L);
        assertThat(account.getAvailableUnits()).isEqualTo(80L);
        assertThat(account.getReservedUnits()).isZero();
        assertThat(account.getConsumedUnits()).isEqualTo(30L);
        assertThat(account.getRefundedUnits()).isEqualTo(10L);
        assertThat(ledgers).extracting(AiResourceLedger::getType)
                .containsExactly(AiLedgerType.Grant, AiLedgerType.Reserve, AiLedgerType.Release,
                        AiLedgerType.Consume, AiLedgerType.Refund);
        assertThat(ledgers.get(1).getBalanceBefore()).isEqualTo(100L);
        assertThat(ledgers.get(1).getBalanceAfter()).isEqualTo(60L);
        assertThat(ledgers.get(3).getBalanceBefore()).isEqualTo(100L);
        assertThat(ledgers.get(3).getBalanceAfter()).isEqualTo(70L);
    }

    @Test
    void releaseShouldReturnReservedUnitsWithoutConsume() {
        AiBillingContext context = new AiBillingContext(accountRepository, ledgerRepository, rateCardRepository, true);
        context.grant(boss, company, 50L, "测试发放");
        context.reserve(boss, call, 20L);
        context.release(boss, call, "模型失败");

        assertThat(account.getAvailableUnits()).isEqualTo(50L);
        assertThat(account.getReservedUnits()).isZero();
        assertThat(account.getConsumedUnits()).isZero();
    }

    @Test
    void formalFlagShouldBlockReserveByDefault() {
        AiBillingContext context = new AiBillingContext(accountRepository, ledgerRepository, rateCardRepository, false);
        assertThatThrownBy(() -> context.reserve(boss, call, 1L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("正式AI点数扣减未启用");
    }

    @Test
    void rateCardShouldCalculateCeilingPoints() {
        AiRateCardVersion rate = new AiRateCardVersion();
        rate.setCapabilityCode("boss-brief");
        rate.setUnitsPerPoint(BigDecimal.valueOf(1000));
        rate.setModelMultiplier(BigDecimal.valueOf(2));
        rate.setQualityMultiplier(BigDecimal.ONE);
        rate.setStatus(AiRateCardStatus.Active);
        when(rateCardRepository.findFirstByCapabilityCodeAndStatusOrderByVersionNoDesc(anyString(), any()))
                .thenReturn(Optional.of(rate));

        AiBillingContext context = new AiBillingContext(accountRepository, ledgerRepository, rateCardRepository, true);
        assertThat(context.calculate("boss-brief", 1200, BigDecimal.ONE)).isEqualTo(3L);
        assertThat(context.calculate("boss-brief", 0, BigDecimal.ONE)).isZero();
    }
}
