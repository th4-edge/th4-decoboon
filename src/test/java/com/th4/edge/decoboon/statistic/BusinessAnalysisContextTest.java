package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.context.StatisticStoreAggregateContext;
import com.th4.edge.decoboon.statistic.context.BusinessAnalysisContext;
import com.th4.edge.decoboon.statistic.model.BusinessAnalysisQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAnalysisReport;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 业务分析：渠道 / 区域 / 客单 / 漏斗，以及 MET-001 + DEC-001 冻结的报价接受率双指标。
 */
class BusinessAnalysisContextTest {

    private static final long COM_ID = 31L;
    private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final com.th4.edge.tenant.user.repository.StoreRepository storeRepository =
            mock(com.th4.edge.tenant.user.repository.StoreRepository.class);
    private final StatisticStoreAggregateContext aggregate = mock(StatisticStoreAggregateContext.class);

    private final BusinessAnalysisContext context =
            new BusinessAnalysisContext(tenantContext, quoteRepository, storeRepository, aggregate);

    private Employee boss;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(COM_ID);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        when(tenantContext.requireBoss(any())).thenReturn(boss);
    }

    @Test
    void shouldAggregateChannelRegionOrderSizeAndFunnelFromSqlAggregates() {
        // P0B-02：漏斗、渠道、区域、客单分布、未归属条数全部来自 SQL 聚合（含门店条件），
        // 本用例只验证报表组装与换算（比率、均值、中位数、最大值）。
        when(aggregate.customerFunnel(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(new StatisticStoreAggregateContext.CustomerFunnel(2, 1, 0));
        when(aggregate.monthQuoteCounts(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(new StatisticStoreAggregateContext.QuoteCounts(1, 1, 0));
        when(aggregate.monthSignedContractCount(eq(COM_ID), anyLong(), eq(MONTH))).thenReturn(2L);
        when(aggregate.customerChannelCounts(eq(COM_ID), anyLong(), eq(MONTH))).thenReturn(
                java.util.Arrays.<Object[]>asList(
                        new Object[]{CustomerSource.Referral, 1L, 0L},
                        new Object[]{CustomerSource.WalkIn, 1L, 0L}));
        when(aggregate.contractChannelMetrics(eq(COM_ID), anyLong(), eq(MONTH))).thenReturn(
                java.util.Collections.singletonList(new Object[]{CustomerSource.Referral, 1L, new BigDecimal("260000")}));
        when(aggregate.regionMetrics(eq(COM_ID), anyLong(), eq(MONTH), eq(5))).thenReturn(
                java.util.Arrays.<Object[]>asList(
                        new Object[]{"滨湖新区", 1L, new BigDecimal("260000")},
                        new Object[]{"政务区", 1L, new BigDecimal("180000")}));
        when(aggregate.orderSizeBuckets(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(new long[]{0, 1, 1, 0, 0});
        when(aggregate.contractAmountsAsc(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(List.of(new BigDecimal("180000"), new BigDecimal("260000")));
        when(aggregate.unassignedCustomers(COM_ID)).thenReturn(0L);
        when(quoteRepository.findOpportunityQuotesByStoreScope(anyLong(), any(), any(), any(), anyBoolean(), anyLong()))
                .thenReturn(List.of());

        BusinessAnalysisReport report = context.report(boss, query());
        Map<String, BusinessAnalysisReport.FunnelStage> funnel = report.getFunnel().stream()
                .collect(Collectors.toMap(BusinessAnalysisReport.FunnelStage::getKey, Function.identity()));

        assertThat(funnel.get("lead").getCount()).isEqualTo(2);
        assertThat(funnel.get("visit").getCount()).isEqualTo(1);
        assertThat(funnel.get("quote").getCount()).isEqualTo(1);
        assertThat(funnel.get("sign").getCount()).isEqualTo(2);
        assertThat(report.getOverallConversionRate()).isEqualByComparingTo("100.0");
        assertThat(report.getStoreScope().getScopeType()).isEqualTo("AllStores");

        Map<String, BusinessAnalysisReport.ChannelMetric> channels = report.getChannels().stream()
                .collect(Collectors.toMap(BusinessAnalysisReport.ChannelMetric::getSource, Function.identity()));
        assertThat(channels.get("Referral").getLeadCount()).isEqualTo(1);
        assertThat(channels.get("Referral").getSignedCount()).isEqualTo(1);
        assertThat(channels.get("Referral").getSignRate()).isEqualByComparingTo("100.0");
        assertThat(channels.get("Referral").getAvgOrderAmount()).isEqualByComparingTo("260000.00");
        // 自然到店：本月有线索但无签约
        assertThat(channels.get("WalkIn").getLeadCount()).isEqualTo(1);
        assertThat(channels.get("WalkIn").getSignedCount()).isZero();
        // 草稿/替代等无数据渠道仍完整返回（Enum 全量投影）
        assertThat(channels).containsKeys("Video", "OldCustomer", "Other");

        assertThat(report.getRegions()).extracting(BusinessAnalysisReport.RegionMetric::getCommunity)
                .containsExactly("滨湖新区", "政务区");
        Map<String, Integer> buckets = report.getOrderSizes().stream()
                .collect(Collectors.toMap(BusinessAnalysisReport.OrderSizeBucket::getKey,
                        BusinessAnalysisReport.OrderSizeBucket::getCount));
        assertThat(buckets.get("200kTo300k")).isEqualTo(1);
        assertThat(buckets.get("100kTo200k")).isEqualTo(1);
        assertThat(buckets.get("lt100k")).isZero();
        assertThat(report.getMedianOrderAmount()).isEqualByComparingTo("220000");
        assertThat(report.getMaxOrderAmount()).isEqualByComparingTo("260000");
    }

    @Test
    void shouldFollowMet001DualMetricsForQuoteAcceptance() {
        Customer c1 = customer(1L, "机会一", CustomerSource.Referral, "滨湖新区", MONTH, false);
        Customer c2 = customer(2L, "机会二", CustomerSource.WalkIn, "政务区", MONTH, false);
        Property p1 = property(11L);
        Property p2 = property(12L);
        Customer c3 = customer(3L, "机会三", CustomerSource.Other, "蜀山区", MONTH, false);
        Property p3 = property(13L);

        mockQuotes(
                // 机会一：v1 被拒（首版未命中），v2 被接受 -> 最终接受
                quote(1L, c1, p1, 1, QuoteStatus.Rejected, MONTH.plusDays(2), MONTH.plusDays(6)),
                quote(2L, c1, p1, 2, QuoteStatus.Accepted, MONTH.plusDays(8), MONTH.plusDays(12)),
                // 机会二：v1 接受（首版命中）-> 最终接受
                quote(3L, c2, p2, 1, QuoteStatus.Accepted, MONTH.plusDays(3), MONTH.plusDays(7)),
                // 机会三：只有草稿与替代稿 -> 不构成有效机会
                quote(4L, c3, p3, 1, QuoteStatus.Draft, MONTH.plusDays(4), null),
                quote(5L, c3, p3, 2, QuoteStatus.Superseded, MONTH.plusDays(5), MONTH.plusDays(9)),
                // 待决策报价：计入待决策量，但不进接受率分母
                quote(6L, c1, p1, 3, QuoteStatus.Submitted, MONTH.plusDays(14), null));
        when(aggregate.monthQuoteCounts(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(new StatisticStoreAggregateContext.QuoteCounts(6, 4, 1));
        when(aggregate.customerFunnel(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(new StatisticStoreAggregateContext.CustomerFunnel(0, 0, 0));
        when(aggregate.monthSignedContractCount(eq(COM_ID), anyLong(), eq(MONTH))).thenReturn(0L);
        when(aggregate.customerChannelCounts(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(java.util.Collections.<Object[]>emptyList());
        when(aggregate.contractChannelMetrics(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(java.util.Collections.<Object[]>emptyList());
        when(aggregate.regionMetrics(eq(COM_ID), anyLong(), eq(MONTH), eq(5)))
                .thenReturn(java.util.Collections.<Object[]>emptyList());
        when(aggregate.orderSizeBuckets(eq(COM_ID), anyLong(), eq(MONTH))).thenReturn(new long[]{0, 0, 0, 0, 0});
        when(aggregate.contractAmountsAsc(eq(COM_ID), anyLong(), eq(MONTH))).thenReturn(List.of());
        when(aggregate.unassignedCustomers(COM_ID)).thenReturn(0L);

        BusinessAnalysisReport report = context.report(boss, query());
        BusinessAnalysisReport.QuoteConversion conversion = report.getQuoteConversion();

        // 分母 = 机会一 + 机会二 = 2（草稿/替代/待决策均排除）；分子 = 2，最终接受率 100%
        assertThat(conversion.getDecidedOpportunityCount()).isEqualTo(2);
        assertThat(conversion.getFinalQuoteAcceptanceRate()).isEqualByComparingTo("100.0");
        // 首版：机会一首版被拒（未命中）、机会二首版接受 -> 1/2 = 50%
        assertThat(conversion.getFirstQuoteAcceptanceRate()).isEqualByComparingTo("50.0");
        // 待决策报价量单独展示，不混入接受率
        assertThat(conversion.getPendingDecisionQuoteCount()).isEqualTo(1);
        assertThat(conversion.getSubmittedQuoteCount()).isEqualTo(4);
    }

    // ------------------------------------------------------------------ 构造

    private BusinessAnalysisQuery query() {
        BusinessAnalysisQuery query = new BusinessAnalysisQuery();
        query.setMonth(MONTH);
        return query;
    }

    private void mockQuotes(Quote... quotes) {
        when(quoteRepository.findOpportunityQuotesByStoreScope(eq(COM_ID),
                eq(com.th4.edge.decoboon.common.configure.BusinessOrigin.New),
                eq(com.th4.edge.decoboon.quote.configure.QuoteStatus.Draft),
                eq(com.th4.edge.decoboon.quote.configure.QuoteStatus.Superseded), anyBoolean(), anyLong()))
                .thenReturn(List.of(quotes));
    }

    private Customer customer(Long id, String name, CustomerSource source, String community,
                              LocalDate createdAt, boolean visited) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setName(name);
        customer.setSource(source);
        customer.setCommunity(community);
        customer.setStatus(CustStatus.Following);
        if (visited) {
            customer.setLastVisitAt(createdAt.plusDays(1).atStartOfDay());
        }
        customer.setMeasureAt(visited ? createdAt.plusDays(2).atStartOfDay() : null);
        setCreateTime(customer, createdAt.atStartOfDay());
        return customer;
    }

    private void setCreateTime(Customer customer, java.time.LocalDateTime value) {
        customer.setCreateTime(new com.th4.edge.framework.data.date.model.DateTime(value));
    }

    private Property property(Long id) {
        Property property = new Property();
        property.setPropertyId(id);
        return property;
    }

    private Quote quote(Long id, Customer customer, Integer version, QuoteStatus status,
                        LocalDate submittedAt, LocalDate decidedAt) {
        return quote(id, customer, property(id * 10), version, status, submittedAt, decidedAt);
    }

    private Quote quote(Long id, Customer customer, Property property, Integer version, QuoteStatus status,
                        LocalDate submittedAt, LocalDate decidedAt) {
        Quote quote = new Quote();
        quote.setQuoteId(id);
        quote.setNo("Q-" + id);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setVersion(version);
        quote.setStatus(status);
        quote.setAmount(new BigDecimal("100000"));
        if (status != QuoteStatus.Draft) {
            quote.setSubmittedAt(submittedAt.atStartOfDay());
        }
        if (decidedAt != null) {
            if (status == QuoteStatus.Rejected) {
                quote.setRejectedAt(decidedAt.atStartOfDay());
            } else if (status == QuoteStatus.Converted) {
                quote.setConvertedAt(decidedAt.atStartOfDay());
            } else if (status == QuoteStatus.Accepted) {
                quote.setAcceptedAt(decidedAt.atStartOfDay());
            }
        }
        return quote;
    }

}
