package com.th4.edge.decoboon.carryover;

import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceType;
import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationTheme;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverReconciliationContext;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationItem;
import com.th4.edge.decoboon.carryover.repository.CarryoverBatchRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverReconciliationItemRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverReconciliationRepository;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 首月双轨：四主题自动创建、装得清金额带出、差异计算。 */
class CarryoverReconciliationContextTest {

    private final CarryoverAccessContext accessContext = mock(CarryoverAccessContext.class);
    private final CarryoverBatchRepository batchRepository = mock(CarryoverBatchRepository.class);
    private final CarryoverReconciliationRepository reconciliationRepository = mock(CarryoverReconciliationRepository.class);
    private final CarryoverReconciliationItemRepository itemRepository = mock(CarryoverReconciliationItemRepository.class);
    private final FinanceReportContext financeReportContext = mock(FinanceReportContext.class);
    private final CarryoverReconciliationContext context = new CarryoverReconciliationContext(
            accessContext, batchRepository, reconciliationRepository, itemRepository, financeReportContext);

    @Test
    void shouldCreateFourThemesAndCalculateDifference() {
        Employee boss = boss();
        CarryoverBatch batch = new CarryoverBatch();
        batch.setBatchId(9L);
        batch.setCompany(boss.getCompany());
        batch.setAsOfDate(LocalDate.of(2026, 10, 1));
        when(accessContext.requireFinance(boss)).thenReturn(boss);
        when(batchRepository.findByBatchIdAndCompany_ComId(9L, 1L)).thenReturn(Optional.of(batch));
        when(reconciliationRepository.findFirstByCompany_ComIdAndBatch_BatchIdOrderByReconciliationIdDesc(1L, 9L))
                .thenReturn(Optional.empty());

        Map<CarryoverReconciliationTheme, CarryoverReconciliationItem> store =
                new EnumMap<>(CarryoverReconciliationTheme.class);
        AtomicLong itemIds = new AtomicLong(1);
        AtomicReference<CarryoverReconciliation> savedReconciliation = new AtomicReference<>();
        when(reconciliationRepository.save(any(CarryoverReconciliation.class))).thenAnswer(invocation -> {
            CarryoverReconciliation reconciliation = invocation.getArgument(0);
            if (reconciliation.getReconciliationId() == null) reconciliation.setReconciliationId(100L);
            savedReconciliation.set(reconciliation);
            return reconciliation;
        });
        when(reconciliationRepository.findByCompany_ComIdAndReconciliationId(anyLong(), anyLong()))
                .thenAnswer(invocation -> java.util.Optional.ofNullable(savedReconciliation.get()));
        when(itemRepository.findByCompany_ComIdAndReconciliation_ReconciliationIdAndTheme(anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(2))));
        when(itemRepository.save(any(CarryoverReconciliationItem.class))).thenAnswer(invocation -> {
            CarryoverReconciliationItem item = invocation.getArgument(0);
            if (item.getItemId() == null) item.setItemId(itemIds.getAndIncrement());
            store.put(item.getTheme(), item);
            return item;
        });
        when(itemRepository.findByCompany_ComIdAndReconciliation_ReconciliationIdOrderByThemeAsc(anyLong(), anyLong()))
                .thenAnswer(invocation -> new ArrayList<>(store.values()));
        when(itemRepository.findByCompany_ComIdAndItemId(anyLong(), anyLong())).thenAnswer(invocation -> {
            Long itemId = invocation.getArgument(1);
            return store.values().stream().filter(item -> itemId.equals(item.getItemId())).findFirst();
        });
        when(financeReportContext.summary(boss)).thenReturn(summary());

        var report = context.save(boss, 9L, LocalDate.of(2026, 10, 1), null, "首月双轨");
        assertThat(report.getItems()).hasSize(4);
        assertThat(report.getItems()).extracting(CarryoverReconciliationItem::getTheme)
                .containsExactlyInAnyOrder(CarryoverReconciliationTheme.values());
        assertThat(amount(report, CarryoverReconciliationTheme.Receivable)).isEqualByComparingTo("120000.00");
        assertThat(amount(report, CarryoverReconciliationTheme.Payable)).isEqualByComparingTo("40000.00");
        assertThat(amount(report, CarryoverReconciliationTheme.Cash)).isEqualByComparingTo("200000.00");
        assertThat(amount(report, CarryoverReconciliationTheme.Cost)).isEqualByComparingTo("150000.00");

        var receivable = report.getItems().stream()
                .filter(item -> item.getTheme() == CarryoverReconciliationTheme.Receivable)
                .findFirst().orElseThrow();
        var updated = context.saveItem(boss, receivable.getItemId(), new BigDecimal("130000.00"),
                "原账应收账款", true, CarryoverDifferenceType.MissingData,
                CarryoverDifferenceStatus.Handling, "客户已付款未入账", "补录收款", 2L, LocalDate.of(2026, 10, 31));
        var receivableAfter = updated.getItems().stream()
                .filter(item -> item.getTheme() == CarryoverReconciliationTheme.Receivable)
                .findFirst().orElseThrow();
        assertThat(receivableAfter.getDifference()).isEqualByComparingTo("10000.00");
        assertThat(receivableAfter.getDifferenceType()).isEqualTo(CarryoverDifferenceType.MissingData);
        assertThat(receivableAfter.getDifferenceStatus()).isEqualTo(CarryoverDifferenceStatus.Handling);
        assertThat(updated.getUnclassifiedCount()).isZero();
        assertThat(updated.getPendingCount()).isEqualTo(1);

        // 把四个主题都确认为与原账一致后，完成度通过，可提交/确认/关闭，并可导出 CSV。
        for (CarryoverReconciliationItem item : updated.getItems()) {
            updated = context.saveItem(boss, item.getItemId(), item.getSystemAmount(), "原账核对",
                    true, null, CarryoverDifferenceStatus.Accepted, null, null, null, null);
        }
        assertThat(updated.getComplete()).isTrue();
        assertThat(updated.getBlockers()).isEmpty();

        updated = context.updateStatus(boss, 100L,
                com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus.Pending, "提交待确认");
        assertThat(updated.getStatus())
                .isEqualTo(com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus.Pending);
        updated = context.updateStatus(boss, 100L,
                com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus.Confirmed, "确认");
        assertThat(updated.getStatus())
                .isEqualTo(com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus.Confirmed);
        updated = context.updateStatus(boss, 100L,
                com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus.Closed, "关闭");
        assertThat(updated.getStatus())
                .isEqualTo(com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus.Closed);

        var export = context.export(boss, 100L);
        assertThat(export.getContent()).contains("首月双轨对账表").contains("项目未收").contains("项目成本");
    }

    private BigDecimal amount(com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport report,
                              CarryoverReconciliationTheme theme) {
        return report.getItems().stream()
                .filter(item -> item.getTheme() == theme)
                .findFirst().orElseThrow()
                .getSystemAmount();
    }

    private FinanceSummary summary() {
        FinanceSummary summary = new FinanceSummary();
        summary.setOutstandingAmount(new BigDecimal("120000.00"));
        summary.setUnpaidAmount(new BigDecimal("40000.00"));
        summary.setCashBalance(new BigDecimal("200000.00"));
        summary.setActualCost(new BigDecimal("150000.00"));
        return summary;
    }

    private Employee boss() {
        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(1L);
        boss.setCompany(company);
        boss.setRole(EmployeeRole.Boss);
        return boss;
    }
}
