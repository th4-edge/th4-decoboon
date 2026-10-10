package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.supply.configure.StockMovementType;
import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.decoboon.supply.repository.StockMovementRepository;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 经营预警规则计算。 */
@Component
public class BusinessAlertContext {

    private static final BigDecimal CHANGE_WARNING_RATE = BigDecimal.valueOf(10);
    private static final BigDecimal CHANGE_SERIOUS_RATE = BigDecimal.valueOf(20);
    private static final int LIST_LIMIT = 50;

    private final TenantContext tenantContext;
    private final ProjectRepository projectRepository;
    private final ProjectContractRepository contractRepository;
    private final ReceivableRepository receivableRepository;
    private final StockMovementRepository movementRepository;
    private final ContractChangeRepository changeRepository;

    public BusinessAlertContext(TenantContext tenantContext,
                                ProjectRepository projectRepository,
                                ProjectContractRepository contractRepository,
                                ReceivableRepository receivableRepository,
                                StockMovementRepository movementRepository,
                                ContractChangeRepository changeRepository) {
        this.tenantContext = tenantContext;
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
        this.receivableRepository = receivableRepository;
        this.movementRepository = movementRepository;
        this.changeRepository = changeRepository;
    }

    public BusinessAlertReport report(Employee principal, BusinessAlertQuery query) {
        Employee current = tenantContext.requireRole(principal,
                EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager, EmployeeRole.TeamLeader);
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        LocalDate asOf = query == null || query.getAsOf() == null ? LocalDate.now() : query.getAsOf();
        Long comId = current.getCompany().getComId();
        List<Project> projects = visibleProjects(current,
                projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId));
        Set<Long> projectIds = projects.stream().map(Project::getProjectId).collect(Collectors.toSet());

        List<BusinessAlert> overdue = overdueReceivables(
                receivableRepository.findByCompany_ComIdOrderByDueDateAsc(comId), projectIds, asOf);
        List<BusinessAlert> suspensions = suspensionRisks(projects, asOf);
        List<BusinessAlert> overIssues = materialOverIssues(
                movementRepository.findByCompany_ComIdOrderByMovementIdDesc(comId), projectIds);
        List<BusinessAlert> changeAnomalies = changeAnomalies(
                contractRepository.findByCompany_ComIdOrderByContractIdDesc(comId),
                changeRepository.findByCompany_ComIdOrderByChangeIdDesc(comId), projectIds);

        BusinessAlertReport report = new BusinessAlertReport();
        report.setAsOf(asOf);
        report.setOverdueReceivables(limit(overdue));
        report.setSuspensionRisks(limit(suspensions));
        report.setMaterialOverIssues(limit(overIssues));
        report.setChangeAnomalies(limit(changeAnomalies));

        List<BusinessAlert> all = new ArrayList<>();
        all.addAll(overdue);
        all.addAll(suspensions);
        all.addAll(overIssues);
        all.addAll(changeAnomalies);
        report.setTotalCount(all.size());
        report.setHighCount(count(all, BusinessAlertLevel.High));
        report.setMediumCount(count(all, BusinessAlertLevel.Medium));
        report.setLowCount(count(all, BusinessAlertLevel.Low));
        return report;
    }

    private List<BusinessAlert> overdueReceivables(List<Receivable> receivables,
                                                   Set<Long> projectIds,
                                                   LocalDate asOf) {
        return receivables.stream()
                .filter(receivable -> receivable.getProject() != null
                        && projectIds.contains(receivable.getProject().getProjectId()))
                .filter(receivable -> receivable.getStatus() != ReceivableStatus.Received
                        && receivable.getStatus() != ReceivableStatus.Closed)
                .filter(receivable -> receivable.getStatus() == ReceivableStatus.Overdue
                        || (receivable.getDueDate() != null && receivable.getDueDate().isBefore(asOf)))
                .map(receivable -> {
                    long overdueDays = receivable.getDueDate() == null ? 0
                            : Math.max(0, ChronoUnit.DAYS.between(receivable.getDueDate(), asOf));
                    BusinessAlert alert = base(BusinessAlertType.OverdueReceivable,
                            overdueDays >= 30 ? BusinessAlertLevel.High
                                    : overdueDays >= 7 ? BusinessAlertLevel.Medium : BusinessAlertLevel.Low,
                            receivable.getProject());
                    alert.setTitle("逾期应收");
                    alert.setBusinessType("Receivable");
                    alert.setBusinessId(receivable.getReceivableId());
                    alert.setBusinessNo(receivable.getName());
                    alert.setDueDate(receivable.getDueDate());
                    alert.setAmount(outstanding(receivable));
                    alert.setMessage(receivable.getName() + " 逾期 " + overdueDays + " 天，待收 "
                            + alert.getAmount().stripTrailingZeros().toPlainString() + " 元");
                    return alert;
                })
                .sorted(alertComparator())
                .toList();
    }

    private List<BusinessAlert> suspensionRisks(List<Project> projects, LocalDate asOf) {
        return projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Suspended
                        || isOverdueActiveProject(project, asOf))
                .map(project -> {
                    boolean suspended = project.getStatus() == ProjectStatus.Suspended;
                    BusinessAlert alert = base(BusinessAlertType.ProjectSuspensionRisk,
                            suspended ? BusinessAlertLevel.High : BusinessAlertLevel.Medium, project);
                    alert.setTitle(suspended ? "项目已停工" : "项目交付风险");
                    alert.setBusinessType("Project");
                    alert.setBusinessId(project.getProjectId());
                    alert.setBusinessNo(project.getNo());
                    alert.setDueDate(project.getPlanEndDate());
                    alert.setMessage(suspended
                            ? project.getName() + " 当前处于停工状态，请核查原因和恢复计划"
                            : project.getName() + " 已超过计划完工日期且尚未完工");
                    return alert;
                })
                .sorted(alertComparator())
                .toList();
    }

    private List<BusinessAlert> materialOverIssues(List<StockMovement> movements, Set<Long> projectIds) {
        Map<Long, List<StockMovement>> issueByPlan = movements.stream()
                .filter(movement -> movement.getType() == StockMovementType.Issue)
                .filter(movement -> movement.getProject() != null
                        && projectIds.contains(movement.getProject().getProjectId()))
                .filter(movement -> movement.getMaterialPlan() != null
                        && movement.getMaterialPlan().getPlanId() != null)
                .collect(Collectors.groupingBy(movement -> movement.getMaterialPlan().getPlanId()));

        return issueByPlan.values().stream()
                .filter(items -> !items.isEmpty())
                .map(items -> {
                    StockMovement first = items.get(0);
                    MaterialPlan plan = first.getMaterialPlan();
                    BigDecimal issued = items.stream().map(StockMovement::getQuantity)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal planned = value(plan.getQuantity());
                    BigDecimal overQuantity = issued.subtract(planned);
                    if (overQuantity.signum() <= 0) {
                        return null;
                    }
                    BigDecimal overRate = percentage(overQuantity, planned);
                    BigDecimal averageCost = averageUnitCost(items);
                    BusinessAlert alert = base(BusinessAlertType.MaterialOverIssue,
                            overRate.compareTo(BigDecimal.valueOf(20)) >= 0
                                    ? BusinessAlertLevel.High : BusinessAlertLevel.Medium,
                            first.getProject());
                    alert.setTitle("材料超领");
                    alert.setBusinessType("MaterialPlan");
                    alert.setBusinessId(plan.getPlanId());
                    alert.setBusinessNo(first.getSourceNo());
                    alert.setAmount(overQuantity.multiply(averageCost));
                    alert.setRate(overRate);
                    alert.setMessage(first.getProject().getName() + " 实际领料超出计划 "
                            + overQuantity.stripTrailingZeros().toPlainString() + "，超出比例 "
                            + overRate.stripTrailingZeros().toPlainString() + "%");
                    return alert;
                })
                .filter(Objects::nonNull)
                .sorted(alertComparator())
                .toList();
    }

    private BigDecimal averageUnitCost(List<StockMovement> movements) {
        List<StockMovement> costed = movements.stream()
                .filter(movement -> value(movement.getUnitCost()).signum() > 0)
                .toList();
        if (costed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return costed.stream().map(StockMovement::getUnitCost).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(costed.size()), 2, RoundingMode.HALF_UP);
    }

    private List<BusinessAlert> changeAnomalies(List<ProjectContract> contracts,
                                                List<ContractChange> changes,
                                                Set<Long> projectIds) {
        Map<Long, BigDecimal> amountByProject = changes.stream()
                .filter(change -> change.getStatus() == ContractChangeStatus.CustomerConfirmed
                        || change.getStatus() == ContractChangeStatus.Approved)
                .filter(change -> change.getProject() != null
                        && projectIds.contains(change.getProject().getProjectId()))
                .collect(Collectors.groupingBy(change -> change.getProject().getProjectId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                change -> value(change.getAmount()).abs(), BigDecimal::add)));

        return contracts.stream()
                .filter(contract -> contract.getProject() != null
                        && projectIds.contains(contract.getProject().getProjectId()))
                .map(contract -> {
                    BigDecimal changed = amountByProject.getOrDefault(contract.getProject().getProjectId(), BigDecimal.ZERO);
                    BigDecimal baseline = value(contract.getBaselineAmount());
                    BigDecimal rate = percentage(changed, baseline);
                    if (rate.compareTo(CHANGE_WARNING_RATE) < 0) {
                        return null;
                    }
                    BusinessAlert alert = base(BusinessAlertType.AbnormalChangeRate,
                            rate.compareTo(CHANGE_SERIOUS_RATE) >= 0
                                    ? BusinessAlertLevel.High : BusinessAlertLevel.Medium,
                            contract.getProject());
                    alert.setTitle("合同变更率异常");
                    alert.setBusinessType("ProjectContract");
                    alert.setBusinessId(contract.getContractId());
                    alert.setBusinessNo(contract.getNo());
                    alert.setAmount(changed);
                    alert.setRate(rate);
                    alert.setMessage(contract.getNo() + " 已生效变更率 "
                            + rate.stripTrailingZeros().toPlainString() + "%，请复核预算与客户确认");
                    return alert;
                })
                .filter(Objects::nonNull)
                .sorted(alertComparator())
                .toList();
    }

    private List<Project> visibleProjects(Employee current, List<Project> projects) {
        if (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance) {
            return projects;
        }
        if (current.getRole() == EmployeeRole.TeamLeader) {
            Long teamId = current.getTeam() == null ? null : current.getTeam().getTeamId();
            if (teamId == null) {
                return List.of();
            }
            return projects.stream()
                    .filter(project -> project.getTeam() != null
                            && teamId.equals(project.getTeam().getTeamId()))
                    .toList();
        }
        Long storeId = current.getStore() == null ? null : current.getStore().getStoreId();
        if (storeId == null) {
            return List.of();
        }
        return projects.stream()
                .filter(project -> project.getCustomer() != null && project.getCustomer().getStore() != null
                        && storeId.equals(project.getCustomer().getStore().getStoreId()))
                .toList();
    }

    private boolean isOverdueActiveProject(Project project, LocalDate asOf) {
        return project.getPlanEndDate() != null
                && project.getPlanEndDate().isBefore(asOf)
                && project.getStatus() != ProjectStatus.Finished
                && project.getStatus() != ProjectStatus.Settled;
    }

    private BusinessAlert base(BusinessAlertType type, BusinessAlertLevel level, Project project) {
        BusinessAlert alert = new BusinessAlert();
        alert.setType(type);
        alert.setLevel(level);
        alert.setProject(project);
        if (project != null) {
            alert.setOwner(project.getManager());
        }
        return alert;
    }

    private BigDecimal outstanding(Receivable receivable) {
        return value(receivable.getAmount()).subtract(value(receivable.getReceivedAmount())).max(BigDecimal.ZERO);
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator.abs(), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int count(List<BusinessAlert> alerts, BusinessAlertLevel level) {
        return (int) alerts.stream().filter(alert -> alert.getLevel() == level).count();
    }

    private List<BusinessAlert> limit(List<BusinessAlert> alerts) {
        return alerts.stream().limit(LIST_LIMIT).toList();
    }

    private Comparator<BusinessAlert> alertComparator() {
        return Comparator.comparingInt((BusinessAlert alert) -> switch (alert.getLevel()) {
                    case High -> 0;
                    case Medium -> 1;
                    case Low -> 2;
                })
                .thenComparing(BusinessAlert::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
