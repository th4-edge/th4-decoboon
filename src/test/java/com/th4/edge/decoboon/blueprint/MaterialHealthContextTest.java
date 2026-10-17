package com.th4.edge.decoboon.blueprint;

import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.context.MaterialHealthContext;
import com.th4.edge.decoboon.material.context.MaterialPlanWorkbenchContext;
import com.th4.edge.decoboon.material.model.MaterialHealthIssue;
import com.th4.edge.decoboon.material.model.MaterialHealthReport;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbench;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbenchItem;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialHealthContextTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final FinanceReportContext financeReportContext = mock(FinanceReportContext.class);
    private final MaterialPlanWorkbenchContext workbenchContext = mock(MaterialPlanWorkbenchContext.class);
    private final PurchaseOrderRepository purchaseOrderRepository = mock(PurchaseOrderRepository.class);
    private final MaterialHealthContext context = new MaterialHealthContext(
            tenantContext, financeReportContext, workbenchContext, purchaseOrderRepository);
    private final Employee finance = mock(Employee.class);

    @org.junit.jupiter.api.BeforeEach
    void hydrateEmployee() {
        when(tenantContext.requireEmployee(finance)).thenReturn(finance);
    }

    @Test
    void reportMustExposeSelectionAndCostBlocksWithRealCounts() {
        Project project = project();
        FinanceProjectReport financeReport = financeReport(project);
        financeReport.setCostComplete(false);
        financeReport.setCostIncompleteCount(2);
        financeReport.setMaterialCost(new BigDecimal("1200.00"));
        financeReport.setEstimatedCost(new BigDecimal("1100.00"));
        financeReport.setActualCost(new BigDecimal("900.00"));
        financeReport.setProfitAmount(new BigDecimal("9999.00"));
        when(financeReportContext.project(finance, 1L)).thenReturn(financeReport);

        MaterialPlanWorkbench workbench = new MaterialPlanWorkbench();
        workbench.setProjectId(1L);
        workbench.setProjectNo("P-001");
        workbench.setProjectName("示例项目");
        workbench.setTotalItems(2);
        workbench.setReadyToOrder(0);
        workbench.setPendingArrival(1);
        workbench.setDelayed(1);
        MaterialPlanWorkbenchItem unselected = item(101L, "客厅地砖", "Planned", null, true);
        MaterialPlanWorkbenchItem ordered = item(102L, "卫生间墙砖", "Ordered", SelectionStatus.Selected, false);
        workbench.setItems(List.of(unselected, ordered));
        when(workbenchContext.summarize(finance, 1L, null)).thenReturn(List.of(workbench));

        PurchaseOrder order = new PurchaseOrder();
        order.setStatus(PurchaseStatus.Ordered);
        order.setSupplierName(" ");
        order.setPriceState(SheetPriceState.Unpriced);
        when(purchaseOrderRepository.findByProjectOrderByPurchaseIdDesc(project)).thenReturn(List.of(order));

        MaterialHealthReport report = context.report(finance, 1L);

        assertThat(report.getStageKey()).isEqualTo("selection");
        assertThat(report.getSelectedItems()).isEqualTo(1);
        assertThat(report.getUnselectedItems()).isEqualTo(1);
        assertThat(report.getDelayed()).isEqualTo(1);
        assertThat(report.getMissingSupplierCount()).isEqualTo(1);
        assertThat(report.getUnpricedPurchaseCount()).isEqualTo(1);
        assertThat(report.isCostComplete()).isFalse();
        assertThat(report.getProfitAmount()).isNull();
        assertThat(report.getRows().get(0).isDelayed()).isTrue();
        assertThat(report.getIssues()).extracting(MaterialHealthIssue::getId)
                .containsExactly("CHK-UNSELECTED", "CHK-NO-SUPPLIER", "CHK-UNPRICED", "CHK-COST-INCOMPLETE");
    }

    @Test
    void reportMustUseSettlementStageWhenMaterialAndCostAreComplete() {
        Project project = project();
        FinanceProjectReport financeReport = financeReport(project);
        financeReport.setCostComplete(true);
        financeReport.setCostIncompleteCount(0);
        financeReport.setProfitAmount(new BigDecimal("300.00"));
        when(financeReportContext.project(finance, 1L)).thenReturn(financeReport);

        MaterialPlanWorkbench workbench = new MaterialPlanWorkbench();
        workbench.setProjectId(1L);
        workbench.setTotalItems(1);
        workbench.setArrived(1);
        workbench.setItems(List.of(item(201L, "瓷砖", "Arrived", SelectionStatus.Selected, false)));
        when(workbenchContext.summarize(finance, 1L, null)).thenReturn(List.of(workbench));
        when(purchaseOrderRepository.findByProjectOrderByPurchaseIdDesc(project)).thenReturn(List.of());

        MaterialHealthReport report = context.report(finance, 1L);

        assertThat(report.getStageKey()).isEqualTo("settlement");
        assertThat(report.isCostComplete()).isTrue();
        assertThat(report.getProfitAmount()).isEqualByComparingTo("300.00");
        assertThat(report.getIssues()).isEmpty();
    }

    @Test
    void reportMustNotPretendAnEmptyProjectIsComplete() {
        Project project = project();
        FinanceProjectReport financeReport = financeReport(project);
        when(financeReportContext.project(finance, 1L)).thenReturn(financeReport);
        when(workbenchContext.summarize(finance, 1L, null)).thenReturn(List.of());
        when(purchaseOrderRepository.findByProjectOrderByPurchaseIdDesc(project)).thenReturn(List.of());

        MaterialHealthReport report = context.report(finance, 1L);

        assertThat(report.isHasMaterialPlan()).isFalse();
        assertThat(report.getStageKey()).isEqualTo("material-plan");
        assertThat(report.getStageSummary()).contains("尚无可见材料计划");
    }

    private Project project() {
        Project project = new Project();
        project.setProjectId(1L);
        project.setNo("P-001");
        project.setName("示例项目");
        return project;
    }

    private FinanceProjectReport financeReport(Project project) {
        FinanceProjectReport report = new FinanceProjectReport();
        report.setProject(project);
        return report;
    }

    private MaterialPlanWorkbenchItem item(Long planId, String name, String status,
                                           SelectionStatus selectionStatus, boolean delayed) {
        MaterialPlanWorkbenchItem item = new MaterialPlanWorkbenchItem();
        item.setPlanId(planId);
        item.setMaterialName(name);
        item.setStatus(status);
        item.setSelectionStatus(selectionStatus == null ? null : selectionStatus.name());
        item.setSelectionStatusName(selectionStatus == null ? null : selectionStatus.getName());
        item.setDelayed(delayed);
        item.setQuantity(BigDecimal.TEN);
        item.setRequiredAt(delayed ? LocalDate.now().minusDays(3) : LocalDate.now().plusDays(3));
        item.setNextAction("待处理");
        return item;
    }
}
