package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.configure.WorkbenchSignalStatus;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossWorkbench;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.statistic.model.BusinessEvent;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import com.th4.edge.decoboon.statistic.model.WorkbenchIndicator;
import com.th4.edge.decoboon.statistic.model.WorkbenchProject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 企业负责人首页聚合计算。 */
@Component
public class BossWorkbenchContext {

    public BossWorkbench build(BossDashboard dashboard,
                               BusinessAlertReport alerts,
                               List<BusinessEvent> events,
                               LocalDate month,
                               LocalDate asOf) {
        BossWorkbench workbench = new BossWorkbench();
        workbench.setMonth(month);
        workbench.setAsOf(asOf);
        workbench.setIndicators(List.of(
                signedIndicator(dashboard == null ? null : dashboard.getSignedAmount()),
                receivedIndicator(dashboard == null ? null : dashboard.getReceivedAmount(), alerts),
                grossProfitIndicator(dashboard == null ? null : dashboard.getGrossProfit()),
                activeProjectIndicator(dashboard == null ? null : dashboard.getActiveProjectCount(), alerts)
        ));
        workbench.setAttentionProjects(attentionProjects(alerts));
        workbench.setBusinessEvents(events == null ? List.of() : events);
        workbench.setAlertReport(alerts == null ? new BusinessAlertReport() : alerts);
        return workbench;
    }

    private WorkbenchIndicator signedIndicator(DashboardMetric metric) {
        WorkbenchIndicator indicator = indicator("signedAmount", "签约额", "amount", metric);
        BigDecimal achievement = value(metric == null ? null : metric.getAchievementRate());
        BigDecimal target = value(metric == null ? null : metric.getTarget());
        if (target.signum() == 0) {
            BigDecimal trend = value(metric == null ? null : metric.getMonthOverMonthRate());
            setStatus(indicator, trend.compareTo(BigDecimal.valueOf(-20)) < 0
                    ? WorkbenchSignalStatus.Attention : WorkbenchSignalStatus.Normal, null);
            indicator.setDetail("未设置目标，按环比判断 · 环比 " + signed(trend) + "%");
            return indicator;
        }
        WorkbenchSignalStatus status = achievement.compareTo(BigDecimal.valueOf(80)) >= 0
                ? WorkbenchSignalStatus.Normal
                : achievement.compareTo(BigDecimal.valueOf(60)) >= 0
                ? WorkbenchSignalStatus.Attention : WorkbenchSignalStatus.Danger;
        setStatus(indicator, status, null);
        indicator.setDetail("目标达成 " + decimal(achievement) + "% · 环比 "
                + signed(metric == null ? null : metric.getMonthOverMonthRate()) + "%");
        return indicator;
    }

    private WorkbenchIndicator receivedIndicator(DashboardMetric metric, BusinessAlertReport alerts) {
        WorkbenchIndicator indicator = indicator("receivedAmount", "回款额", "amount", metric);
        int overdueCount = alerts == null || alerts.getOverdueReceivables() == null
                ? 0 : alerts.getOverdueReceivables().size();
        BigDecimal achievement = value(metric == null ? null : metric.getAchievementRate());
        BigDecimal target = value(metric == null ? null : metric.getTarget());
        WorkbenchSignalStatus status = overdueCount > 0
                ? WorkbenchSignalStatus.Danger
                : target.signum() > 0 && achievement.compareTo(BigDecimal.valueOf(60)) < 0
                ? WorkbenchSignalStatus.Attention : WorkbenchSignalStatus.Normal;
        setStatus(indicator, status, null);
        indicator.setDetail("逾期应收 " + overdueCount + " 笔 · "
                + (target.signum() == 0 ? "未设置目标" : "目标达成 " + decimal(achievement) + "%"));
        return indicator;
    }

    private WorkbenchIndicator grossProfitIndicator(DashboardMetric metric) {
        WorkbenchIndicator indicator = indicator("grossProfit", "项目毛利", "amount", metric);
        if (metric == null || Boolean.FALSE.equals(metric.getComplete())) {
            indicator.setValue(null);
            setStatus(indicator, WorkbenchSignalStatus.Unknown, "成本未完整");
            indicator.setDetail(metric == null || metric.getWarning() == null || metric.getWarning().isBlank()
                    ? "成本补齐后再计算可靠毛利" : metric.getWarning());
            return indicator;
        }
        BigDecimal profit = value(metric.getValue());
        if (profit.signum() < 0) {
            setStatus(indicator, WorkbenchSignalStatus.Danger, "项目亏损");
            indicator.setDetail("当前毛利为负，请检查成本与变更");
            return indicator;
        }
        BigDecimal target = value(metric.getTarget());
        BigDecimal achievement = value(metric.getAchievementRate());
        WorkbenchSignalStatus status = target.signum() > 0 && achievement.compareTo(BigDecimal.valueOf(80)) < 0
                ? WorkbenchSignalStatus.Attention : WorkbenchSignalStatus.Normal;
        setStatus(indicator, status, null);
        indicator.setDetail(target.signum() == 0 ? "未设置目标" : "目标达成 " + decimal(achievement) + "%");
        return indicator;
    }

    private WorkbenchIndicator activeProjectIndicator(DashboardMetric metric, BusinessAlertReport alerts) {
        WorkbenchIndicator indicator = indicator("activeProjectCount", "在建项目", "count", metric);
        int riskCount = alerts == null || alerts.getSuspensionRisks() == null
                ? 0 : alerts.getSuspensionRisks().size();
        setStatus(indicator, riskCount > 0 ? WorkbenchSignalStatus.Danger : WorkbenchSignalStatus.Normal, null);
        indicator.setDetail(riskCount > 0
                ? "停工/逾期风险 " + riskCount + " 项 · 数量目标不参与健康判定"
                : "当前项目运行正常 · 数量目标不参与健康判定");
        return indicator;
    }

    private WorkbenchIndicator indicator(String key, String label, String valueType, DashboardMetric metric) {
        WorkbenchIndicator indicator = new WorkbenchIndicator();
        indicator.setKey(key);
        indicator.setLabel(label);
        indicator.setValue(metric == null ? BigDecimal.ZERO : metric.getValue());
        indicator.setValueType(valueType);
        return indicator;
    }

    private void setStatus(WorkbenchIndicator indicator, WorkbenchSignalStatus status, String text) {
        indicator.setStatus(status);
        indicator.setStatusText(text == null ? status.getName() : text);
    }

    private List<WorkbenchProject> attentionProjects(BusinessAlertReport alerts) {
        if (alerts == null) return List.of();
        Map<Long, ProjectRisk> grouped = new LinkedHashMap<>();
        for (BusinessAlert alert : allAlerts(alerts)) {
            if (alert.getProject() == null || alert.getProject().getProjectId() == null) continue;
            grouped.computeIfAbsent(alert.getProject().getProjectId(), id -> new ProjectRisk(alert.getProject()))
                    .add(alert);
        }
        return grouped.values().stream()
                .map(ProjectRisk::snapshot)
                .sorted(Comparator.comparingInt((WorkbenchProject item) -> riskRank(item.getRiskLevel()))
                        .thenComparing(WorkbenchProject::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WorkbenchProject::getProjectId))
                .limit(4)
                .toList();
    }

    private List<BusinessAlert> allAlerts(BusinessAlertReport alerts) {
        List<BusinessAlert> all = new ArrayList<>();
        add(all, alerts.getOverdueReceivables());
        add(all, alerts.getSuspensionRisks());
        add(all, alerts.getMaterialOverIssues());
        add(all, alerts.getChangeAnomalies());
        return all;
    }

    private void add(List<BusinessAlert> target, List<BusinessAlert> source) {
        if (source != null) target.addAll(source);
    }

    private int riskRank(BusinessAlertLevel level) {
        if (level == BusinessAlertLevel.High) return 0;
        if (level == BusinessAlertLevel.Medium) return 1;
        return 2;
    }

    private String nextAction(BusinessAlertType type) {
        if (type == null) return "进入项目详情核实风险";
        return type.nextAction();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String signed(BigDecimal value) {
        if (value == null) return "0";
        return value.signum() > 0 ? "+" + decimal(value) : decimal(value);
    }

    private final class ProjectRisk {
        private final com.th4.edge.decoboon.project.model.Project project;
        private int count;
        private BusinessAlert representative;

        private ProjectRisk(com.th4.edge.decoboon.project.model.Project project) {
            this.project = project;
        }

        private void add(BusinessAlert alert) {
            count++;
            if (representative == null
                    || riskRank(alert.getLevel()) < riskRank(representative.getLevel())
                    || (riskRank(alert.getLevel()) == riskRank(representative.getLevel())
                    && earlier(alert, representative))) {
                representative = alert;
            }
        }

        private boolean earlier(BusinessAlert candidate, BusinessAlert current) {
            if (candidate.getDueDate() == null) return false;
            if (current.getDueDate() == null) return true;
            return candidate.getDueDate().isBefore(current.getDueDate());
        }

        private WorkbenchProject snapshot() {
            WorkbenchProject item = new WorkbenchProject();
            item.setProjectId(project.getProjectId());
            item.setNo(project.getNo());
            item.setName(project.getName());
            item.setStatus(project.getStatus());
            item.setProgress(project.getProgress());
            item.setManagerName(project.getManager() == null ? null : project.getManager().getName());
            item.setRiskLevel(representative.getLevel());
            item.setRiskCount(count);
            item.setMainRiskType(representative.getType());
            item.setRiskReason(representative.getMessage());
            item.setNextAction(nextAction(representative.getType()));
            item.setAmount(representative.getAmount() == null || representative.getAmount().signum() == 0
                    ? null : representative.getAmount());
            item.setDueDate(representative.getDueDate());
            return item;
        }
    }
}
