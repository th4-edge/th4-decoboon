package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.statistic.model.StoreProfitReport;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 门店经营利润（参考）聚合（MET-011 草案）。
 *
 * <p>门店项目毛利 + 门店非项目其他收入 − 门店直接归属企业费用。
 * 任一项目成本不完整时利润返回 null；未分摊总部/公共费用不强行分摊。</p>
 */
@Note("门店经营利润聚合")
@Component
public class StoreProfitContext {

    private final ProjectRepository projectRepository;
    private final ProjectCostContext projectCostContext;
    private final LedgerRepository ledgerRepository;
    private final StoreRepository storeRepository;

    public StoreProfitContext(ProjectRepository projectRepository,
                              ProjectCostContext projectCostContext,
                              LedgerRepository ledgerRepository,
                              StoreRepository storeRepository) {
        this.projectRepository = projectRepository;
        this.projectCostContext = projectCostContext;
        this.ledgerRepository = ledgerRepository;
        this.storeRepository = storeRepository;
    }

    public StoreProfitReport calculate(Employee current, Long requestedStoreId) {
        Store store = resolveStore(current, requestedStoreId);
        Long comId = current.getCompany().getComId();
        Long storeId = store.getStoreId();

        List<Project> projects = projectRepository.findByCompany_ComIdAndStore_StoreId(comId, storeId);
        Map<Long, ProjectCostContext.CostSummary> costs = projectCostContext.summarizeAll(projects);
        BigDecimal projectGrossProfit = BigDecimal.ZERO;
        boolean complete = true;
        for (Project project : projects) {
            ProjectCostContext.CostSummary cost = costs.get(project.getProjectId());
            if (cost == null || !cost.complete() || project.getContractAmount() == null) {
                complete = false;
                break;
            }
            projectGrossProfit = projectGrossProfit.add(
                    project.getContractAmount().subtract(cost.actualCost()));
        }

        BigDecimal otherIncome = ledgerRepository.sumStoreLedger(
                comId, storeId, LedgerDirection.Income, LedgerManualPurpose.OtherIncome);
        BigDecimal directCompanyExpense = ledgerRepository.sumStoreLedger(
                comId, storeId, LedgerDirection.Expense, LedgerManualPurpose.CompanyExpense);

        StoreProfitReport report = new StoreProfitReport();
        report.setStoreId(storeId);
        report.setStoreName(store.getName());
        report.setPeriodLabel("累计");
        report.setOtherIncome(money(otherIncome));
        report.setDirectCompanyExpense(money(directCompanyExpense));
        report.setCostComplete(complete);
        if (complete) {
            report.setProjectGrossProfit(money(projectGrossProfit));
            report.setStoreProfit(money(projectGrossProfit.add(otherIncome).subtract(directCompanyExpense)));
        } else {
            report.setProjectGrossProfit(null);
            report.setStoreProfit(null);
            report.setProfitWarning("成本未完整");
        }
        return report;
    }

    private Store resolveStore(Employee current, Long requestedStoreId) {
        Long storeId;
        if (current.getRole() == EmployeeRole.Boss) {
            if (requestedStoreId == null || requestedStoreId <= 0) {
                throw new DefinedException(new DefaultErrorCode("400", "企业负责人查看门店利润需指定门店"));
            }
            storeId = requestedStoreId;
        } else {
            Store own = current.getStore();
            if (own == null || own.getStoreId() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "当前账号未归属门店"));
            }
            storeId = own.getStoreId();
        }
        Store store = storeRepository.findByStoreId(storeId);
        if (store == null || store.getCompany() == null
                || !store.getCompany().getComId().equals(current.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("404", "门店不存在或无权访问"));
        }
        return store;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
