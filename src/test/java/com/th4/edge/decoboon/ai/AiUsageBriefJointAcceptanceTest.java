package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.brief.context.AiBossBriefContext;
import com.th4.edge.decoboon.ai.brief.generator.AiBriefGenerator;
import com.th4.edge.decoboon.ai.brief.model.AiBriefQuery;
import com.th4.edge.decoboon.ai.brief.model.AiBriefReport;
import com.th4.edge.decoboon.ai.configure.AiBillingStatus;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.ai.context.AiUsageContext;
import com.th4.edge.decoboon.ai.context.AiUsageReportContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.model.AiUsageQuery;
import com.th4.edge.decoboon.ai.model.AiUsageReport;
import com.th4.edge.decoboon.ai.repository.AiUsageCallRepository;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.context.BossDashboardContext;
import com.th4.edge.decoboon.statistic.context.BusinessAlertContext;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A0.1 影子用量与 A1 经营参谋联合独立验收。 */
class AiUsageBriefJointAcceptanceTest {

    private Employee boss;
    private Store store;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(41L);
        store = new Store();
        store.setStoreId(10L);
        store.setName("总店");
        store.setCompany(company);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setName("周总");
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        boss.setStore(store);
    }

    @Test
    void companyTotalMustEqualStoreAllocationPlusEnterprisePublicAndFailureCancelledMustBeZeroPoints() {
        AiUsageCallRepository repository = mock(AiUsageCallRepository.class);
        TenantContext tenant = mock(TenantContext.class);
        when(tenant.requireBoss(boss)).thenReturn(boss);
        AiUsageReportContext context = new AiUsageReportContext(tenant, repository);

        LocalDate today = LocalDate.now();
        List<AiUsageCall> calls = List.of(
                call(1L, store, AiUsageStatus.Succeeded, 100L, 10L, today),
                call(2L, null, AiUsageStatus.Succeeded, 20L, 2L, today),
                call(3L, store, AiUsageStatus.Failed, 50L, 50L, today),
                call(4L, store, AiUsageStatus.Cancelled, 30L, 30L, today));
        when(repository.findAll(any(Specification.class))).thenReturn(calls);

        AiUsageReport report = context.report(boss, new AiUsageQuery());

        assertThat(report.getResourceUnits()).isEqualTo(200L);
        assertThat(report.getShadowBillableUnits()).isEqualTo(12L);
        assertThat(report.getRefundedUnits()).isZero();
        assertThat(report.getNetBillableUnits()).isEqualTo(12L);
        assertThat(report.getStores().stream().mapToLong(item -> item.getResourceUnits()).sum()).isEqualTo(200L);
        assertThat(report.getStores().stream().mapToLong(item -> item.getShadowBillableUnits()).sum()).isEqualTo(12L);
        assertThat(report.getStores()).anySatisfy(item -> {
            if (item.getStoreId() == null) {
                assertThat(item.getStoreName()).isEqualTo("企业公共");
                assertThat(item.getAllocationType()).isEqualTo(AiStoreAllocation.EnterprisePublic);
            }
        });
    }

    @Test
    void nonBossMustBeRejectedByAllThreeAiProviders() {
        TenantContext tenant = mock(TenantContext.class);
        AiUsageCallRepository repository = mock(AiUsageCallRepository.class);
        Employee storeManager = new Employee();
        storeManager.setEmpId(2L);
        storeManager.setRole(EmployeeRole.StoreManager);
        storeManager.setCompany(boss.getCompany());
        when(tenant.requireBoss(storeManager))
                .thenThrow(new com.th4.edge.framework.exception.DefinedException(
                        new com.th4.edge.framework.exception.DefaultErrorCode("403", "仅企业负责人可访问")));

        AiUsageReportContext usageContext = new AiUsageReportContext(tenant, repository);
        assertThatThrownBy(() -> usageContext.report(storeManager, new AiUsageQuery()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class);
        assertThatThrownBy(() -> usageContext.page(storeManager, new AiUsageQuery()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class);

        AiBossBriefContext briefContext = new AiBossBriefContext(
                tenant, mock(BossDashboardContext.class), mock(FinanceReportContext.class),
                mock(BusinessAlertContext.class), mock(AiBriefGenerator.class), mock(AiUsageContext.class));
        assertThatThrownBy(() -> briefContext.report(storeManager, new AiBriefQuery()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class);
    }

    @Test
    void businessStorePriorityAndExplicitEnterprisePublicMustOverrideEmployeeStore() {
        TenantContext tenant = mock(TenantContext.class);
        AiUsageCallRepository repository = mock(AiUsageCallRepository.class);
        AiUsageContext context = new AiUsageContext(tenant, repository);
        Store businessStore = new Store();
        businessStore.setStoreId(20L);
        businessStore.setName("滨湖店");
        businessStore.setCompany(boss.getCompany());
        when(tenant.requireEmployee(any())).thenReturn(boss);
        when(repository.findByCompany_ComIdAndIdempotencyKey(any(), any())).thenReturn(java.util.Optional.empty());
        when(repository.save(any(AiUsageCall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiUsageCall business = usage("business-first");
        business.setBusinessStore(businessStore);
        AiUsageCall savedBusiness = context.record(boss, business);
        assertThat(savedBusiness.getAllocationStore()).isSameAs(businessStore);
        assertThat(savedBusiness.getAllocationType()).isEqualTo(AiStoreAllocation.BusinessStore);

        AiUsageCall enterprise = usage("enterprise-public");
        enterprise.setAllocationType(AiStoreAllocation.EnterprisePublic);
        AiUsageCall savedEnterprise = context.record(boss, enterprise);
        assertThat(savedEnterprise.getAllocationStore()).isNull();
        assertThat(savedEnterprise.getAllocationType()).isEqualTo(AiStoreAllocation.EnterprisePublic);
    }

    @Test
    void repeatedBossBriefActionMustUseStableIdempotencyKey() {
        CapturedBrief fixture = briefFixture(null);
        fixture.context.report(boss, new AiBriefQuery());
        fixture.context.report(boss, new AiBriefQuery());

        ArgumentCaptor<AiUsageCall> captor = ArgumentCaptor.forClass(AiUsageCall.class);
        verify(fixture.usageContext, org.mockito.Mockito.times(2)).record(any(), captor.capture());
        String firstKey = captor.getAllValues().get(0).getIdempotencyKey();
        assertThat(captor.getAllValues()).extracting(AiUsageCall::getIdempotencyKey)
                .as("重复点击/网络重试必须使用同一简报动作幂等键")
                .containsOnly(firstKey);
    }

    @Test
    void partialDataSourceFailureMustStillReturnAvailableFacts() {
        CapturedBrief fixture = briefFixture(null);
        when(fixture.dashboardContext.dashboard(any(), any()))
                .thenThrow(new IllegalStateException("dashboard unavailable"));
        when(fixture.financeContext.summary(any())).thenReturn(finance(true));
        when(fixture.alertContext.report(any(), any())).thenReturn(new BusinessAlertReport());

        AiBriefReport report = fixture.context.report(boss, new AiBriefQuery());

        assertThat(report.getFacts()).anySatisfy(item -> assertThat(item.getCategory()).isEqualTo("Cash"));
        assertThat(report.getSummary()).contains("暂不可确认");
    }

    @Test
    void costStatusMustNotTreatMissingFinanceAsComplete() {
        CapturedBrief fixture = briefFixture(null);
        when(fixture.dashboardContext.dashboard(any(), any())).thenReturn(dashboard(true));
        when(fixture.financeContext.summary(any())).thenReturn(null);
        when(fixture.alertContext.report(any(), any())).thenReturn(new BusinessAlertReport());

        AiBriefReport report = fixture.context.report(boss, new AiBriefQuery());

        assertThat(report.getCostStatus().getComplete()).isFalse();
        assertThat(report.getCostStatus().getMessage()).contains("暂不可确认");
    }

    @Test
    void actionSuggestionsMustTakeAtMostOnePerRiskCategory() {
        CapturedBrief fixture = briefFixture(null);
        when(fixture.dashboardContext.dashboard(any(), any())).thenReturn(dashboard(true));
        when(fixture.financeContext.summary(any())).thenReturn(finance(true));
        BusinessAlertReport alerts = new BusinessAlertReport();
        alerts.setOverdueReceivables(List.of(
                alert(BusinessAlertType.OverdueReceivable, BusinessAlertLevel.High, "应收逾期一"),
                alert(BusinessAlertType.OverdueReceivable, BusinessAlertLevel.Medium, "应收逾期二")));
        alerts.setSuspensionRisks(List.of(
                alert(BusinessAlertType.ProjectSuspensionRisk, BusinessAlertLevel.High, "项目停工")));
        alerts.setMaterialOverIssues(List.of());
        alerts.setChangeAnomalies(List.of());
        alerts.setTotalCount(3);
        alerts.setHighCount(2);
        alerts.setMediumCount(1);
        alerts.setLowCount(0);
        when(fixture.alertContext.report(any(), any())).thenReturn(alerts);

        AiBriefReport report = fixture.context.report(boss, new AiBriefQuery());

        assertThat(report.getActions()).extracting(AiBriefReport.AiBriefAction::getBusinessType)
                .doesNotHaveDuplicates();
    }

    private CapturedBrief briefFixture(Store businessStore) {
        TenantContext tenant = mock(TenantContext.class);
        BossDashboardContext dashboardContext = mock(BossDashboardContext.class);
        FinanceReportContext financeContext = mock(FinanceReportContext.class);
        BusinessAlertContext alertContext = mock(BusinessAlertContext.class);
        AiBriefGenerator generator = mock(AiBriefGenerator.class);
        AiUsageContext usageContext = mock(AiUsageContext.class);
        when(tenant.requireBoss(boss)).thenReturn(boss);
        when(generator.generate(any())).thenReturn("本月经营概况：测试。");
        when(generator.version()).thenReturn("baseline-v1");
        when(usageContext.record(any(), any())).thenAnswer(invocation -> {
            AiUsageCall call = invocation.getArgument(1);
            call.setCallId(1001L);
            return call;
        });
        AiBossBriefContext context = new AiBossBriefContext(
                tenant, dashboardContext, financeContext, alertContext, generator, usageContext);
        return new CapturedBrief(context, dashboardContext, financeContext, alertContext, usageContext);
    }

    private AiUsageCall usage(String key) {
        AiUsageCall call = new AiUsageCall();
        call.setIdempotencyKey(key);
        call.setBusinessModule("Analytics");
        call.setCapabilityCode("boss-brief");
        call.setStatus(AiUsageStatus.Succeeded);
        call.setStartedAt(LocalDateTime.now().minusSeconds(1));
        call.setCompletedAt(LocalDateTime.now());
        return call;
    }

    private AiUsageCall call(Long id, Store allocationStore, AiUsageStatus status,
                             Long resource, Long shadow, LocalDate date) {
        AiUsageCall call = new AiUsageCall();
        call.setCallId(id);
        call.setCompany(boss.getCompany());
        call.setAllocationStore(allocationStore);
        call.setAllocationType(allocationStore == null
                ? AiStoreAllocation.EnterprisePublic : AiStoreAllocation.BusinessStore);
        call.setCapabilityCode("boss-brief");
        call.setStatus(status);
        call.setBillingStatus(AiBillingStatus.Calculated);
        call.setResourceUnits(resource);
        call.setShadowBillableUnits(shadow);
        call.setRefundedUnits(0L);
        call.setStartedAt(LocalDateTime.of(date, java.time.LocalTime.NOON));
        return call;
    }

    private BossDashboard dashboard(boolean complete) {
        BossDashboard dashboard = new BossDashboard();
        DashboardMetric signed = new DashboardMetric();
        signed.setValue(BigDecimal.valueOf(120000));
        signed.setMonthOverMonthRate(BigDecimal.valueOf(10));
        signed.setComplete(complete);
        dashboard.setSignedAmount(signed);
        DashboardMetric active = new DashboardMetric();
        active.setValue(BigDecimal.valueOf(8));
        active.setComplete(true);
        dashboard.setActiveProjectCount(active);
        DashboardMetric profit = new DashboardMetric();
        profit.setValue(null);
        profit.setComplete(complete);
        dashboard.setGrossProfit(profit);
        return dashboard;
    }

    private FinanceSummary finance(boolean complete) {
        FinanceSummary finance = new FinanceSummary();
        finance.setOutstandingAmount(BigDecimal.valueOf(50000));
        finance.setOverdueAmount(BigDecimal.valueOf(12000));
        finance.setCollectionRate(BigDecimal.valueOf(68.5));
        finance.setCostComplete(complete);
        finance.setCostIncompleteProjectCount(complete ? 0 : 2);
        return finance;
    }

    private BusinessAlert alert(BusinessAlertType type, BusinessAlertLevel level, String title) {
        BusinessAlert alert = new BusinessAlert();
        alert.setType(type);
        alert.setLevel(level);
        alert.setTitle(title);
        alert.setMessage(title + "需要复核");
        alert.setBusinessType(type == BusinessAlertType.OverdueReceivable ? "Receivable" : "Project");
        alert.setBusinessId(9L);
        alert.setDueDate(LocalDate.now().plusDays(1));
        return alert;
    }

    private record CapturedBrief(AiBossBriefContext context,
                                 BossDashboardContext dashboardContext,
                                 FinanceReportContext financeContext,
                                 BusinessAlertContext alertContext,
                                 AiUsageContext usageContext) {
    }
}
