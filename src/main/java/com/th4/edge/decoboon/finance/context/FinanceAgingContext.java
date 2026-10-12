package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.AgingBucket;
import com.th4.edge.decoboon.finance.model.FinanceAgingQuery;
import com.th4.edge.decoboon.finance.model.FinanceAgingReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.th4.edge.decoboon.common.context.StoreScope;

/**
 * 应收/应付账龄计算。
 *
 * <p>口径：</p>
 * <ul>
 *   <li>金额取未结清余额（应收 = 金额 - 已收；应付 = 金额 - 已付），已收清/已付清/已关闭不参与；</li>
 *   <li>截止日之前到期 = 未结清即为逾期，按「截止日 - 到期日」归入 0-30/31-60/61-90/90+（应付为 0-30/31-60/60+）；</li>
 *   <li>{@code dueDate} 为空的历史数据单独归入「待确认账龄」，不并入正常档；</li>
 *   <li>应收与应付使用同一套算法，仅分档边界不同。</li>
 * </ul>
 */
@Component
public class FinanceAgingContext {

    private final FinanceAccessContext accessContext;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;

    public FinanceAgingContext(FinanceAccessContext accessContext,
                               ReceivableRepository receivableRepository,
                               PayableRepository payableRepository) {
        this.accessContext = accessContext;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
    }

    public FinanceAgingReport report(Employee principal, FinanceAgingQuery query, Long requestedStoreId) {
        Employee current = accessContext.requireReader(principal);
        LocalDate asOf = query == null || query.getAsOf() == null ? LocalDate.now() : query.getAsOf();

        Long storeFilter = accessContext.resolveStoreFilter(current, requestedStoreId);
        List<Receivable> receivables = receivableRepository.findAll(accessContext.visible(current, Receivable.class))
                .stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .toList();
        List<Payable> payables = payableRepository.findAll(accessContext.visible(current, Payable.class))
                .stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .toList();

        FinanceAgingReport report = new FinanceAgingReport();
        report.setAsOf(asOf);
        report.setReceivableBuckets(receivableBuckets(receivables, asOf));
        report.setPayableBuckets(payableBuckets(payables, asOf));
        report.setReceivableTotal(sum(report.getReceivableBuckets()));
        report.setPayableTotal(sum(report.getPayableBuckets()));
        report.setReceivableOverdueRate(overdueRate(report.getReceivableBuckets(), report.getReceivableTotal()));
        report.setPayableOverdueRate(overdueRate(report.getPayableBuckets(), report.getPayableTotal()));
        return report;
    }

    // ------------------------------------------------------------------ 应收

    private List<AgingBucket> receivableBuckets(List<Receivable> receivables, LocalDate asOf) {
        AgingBucket notDue = new AgingBucket("notDue", "未到期", "normal");
        AgingBucket days0to30 = new AgingBucket("days0to30", "逾期 0-30 天", "normal");
        AgingBucket days31to60 = new AgingBucket("days31to60", "逾期 31-60 天", "warn");
        AgingBucket days61to90 = new AgingBucket("days61to90", "逾期 61-90 天", "warn");
        AgingBucket days90plus = new AgingBucket("days90plus", "逾期 90 天以上", "danger");
        AgingBucket unknown = new AgingBucket("unknown", "待确认账龄", "unknown");

        for (Receivable receivable : receivables) {
            if (receivable.getStatus() == ReceivableStatus.Received
                    || receivable.getStatus() == ReceivableStatus.Closed) {
                continue;
            }
            BigDecimal outstanding = accessContext.outstanding(receivable.getAmount(), receivable.getReceivedAmount());
            if (outstanding.signum() <= 0) {
                continue;
            }
            if (receivable.getDueDate() == null) {
                add(unknown, outstanding);
                continue;
            }
            long days = ChronoUnit.DAYS.between(receivable.getDueDate(), asOf);
            if (days < 0) {
                add(notDue, outstanding);
            } else if (days <= 30) {
                add(days0to30, outstanding);
            } else if (days <= 60) {
                add(days31to60, outstanding);
            } else if (days <= 90) {
                add(days61to90, outstanding);
            } else {
                add(days90plus, outstanding);
            }
        }
        return List.of(notDue, days0to30, days31to60, days61to90, days90plus, unknown);
    }

    // ------------------------------------------------------------------ 应付

    private List<AgingBucket> payableBuckets(List<Payable> payables, LocalDate asOf) {
        AgingBucket notDue = new AgingBucket("notDue", "未到期", "normal");
        AgingBucket days0to30 = new AgingBucket("days0to30", "逾期 0-30 天", "normal");
        AgingBucket days31to60 = new AgingBucket("days31to60", "逾期 31-60 天", "warn");
        AgingBucket days60plus = new AgingBucket("days60plus", "逾期 60 天以上", "danger");
        AgingBucket unknown = new AgingBucket("unknown", "待确认账龄", "unknown");

        for (Payable payable : payables) {
            if (payable.getStatus() == PayableStatus.Paid || payable.getStatus() == PayableStatus.Closed) {
                continue;
            }
            BigDecimal outstanding = accessContext.outstanding(payable.getAmount(), payable.getPaidAmount());
            if (outstanding.signum() <= 0) {
                continue;
            }
            if (payable.getDueDate() == null) {
                add(unknown, outstanding);
                continue;
            }
            long days = ChronoUnit.DAYS.between(payable.getDueDate(), asOf);
            if (days < 0) {
                add(notDue, outstanding);
            } else if (days <= 30) {
                add(days0to30, outstanding);
            } else if (days <= 60) {
                add(days31to60, outstanding);
            } else {
                add(days60plus, outstanding);
            }
        }
        return List.of(notDue, days0to30, days31to60, days60plus, unknown);
    }

    private void add(AgingBucket bucket, BigDecimal amount) {
        bucket.setAmount(bucket.getAmount().add(amount));
        bucket.setCount(bucket.getCount() + 1);
    }

    private BigDecimal sum(List<AgingBucket> buckets) {
        return buckets.stream().map(AgingBucket::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 逾期率 = 逾期档（不含未到期与待确认账龄）÷ 未结清合计；分母为 0 时返回 0。 */
    private BigDecimal overdueRate(List<AgingBucket> buckets, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal overdue = buckets.stream()
                .filter(bucket -> !"notDue".equals(bucket.getKey()) && !"unknown".equals(bucket.getKey()))
                .map(AgingBucket::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return overdue.multiply(BigDecimal.valueOf(100))
                .divide(total, 1, java.math.RoundingMode.HALF_UP);
    }
}
