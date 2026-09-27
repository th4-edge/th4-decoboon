package com.th4.edge.decoboon.ai.brief.context;

import com.th4.edge.decoboon.ai.brief.generator.AiBriefGenerator;
import com.th4.edge.decoboon.ai.brief.generator.AiBriefInput;
import com.th4.edge.decoboon.ai.brief.model.AiBriefQuery;
import com.th4.edge.decoboon.ai.brief.model.AiBriefReport;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.ai.context.AiUsageContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.context.BossDashboardContext;
import com.th4.edge.decoboon.statistic.context.BusinessAlertContext;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AI 经营参谋 P0 上下文。只读经营事实，生成建议并记录影子用量。 */
@Component
public class AiBossBriefContext {

    private static final int ACTION_LIMIT = 3;

    private final TenantContext tenantContext;
    private final BossDashboardContext dashboardContext;
    private final FinanceReportContext financeContext;
    private final BusinessAlertContext alertContext;
    private final AiBriefGenerator generator;
    private final AiUsageContext usageContext;

    public AiBossBriefContext(TenantContext tenantContext,
                              BossDashboardContext dashboardContext,
                              FinanceReportContext financeContext,
                              BusinessAlertContext alertContext,
                              AiBriefGenerator generator,
                              AiUsageContext usageContext) {
        this.tenantContext = tenantContext;
        this.dashboardContext = dashboardContext;
        this.financeContext = financeContext;
        this.alertContext = alertContext;
        this.generator = generator;
        this.usageContext = usageContext;
    }

    @Transactional
    public AiBriefReport report(Employee principal, AiBriefQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1) : query.getMonth().withDayOfMonth(1);
        Long storeId = query == null ? null : query.getStoreId();

        BossDashboardQuery dashboardQuery = new BossDashboardQuery();
        dashboardQuery.setMonth(month);
        dashboardQuery.setStoreId(storeId);
        BossDashboard dashboard = safeDashboard(current, dashboardQuery);
        FinanceSummary finance = safeFinance(current);
        BusinessAlertQuery alertQuery = new BusinessAlertQuery();
        alertQuery.setAsOf(LocalDate.now());
        BusinessAlertReport alerts = safeAlerts(current, alertQuery);

        LocalDateTime startedAt = LocalDateTime.now();
        AiBriefReport report = buildReport(current, month, dashboard, finance, alerts, startedAt);
        appendUnavailableNotice(report, dashboard == null, finance == null, alerts == null);
        LocalDateTime completedAt = LocalDateTime.now();
        AiUsageCall usage = recordUsage(current, report, startedAt, completedAt);
        report.getUsage().setCallId(usage.getCallId());
        report.setAsOf(completedAt);
        return report;
    }

    private AiBriefReport buildReport(Employee current, LocalDate month, BossDashboard dashboard,
                                      FinanceSummary finance, BusinessAlertReport alerts,
                                      LocalDateTime generatedAt) {
        AiBriefReport report = new AiBriefReport();
        report.setMonth(month);
        report.setAsOf(generatedAt);
        report.setSummary(generator.generate(new AiBriefInput(month, dashboard, finance, alerts)));
        report.setGeneratorVersion(generator.version());
        report.setSources(sources(current, month, dashboard, finance, alerts, generatedAt));
        report.setFacts(facts(dashboard, finance, alerts));
        report.setActions(actions(alerts));
        report.setCostStatus(costStatus(dashboard, finance));
        report.getUsage().setResourceUnits((long) report.getFacts().size() + report.getActions().size() + report.getSummary().length());
        report.getUsage().setShadowBillableUnits(report.getUsage().getResourceUnits());
        report.getUsage().setRateCardVersion("shadow-direct-v1");
        return report;
    }

    private List<AiBriefReport.AiBriefSource> sources(Employee current, LocalDate month,
                                                       BossDashboard dashboard, FinanceSummary finance,
                                                       BusinessAlertReport alerts, LocalDateTime generatedAt) {
        Long companyId = current.getCompany() == null ? null : current.getCompany().getComId();
        List<AiBriefReport.AiBriefSource> sources = new ArrayList<>();
        if (dashboard != null) {
            sources.add(new AiBriefReport.AiBriefSource("statistic/boss/dashboard", "Company",
                    companyId, month == null ? null : month.toString(), generatedAt));
        }
        if (finance != null) {
            sources.add(new AiBriefReport.AiBriefSource("finance/summary/report", "Company",
                    companyId, generatedAt.toLocalDate().toString(), generatedAt));
        }
        if (alerts != null) {
            sources.add(new AiBriefReport.AiBriefSource("statistic/alert/report", "Company",
                    companyId, alerts.getAsOf() == null ? null : alerts.getAsOf().toString(), generatedAt));
        }
        return sources;
    }

    private void appendUnavailableNotice(AiBriefReport report, boolean dashboardUnavailable,
                                         boolean financeUnavailable, boolean alertsUnavailable) {
        List<String> unavailable = new ArrayList<>();
        if (dashboardUnavailable) unavailable.add("经营指标");
        if (financeUnavailable) unavailable.add("财务汇总");
        if (alertsUnavailable) unavailable.add("经营预警");
        if (!unavailable.isEmpty()) {
            report.setSummary(report.getSummary() + " " + String.join("、", unavailable) + "暂不可确认，请稍后重试。");
        }
    }

    private List<AiBriefReport.AiBriefFact> facts(BossDashboard dashboard, FinanceSummary finance,
                                                  BusinessAlertReport alerts) {
        List<AiBriefReport.AiBriefFact> facts = new ArrayList<>();
        DashboardMetric signed = dashboard == null ? null : dashboard.getSignedAmount();
        facts.add(new AiBriefReport.AiBriefFact(
                "Business", "本月签约额",
                signed == null || signed.getValue() == null ? "暂不可确认" : wan(signed.getValue()) + " 万",
                signed == null || signed.getMonthOverMonthRate() == null
                        ? "暂无可比上期" : "环比 " + signed(signed.getMonthOverMonthRate()),
                "brand"));
        DashboardMetric active = dashboard == null ? null : dashboard.getActiveProjectCount();
        facts.add(new AiBriefReport.AiBriefFact(
                "Business", "在建项目",
                active == null || active.getValue() == null ? "暂不可确认" : active.getValue().stripTrailingZeros().toPlainString() + " 个",
                "按现有项目状态口径", "plain"));

        long deliveryRisks = alerts == null || alerts.getSuspensionRisks() == null ? 0 : alerts.getSuspensionRisks().size();
        facts.add(new AiBriefReport.AiBriefFact(
                "Delivery", "交付风险",
                deliveryRisks + " 个",
                deliveryRisks == 0 ? "当前未发现停工/工期预警" : "来自现有经营预警，需责任人复核", deliveryRisks > 0 ? "warn" : "ok"));
        long highCount = alerts == null || alerts.getHighCount() == null ? 0 : alerts.getHighCount();
        facts.add(new AiBriefReport.AiBriefFact(
                "Delivery", "高风险事项",
                highCount + " 项",
                highCount == 0 ? "当前无高风险预警" : "按现有预警级别统计", highCount > 0 ? "danger" : "ok"));

        facts.add(new AiBriefReport.AiBriefFact(
                "Cash", "应收未收",
                finance == null ? "暂不可确认" : wan(finance.getOutstandingAmount()) + " 万",
                finance == null ? "财务汇总暂不可确认" : "财务工作台口径", "danger"));
        facts.add(new AiBriefReport.AiBriefFact(
                "Cash", "其中逾期",
                finance == null ? "暂不可确认" : wan(finance.getOverdueAmount()) + " 万",
                finance == null ? "财务汇总暂不可确认" : "逾期金额不影响合同与项目状态", "danger"));
        facts.add(new AiBriefReport.AiBriefFact(
                "Cash", "回款率",
                finance == null || finance.getCollectionRate() == null ? "暂不可确认" : percent(finance.getCollectionRate()),
                "已收 / 应收", "ok"));
        return facts;
    }

    private List<AiBriefReport.AiBriefAction> actions(BusinessAlertReport alerts) {
        if (alerts == null) return List.of();
        List<BusinessAlert> all = new ArrayList<>();
        add(all, alerts.getOverdueReceivables());
        add(all, alerts.getSuspensionRisks());
        add(all, alerts.getMaterialOverIssues());
        add(all, alerts.getChangeAnomalies());
        Map<String, BusinessAlert> selected = new LinkedHashMap<>();
        all.stream()
                .sorted(Comparator
                        .comparingInt((BusinessAlert item) -> levelOrder(item.getLevel()))
                        .thenComparing(BusinessAlert::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(item -> selected.putIfAbsent(String.valueOf(item.getBusinessType()), item));
        return selected.values().stream().limit(ACTION_LIMIT).map(this::action).toList();
    }

    private AiBriefReport.AiBriefAction action(BusinessAlert alert) {
        AiBriefReport.AiBriefAction action = new AiBriefReport.AiBriefAction();
        action.setLevel(alert.getLevel() == null ? "Low" : alert.getLevel().name());
        action.setTitle(alert.getTitle());
        action.setReason(alert.getMessage());
        action.setOwnerName(alert.getOwner() == null || alert.getOwner().getName() == null
                ? "待确认" : alert.getOwner().getName());
        action.setDueText(alert.getDueDate() == null ? "未设置期限" : "到期 " + alert.getDueDate());
        action.setBusinessType(alert.getBusinessType());
        action.setBusinessId(alert.getBusinessId());
        action.setTargetRoute(route(alert.getType()));
        return action;
    }

    private AiBriefReport.AiBriefCostStatus costStatus(BossDashboard dashboard, FinanceSummary finance) {
        AiBriefReport.AiBriefCostStatus status = new AiBriefReport.AiBriefCostStatus();
        if (finance == null) {
            status.setComplete(false);
            status.setIncompleteProjectCount(0);
            status.setMessage("财务汇总暂不可确认，成本完整性暂不可确认");
            return status;
        }
        boolean dashboardComplete = dashboard == null || dashboard.getGrossProfit() == null
                || !Boolean.FALSE.equals(dashboard.getGrossProfit().getComplete());
        boolean complete = dashboardComplete && !Boolean.FALSE.equals(finance.getCostComplete());
        status.setComplete(complete);
        status.setIncompleteProjectCount(finance.getCostIncompleteProjectCount() == null
                ? 0 : finance.getCostIncompleteProjectCount());
        status.setMessage(complete ? null : "成本未完整，暂不展示可靠毛利");
        return status;
    }

    private BossDashboard safeDashboard(Employee current, BossDashboardQuery query) {
        try { return dashboardContext.dashboard(current, query); }
        catch (RuntimeException ignored) { return null; }
    }

    private FinanceSummary safeFinance(Employee current) {
        try { return financeContext.summary(current); }
        catch (RuntimeException ignored) { return null; }
    }

    private BusinessAlertReport safeAlerts(Employee current, BusinessAlertQuery query) {
        try { return alertContext.report(current, query); }
        catch (RuntimeException ignored) { return null; }
    }

    private String idempotencyKey(Employee current, AiBriefReport report) {
        String companyId = current.getCompany() == null ? "0" : String.valueOf(current.getCompany().getComId());
        StringBuilder fingerprint = new StringBuilder();
        for (AiBriefReport.AiBriefFact fact : report.getFacts()) {
            fingerprint.append(fact.getCategory()).append(':').append(fact.getLabel()).append(':')
                    .append(fact.getValue()).append('|');
        }
        for (AiBriefReport.AiBriefAction action : report.getActions()) {
            fingerprint.append(action.getBusinessType()).append(':').append(action.getBusinessId()).append('|');
        }
        fingerprint.append(report.getMonth()).append('|').append(report.getCostStatus().getComplete());
        return "boss-brief:" + companyId + ":" + current.getEmpId() + ":" + Integer.toHexString(fingerprint.toString().hashCode());
    }

    private AiUsageCall recordUsage(Employee current, AiBriefReport report,
                                    LocalDateTime startedAt, LocalDateTime completedAt) {
        AiUsageCall call = new AiUsageCall();
        call.setIdempotencyKey(idempotencyKey(current, report));
        call.setBusinessModule("Analytics");
        call.setCapabilityCode("boss-brief");
        call.setSceneCode("boss-monday-review");
        call.setBusinessType("Company");
        call.setBusinessId(current.getCompany() == null ? null : current.getCompany().getComId());
        call.setProviderCode("baseline");
        call.setModelCode("rule-baseline");
        call.setModelVersion(report.getGeneratorVersion());
        call.setCapabilityVersion(report.getGeneratorVersion());
        call.setRateCardVersion("shadow-direct-v1");
        call.setAllocationType(AiStoreAllocation.EnterprisePublic);
        call.setInputUnits((long) report.getFacts().size() + report.getActions().size());
        call.setOutputUnits((long) report.getSummary().length());
        call.setResourceUnits(report.getUsage().getResourceUnits());
        call.setShadowBillableUnits(report.getUsage().getShadowBillableUnits());
        call.setStatus(AiUsageStatus.Succeeded);
        call.setStartedAt(startedAt);
        call.setCompletedAt(completedAt);
        call.setEstimated(Boolean.TRUE);
        return usageContext.record(current, call);
    }

    private String route(BusinessAlertType type) {
        if (type == BusinessAlertType.OverdueReceivable) return "/boss/finance/overview";
        if (type == BusinessAlertType.MaterialOverIssue) return "/boss/project/supervision";
        if (type == BusinessAlertType.AbnormalChangeRate) return "/boss/project/supervision";
        return "/boss/statistic/risk";
    }

    private int levelOrder(BusinessAlertLevel level) {
        if (level == BusinessAlertLevel.High) return 0;
        if (level == BusinessAlertLevel.Medium) return 1;
        return 2;
    }

    private void add(List<BusinessAlert> target, List<BusinessAlert> source) {
        if (source != null) target.addAll(source);
    }

    private String wan(BigDecimal value) {
        if (value == null) return "—";
        return value.divide(BigDecimal.valueOf(10000), 1, RoundingMode.HALF_UP).toPlainString();
    }

    private String percent(BigDecimal value) {
        if (value == null) return "—";
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String signed(BigDecimal value) {
        if (value == null) return "—";
        String prefix = value.signum() > 0 ? "+" : "";
        return prefix + value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
