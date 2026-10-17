package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.model.MaterialHealthIssue;
import com.th4.edge.decoboon.material.model.MaterialHealthReport;
import com.th4.edge.decoboon.material.model.MaterialHealthRow;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbench;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbenchItem;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 项目材料业务体检聚合。
 *
 * <p>只消费既有领域事实：材料计划工作台负责计划/选材/下单/到场，财务项目报告负责成本完整性，
 * 采购单负责供应商和价格状态。页面不自行计算业务状态或金额。</p>
 */
@Note("项目材料业务体检聚合")
@Component
public class MaterialHealthContext {

    private static final Set<PurchaseStatus> ACTIVE_PURCHASES = Set.of(
            PurchaseStatus.Approved, PurchaseStatus.Ordered,
            PurchaseStatus.PartiallyReceived, PurchaseStatus.Received);
    private static final int ROW_LIMIT = 20;

    private final TenantContext tenantContext;
    private final FinanceReportContext financeReportContext;
    private final MaterialPlanWorkbenchContext workbenchContext;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public MaterialHealthContext(TenantContext tenantContext,
                                 FinanceReportContext financeReportContext,
                                 MaterialPlanWorkbenchContext workbenchContext,
                                 PurchaseOrderRepository purchaseOrderRepository) {
        this.tenantContext = tenantContext;
        this.financeReportContext = financeReportContext;
        this.workbenchContext = workbenchContext;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional(readOnly = true)
    public MaterialHealthReport report(Employee employee, Long projectId) {
        Employee current = tenantContext.requireEmployee(employee);
        FinanceProjectReport finance = financeReportContext.project(current, projectId);
        Project project = finance.getProject();
        MaterialPlanWorkbench workbench = workbenchContext.summarize(current, projectId, null).stream()
                .findFirst()
                .orElseGet(MaterialPlanWorkbench::new);
        List<PurchaseOrder> purchases = purchaseOrderRepository.findByProjectOrderByPurchaseIdDesc(project).stream()
                .filter(order -> order.getStatus() != null && ACTIVE_PURCHASES.contains(order.getStatus()))
                .toList();

        List<MaterialPlanWorkbenchItem> items = workbench.getItems() == null ? List.of() : workbench.getItems();
        int selected = (int) items.stream()
                .filter(item -> SelectionStatus.Selected.name().equals(item.getSelectionStatus()))
                .count();
        int partiallyArrived = (int) items.stream()
                .filter(item -> "PartiallyArrived".equals(item.getStatus()))
                .count();
        int planned = (int) items.stream()
                .filter(item -> "Planned".equals(item.getStatus()))
                .count();
        int missingSupplier = (int) purchases.stream()
                .filter(order -> order.getSupplierName() == null || order.getSupplierName().isBlank())
                .count();
        int unpriced = (int) purchases.stream()
                .filter(order -> order.getPriceState() == SheetPriceState.Unpriced
                        || order.getPriceState() == SheetPriceState.Mixed)
                .count();

        MaterialHealthReport report = new MaterialHealthReport();
        report.setProjectId(project.getProjectId());
        report.setProjectNo(firstNonBlank(workbench.getProjectNo(), project.getNo()));
        report.setProjectName(firstNonBlank(workbench.getProjectName(), project.getName()));
        report.setProjectStatusName(firstNonBlank(workbench.getProjectStatusName(),
                project.getStatus() == null ? null : project.getStatus().getName()));
        report.setPropertyAddress(firstNonBlank(workbench.getPropertyAddress(),
                project.getProperty() == null ? null : project.getProperty().getAddress()));
        report.setCustomerName(firstNonBlank(workbench.getCustomerName(),
                project.getCustomer() == null ? null : project.getCustomer().getName()));
        report.setDesignerName(firstNonBlank(workbench.getDesignerName(),
                project.getDesigner() == null ? null : project.getDesigner().getName()));
        report.setManagerName(firstNonBlank(workbench.getManagerName(),
                project.getManager() == null ? null : project.getManager().getName()));
        report.setHasMaterialPlan(workbench.getTotalItems() > 0);
        report.setTotalItems(workbench.getTotalItems());
        report.setSelectedItems(selected);
        report.setUnselectedItems(Math.max(0, workbench.getTotalItems() - selected));
        report.setReadyToOrder(workbench.getReadyToOrder());
        report.setPartiallyOrdered(workbench.getPartiallyOrdered());
        report.setPendingArrival(workbench.getPendingArrival());
        report.setPartiallyArrived(partiallyArrived);
        report.setArrived(workbench.getArrived());
        report.setDelayed(workbench.getDelayed());
        report.setPurchaseCount(purchases.size());
        report.setUnpricedPurchaseCount(unpriced);
        report.setMissingSupplierCount(missingSupplier);
        report.setCostComplete(Boolean.TRUE.equals(finance.getCostComplete()));
        report.setCostIncompleteCount(finance.getCostIncompleteCount() == null ? 0 : finance.getCostIncompleteCount());
        report.setMaterialCost(money(finance.getMaterialCost()));
        report.setEstimatedCost(money(finance.getEstimatedCost()));
        report.setActualCost(money(finance.getActualCost()));
        report.setProfitAmount(report.isCostComplete() ? finance.getProfitAmount() : null);
        report.setCostLabel(report.isCostComplete() ? "成本完整" : "成本未完整");
        Settlement settlement = finance.getSettlement();
        report.setSettlementStatus(settlement == null || settlement.getStatus() == null
                ? "未生成结算" : settlement.getStatus().getName());

        report.setIssues(issues(report, missingSupplier, unpriced));
        stage(report, planned);
        report.setRows(focusRows(items));
        return report;
    }

    private List<MaterialHealthIssue> issues(MaterialHealthReport report, int missingSupplier, int unpriced) {
        List<MaterialHealthIssue> issues = new ArrayList<>();
        if (report.getUnselectedItems() > 0) {
            issues.add(issue("CHK-UNSELECTED", "未完成选材", "P0", report.getUnselectedItems(),
                    "设计师完成选材并交付财务",
                    report.getUnselectedItems() + " 条计划行尚无 Selected 选材记录",
                    List.of("BR-009", "DEC-008", "DEC-056")));
        }
        if (missingSupplier > 0) {
            issues.add(issue("CHK-NO-SUPPLIER", "采购缺少供应商", "P0", missingSupplier,
                    "财务选择合作方档案中的供应商",
                    missingSupplier + " 张有效采购单未记录供应商",
                    List.of("SCN-005")));
        }
        if (unpriced > 0) {
            issues.add(issue("CHK-UNPRICED", "采购未定价", "P1", unpriced,
                    "等待结算或补结算价；成本保持不完整",
                    unpriced + " 张有效采购单为未定价或混合价格状态",
                    List.of("BR-045", "DEC-072")));
        }
        if (!report.isCostComplete()) {
            issues.add(issue("CHK-COST-INCOMPLETE", "成本不完整", "P1",
                    Math.max(1, report.getCostIncompleteCount()),
                    "完成采购结算或补齐成本；利润保持为空",
                    report.getCostIncompleteCount() + " 项成本未完整",
                    List.of("BR-045", "MET-002")));
        }
        return issues;
    }

    private void stage(MaterialHealthReport report, int planned) {
        if (!report.isHasMaterialPlan()) {
            report.setStageKey("material-plan");
            report.setStageLabel("材料计划");
            report.setStageSummary("当前项目尚无可见材料计划，无法确认材料履约是否已开始。");
            report.setNextAction("核对合同材料行或变更是否已生成材料计划");
            return;
        }
        if (report.getUnselectedItems() > 0) {
            report.setStageKey("selection");
            report.setStageLabel("选材 / 现场确认");
            report.setStageSummary("仍有材料计划行未完成选材，尚不能进入财务下单。");
            report.setNextAction("设计师完成选材并交付财务");
            return;
        }
        boolean waitingPurchase = report.getReadyToOrder() > 0 || report.getPartiallyOrdered() > 0
                || planned > 0 || report.getMissingSupplierCount() > 0
                || (report.getUnpricedPurchaseCount() > 0 && report.getPendingArrival() == 0
                    && report.getPartiallyArrived() == 0 && report.getArrived() == 0);
        if (waitingPurchase) {
            report.setStageKey("purchase-order");
            report.setStageLabel("财务核价 / 下单");
            report.setStageSummary("选材已完成，材料仍在待下单、部分下单或价格待收敛阶段。");
            report.setNextAction("财务核价、选择供应商并完成下单");
            return;
        }
        if (report.getPendingArrival() > 0 || report.getPartiallyArrived() > 0 || report.getDelayed() > 0) {
            report.setStageKey("receipt");
            report.setStageLabel("到场确认");
            report.setStageSummary("采购已进入履约，仍需确认剩余到场数量或逾期风险。");
            report.setNextAction("项目经理/设计师确认到场，财务回填账期");
            return;
        }
        if (!report.isCostComplete()) {
            report.setStageKey("settlement");
            report.setStageLabel("结算 / 成本收敛");
            report.setStageSummary("材料已到场，但仍有成本未完整，利润不可确认。");
            report.setNextAction("完成采购结算或补齐成本");
            return;
        }
        report.setStageKey("settlement");
        report.setStageLabel("结算 / 成本收敛");
        report.setStageSummary("材料已到场且成本完整，可进入结算核对与项目收口。");
        report.setNextAction("核对结算金额与项目收口");
    }

    private List<MaterialHealthRow> focusRows(List<MaterialPlanWorkbenchItem> items) {
        return items.stream()
                .sorted(Comparator.comparingInt(this::priority)
                        .thenComparing(item -> item.getRequiredAt() == null ? LocalDate.MAX : item.getRequiredAt())
                        .thenComparing(item -> item.getPlanId() == null ? Long.MAX_VALUE : item.getPlanId()))
                .limit(ROW_LIMIT)
                .map(this::row)
                .toList();
    }

    private int priority(MaterialPlanWorkbenchItem item) {
        if (item.isDelayed()) return 0;
        if (!SelectionStatus.Selected.name().equals(item.getSelectionStatus())) return 1;
        if ("Planned".equals(item.getStatus())) return 2;
        if ("ReadyToOrder".equals(item.getStatus()) || "Requested".equals(item.getStatus())
                || "Approved".equals(item.getStatus())) return 3;
        if ("PartiallyOrdered".equals(item.getStatus())) return 4;
        if ("Ordered".equals(item.getStatus())) return 5;
        if ("PartiallyArrived".equals(item.getStatus())) return 6;
        return 7;
    }

    private MaterialHealthRow row(MaterialPlanWorkbenchItem item) {
        MaterialHealthRow row = new MaterialHealthRow();
        row.setPlanId(item.getPlanId());
        row.setSpaceName(item.getSpaceName());
        row.setCategory(item.getCategory());
        row.setMaterialName(item.getMaterialName());
        row.setStatus(item.getStatus());
        row.setStatusName(item.getStatusName());
        row.setSelectionStatus(item.getSelectionStatus());
        row.setSelectionStatusName(item.getSelectionStatusName());
        row.setQuantity(item.getQuantity());
        row.setOrderedQuantity(item.getOrderedQuantity());
        row.setReceivedQuantity(item.getReceivedQuantity());
        row.setRequiredAt(item.getRequiredAt());
        row.setSupplierName(item.getSupplierName());
        row.setPurchaseNo(item.getPurchaseNo());
        row.setDelayed(item.isDelayed());
        row.setNextAction(item.getNextAction());
        return row;
    }

    private MaterialHealthIssue issue(String id, String label, String severity, int count,
                                      String nextAction, String evidence, List<String> refs) {
        MaterialHealthIssue issue = new MaterialHealthIssue();
        issue.setId(id);
        issue.setLabel(label);
        issue.setSeverity(severity);
        issue.setCount(count);
        issue.setNextAction(nextAction);
        issue.setEvidence(evidence);
        issue.setRefs(refs);
        return issue;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
