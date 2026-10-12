package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceAgingContext;
import com.th4.edge.decoboon.finance.model.AgingBucket;
import com.th4.edge.decoboon.finance.model.FinanceAgingQuery;
import com.th4.edge.decoboon.finance.model.FinanceAgingReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 应收/应付账龄：
 * 未结清余额口径、逾期分档边界、待确认账龄独立成档、已结清与已关闭不参与。
 */
class FinanceAgingContextTest {

    private static final long COM_ID = 11L;
    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 23);

    private final FinanceAccessContext accessContext = mock(FinanceAccessContext.class);
    private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
    private final PayableRepository payableRepository = mock(PayableRepository.class);

    private final FinanceAgingContext context =
            new FinanceAgingContext(accessContext, receivableRepository, payableRepository);

    @SuppressWarnings("unchecked")
    private static final org.springframework.data.jpa.domain.Specification<Object> SPEC =
            mock(org.springframework.data.jpa.domain.Specification.class);

    private Employee boss;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(COM_ID);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        when(accessContext.requireReader(any())).thenReturn(boss);
        // visible 返回一个非空 Specification，供 repository.findAll(spec) 匹配
        when(accessContext.visible(any(), any())).thenReturn(SPEC);
        // 真实口径：未结清余额 = 金额 - 已结清，且不小于 0
        when(accessContext.outstanding(any(), any())).thenAnswer(invocation -> {
            BigDecimal total = invocation.getArgument(0, BigDecimal.class);
            BigDecimal settled = invocation.getArgument(1, BigDecimal.class);
            BigDecimal result = (total == null ? BigDecimal.ZERO : total)
                    .subtract(settled == null ? BigDecimal.ZERO : settled);
            return result.signum() < 0 ? BigDecimal.ZERO : result;
        });
    }

    @Test
    void shouldBucketReceivablesByOverdueDaysAndKeepUnknownSeparate() {
        mockReceivables(
                // 未到期
                receivable("10000", "0", ReceivableStatus.Pending, AS_OF.plusDays(5)),
                // 逾期 0-30（边界：正好 30 天）
                receivable("20000", "5000", ReceivableStatus.Overdue, AS_OF.minusDays(30)),
                // 逾期 31-60
                receivable("30000", "0", ReceivableStatus.Partial, AS_OF.minusDays(45)),
                // 逾期 61-90
                receivable("40000", "0", ReceivableStatus.Partial, AS_OF.minusDays(90)),
                // 逾期 90 天以上
                receivable("50000", "0", ReceivableStatus.Overdue, AS_OF.minusDays(91)),
                // 无到期日 -> 待确认账龄
                receivable("60000", "0", ReceivableStatus.Pending, null),
                // 已收清 / 已关闭 -> 不参与
                receivable("70000", "70000", ReceivableStatus.Received, AS_OF.minusDays(120)),
                receivable("80000", "0", ReceivableStatus.Closed, AS_OF.minusDays(10)));
        mockPayables();

        FinanceAgingReport report = context.report(boss, query(), null);
        Map<String, AgingBucket> buckets = byKey(report.getReceivableBuckets());

        assertThat(buckets.get("notDue").getAmount()).isEqualByComparingTo("10000");
        assertThat(buckets.get("days0to30").getAmount()).isEqualByComparingTo("15000");
        assertThat(buckets.get("days31to60").getAmount()).isEqualByComparingTo("30000");
        assertThat(buckets.get("days61to90").getAmount()).isEqualByComparingTo("40000");
        assertThat(buckets.get("days90plus").getAmount()).isEqualByComparingTo("50000");
        assertThat(buckets.get("unknown").getAmount()).isEqualByComparingTo("60000");
        assertThat(buckets.get("unknown").getCount()).isEqualTo(1);

        // 合计 = 各档之和，且不含已收清/已关闭
        assertThat(report.getReceivableTotal()).isEqualByComparingTo("205000");
        // 逾期率 = (15000+30000+40000+50000) / 205000 = 65.9%（不含未到期与待确认账龄）
        assertThat(report.getReceivableOverdueRate()).isEqualByComparingTo("65.9");
        assertThat(report.getAsOf()).isEqualTo(AS_OF);
        // 分档色调：61-90 关注、90+ 严重、待确认 unknown
        assertThat(buckets.get("days61to90").getLevel()).isEqualTo("warn");
        assertThat(buckets.get("days90plus").getLevel()).isEqualTo("danger");
        assertThat(buckets.get("unknown").getLevel()).isEqualTo("unknown");
    }

    @Test
    void shouldBucketPayablesWithThreeOverdueTiers() {
        mockReceivables();
        mockPayables(
                payable("120000", "0", PayableStatus.Pending, AS_OF.plusDays(3)),
                payable("50000", "10000", PayableStatus.Partial, AS_OF.minusDays(10)),
                payable("60000", "0", PayableStatus.Overdue, AS_OF.minusDays(60)),
                payable("70000", "0", PayableStatus.Overdue, AS_OF.minusDays(61)),
                payable("80000", "0", PayableStatus.Pending, null),
                payable("90000", "90000", PayableStatus.Paid, AS_OF.minusDays(40)),
                payable("95000", "0", PayableStatus.Closed, AS_OF.minusDays(40)));

        FinanceAgingReport report = context.report(boss, query(), null);
        Map<String, AgingBucket> buckets = byKey(report.getPayableBuckets());

        assertThat(buckets).containsOnlyKeys("notDue", "days0to30", "days31to60", "days60plus", "unknown");
        assertThat(buckets.get("notDue").getAmount()).isEqualByComparingTo("120000");
        assertThat(buckets.get("days0to30").getAmount()).isEqualByComparingTo("40000");
        assertThat(buckets.get("days31to60").getAmount()).isEqualByComparingTo("60000");
        assertThat(buckets.get("days60plus").getAmount()).isEqualByComparingTo("70000");
        assertThat(buckets.get("unknown").getAmount()).isEqualByComparingTo("80000");
        assertThat(report.getPayableTotal()).isEqualByComparingTo("370000");
        // 逾期率 = (40000+60000+70000) / 370000 = 45.9%
        assertThat(report.getPayableOverdueRate()).isEqualByComparingTo("45.9");
    }

    // ------------------------------------------------------------------ 构造

    private FinanceAgingQuery query() {
        FinanceAgingQuery query = new FinanceAgingQuery();
        query.setAsOf(AS_OF);
        return query;
    }

    private Map<String, AgingBucket> byKey(List<AgingBucket> buckets) {
        return buckets.stream().collect(Collectors.toMap(AgingBucket::getKey, Function.identity()));
    }

    private void mockReceivables(Receivable... receivables) {
        when(receivableRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(receivables));
    }

    private void mockPayables(Payable... payables) {
        when(payableRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(payables));
    }

    private Receivable receivable(String amount, String received, ReceivableStatus status, LocalDate dueDate) {
        Receivable receivable = new Receivable();
        receivable.setAmount(new BigDecimal(amount));
        receivable.setReceivedAmount(new BigDecimal(received));
        receivable.setStatus(status);
        receivable.setDueDate(dueDate);
        return receivable;
    }

    private Payable payable(String amount, String paid, PayableStatus status, LocalDate dueDate) {
        Payable payable = new Payable();
        payable.setAmount(new BigDecimal(amount));
        payable.setPaidAmount(new BigDecimal(paid));
        payable.setStatus(status);
        payable.setDueDate(dueDate);
        return payable;
    }
}
