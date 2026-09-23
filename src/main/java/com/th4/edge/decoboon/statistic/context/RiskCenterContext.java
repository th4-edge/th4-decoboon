package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.statistic.model.RiskCenterQuery;
import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.decoboon.statistic.model.RiskCenterReport;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险中心读侧聚合：复用经营预警口径，按页面形态投影。
 *
 * <p>不新增风险规则：类型、级别、责任人、时效全部来自 {@link BusinessAlertContext}；
 * 处置动作统一取 {@link BusinessAlertType#nextAction()}。</p>
 */
@Component
public class RiskCenterContext {

    private final BusinessAlertContext alertContext;
    private final RiskDispositionContext dispositionContext;

    public RiskCenterContext(BusinessAlertContext alertContext,
                             RiskDispositionContext dispositionContext) {
        this.alertContext = alertContext;
        this.dispositionContext = dispositionContext;
    }

    public RiskCenterReport report(Employee principal, RiskCenterQuery query) {
        LocalDate asOf = query == null || query.getAsOf() == null ? LocalDate.now() : query.getAsOf();
        BusinessAlertQuery alertQuery = new BusinessAlertQuery();
        alertQuery.setAsOf(asOf);
        BusinessAlertReport alertReport = alertContext.report(principal, alertQuery);

        List<BusinessAlert> all = new ArrayList<>();
        all.addAll(alertReport.getOverdueReceivables() == null ? List.of() : alertReport.getOverdueReceivables());
        all.addAll(alertReport.getSuspensionRisks() == null ? List.of() : alertReport.getSuspensionRisks());
        all.addAll(alertReport.getMaterialOverIssues() == null ? List.of() : alertReport.getMaterialOverIssues());
        all.addAll(alertReport.getChangeAnomalies() == null ? List.of() : alertReport.getChangeAnomalies());

        RiskCenterReport report = new RiskCenterReport();
        report.setAsOf(asOf);
        report.setTotalCount(all.size());
        report.setHighCount((int) all.stream().filter(item -> item.getLevel() == BusinessAlertLevel.High).count());
        report.setMediumCount((int) all.stream().filter(item -> item.getLevel() == BusinessAlertLevel.Medium).count());
        report.setLowCount((int) all.stream().filter(item -> item.getLevel() == BusinessAlertLevel.Low).count());
        report.setTypeDistribution(typeDistribution(all));
        report.setResponsibilityDistribution(responsibilities(all));
        // 处置单按稳定 alertKey 与实时预警对齐（新预警建单、升级重开、同级复现累计、条件消失自动消解）
        Map<String, RiskItem> disposals = dispositionContext.sync(principal, all);
        report.setItems(items(all, asOf, disposals));
        return report;
    }

    // ------------------------------------------------------------------ 类型分布

    private List<RiskCenterReport.RiskTypeDistribution> typeDistribution(List<BusinessAlert> all) {
        Map<BusinessAlertType, List<BusinessAlert>> grouped = new LinkedHashMap<>();
        for (BusinessAlert alert : all) {
            grouped.computeIfAbsent(alert.getType(), key -> new ArrayList<>()).add(alert);
        }
        List<RiskCenterReport.RiskTypeDistribution> result = new ArrayList<>();
        for (BusinessAlertType type : BusinessAlertType.values()) {
            List<BusinessAlert> items = grouped.getOrDefault(type, List.of());
            result.add(new RiskCenterReport.RiskTypeDistribution(
                    type.name(), type.getName(), description(type), items.size(),
                    (int) items.stream().filter(item -> item.getLevel() == BusinessAlertLevel.High).count()));
        }
        return result;
    }

    private String description(BusinessAlertType type) {
        return switch (type) {
            case OverdueReceivable -> "应收逾期未收、回款计划未执行";
            case ProjectSuspensionRisk -> "项目停工或交付节点延期";
            case MaterialOverIssue -> "实际领料超出材料计划";
            case AbnormalChangeRate -> "已生效合同变更率异常";
        };
    }

    // ------------------------------------------------------------------ 责任人分布

    private List<RiskCenterReport.RiskResponsibility> responsibilities(List<BusinessAlert> all) {
        Map<String, long[]> grouped = new LinkedHashMap<>();
        Map<String, Long> high = new LinkedHashMap<>();
        for (BusinessAlert alert : all) {
            String name = alert.getOwner() == null || alert.getOwner().getName() == null
                    ? "待指派" : alert.getOwner().getName();
            grouped.computeIfAbsent(name, key -> new long[1])[0]++;
            if (alert.getLevel() == BusinessAlertLevel.High) {
                high.merge(name, 1L, Long::sum);
            }
        }
        return grouped.entrySet().stream()
                .map(entry -> new RiskCenterReport.RiskResponsibility(entry.getKey(),
                        (int) entry.getValue()[0], high.getOrDefault(entry.getKey(), 0L).intValue()))
                .sorted(Comparator.comparing(RiskCenterReport.RiskResponsibility::getCount).reversed())
                .toList();
    }

    // ------------------------------------------------------------------ 清单

    private List<RiskCenterReport.RiskItemView> items(List<BusinessAlert> all, LocalDate asOf,
                                                      Map<String, RiskItem> disposals) {
        return all.stream()
                .sorted(Comparator.comparing((BusinessAlert item) -> levelOrder(item.getLevel()))
                        .thenComparing(item -> item.getDueDate() == null ? LocalDate.MAX : item.getDueDate()))
                .map(item -> toView(item, asOf, disposals.get(dispositionContext.alertKey(item))))
                .toList();
    }

    private RiskCenterReport.RiskItemView toView(BusinessAlert alert, LocalDate asOf, RiskItem disposal) {
        RiskCenterReport.RiskItemView view = new RiskCenterReport.RiskItemView();
        view.setKey(alert.getBusinessType() + "-" + alert.getBusinessId() + "-" + alert.getType());
        view.setType(alert.getType() == null ? null : alert.getType().name());
        view.setTypeLabel(alert.getType() == null ? null : alert.getType().getName());
        view.setLevel(alert.getLevel() == null ? BusinessAlertLevel.Low.name() : alert.getLevel().name());
        view.setLevelLabel(alert.getLevel() == null ? BusinessAlertLevel.Low.getName() : alert.getLevel().getName());
        view.setTitle(alert.getTitle());
        view.setMessage(alert.getMessage());
        view.setProjectName(alert.getProject() == null ? null : alert.getProject().getName());
        view.setCustomerName(alert.getProject() != null && alert.getProject().getCustomer() != null
                ? alert.getProject().getCustomer().getName() : null);
        view.setOwnerName(alert.getOwner() == null ? null : alert.getOwner().getName());
        view.setAmount(alert.getAmount());
        view.setDueDate(alert.getDueDate());
        view.setNextAction(alert.getType() == null ? null : alert.getType().nextAction());
        applyTime(view, alert.getDueDate(), asOf);
        applyDisposal(view, disposal, asOf);
        return view;
    }

    /** 把处置单的处理态叠加到清单行上；disposal 为空说明同步异常（前端应禁用动作）。 */
    private void applyDisposal(RiskCenterReport.RiskItemView view, RiskItem disposal, LocalDate asOf) {
        if (disposal == null) {
            return;
        }
        view.setRiskId(disposal.getRiskId());
        view.setStatus(disposal.getStatus() == null ? null : disposal.getStatus().name());
        view.setStatusLabel(disposal.getStatus() == null ? null : disposal.getStatus().getName());
        view.setDisposerName(disposal.getOwner() == null ? null : disposal.getOwner().getName());
        view.setDueAt(disposal.getDueAt());
        view.setRoundNo(disposal.getRoundNo());
        view.setRecurrenceCount(disposal.getRecurrenceCount());
        applyDue(view, disposal.getDueAt(), asOf);
    }

    /** 处置时限提示：相对 SLA 截止时间，仅用于看板排序与颜色。 */
    private void applyDue(RiskCenterReport.RiskItemView view, java.time.LocalDateTime dueAt, LocalDate asOf) {
        if (dueAt == null) {
            return;
        }
        long hours = java.time.Duration.between(java.time.LocalDateTime.now(), dueAt).toHours();
        if (hours < 0) {
            view.setDueText("已超时 " + Math.abs(hours) + " 小时");
            view.setDueTone("danger");
        } else if (hours <= 4) {
            view.setDueText("剩余 " + hours + " 小时");
            view.setDueTone("warn");
        } else {
            view.setDueText("剩余 " + (hours / 24 >= 1 ? (hours / 24) + " 天" : hours + " 小时"));
            view.setDueTone("normal");
        }
    }

    private void applyTime(RiskCenterReport.RiskItemView view, LocalDate dueDate, LocalDate asOf) {
        if (dueDate == null) {
            view.setTimeText("未设到期日");
            view.setTimeTone("normal");
            return;
        }
        long days = ChronoUnit.DAYS.between(asOf, dueDate);
        if (days < 0) {
            view.setTimeText("逾期 " + Math.abs(days) + " 天");
            view.setTimeTone(Math.abs(days) > 7 ? "danger" : "warn");
        } else if (days == 0) {
            view.setTimeText("今天到期");
            view.setTimeTone("warn");
        } else {
            view.setTimeText("剩余 " + days + " 天");
            view.setTimeTone(days <= 3 ? "warn" : "normal");
        }
    }

    /** 高风险在前：High=0、Medium=1、Low=2。 */
    private int levelOrder(BusinessAlertLevel level) {
        if (level == BusinessAlertLevel.High) return 0;
        if (level == BusinessAlertLevel.Medium) return 1;
        return 2;
    }
}
