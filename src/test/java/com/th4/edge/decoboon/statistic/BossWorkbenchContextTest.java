package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.configure.BusinessEventCategory;
import com.th4.edge.decoboon.statistic.configure.BusinessEventLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessEventType;
import com.th4.edge.decoboon.statistic.configure.WorkbenchSignalStatus;
import com.th4.edge.decoboon.statistic.context.BossWorkbenchContext;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossWorkbench;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.statistic.model.BusinessEvent;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BossWorkbenchContextTest {

    @Test
    void shouldBuildFourIndicatorsAndGroupProjectRisks() {
        BossWorkbenchContext context = new BossWorkbenchContext();
        BossDashboard dashboard = new BossDashboard();
        dashboard.setMonth(LocalDate.of(2026, 9, 1));
        dashboard.setSignedAmount(metric("10000", "12000", "83.3", "8", true));
        dashboard.setReceivedAmount(metric("3000", "5000", "60", "-5", true));
        dashboard.setGrossProfit(metric(null, "6000", "0", "0", false));
        dashboard.setActiveProjectCount(metric("2", "3", "66.7", "0", true));

        Project project = new Project();
        project.setProjectId(1L);
        project.setNo("P-001");
        project.setName("测试项目");
        project.setStatus(ProjectStatus.Suspended);
        project.setProgress(40);
        Employee manager = new Employee();
        manager.setName("张经理");
        project.setManager(manager);

        BusinessAlert overdue = alert(project, BusinessAlertType.OverdueReceivable, BusinessAlertLevel.High,
                "首期款逾期 30 天，待收 30000 元", LocalDate.of(2026, 9, 1));
        BusinessAlert suspension = alert(project, BusinessAlertType.ProjectSuspensionRisk, BusinessAlertLevel.Medium,
                "项目已停工 5 天，请确认复工计划", LocalDate.of(2026, 9, 3));
        BusinessAlertReport alerts = new BusinessAlertReport();
        alerts.setOverdueReceivables(List.of(overdue));
        alerts.setSuspensionRisks(List.of(suspension));

        BusinessEvent event = new BusinessEvent();
        event.setEventId("event-1");
        event.setCategory(BusinessEventCategory.Finance);
        event.setType(BusinessEventType.ReceiptReceived);
        event.setLevel(BusinessEventLevel.Milestone);
        event.setOccurredAt(LocalDateTime.of(2026, 9, 20, 10, 0));

        BossWorkbench workbench = context.build(dashboard, alerts, List.of(event),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20));

        assertThat(workbench.getIndicators()).hasSize(4);
        assertThat(workbench.getIndicators().get(0).getStatus()).isEqualTo(WorkbenchSignalStatus.Normal);
        assertThat(workbench.getIndicators().get(1).getStatus()).isEqualTo(WorkbenchSignalStatus.Danger);
        assertThat(workbench.getIndicators().get(2).getStatus()).isEqualTo(WorkbenchSignalStatus.Unknown);
        assertThat(workbench.getIndicators().get(2).getValue()).isNull();
        assertThat(workbench.getIndicators().get(3).getStatus()).isEqualTo(WorkbenchSignalStatus.Danger);

        assertThat(workbench.getAttentionProjects()).hasSize(1);
        assertThat(workbench.getAttentionProjects().get(0).getRiskCount()).isEqualTo(2);
        assertThat(workbench.getAttentionProjects().get(0).getRiskLevel()).isEqualTo(BusinessAlertLevel.High);
        assertThat(workbench.getAttentionProjects().get(0).getNextAction()).contains("应收回款");
        assertThat(workbench.getBusinessEvents()).containsExactly(event);
    }

    @Test
    void shouldExplainNoTargetWithoutShowingZeroAchievement() {
        BossWorkbenchContext context = new BossWorkbenchContext();
        BossDashboard dashboard = new BossDashboard();
        dashboard.setSignedAmount(metric("0", "0", "0", "-25", true));
        dashboard.setReceivedAmount(metric("0", "0", "0", "0", true));
        dashboard.setGrossProfit(metric("0", "0", "0", "0", true));
        dashboard.setActiveProjectCount(metric("0", "0", "0", "0", true));

        BossWorkbench workbench = context.build(dashboard, null, List.of(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20));

        assertThat(workbench.getIndicators().get(0).getStatus()).isEqualTo(WorkbenchSignalStatus.Attention);
        assertThat(workbench.getIndicators().get(0).getDetail())
                .contains("未设置目标", "按环比判断", "-25%");
        assertThat(workbench.getIndicators().get(1).getDetail())
                .contains("未设置目标")
                .doesNotContain("目标达成 0%");
        assertThat(workbench.getIndicators().get(3).getDetail())
                .contains("数量目标不参与健康判定");
    }

    private DashboardMetric metric(String value, String target, String achievement,
                                   String monthOverMonth, boolean complete) {
        DashboardMetric metric = new DashboardMetric();
        metric.setValue(value == null ? null : new BigDecimal(value));
        metric.setTarget(new BigDecimal(target));
        metric.setAchievementRate(new BigDecimal(achievement));
        metric.setMonthOverMonthRate(new BigDecimal(monthOverMonth));
        metric.setComplete(complete);
        metric.setWarning(complete ? null : "1 个项目的成本未完整，暂不展示可靠毛利");
        return metric;
    }

    private BusinessAlert alert(Project project, BusinessAlertType type, BusinessAlertLevel level,
                                String message, LocalDate dueDate) {
        BusinessAlert alert = new BusinessAlert();
        alert.setProject(project);
        alert.setType(type);
        alert.setLevel(level);
        alert.setMessage(message);
        alert.setAmount(new BigDecimal("30000"));
        alert.setDueDate(dueDate);
        return alert;
    }
}
