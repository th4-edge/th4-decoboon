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
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;

    public BusinessAnalysisContext(TenantContext tenantContext,
                                   CustomerRepository customerRepository,
                                   QuoteRepository quoteRepository,
                                   ProjectContractRepository contractRepository) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
    }

    public BusinessAnalysisReport report(Employee principal, BusinessAnalysisQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1)
                : query.getMonth().withDayOfMonth(1);
        Long storeId = query == null ? null : query.getStoreId();
        int topN = topN(query);

        List<Customer> customers = customerRepository.findByCompany_ComId(comId).stream()
                .filter(item -> matchesStore(item.getStore() == null ? null : item.getStore().getStoreId(), storeId))
                .toList();
        List<Quote> quotes = quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(comId).stream()
                .filter(item -> BusinessOriginFilter.isNormal(item.getBusinessOrigin()))
                .filter(item -> matchesStore(quoteStoreId(item), storeId))
                .toList();
        List<ProjectContract> contracts = contractRepository.findByCompany_ComIdOrderByContractIdDesc(comId).stream()
                .filter(item -> BusinessOriginFilter.isNormal(item.getBusinessOrigin()))
                .filter(item -> matchesStore(contractStoreId(item), storeId))
                .toList();

        List<ProjectContract> monthContracts = contracts.stream()
                .filter(item -> inMonth(item.getSignedAt(), month))
                .toList();
        List<Customer> monthCustomers = customers.stream()
                .filter(item -> inMonth(customerCreatedAt(item), month))
                .toList();
        List<Quote> monthQuotes = quotes.stream()
                .filter(item -> inMonth(item.getSubmittedAt(), month))
                .toList();

        BusinessAnalysisReport report = new BusinessAnalysisReport();
        report.setMonth(month);
        report.setFunnel(funnel(customers, monthCustomers, monthQuotes, monthContracts));
        report.setOverallConversionRate(rate(monthContracts.size(), monthCustomers.size()));
        report.setQuoteConversion(quoteConversion(quotes, monthQuotes));
        report.setChannels(channels(customers, monthCustomers, monthContracts));
        report.setRegions(regions(monthContracts, topN));
        report.setOrderSizes(orderSizes(monthContracts));
        report.setMedianOrderAmount(median(monthContracts));
        report.setMaxOrderAmount(monthContracts.stream().map(this::contractAmount)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
        return report;
    }

    // ------------------------------------------------------------------ 漏斗

    private List<BusinessAnalysisReport.FunnelStage> funnel(List<Customer> allCustomers,
                                                            List<Customer> monthCustomers,
                                                            List<Quote> monthQuotes,
                                                            List<ProjectContract> monthContracts) {
        long visits = monthCustomers.stream().filter(item -> item.getLastVisitAt() != null).count();
        long measures = monthCustomers.stream().filter(item -> item.getMeasureAt() != null).count();
        return List.of(
                new BusinessAnalysisReport.FunnelStage("lead", "线索", monthCustomers.size(), "条"),
                new BusinessAnalysisReport.FunnelStage("visit", "到店", (int) visits, "位"),
                new BusinessAnalysisReport.FunnelStage("measure", "量房", (int) measures, "户"),
                new BusinessAnalysisReport.FunnelStage("quote", "报价", monthQuotes.size(), "份"),
                new BusinessAnalysisReport.FunnelStage("sign", "签约", monthContracts.size(), "单"));
    }

    // ------------------------------------------------------------------ 报价双指标

    private BusinessAnalysisReport.QuoteConversion quoteConversion(List<Quote> allQuotes, List<Quote> monthQuotes) {
        // 统一口径：机会按客户 + 房屋聚合，草稿、待决策、已被替代不进正式分母（MET-001 / DEC-001 决策一）。
        QuoteAcceptanceContext.Result result = QuoteAcceptanceContext.summarize(
                QuoteAcceptanceContext.opportunities(allQuotes), monthQuotes);
        BusinessAnalysisReport.QuoteConversion conversion = new BusinessAnalysisReport.QuoteConversion();
        conversion.setSubmittedQuoteCount(result.submittedQuoteCount());
        conversion.setPendingDecisionQuoteCount(result.pendingDecisionQuoteCount());
        conversion.setDecidedOpportunityCount(result.decidedOpportunityCount());
        conversion.setFinalQuoteAcceptanceRate(result.finalAcceptanceRate());
        conversion.setFirstQuoteAcceptanceRate(result.firstAcceptanceRate());
        return conversion;
    }

    // ------------------------------------------------------------------ 渠道

    private List<BusinessAnalysisReport.ChannelMetric> channels(List<Customer> allCustomers,
                                                                List<Customer> monthCustomers,
                                                                List<ProjectContract> monthContracts) {
        Map<CustomerSource, BusinessAnalysisReport.ChannelMetric> metrics = new LinkedHashMap<>();
        for (CustomerSource source : CustomerSource.values()) {
            BusinessAnalysisReport.ChannelMetric metric = new BusinessAnalysisReport.ChannelMetric();
            metric.setSource(source.name());
            metric.setLabel(source.getName());
            metrics.put(source, metric);
        }
        for (Customer customer : monthCustomers) {
            CustomerSource source = customer.getSource() == null ? CustomerSource.Other : customer.getSource();
            BusinessAnalysisReport.ChannelMetric metric = metrics.get(source);
            metric.setLeadCount(metric.getLeadCount() + 1);
            if (customer.getLastVisitAt() != null) {
                metric.setVisitCount(metric.getVisitCount() + 1);
            }
        }
        Map<CustomerSource, BigDecimal> amountBySource = new LinkedHashMap<>();
        for (ProjectContract contract : monthContracts) {
            Customer customer = contractCustomer(contract);
            CustomerSource source = customer == null || customer.getSource() == null
                    ? CustomerSource.Other : customer.getSource();
            BusinessAnalysisReport.ChannelMetric metric = metrics.get(source);
            metric.setSignedCount(metric.getSignedCount() + 1);
            amountBySource.merge(source, contractAmount(contract), BigDecimal::add);
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

    private List<BusinessAnalysisReport.RegionMetric> regions(List<ProjectContract> monthContracts, int topN) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (ProjectContract contract : monthContracts) {
            String community = communityOf(contract);
            counts.computeIfAbsent(community, key -> new int[1])[0]++;
            amounts.merge(community, contractAmount(contract), BigDecimal::add);
        }
        return amounts.entrySet().stream()
                .map(entry -> new BusinessAnalysisReport.RegionMetric(entry.getKey(),
                        counts.get(entry.getKey())[0], entry.getValue()))
                .sorted(Comparator.comparing(BusinessAnalysisReport.RegionMetric::getSignedAmount).reversed())
                .limit(topN)
                .toList();
    }

    private String communityOf(ProjectContract contract) {
        Customer customer = contractCustomer(contract);
        if (customer != null && customer.getCommunity() != null && !customer.getCommunity().isBlank()) {
            return customer.getCommunity();
        }
        if (contract.getProject() != null && contract.getProject().getProperty() != null
                && contract.getProject().getProperty().getCommunity() != null
                && !contract.getProject().getProperty().getCommunity().isBlank()) {
            return contract.getProject().getProperty().getCommunity();
        }
        return "未标注区域";
    }

    private List<BusinessAnalysisReport.OrderSizeBucket> orderSizes(List<ProjectContract> monthContracts) {
        int[] buckets = new int[5];
        for (ProjectContract contract : monthContracts) {
            buckets[indexOf(contractAmount(contract))]++;
        }
        return List.of(
                new BusinessAnalysisReport.OrderSizeBucket("lt100k", "10 万以下", buckets[0]),
                new BusinessAnalysisReport.OrderSizeBucket("100kTo200k", "10-20 万", buckets[1]),
                new BusinessAnalysisReport.OrderSizeBucket("200kTo300k", "20-30 万", buckets[2]),
                new BusinessAnalysisReport.OrderSizeBucket("300kTo500k", "30-50 万", buckets[3]),
                new BusinessAnalysisReport.OrderSizeBucket("gte500k", "50 万以上", buckets[4]));
    }

    private int indexOf(BigDecimal amount) {
        BigDecimal wan = amount.divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
        if (wan.compareTo(BigDecimal.TEN) < 0) return 0;
        if (wan.compareTo(BigDecimal.valueOf(20)) < 0) return 1;
        if (wan.compareTo(BigDecimal.valueOf(30)) < 0) return 2;
        if (wan.compareTo(BigDecimal.valueOf(50)) < 0) return 3;
        return 4;
    }

    private BigDecimal median(List<ProjectContract> contracts) {
        if (contracts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> amounts = contracts.stream().map(this::contractAmount).sorted().toList();
        int size = amounts.size();
        if (size % 2 == 1) {
            return amounts.get(size / 2);
        }
        return amounts.get(size / 2 - 1).add(amounts.get(size / 2))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ 工具

    private BigDecimal contractAmount(ProjectContract contract) {
        BigDecimal current = contract.getCurrentAmount();
        return current == null || current.signum() == 0
                ? value(contract.getBaselineAmount()) : current;
    }

    private Customer contractCustomer(ProjectContract contract) {
        if (contract.getProject() != null && contract.getProject().getCustomer() != null) {
            return contract.getProject().getCustomer();
        }
        return null;
    }

    private Long quoteStoreId(Quote quote) {
        return quote.getCustomer() != null && quote.getCustomer().getStore() != null
                ? quote.getCustomer().getStore().getStoreId() : null;
    }

    private Long contractStoreId(ProjectContract contract) {
        return contract.getProject() != null && contract.getProject().getStore() != null
                ? contract.getProject().getStore().getStoreId() : null;
    }

    private LocalDateTime customerCreatedAt(Customer customer) {
        return customer.getCreateTime() == null ? null : customer.getCreateTime().toLocalDateTime();
    }

    private boolean matchesStore(Long storeIdOfRecord, Long storeId) {
        return storeId == null || Objects.equals(storeIdOfRecord, storeId);
    }

    private boolean inMonth(LocalDateTime dateTime, LocalDate month) {
        return dateTime != null && YearMonth.from(dateTime).equals(YearMonth.from(month));
    }

    private boolean inMonth(LocalDate date, LocalDate month) {
        return date != null && YearMonth.from(date).equals(YearMonth.from(month));
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int topN(BusinessAnalysisQuery query) {
        if (query == null || query.getTopN() == null || query.getTopN() <= 0) {
            return DEFAULT_TOP_N;
        }
        return Math.min(query.getTopN(), MAX_TOP_N);
    }
}
