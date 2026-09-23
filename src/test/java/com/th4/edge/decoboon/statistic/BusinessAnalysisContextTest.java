package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 业务分析：渠道 / 区域 / 客单 / 漏斗，以及 MET-001 + DEC-001 冻结的报价接受率双指标。
 */
class BusinessAnalysisContextTest {

    private static final long COM_ID = 31L;
    private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);

    private final BusinessAnalysisContext context =
            new BusinessAnalysisContext(tenantContext, customerRepository, quoteRepository, contractRepository);

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
    void shouldAggregateChannelRegionOrderSizeAndFunnel() {
        Customer referral = customer(1L, "老客转介绍客户", CustomerSource.Referral, "滨湖新区", MONTH.plusDays(3), true);
        Customer walkIn = customer(2L, "自然到店客户", CustomerSource.WalkIn, "政务区", MONTH.plusDays(6), false);
        // 上个月线索，不进本月漏斗
        Customer lastMonth = customer(3L, "上月客户", CustomerSource.Video, "蜀山区", MONTH.minusMonths(1), false);
        mockCustomers(referral, walkIn, lastMonth);
        mockQuotes(quote(1L, referral, 1, QuoteStatus.Submitted, MONTH.plusDays(5), null));
        mockContracts(
                contract(referral, "260000", MONTH.plusDays(8)),
                contract(walkIn, "180000", MONTH.plusDays(9)));

        BusinessAnalysisReport report = context.report(boss, query());
        Map<String, BusinessAnalysisReport.FunnelStage> funnel = report.getFunnel().stream()
                .collect(Collectors.toMap(BusinessAnalysisReport.FunnelStage::getKey, Function.identity()));

        assertThat(funnel.get("lead").getCount()).isEqualTo(2);
        assertThat(funnel.get("visit").getCount()).isEqualTo(1);
        assertThat(funnel.get("quote").getCount()).isEqualTo(1);
        assertThat(funnel.get("sign").getCount()).isEqualTo(2);
        assertThat(report.getOverallConversionRate()).isEqualByComparingTo("100.0");

        Map<String, BusinessAnalysisReport.ChannelMetric> channels = report.getChannels().stream()
                .collect(Collectors.toMap(BusinessAnalysisReport.ChannelMetric::getSource, Function.identity()));
        assertThat(channels.get("Referral").getLeadCount()).isEqualTo(1);
        assertThat(channels.get("Referral").getSignedCount()).isEqualTo(1);
        assertThat(channels.get("Referral").getSignRate()).isEqualByComparingTo("100.0");
        assertThat(channels.get("Referral").getAvgOrderAmount()).isEqualByComparingTo("260000.00");
        // 自然到店：本月有线索但无签约（签约归属 referral/政务区客户）
        assertThat(channels.get("Video").getLeadCount()).isZero();

        // 区域按签约额倒序
        assertThat(report.getRegions()).extracting(BusinessAnalysisReport.RegionMetric::getCommunity)
                .containsExactly("滨湖新区", "政务区");
        // 客单分桶：26 万 → 20-30 万；18 万 → 10-20 万
        Map<String, Integer> buckets = report.getOrderSizes().stream()
                .collect(Collectors.toMap(BusinessAnalysisReport.OrderSizeBucket::getKey,
                        BusinessAnalysisReport.OrderSizeBucket::getCount));
        assertThat(buckets.get("200kTo300k")).isEqualTo(1);
        assertThat(buckets.get("100kTo200k")).isEqualTo(1);
        assertThat(buckets.get("lt100k")).isZero();
        // 中位数 = (18 + 26) / 2 = 22 万
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

        mockCustomers(c1, c2, c3);
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
        mockContracts();

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

    private void mockCustomers(Customer... customers) {
        when(customerRepository.findByCompany_ComId(COM_ID)).thenReturn(List.of(customers));
    }

    private void mockQuotes(Quote... quotes) {
        when(quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(COM_ID)).thenReturn(List.of(quotes));
    }

    private void mockContracts(ProjectContract... contracts) {
        when(contractRepository.findByCompany_ComIdOrderByContractIdDesc(COM_ID)).thenReturn(List.of(contracts));
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

    private ProjectContract contract(Customer customer, String amount, LocalDate signedAt) {
        Project project = new Project();
        project.setProjectId(customer.getCustId() * 100);
        project.setName("项目-" + customer.getName());
        project.setCustomer(customer);
        ProjectContract contract = new ProjectContract();
        contract.setProject(project);
        contract.setCurrentAmount(new BigDecimal(amount));
        contract.setBaselineAmount(new BigDecimal(amount));
        contract.setSignedAt(signedAt);
        return contract;
    }
}
