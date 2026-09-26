package com.th4.edge.decoboon.ai;

import com.th4.edge.decoboon.ai.brief.context.AiBossBriefContext;
import com.th4.edge.decoboon.ai.brief.generator.AiBriefGenerator;
import com.th4.edge.decoboon.ai.brief.model.AiBriefQuery;
import com.th4.edge.decoboon.ai.brief.model.AiBriefReport;
import com.th4.edge.decoboon.ai.context.AiUsageContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiBossBriefContextTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final BossDashboardContext dashboardContext = mock(BossDashboardContext.class);
    private final FinanceReportContext financeContext = mock(FinanceReportContext.class);
    private final BusinessAlertContext alertContext = mock(BusinessAlertContext.class);
    private final AiBriefGenerator generator = mock(AiBriefGenerator.class);
    private final AiUsageContext usageContext = mock(AiUsageContext.class);
    private final AiBossBriefContext context = new AiBossBriefContext(
            tenantContext, dashboardContext, financeContext, alertContext, generator, usageContext);

    private Employee boss;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(41L);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setName("周总");
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        when(tenantContext.requireBoss(boss)).thenReturn(boss);
        when(generator.generate(any())).thenReturn("本月经营概况：测试。");
        when(generator.version()).thenReturn("baseline-test");
        when(usageContext.record(any(), any())).thenAnswer(invocation -> {
            AiUsageCall call = invocation.getArgument(1);
            call.setCallId(1001L);
            return call;
        });
    }

    @Test
    void shouldBuildFactsAndPrioritizeHighRiskAction() {
        BossDashboard dashboard = dashboard(BigDecimal.valueOf(120000), BigDecimal.valueOf(10), true);
        FinanceSummary finance = finance(false);
        BusinessAlertReport alerts = alerts();
        when(dashboardContext.dashboard(any(), any())).thenReturn(dashboard);
        when(financeContext.summary(any())).thenReturn(finance);
        when(alertContext.report(any(), any())).thenReturn(alerts);

        AiBriefReport report = context.report(boss, new AiBriefQuery());

        assertThat(report.getSummary()).contains("测试");
        assertThat(report.getFacts()).extracting(AiBriefReport.AiBriefFact::getCategory)
                .contains("Business", "Delivery", "Cash");
        assertThat(report.getActions()).hasSize(2);
        assertThat(report.getActions().get(0).getLevel()).isEqualTo("High");
        assertThat(report.getActions().get(0).getTargetRoute()).isEqualTo("/boss/finance/overview");
        assertThat(report.getCostStatus().getComplete()).isFalse();
        assertThat(report.getCostStatus().getMessage()).contains("成本未完整");
        assertThat(report.getUsage().getCallId()).isEqualTo(1001L);
        assertThat(report.getUsage().getShadowBillableUnits()).isPositive();

        ArgumentCaptor<AiUsageCall> captor = ArgumentCaptor.forClass(AiUsageCall.class);
        verify(usageContext).record(any(), captor.capture());
        assertThat(captor.getValue().getBusinessModule()).isEqualTo("Analytics");
        assertThat(captor.getValue().getCapabilityCode()).isEqualTo("boss-brief");
        assertThat(captor.getValue().getBusinessType()).isEqualTo("Company");
    }

    private BossDashboard dashboard(BigDecimal signed, BigDecimal monthOverMonth, boolean profitComplete) {
        BossDashboard dashboard = new BossDashboard();
        dashboard.setMonth(LocalDate.now().withDayOfMonth(1));
        dashboard.setSignedAmount(metric(signed, monthOverMonth, profitComplete));
        dashboard.setActiveProjectCount(metric(BigDecimal.valueOf(8), BigDecimal.ZERO, true));
        dashboard.setGrossProfit(metric(null, null, profitComplete));
        return dashboard;
    }

    private DashboardMetric metric(BigDecimal value, BigDecimal monthOverMonth, boolean complete) {
        DashboardMetric metric = new DashboardMetric();
        metric.setValue(value);
        metric.setMonthOverMonthRate(monthOverMonth);
        metric.setComplete(complete);
        return metric;
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

    private BusinessAlertReport alerts() {
        BusinessAlert high = alert(BusinessAlertType.OverdueReceivable, BusinessAlertLevel.High, "逾期应收");
        BusinessAlert medium = alert(BusinessAlertType.ProjectSuspensionRisk, BusinessAlertLevel.Medium, "交付风险");
        BusinessAlertReport report = new BusinessAlertReport();
        report.setTotalCount(2);
        report.setHighCount(1);
        report.setMediumCount(1);
        report.setLowCount(0);
        report.setOverdueReceivables(List.of(high));
        report.setSuspensionRisks(List.of(medium));
        report.setMaterialOverIssues(List.of());
        report.setChangeAnomalies(List.of());
        return report;
    }

    private BusinessAlert alert(BusinessAlertType type, BusinessAlertLevel level, String title) {
        BusinessAlert alert = new BusinessAlert();
        alert.setType(type);
        alert.setLevel(level);
        alert.setTitle(title);
        alert.setMessage(title + "需要复核");
        alert.setBusinessType(type == BusinessAlertType.OverdueReceivable ? "Receivable" : "Project");
        alert.setBusinessId(9L);
        alert.setOwner(boss);
        alert.setDueDate(LocalDate.now().plusDays(1));
        return alert;
    }
}
