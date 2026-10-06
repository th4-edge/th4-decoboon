package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 手工记账边界与项目口径：项目零星成本、关联项目其他收入。 */
@Component
public class ManualLedgerContext {

    private static final List<String> INCOME_FORBIDDEN = List.of(
            "工程款", "首期款", "进度款", "尾款", "质保金", "增项款", "合同款", "客户工程款", "工程收款");
    private static final List<String> MATERIAL_FORBIDDEN = List.of(
            "材料款", "材料采购", "主材采购", "辅材采购", "五金采购", "供应商款", "材料货款");
    private static final List<String> SALARY_FORBIDDEN = List.of(
            "工人工资", "工资", "班组结算", "项目经理费用");
    private static final List<String> PROJECT_FORBIDDEN = List.of(
            "项目款", "分包款", "供应商工程款");

    private final LedgerRepository ledgerRepository;

    public ManualLedgerContext(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /** 手工账务用途缺省值：历史客户端未传时按“其他”处理，保持向后兼容。 */
    public LedgerManualPurpose purposeOrDefault(LedgerManualPurpose purpose) {
        return purpose == null ? LedgerManualPurpose.Other : purpose;
    }

    /** 用途约束：项目零星成本必须关联项目。 */
    public void validatePurpose(LedgerManualPurpose purpose, Project project) {
        if (purpose == LedgerManualPurpose.ProjectIncidentalCost && project == null) {
            throw new DefinedException(new DefaultErrorCode("400", "项目零星成本必须关联项目"));
        }
    }

    /** 禁止在手工记账登记已有应收/应付闭环的业务类型。 */
    public void validateAllowed(LedgerDirection direction, String category, String summary) {
        String text = ((category == null ? "" : category) + " " + (summary == null ? "" : summary))
                .toLowerCase();
        if (direction == LedgerDirection.Income && containsAny(text, INCOME_FORBIDDEN)) {
            throw guide("工程款等客户应收款项请通过「应收计划 → 收款」登记，手工记账只用于其他账务");
        }
        if (direction == LedgerDirection.Expense && containsAny(text, MATERIAL_FORBIDDEN)) {
            throw guide("材料款、供应商款请通过采购到货形成应付后，在「应付/付款」登记");
        }
        if (direction == LedgerDirection.Expense && containsAny(text, SALARY_FORBIDDEN)) {
            throw guide("工人工资、班组结算请通过工资应付和发放登记");
        }
        if (direction == LedgerDirection.Expense && containsAny(text, PROJECT_FORBIDDEN)) {
            throw guide("项目款、分包款请通过项目应付和付款登记");
        }
    }

    /** 项目口径：项目零星成本计入项目实际成本；关联项目的其他收入计入项目收入。 */
    /**
     * 批量计算多项目的「关联项目其他收入 / 项目零星成本」。
     * 过滤口径与 {@link #amounts(Project)} 完全一致，但改为一次取多项目流水后在内存分组。
     */
    public java.util.Map<Long, ManualAmounts> amountsAll(java.util.Collection<Project> projects) {
        java.util.List<Project> list = projects == null ? java.util.List.of()
                : projects.stream().filter(item -> item != null && item.getProjectId() != null
                        && item.getCompany() != null && item.getCompany().getComId() != null).toList();
        if (list.isEmpty()) {
            return java.util.Map.of();
        }
        Long comId = list.get(0).getCompany().getComId();
        java.util.List<Long> projectIds = list.stream().map(Project::getProjectId).distinct().toList();
        java.util.Map<Long, java.util.List<Ledger>> byProject = ledgerRepository
                .findByCompany_ComIdAndProject_ProjectIdIn(comId, projectIds).stream()
                .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                .collect(java.util.stream.Collectors.groupingBy(item -> item.getProject().getProjectId()));
        java.util.Map<Long, ManualAmounts> result = new java.util.LinkedHashMap<>();
        for (Project project : list) {
            result.put(project.getProjectId(), sum(byProject.getOrDefault(project.getProjectId(), java.util.List.of())));
        }
        return result;
    }

    private ManualAmounts sum(java.util.List<Ledger> ledgers) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (Ledger ledger : ledgers) {
            if (Boolean.TRUE.equals(ledger.getVoided())
                    || ledger.getSource() != LedgerSource.Manual
                    || ledger.getPurpose() == null) {
                continue;
            }
            BigDecimal amount = ledger.getAmount() == null ? BigDecimal.ZERO : ledger.getAmount();
            if (ledger.getDirection() == LedgerDirection.Income
                    && ledger.getPurpose() == LedgerManualPurpose.OtherIncome) {
                income = income.add(amount);
            } else if (ledger.getDirection() == LedgerDirection.Expense
                    && ledger.getPurpose() == LedgerManualPurpose.ProjectIncidentalCost) {
                cost = cost.add(amount);
            }
        }
        return new ManualAmounts(income, cost);
    }

    public ManualAmounts amounts(Project project) {
        if (project == null || project.getProjectId() == null || project.getCompany() == null) {
            return ManualAmounts.ZERO;
        }
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        List<Ledger> ledgers = ledgerRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(
                        project.getCompany().getComId(), project.getProjectId());
        if (ledgers == null) return ManualAmounts.ZERO;
        for (Ledger ledger : ledgers) {
            if (Boolean.TRUE.equals(ledger.getVoided())
                    || ledger.getSource() != LedgerSource.Manual
                    || ledger.getPurpose() == null) {
                continue;
            }
            BigDecimal amount = ledger.getAmount() == null ? BigDecimal.ZERO : ledger.getAmount();
            if (ledger.getDirection() == LedgerDirection.Income
                    && ledger.getPurpose() == LedgerManualPurpose.OtherIncome) {
                income = income.add(amount);
            } else if (ledger.getDirection() == LedgerDirection.Expense
                    && ledger.getPurpose() == LedgerManualPurpose.ProjectIncidentalCost) {
                cost = cost.add(amount);
            }
        }
        return new ManualAmounts(income, cost);
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private DefinedException guide(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }

    public record ManualAmounts(BigDecimal income, BigDecimal cost) {
        public static final ManualAmounts ZERO = new ManualAmounts(BigDecimal.ZERO, BigDecimal.ZERO);

        public ManualAmounts {
            income = income == null ? BigDecimal.ZERO : income;
            cost = cost == null ? BigDecimal.ZERO : cost;
        }
    }
}
