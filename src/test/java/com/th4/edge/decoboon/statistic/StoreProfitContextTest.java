package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.statistic.context.StoreProfitContext;
import com.th4.edge.decoboon.statistic.model.StoreProfitReport;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** MET-011 门店经营利润聚合单元测试。 */
class StoreProfitContextTest {

    private ProjectRepository projectRepository;
    private ProjectCostContext projectCostContext;
    private LedgerRepository ledgerRepository;
    private StoreRepository storeRepository;
    private StoreProfitContext context;
    private Employee storeManager;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        projectCostContext = mock(ProjectCostContext.class);
        ledgerRepository = mock(LedgerRepository.class);
        storeRepository = mock(StoreRepository.class);
        context = new StoreProfitContext(projectRepository, projectCostContext, ledgerRepository, storeRepository);

        Company company = new Company();
        company.setComId(10L);
        Store store = new Store();
        store.setStoreId(20L);
        store.setName("滨湖店");
        store.setCompany(company);
        storeManager = new Employee();
        storeManager.setEmpId(1L);
        storeManager.setRole(EmployeeRole.StoreManager);
        storeManager.setCompany(company);
        storeManager.setStore(store);

        when(storeRepository.findByStoreId(20L)).thenReturn(store);
    }

    @Test
    void shouldCalculateStoreProfitWhenCostIsComplete() {
        Project project = project(100L, "300000.00");
        when(projectRepository.findByCompany_ComIdAndStore_StoreId(10L, 20L)).thenReturn(List.of(project));
        when(projectCostContext.summarizeAll(List.of(project))).thenReturn(Map.of(
                100L, new ProjectCostContext.CostSummary(new BigDecimal("200000.00"), new BigDecimal("180000.00"), 0)));
        when(ledgerRepository.sumStoreLedger(10L, 20L, LedgerDirection.Income, LedgerManualPurpose.OtherIncome))
                .thenReturn(new BigDecimal("5000.00"));
        when(ledgerRepository.sumStoreLedger(10L, 20L, LedgerDirection.Expense, LedgerManualPurpose.CompanyExpense))
                .thenReturn(new BigDecimal("12000.00"));

        StoreProfitReport report = context.calculate(storeManager, null);

        assertThat(report.getStoreId()).isEqualTo(20L);
        assertThat(report.getStoreName()).isEqualTo("滨湖店");
        assertThat(report.getCostComplete()).isTrue();
        assertThat(report.getProjectGrossProfit()).isEqualByComparingTo("120000.00");
        assertThat(report.getOtherIncome()).isEqualByComparingTo("5000.00");
        assertThat(report.getDirectCompanyExpense()).isEqualByComparingTo("12000.00");
        assertThat(report.getStoreProfit()).isEqualByComparingTo("113000.00");
        assertThat(report.getProfitWarning()).isNull();
    }

    @Test
    void shouldReturnNullProfitWhenAnyProjectCostIsIncomplete() {
        Project project = project(100L, "300000.00");
        when(projectRepository.findByCompany_ComIdAndStore_StoreId(10L, 20L)).thenReturn(List.of(project));
        when(projectCostContext.summarizeAll(List.of(project))).thenReturn(Map.of(
                100L, new ProjectCostContext.CostSummary(new BigDecimal("200000.00"), new BigDecimal("180000.00"), 1)));
        when(ledgerRepository.sumStoreLedger(10L, 20L, LedgerDirection.Income, LedgerManualPurpose.OtherIncome))
                .thenReturn(new BigDecimal("5000.00"));
        when(ledgerRepository.sumStoreLedger(10L, 20L, LedgerDirection.Expense, LedgerManualPurpose.CompanyExpense))
                .thenReturn(new BigDecimal("12000.00"));

        StoreProfitReport report = context.calculate(storeManager, null);

        assertThat(report.getCostComplete()).isFalse();
        assertThat(report.getStoreProfit()).isNull();
        assertThat(report.getProjectGrossProfit()).isNull();
        assertThat(report.getProfitWarning()).isEqualTo("成本未完整");
    }

    private Project project(Long id, String contractAmount) {
        Project project = new Project();
        project.setProjectId(id);
        project.setContractAmount(new BigDecimal(contractAmount));
        project.setStore(storeManager.getStore());
        return project;
    }
}
