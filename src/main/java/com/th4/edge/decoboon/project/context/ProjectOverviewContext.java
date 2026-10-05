package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ProjectContractStage;
import com.th4.edge.decoboon.project.configure.ProjectRiskLevel;
import com.th4.edge.decoboon.project.configure.ProjectRiskType;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectMetricCard;
import com.th4.edge.decoboon.project.model.ProjectOverviewReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectStatusCount;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.context.BusinessAlertContext;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业负责人项目经营聚合。
 *
 * <p>只做既有事实的投影：项目状态与进度来自 {@code Project}，合同当前金额与履约阶段来自
 * {@code ProjectContract}，应收/已收来自 {@code Receivable}，风险等级与原因复用
 * {@code BusinessAlertContext} 的经营预警口径，成本与毛利复用 {@code ProjectCostContext} 的实际成本口径。
 * 本上下文不新增金额口径，也不写业务逻辑。</p>
 */
@Component
public class ProjectOverviewContext {

    /** 回款率低于该值时提示负责人关注。 */
    private static final BigDecimal LOW_RECEIVED_RATE = BigDecimal.valueOf(60);
    /** 「回款最慢」排序中，尚未生成应收计划的项目排在最后。 */
    private static final BigDecimal NO_PLAN_SORT_RATE = BigDecimal.valueOf(101);

    private final TenantContext tenantContext;
    private final ProjectRepository projectRepository;
    private final ProjectContractRepository contractRepository;
    private final ReceivableRepository receivableRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectCostContext costContext;
    private final BusinessAlertContext alertContext;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.th4.edge.decoboon.finance.context.ManualLedgerContext manualLedgerContext;

    public ProjectOverviewContext(TenantContext tenantContext,
                                  ProjectRepository projectRepository,
                                  ProjectContractRepository contractRepository,
                                  ReceivableRepository receivableRepository,
                                  ProjectStageRepository stageRepository,
                                  ProjectCostContext costContext,
                                  BusinessAlertContext alertContext) {
        this.tenantContext = tenantContext;
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
        this.receivableRepository = receivableRepository;
        this.stageRepository = stageRepository;
        this.costContext = costContext;
        this.alertContext = alertContext;
    }

    @Transactional(readOnly = true)
    public ProjectOverviewReport report(Employee principal,
                                        Integer current,
                                        Integer size,
                                        String status,
                                        String contractStage,
                                        String riskLevel,
                                        String keyword,
                                        String sort,
                                        Long storeId) {
        ProjectStatus statusFilter = parseStatus(status);
        ProjectContractStage contractFilter = parseContractStage(contractStage);
        ProjectRiskLevel riskFilter = parseRiskLevel(riskLevel);
        boolean riskOnly = isRiskOnly(riskLevel);
        Employee boss = tenantContext.requireBoss(principal);
        Long comId = boss.getCompany().getComId();
        LocalDate today = LocalDate.now();

        List<Project> scoped = projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId).stream()
                .filter(project -> matchesStore(project, storeId))
                .toList();

        Map<Long, ProjectContract> contracts = contractsByProject(comId);
        Map<Long, ReceivableRollup> rollups = receivableRollups(comId, today);
        Map<Long, ProjectRisk> risks = risks(alertReport(boss, today));

        scoped.forEach(project -> enrich(project, contracts, rollups, risks, today));

        List<Project> filtered = scoped.stream()
                .filter(project -> matchesKeyword(project, keyword))
                .toList();

        ProjectOverviewReport report = new ProjectOverviewReport();
        report.setAsOf(today);
        report.setMonth(today.withDayOfMonth(1));
        // 指标与计数基于企业范围（门店已过滤、关键词不影响总量），列表再按两个维度、关键词和排序收窄。
        report.setMetrics(metrics(scoped, contracts, today));
        report.setStatusCounts(statusCounts(filtered));
        report.setContractCounts(contractCounts(filtered));

        List<Project> sorted = new ArrayList<>(filtered.stream()
                .filter(project -> matchesStatus(project, statusFilter))
                .filter(project -> matchesContractStage(project, contractFilter))
                .filter(project -> matchesRiskLevel(project, riskFilter, riskOnly))
                .toList());
        sorted.sort(comparator(sort, contracts));
        List<Project> pageItems = slice(report, sorted, current, size);
        pageItems.forEach(project -> applyProfit(project, contracts.get(project.getProjectId())));
        return report;
    }

    // ---------------------------------------------------------------- 投影组装

    private void enrich(Project project,
                        Map<Long, ProjectContract> contracts,
                        Map<Long, ReceivableRollup> rollups,
                        Map<Long, ProjectRisk> risks,
                        LocalDate today) {
        ProjectContract contract = contracts.get(project.getProjectId());
        ReceivableRollup rollup = rollups.getOrDefault(project.getProjectId(), ReceivableRollup.EMPTY);
        ProjectRisk risk = risks.get(project.getProjectId());
        // 合同当前金额是派生汇总字段：有合同时以合同为准，保证卡片与指标同口径。
        if (contract != null) {
            project.setContractAmount(contractAmount(project, contract));
        }
        project.setContractStage(ProjectContractStage.of(contract == null ? null : contract.getStatus()));
        project.setCurrentStageName(currentStageName(project));
        project.setReceivedAmount(rollup.received());
        project.setReceivableAmount(rollup.amount());
        project.setOverdueReceivableCount(rollup.overdueCount());
        project.setOverdueDays(overdueDays(project, today));
        if (risk == null) {
            project.setRiskLevel(ProjectRiskLevel.Normal);
            project.setRiskCount(0);
            project.setMainRiskType(ProjectRiskType.None);
            project.setRiskReason(null);
            project.setNextAction(defaultAction(project));
            return;
        }
        project.setRiskLevel(risk.level());
        project.setRiskCount(risk.count());
        project.setMainRiskType(risk.type());
        project.setRiskReason(risk.reason());
        project.setNextAction(risk.nextAction());
    }

    /**
     * 实际成本口径：只向负责人暴露实际成本、毛利与毛利率。
     * 成本不完整时毛利与毛利率均为空，禁止 0 成本伪利润。
     */
    private void applyProfit(Project project, ProjectContract contract) {
        ProjectCostContext.CostSummary cost = costContext.summarize(project);
        com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts manual = manualAmounts(project);
        BigDecimal actualCost = value(cost.actualCost()).add(manual.cost()).setScale(2, RoundingMode.HALF_UP);
        project.setActualCost(actualCost);
        project.setCostComplete(cost.complete());
        if (!cost.complete()) {
            project.setGrossProfit(null);
            project.setGrossMargin(null);
            return;
        }
        BigDecimal revenue = (contract == null ? value(project.getContractAmount()) : contractAmount(project, contract))
                .add(manual.income());
        BigDecimal profit = revenue.subtract(actualCost).setScale(2, RoundingMode.HALF_UP);
        project.setGrossProfit(profit);
        project.setGrossMargin(percentage(profit, revenue));
    }

    private com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts manualAmounts(Project project) {
        return manualLedgerContext == null
                ? com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts.ZERO
                : manualLedgerContext.amounts(project);
    }

    // ---------------------------------------------------------------- 指标与计数

    private List<ProjectMetricCard> metrics(List<Project> projects,
                                            Map<Long, ProjectContract> contracts,
                                            LocalDate today) {
        List<Project> active = projects.stream().filter(this::isActive).toList();
        long preparing = active.stream().filter(item -> item.getStatus() == ProjectStatus.Preparing).count();
        long ongoing = active.stream().filter(item -> item.getStatus() == ProjectStatus.Ongoing).count();
        long suspended = active.stream().filter(item -> item.getStatus() == ProjectStatus.Suspended).count();
        BigDecimal activeAmount = active.stream()
                .map(item -> amountOf(item, contracts.get(item.getProjectId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allAmount = projects.stream()
                .map(item -> amountOf(item, contracts.get(item.getProjectId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = active.stream().map(item -> value(item.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivable = active.stream().map(item -> value(item.getReceivableAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long overdueReceivables = projects.stream()
                .filter(item -> item.getOverdueReceivableCount() != null && item.getOverdueReceivableCount() > 0)
                .count();

        List<ProjectMetricCard> metrics = new ArrayList<>();
        metrics.add(new ProjectMetricCard("activeProject", "在建项目", BigDecimal.valueOf(active.size()), "count",
                suspended > 0 ? "danger" : "normal",
                "待开工 " + preparing + " · 施工中 " + ongoing));
        metrics.add(new ProjectMetricCard("activeContractAmount", "在建合同额", activeAmount, "amount", "normal",
                "全部项目合同额 ¥" + money(allAmount)));

        ProjectMetricCard receivedRate = new ProjectMetricCard("receivedRate", "回款率", BigDecimal.ZERO, "percent", "normal", null);
        if (active.isEmpty()) {
            receivedRate.setSubText("暂无在建项目");
        } else if (receivable.signum() == 0) {
            receivedRate.setStatus("attention");
            receivedRate.setSubText("尚未生成应收计划");
        } else {
            BigDecimal rate = percentage(received, receivable);
            receivedRate.setValue(rate);
            receivedRate.setStatus(overdueReceivables > 0 ? "danger"
                    : rate.compareTo(LOW_RECEIVED_RATE) < 0 ? "attention" : "normal");
            receivedRate.setSubText("已收 ¥" + money(received) + " / 应收 ¥" + money(receivable)
                    + (overdueReceivables > 0 ? " · 逾期 " + overdueReceivables + " 笔" : ""));
        }
        metrics.add(receivedRate);
        metrics.add(newSignedProjectMetric(projects, contracts, today));
        return metrics;
    }

    /** 第四张指标卡：本月新签项目（用户裁决 2026-09-21 替代风险项目）。 */
    private ProjectMetricCard newSignedProjectMetric(List<Project> projects,
                                                     Map<Long, ProjectContract> contracts,
                                                     LocalDate today) {
        YearMonth month = YearMonth.from(today);
        List<Project> signedThisMonth = projects.stream()
                .filter(project -> signedIn(project, contracts.get(project.getProjectId()), month))
                .toList();
        long previous = projects.stream()
                .filter(project -> signedIn(project, contracts.get(project.getProjectId()), month.minusMonths(1)))
                .count();
        BigDecimal signedAmount = signedThisMonth.stream()
                .map(item -> amountOf(item, contracts.get(item.getProjectId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProjectMetricCard card = new ProjectMetricCard("newSignedProject", "本月新签项目",
                BigDecimal.valueOf(signedThisMonth.size()), "count", "normal",
                "上月 " + previous + " 个 · 本月签约额 ¥" + money(signedAmount));
        card.setTrendRate(rate(signedThisMonth.size(), previous));
        return card;
    }

    private List<ProjectStatusCount> statusCounts(List<Project> projects) {
        Map<ProjectStatus, Long> counts = new LinkedHashMap<>();
        for (ProjectStatus value : ProjectStatus.values()) {
            counts.put(value, 0L);
        }
        projects.forEach(project -> counts.merge(project.getStatus(), 1L, Long::sum));
        List<ProjectStatusCount> statusCounts = new ArrayList<>();
        statusCounts.add(new ProjectStatusCount("all", "全部", projects.size()));
        statusCounts.add(new ProjectStatusCount("preparing", "待开工", counts.get(ProjectStatus.Preparing).intValue()));
        statusCounts.add(new ProjectStatusCount("ongoing", "施工中", counts.get(ProjectStatus.Ongoing).intValue()));
        statusCounts.add(new ProjectStatusCount("suspended", "停工", counts.get(ProjectStatus.Suspended).intValue()));
        statusCounts.add(new ProjectStatusCount("accepting", "待验收", counts.get(ProjectStatus.Accepting).intValue()));
        statusCounts.add(new ProjectStatusCount("finished", "已完工",
                counts.get(ProjectStatus.Finished).intValue() + counts.get(ProjectStatus.Settled).intValue()));
        return statusCounts;
    }

    /** 合同履约维度计数：全部 / 待签约 / 履约中 / 已完成。 */
    private List<ProjectStatusCount> contractCounts(List<Project> projects) {
        Map<ProjectContractStage, Long> counts = new LinkedHashMap<>();
        for (ProjectContractStage value : ProjectContractStage.values()) {
            counts.put(value, 0L);
        }
        projects.forEach(project -> counts.merge(
                project.getContractStage() == null ? ProjectContractStage.Pending : project.getContractStage(),
                1L, Long::sum));
        List<ProjectStatusCount> contractCounts = new ArrayList<>();
        contractCounts.add(new ProjectStatusCount("all", "全部", projects.size()));
        contractCounts.add(new ProjectStatusCount("pending", "待签约", counts.get(ProjectContractStage.Pending).intValue()));
        contractCounts.add(new ProjectStatusCount("active", "履约中", counts.get(ProjectContractStage.Active).intValue()));
        contractCounts.add(new ProjectStatusCount("completed", "已完成", counts.get(ProjectContractStage.Completed).intValue()));
        return contractCounts;
    }

    // ---------------------------------------------------------------- 明细来源

    private Map<Long, ProjectContract> contractsByProject(Long comId) {
        Map<Long, ProjectContract> contracts = new HashMap<>();
        for (ProjectContract contract : contractRepository.findByCompany_ComIdOrderByContractIdDesc(comId)) {
            if (contract.getProject() == null || contract.getProject().getProjectId() == null) {
                continue;
            }
            // 仓储已按 contractId 倒序，首次出现即该项目最新合同。
            contracts.putIfAbsent(contract.getProject().getProjectId(), contract);
        }
        return contracts;
    }

    private Map<Long, ReceivableRollup> receivableRollups(Long comId, LocalDate today) {
        Map<Long, ReceivableRollup> rollups = new HashMap<>();
        for (Receivable receivable : receivableRepository.findByCompany_ComIdOrderByDueDateAsc(comId)) {
            if (receivable.getProject() == null || receivable.getProject().getProjectId() == null) {
                continue;
            }
            boolean overdue = isOverdue(receivable, today);
            rollups.merge(receivable.getProject().getProjectId(),
                    new ReceivableRollup(value(receivable.getAmount()), value(receivable.getReceivedAmount()),
                            overdue ? 1 : 0),
                    ReceivableRollup::plus);
        }
        return rollups;
    }

    private BusinessAlertReport alertReport(Employee boss, LocalDate today) {
        BusinessAlertQuery query = new BusinessAlertQuery();
        query.setAsOf(today);
        return alertContext.report(boss, query);
    }

    private Map<Long, ProjectRisk> risks(BusinessAlertReport report) {
        Map<Long, ProjectRisk> grouped = new LinkedHashMap<>();
        if (report == null) {
            return grouped;
        }
        for (BusinessAlert alert : allAlerts(report)) {
            if (alert.getProject() == null || alert.getProject().getProjectId() == null) {
                continue;
            }
            grouped.computeIfAbsent(alert.getProject().getProjectId(), id -> new ProjectRisk()).add(alert);
        }
        return grouped;
    }

    private List<BusinessAlert> allAlerts(BusinessAlertReport report) {
        List<BusinessAlert> all = new ArrayList<>();
        add(all, report.getOverdueReceivables());
        add(all, report.getSuspensionRisks());
        add(all, report.getMaterialOverIssues());
        add(all, report.getChangeAnomalies());
        return all;
    }

    private void add(List<BusinessAlert> target, List<BusinessAlert> source) {
        if (source != null) {
            target.addAll(source);
        }
    }

    private String currentStageName(Project project) {
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        return stages.stream()
                .filter(stage -> stage.getStatus() == StageStatus.Doing || stage.getStatus() == StageStatus.Accepting)
                .findFirst()
                .or(() -> stages.stream().findFirst())
                .map(ProjectStage::getName)
                .orElse(null);
    }

    // ---------------------------------------------------------------- 分页与排序

    private List<Project> slice(ProjectOverviewReport report, List<Project> sorted,
                                Integer requestedCurrent, Integer requestedSize) {
        int current = requestedCurrent == null || requestedCurrent < 1 ? 1 : requestedCurrent;
        int size = requestedSize == null || requestedSize < 1 ? 20 : Math.min(requestedSize, 100);
        int from = Math.min((current - 1) * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        List<Project> page = sorted.subList(from, to);
        report.setProjects(page);
        report.setCurrent(current);
        report.setSize(size);
        report.setTotal((long) sorted.size());
        report.setHasMore(to < sorted.size());
        return page;
    }

    private Comparator<Project> comparator(String sort, Map<Long, ProjectContract> contracts) {
        if ("amount".equals(sort)) {
            return Comparator.comparing(
                    (Project item) -> amountOf(item, contracts.get(item.getProjectId())),
                    Comparator.reverseOrder());
        }
        if ("received".equals(sort)) {
            return Comparator.comparing(this::receivedRateForSort);
        }
        if ("due".equals(sort)) {
            return Comparator.comparing(Project::getPlanEndDate,
                    Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()));
        }
        if ("newest".equals(sort)) {
            return Comparator.comparing(Project::getProjectId, Comparator.reverseOrder());
        }
        return Comparator.comparingInt((Project item) -> riskRank(item.getRiskLevel()))
                .thenComparing(Project::getOverdueDays, Comparator.nullsLast(Comparator.<Integer>reverseOrder()))
                .thenComparing(Project::getProjectId, Comparator.nullsLast(Comparator.<Long>reverseOrder()));
    }

    private BigDecimal receivedRateForSort(Project project) {
        BigDecimal receivable = value(project.getReceivableAmount());
        if (receivable.signum() == 0) {
            return NO_PLAN_SORT_RATE;
        }
        return percentage(value(project.getReceivedAmount()), receivable);
    }

    private int riskRank(ProjectRiskLevel level) {
        if (level == ProjectRiskLevel.High) return 0;
        if (level == ProjectRiskLevel.Attention) return 1;
        return 2;
    }

    // ---------------------------------------------------------------- 过滤与解析

    /** 接受 UI key（ongoing）与枚举名/中文名；空值与 all 表示不过滤。 */
    private ProjectStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status.trim())) {
            return null;
        }
        String normalized = status.trim();
        for (ProjectStatus value : ProjectStatus.values()) {
            if (value.name().equalsIgnoreCase(normalized) || value.getName().equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    /** 接受 UI key（active）与枚举名/中文名；空值与 all 表示不过滤。 */
    private ProjectContractStage parseContractStage(String contractStage) {
        if (contractStage == null || contractStage.isBlank() || "all".equalsIgnoreCase(contractStage.trim())) {
            return null;
        }
        String normalized = contractStage.trim();
        for (ProjectContractStage value : ProjectContractStage.values()) {
            if (value.name().equalsIgnoreCase(normalized) || value.getName().equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    /** 「已完工」分段同时覆盖 Finished 与 Settled，与状态计数口径一致。 */
    private boolean matchesStatus(Project project, ProjectStatus status) {
        if (status == null) {
            return true;
        }
        if (status == ProjectStatus.Finished) {
            return project.getStatus() == ProjectStatus.Finished || project.getStatus() == ProjectStatus.Settled;
        }
        return project.getStatus() == status;
    }

    /**
     * 风险筛选：
     * <ul>
     *   <li>空值 / {@code all}：不过滤；</li>
     *   <li>{@code risk} / {@code abnormal}：只保留有风险项目（riskLevel != Normal），供「有风险」分段使用；</li>
     *   <li>其余按 {@link ProjectRiskLevel} 的枚举名或中文名精确匹配。</li>
     * </ul>
     */
    private ProjectRiskLevel parseRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank() || "all".equalsIgnoreCase(riskLevel.trim())
                || isRiskOnly(riskLevel)) {
            return null;
        }
        String normalized = riskLevel.trim();
        for (ProjectRiskLevel value : ProjectRiskLevel.values()) {
            if (value.name().equalsIgnoreCase(normalized) || value.getName().equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    private boolean isRiskOnly(String riskLevel) {
        if (riskLevel == null) {
            return false;
        }
        String normalized = riskLevel.trim();
        return "risk".equalsIgnoreCase(normalized) || "abnormal".equalsIgnoreCase(normalized);
    }

    private boolean matchesRiskLevel(Project project, ProjectRiskLevel level, boolean riskOnly) {
        ProjectRiskLevel current = project.getRiskLevel() == null ? ProjectRiskLevel.Normal : project.getRiskLevel();
        if (riskOnly) {
            return current != ProjectRiskLevel.Normal;
        }
        return level == null || current == level;
    }

    private boolean matchesContractStage(Project project, ProjectContractStage stage) {
        if (stage == null) {
            return true;
        }
        ProjectContractStage current = project.getContractStage() == null
                ? ProjectContractStage.Pending : project.getContractStage();
        return current == stage;
    }

    private boolean matchesStore(Project project, Long storeId) {
        if (storeId == null) {
            return true;
        }
        if (project.getCustomer() == null || project.getCustomer().getStore() == null) {
            return false;
        }
        return storeId.equals(project.getCustomer().getStore().getStoreId());
    }

    private boolean matchesKeyword(Project project, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        return contains(project.getName(), kw)
                || contains(project.getNo(), kw)
                || contains(project.getSiteAddr(), kw)
                || (project.getCustomer() != null && contains(project.getCustomer().getName(), kw))
                || (project.getProperty() != null && contains(project.getProperty().getCommunity(), kw));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    // ---------------------------------------------------------------- 基础判断

    private BigDecimal contractAmount(Project project, ProjectContract contract) {
        if (contract == null) {
            return value(project.getContractAmount());
        }
        BigDecimal current = value(contract.getCurrentAmount());
        if (current.signum() > 0) {
            return current;
        }
        BigDecimal baseline = value(contract.getBaselineAmount());
        return baseline.signum() > 0 ? baseline : value(project.getContractAmount());
    }

    private BigDecimal amountOf(Project project, ProjectContract contract) {
        return contractAmount(project, contract);
    }

    private boolean signedIn(Project project, ProjectContract contract, YearMonth month) {
        LocalDate signedAt = contract != null && contract.getSignedAt() != null
                ? contract.getSignedAt()
                : project.getCreateTime() == null ? null : project.getCreateTime().toLocalDate();
        return signedAt != null && YearMonth.from(signedAt).equals(month);
    }

    private boolean isActive(Project project) {
        return project.getStatus() == ProjectStatus.Preparing
                || project.getStatus() == ProjectStatus.Ongoing
                || project.getStatus() == ProjectStatus.Suspended
                || project.getStatus() == ProjectStatus.Accepting;
    }

    private int overdueDays(Project project, LocalDate today) {
        if (!isActive(project) || project.getPlanEndDate() == null
                || !project.getPlanEndDate().isBefore(today)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(project.getPlanEndDate(), today);
    }

    private boolean isOverdue(Receivable receivable, LocalDate today) {
        if (receivable.getStatus() == ReceivableStatus.Received
                || receivable.getStatus() == ReceivableStatus.Closed) {
            return false;
        }
        return receivable.getStatus() == ReceivableStatus.Overdue
                || (receivable.getDueDate() != null && receivable.getDueDate().isBefore(today));
    }

    private String defaultAction(Project project) {
        return switch (project.getStatus()) {
            case Preparing -> "确认开工准备与施工计划";
            case Ongoing -> "按计划推进当前施工阶段";
            case Suspended -> "核实停工原因，明确复工责任和日期";
            case Accepting -> "推进客户验收与整改闭环";
            case Finished -> "完成结算与尾款确认";
            case Settled -> "归档交付资料，维护老客户";
        };
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (value(denominator).signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value(numerator).multiply(BigDecimal.valueOf(100))
                .divide(denominator.abs(), 1, RoundingMode.HALF_UP);
    }

    /** 环比：上月为 0 且本月为 0 时返回 0；上月为 0 且本月有值时返回 null（不制造无穷增长）。 */
    private BigDecimal rate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? BigDecimal.ZERO : null;
        }
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return value(value).stripTrailingZeros().toPlainString();
    }

    /** 应收/已收按项目汇总结果。 */
    private record ReceivableRollup(BigDecimal amount, BigDecimal received, int overdueCount) {
        private static final ReceivableRollup EMPTY = new ReceivableRollup(BigDecimal.ZERO, BigDecimal.ZERO, 0);

        private ReceivableRollup plus(ReceivableRollup other) {
            return new ReceivableRollup(amount.add(other.amount), received.add(other.received),
                    overdueCount + other.overdueCount);
        }
    }

    /** 单项目风险聚合。 */
    private final class ProjectRisk {
        private int count;
        private BusinessAlert representative;

        private void add(BusinessAlert alert) {
            count++;
            if (representative == null
                    || rank(alert.getLevel()) < rank(representative.getLevel())
                    || (rank(alert.getLevel()) == rank(representative.getLevel())
                    && earlier(alert, representative))) {
                representative = alert;
            }
        }

        private boolean earlier(BusinessAlert candidate, BusinessAlert current) {
            if (candidate.getDueDate() == null) return false;
            if (current.getDueDate() == null) return true;
            return candidate.getDueDate().isBefore(current.getDueDate());
        }

        private int rank(BusinessAlertLevel level) {
            if (level == BusinessAlertLevel.High) return 0;
            if (level == BusinessAlertLevel.Medium) return 1;
            return 2;
        }

        private ProjectRiskLevel level() {
            if (representative.getLevel() == BusinessAlertLevel.High) return ProjectRiskLevel.High;
            return ProjectRiskLevel.Attention;
        }

        private ProjectRiskType type() {
            BusinessAlertType type = representative.getType();
            if (type == null) return ProjectRiskType.None;
            return switch (type) {
                case OverdueReceivable -> ProjectRiskType.OverdueReceivable;
                case ProjectSuspensionRisk -> ProjectRiskType.DeliveryRisk;
                case MaterialOverIssue -> ProjectRiskType.MaterialOverIssue;
                case AbnormalChangeRate -> ProjectRiskType.ChangeAnomaly;
            };
        }

        private int count() {
            return count;
        }

        private String reason() {
            return representative.getMessage();
        }

        private String nextAction() {
            return representative.getType() == null
                    ? "进入项目详情核实风险"
                    : representative.getType().nextAction();
        }
    }
}
