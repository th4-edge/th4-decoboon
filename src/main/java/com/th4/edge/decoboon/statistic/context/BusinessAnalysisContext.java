package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.common.context.BusinessOriginFilter;

import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.BusinessAnalysisQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAnalysisReport;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.statistic.model.StoreScopeInfo;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;

/**
 * 业务分析聚合：渠道 · 区域 · 客单 · 转化漏斗 + 报价接受率双指标。
 *
 * <p>报价接受率严格按 {@code MET-001}/{@code DEC-001} 冻结口径：</p>
 * <pre>
 * 最终报价接受率 = 最终为 Accepted/Converted 的机会数 ÷ 最终为 Accepted/Converted/Rejected 的机会数
 * 首版报价接受率 = 首版被接受的机会数 ÷ 首版已产生接受或拒绝结果的机会数
 * 机会 = 客户 + 房屋；草稿、待决策、替代报价不进分母；待决策报价量单独展示。
 * </pre>
 */
@Component
public class BusinessAnalysisContext {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MAX_TOP_N = 20;

    private final TenantContext tenantContext;
    private final QuoteRepository quoteRepository;
    private final StoreRepository storeRepository;
    private final StatisticStoreAggregateContext aggregateContext;

    public BusinessAnalysisContext(TenantContext tenantContext,
                                   QuoteRepository quoteRepository,
                                   StoreRepository storeRepository,
                                   StatisticStoreAggregateContext aggregateContext) {
        this.tenantContext = tenantContext;
        this.quoteRepository = quoteRepository;
        this.storeRepository = storeRepository;
        this.aggregateContext = aggregateContext;
    }

    public BusinessAnalysisReport report(Employee principal, BusinessAnalysisQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1)
                : query.getMonth().withDayOfMonth(1);
        Long storeId = StoreScope.normalize(query == null ? null : query.getStoreId());
        boolean allStores = StoreScope.isAllStores(storeId);
        int topN = topN(query);

        // P0B-02 聚合下推：门店条件（null/0/不传 = 全部门店）与漏斗、渠道、区域、客单等
        // 计数/求和/Top-N 全部在 SQL 侧完成（DEC-036 / BR-031）；报价机会比率仍由唯一口径
        // QuoteAcceptanceContext 计算（输入集合已由 SQL 收窄）。
        StatisticStoreAggregateContext.CustomerFunnel funnelCounts =
                aggregateContext.customerFunnel(comId, storeId, month);
        StatisticStoreAggregateContext.QuoteCounts quoteCounts =
                aggregateContext.monthQuoteCounts(comId, storeId, month);
        long signedContracts = aggregateContext.monthSignedContractCount(comId, storeId, month);
        List<Quote> opportunityQuotes = quoteRepository.findOpportunityQuotesByStoreScope(
                comId, com.th4.edge.decoboon.common.configure.BusinessOrigin.New,
                QuoteStatus.Draft, QuoteStatus.Superseded, allStores, storeId);
        List<BigDecimal> monthContractAmounts = aggregateContext.contractAmountsAsc(comId, storeId, month);

        BusinessAnalysisReport report = new BusinessAnalysisReport();
        report.setMonth(month);
        report.setStoreScope(storeScopeInfo(comId, storeId, allStores));
        report.setFunnel(List.of(
                new BusinessAnalysisReport.FunnelStage("lead", "线索", funnelCounts.leads(), "条"),
                new BusinessAnalysisReport.FunnelStage("visit", "到店", funnelCounts.visits(), "位"),
                new BusinessAnalysisReport.FunnelStage("measure", "量房", funnelCounts.measures(), "户"),
                new BusinessAnalysisReport.FunnelStage("quote", "报价", quoteCounts.total(), "份"),
                new BusinessAnalysisReport.FunnelStage("sign", "签约", (int) signedContracts, "单")));
        report.setOverallConversionRate(rate((int) signedContracts, funnelCounts.leads()));
        report.setQuoteConversion(quoteConversion(opportunityQuotes, quoteCounts));
        report.setChannels(channels(comId, storeId, month));
        report.setRegions(regions(comId, storeId, month, topN));
        report.setOrderSizes(orderSizes(comId, storeId, month));
        report.setMedianOrderAmount(median(monthContractAmounts));
        report.setMaxOrderAmount(monthContractAmounts.isEmpty()
                ? BigDecimal.ZERO : monthContractAmounts.get(monthContractAmounts.size() - 1));
        return report;
    }

    // ------------------------------------------------------------------ 漏斗（SQL 标量）

    // ------------------------------------------------------------------ 报价双指标

    /** 报价双指标：机会口径（客户 + 房屋）由唯一实现计算，本月报价量由 SQL 标量提供。 */
    private BusinessAnalysisReport.QuoteConversion quoteConversion(
            List<Quote> opportunityQuotes,
            StatisticStoreAggregateContext.QuoteCounts quoteCounts) {
        QuoteAcceptanceContext.Result result =
                QuoteAcceptanceContext.summarize(QuoteAcceptanceContext.opportunities(opportunityQuotes), List.of());
        BusinessAnalysisReport.QuoteConversion conversion = new BusinessAnalysisReport.QuoteConversion();
        conversion.setSubmittedQuoteCount(quoteCounts.submitted());
        conversion.setPendingDecisionQuoteCount(quoteCounts.pendingDecision());
        conversion.setDecidedOpportunityCount(result.decidedOpportunityCount());
        conversion.setFinalQuoteAcceptanceRate(result.finalAcceptanceRate());
        conversion.setFirstQuoteAcceptanceRate(result.firstAcceptanceRate());
        return conversion;
    }

    // ------------------------------------------------------------------ 渠道

    /** 渠道结构：线索/到店/签约/签约额由 SQL 分组聚合；比率与均值按服务端口径换算。 */
    private List<BusinessAnalysisReport.ChannelMetric> channels(Long comId, Long storeId, LocalDate month) {
        Map<CustomerSource, BusinessAnalysisReport.ChannelMetric> metrics = new LinkedHashMap<>();
        for (CustomerSource source : CustomerSource.values()) {
            BusinessAnalysisReport.ChannelMetric metric = new BusinessAnalysisReport.ChannelMetric();
            metric.setSource(source.name());
            metric.setLabel(source.getName());
            metrics.put(source, metric);
        }
        for (Object[] row : aggregateContext.customerChannelCounts(comId, storeId, month)) {
            BusinessAnalysisReport.ChannelMetric metric = metrics.get(sourceOf(row[0]));
            metric.setLeadCount(metric.getLeadCount() + number(row[1]));
            metric.setVisitCount(metric.getVisitCount() + number(row[2]));
        }
        Map<CustomerSource, BigDecimal> amountBySource = new LinkedHashMap<>();
        for (Object[] row : aggregateContext.contractChannelMetrics(comId, storeId, month)) {
            CustomerSource source = sourceOf(row[0]);
            BusinessAnalysisReport.ChannelMetric metric = metrics.get(source);
            metric.setSignedCount(metric.getSignedCount() + number(row[1]));
            amountBySource.merge(source, money(row[2]), BigDecimal::add);
        }
        metrics.values().forEach(metric -> {
            metric.setSignRate(rate(metric.getSignedCount(), metric.getLeadCount()));
            BigDecimal amount = amountBySource.getOrDefault(CustomerSource.valueOf(metric.getSource()), BigDecimal.ZERO);
            metric.setAvgOrderAmount(metric.getSignedCount() == 0
                    ? BigDecimal.ZERO
                    : amount.divide(BigDecimal.valueOf(metric.getSignedCount()), 2, RoundingMode.HALF_UP));
        });
        return metrics.values().stream()
                .sorted(Comparator.comparing(BusinessAnalysisReport.ChannelMetric::getLeadCount).reversed())
                .toList();
    }

    // ------------------------------------------------------------------ 区域与客单

    /** 区域结构：`GROUP BY` 小区 + `ORDER BY` 签约额倒序 + `LIMIT topN` 全部在 SQL 完成。 */
    private List<BusinessAnalysisReport.RegionMetric> regions(Long comId, Long storeId, LocalDate month, int topN) {
        List<BusinessAnalysisReport.RegionMetric> metrics = new ArrayList<>();
        for (Object[] row : aggregateContext.regionMetrics(comId, storeId, month, topN)) {
            metrics.add(new BusinessAnalysisReport.RegionMetric(
                    row[0] == null ? "未标注区域" : String.valueOf(row[0]), number(row[1]), money(row[2])));
        }
        return metrics;
    }

    /** 客单分布：5 个桶的计数在 SQL 侧完成（阈值与整改前万元 HALF_UP 口径等价）。 */
    private List<BusinessAnalysisReport.OrderSizeBucket> orderSizes(Long comId, Long storeId, LocalDate month) {
        long[] buckets = aggregateContext.orderSizeBuckets(comId, storeId, month);
        return List.of(
                new BusinessAnalysisReport.OrderSizeBucket("lt100k", "10 万以下", (int) buckets[0]),
                new BusinessAnalysisReport.OrderSizeBucket("100kTo200k", "10-20 万", (int) buckets[1]),
                new BusinessAnalysisReport.OrderSizeBucket("200kTo300k", "20-30 万", (int) buckets[2]),
                new BusinessAnalysisReport.OrderSizeBucket("300kTo500k", "30-50 万", (int) buckets[3]),
                new BusinessAnalysisReport.OrderSizeBucket("gte500k", "50 万以上", (int) buckets[4]));
    }

    /** 中位数：金额由 SQL 单列投影并 `ORDER BY` 排序（分布统计，SQL 无法用 SUM/COUNT 表达）。 */
    private BigDecimal median(List<BigDecimal> sortedAmounts) {
        if (sortedAmounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int size = sortedAmounts.size();
        if (size % 2 == 1) {
            return sortedAmounts.get(size / 2);
        }
        return sortedAmounts.get(size / 2 - 1).add(sortedAmounts.get(size / 2))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ 工具

    /**
     * 门店维度范围标注（`DEC-067` BOSS-STORE-03）：全部门店口径的未归属条数由 SQL `COUNT` 提供；
     * 单店视图不含未归属 → `unassignedCount = 0`。
     */
    private StoreScopeInfo storeScopeInfo(Long comId, Long storeId, boolean allStores) {
        StoreScopeInfo info = new StoreScopeInfo();
        info.setScopeType(allStores ? "AllStores" : "Store");
        if (!allStores) {
            info.setStoreId(storeId);
            Store store = storeRepository.findByStoreId(storeId);
            if (store != null) {
                info.setStoreName(store.getName());
            }
            return info;
        }
        info.setUnassignedCount((int) aggregateContext.unassignedCustomers(comId));
        return info;
    }

    private CustomerSource sourceOf(Object raw) {
        return raw instanceof CustomerSource source ? source : CustomerSource.Other;
    }

    private int number(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private int topN(BusinessAnalysisQuery query) {
        if (query == null || query.getTopN() == null || query.getTopN() <= 0) {
            return DEFAULT_TOP_N;
        }
        return Math.min(query.getTopN(), MAX_TOP_N);
    }
}
