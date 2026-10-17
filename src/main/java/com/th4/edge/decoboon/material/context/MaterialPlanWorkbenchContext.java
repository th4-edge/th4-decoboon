package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbench;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbenchItem;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialSourceType;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 材料计划工作台聚合。
 *
 * <p>把"选材确认后的合同材料"与"增减项主材"统一收口到待下单清单；
 * 下单/到场进度由采购单实时汇总派生，不使用前端本地状态（可完成状态投影）。</p>
 */
@Note("材料计划工作台聚合")
@Component
public class MaterialPlanWorkbenchContext {

    private static final Set<PurchaseStatus> COUNTED = Set.of(
            PurchaseStatus.Approved, PurchaseStatus.Ordered,
            PurchaseStatus.PartiallyReceived, PurchaseStatus.Received);

    private final ProjectRepository projectRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;

    public MaterialPlanWorkbenchContext(ProjectRepository projectRepository,
                                        MaterialPlanRepository materialPlanRepository,
                                        PurchaseOrderRepository purchaseRepository,
                                        PurchaseOrderItemRepository purchaseItemRepository) {
        this.projectRepository = projectRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
    }

    /** 数据范围内可见的项目聚合；projectId 非空时只返回该项目。 */
    @Transactional(readOnly = true)
    public List<MaterialPlanWorkbench> summarize(Employee current, Long projectId, String group) {
        if (current == null) {
            throw new DefinedException(new DefaultErrorCode("401", "请先登录"));
        }
        List<Project> projects = new ArrayList<>();
        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问")));
            if (!visible(current, project)) {
                throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
            }
            projects.add(project);
        } else {
            projects.addAll(visibleProjects(current));
        }
        List<Long> projectIds = projects.stream()
                .map(Project::getProjectId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<MaterialPlan>> plansByProject = projectIds.isEmpty() ? Map.of()
                : materialPlanRepository.findByProject_ProjectIdIn(projectIds).stream()
                        .filter(plan -> plan.getProject() != null && plan.getProject().getProjectId() != null)
                        .collect(Collectors.groupingBy(plan -> plan.getProject().getProjectId()));
        // 一次性取回全部计划的采购明细与采购单，循环内不再查库（DEC-036 反模式 A2b）
        List<Long> planIds = plansByProject.values().stream().flatMap(List::stream)
                .map(MaterialPlan::getPlanId).filter(Objects::nonNull).distinct().toList();
        Map<Long, List<PurchaseOrderItem>> purchaseItemsByPlan = planIds.isEmpty() ? Map.of()
                : purchaseItemRepository.findByMaterialPlan_PlanIdIn(planIds).stream()
                        .filter(row -> row.getMaterialPlan() != null && row.getMaterialPlan().getPlanId() != null)
                        .collect(Collectors.groupingBy(row -> row.getMaterialPlan().getPlanId()));
        Map<Long, List<PurchaseOrder>> purchasesByPlan = planIds.isEmpty() ? Map.of()
                : purchaseRepository.findByMaterialPlan_PlanIdIn(planIds).stream()
                        .filter(order -> order.getMaterialPlan() != null && order.getMaterialPlan().getPlanId() != null)
                        .collect(Collectors.groupingBy(order -> order.getMaterialPlan().getPlanId()));
        return projects.stream()
                .map(project -> summarizeProject(project, group,
                        plansByProject.getOrDefault(project.getProjectId(), List.of()),
                        purchaseItemsByPlan, purchasesByPlan))
                .filter(workbench -> workbench.getTotalItems() > 0)
                .sorted(Comparator.comparing(MaterialPlanWorkbench::getProjectId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private MaterialPlanWorkbench summarizeProject(Project project, String group, List<MaterialPlan> plans,
                                                  Map<Long, List<PurchaseOrderItem>> purchaseItemsByPlan,
                                                  Map<Long, List<PurchaseOrder>> purchasesByPlan) {
        MaterialPlanWorkbench workbench = new MaterialPlanWorkbench();
        workbench.setProjectId(project.getProjectId());
        workbench.setProjectNo(project.getNo());
        workbench.setProjectName(project.getName());
        workbench.setProjectStatusName(project.getStatus() == null ? null : project.getStatus().getName());
        workbench.setPropertyAddress(project.getProperty() == null ? null : project.getProperty().getAddress());
        workbench.setCustomerName(project.getCustomer() == null ? null : project.getCustomer().getName());
        workbench.setDesignerName(project.getDesigner() == null ? null : project.getDesigner().getName());
        workbench.setManagerName(project.getManager() == null ? null : project.getManager().getName());

        List<MaterialPlanWorkbenchItem> items = new ArrayList<>();
        for (MaterialPlan plan : plans) {
            if (plan.getStatus() == MaterialPlanStatus.Closed || plan.getStatus() == MaterialPlanStatus.Cancelled) {
                continue;
            }
            MaterialPlanWorkbenchItem item = toItem(project, plan,
                    purchaseItemsByPlan.getOrDefault(plan.getPlanId(), List.of()),
                    purchasesByPlan.getOrDefault(plan.getPlanId(), List.of()));
            if (matchGroup(item, group)) {
                items.add(item);
            }
        }
        items.sort(Comparator.comparing(MaterialPlanWorkbenchItem::getPlanId,
                Comparator.nullsLast(Comparator.naturalOrder())));
        workbench.setItems(items);
        workbench.setTotalItems(items.size());
        workbench.setReadyToOrder((int) items.stream()
                .filter(item -> "ReadyToOrder".equals(item.getStatus())).count());
        workbench.setPartiallyOrdered((int) items.stream()
                .filter(item -> "PartiallyOrdered".equals(item.getStatus())).count());
        workbench.setPendingArrival((int) items.stream()
                .filter(item -> MaterialPlanStatus.Ordered.name().equals(item.getStatus())
                        || MaterialPlanStatus.PartiallyArrived.name().equals(item.getStatus())).count());
        workbench.setArrived((int) items.stream()
                .filter(item -> MaterialPlanStatus.Arrived.name().equals(item.getStatus())).count());
        workbench.setDelayed((int) items.stream().filter(MaterialPlanWorkbenchItem::isDelayed).count());
        return workbench;
    }

    private MaterialPlanWorkbenchItem toItem(Project project, MaterialPlan plan,
                                            List<PurchaseOrderItem> items,
                                            List<PurchaseOrder> purchases) {
        MaterialPlanWorkbenchItem item = new MaterialPlanWorkbenchItem();
        item.setPlanId(plan.getPlanId());
        item.setProjectId(project.getProjectId());
        item.setProjectName(project.getName());
        item.setPropertyAddress(project.getProperty() == null ? null : project.getProperty().getAddress());
        MaterialSourceType sourceType = plan.getSourceType();
        item.setSourceType(sourceType == null ? null : sourceType.name());
        item.setSourceTypeName(sourceType == null ? "合同选材" : sourceType.getName());
        if (plan.getContractLine() != null) {
            item.setSpaceName(plan.getContractLine().getSpaceName());
            item.setCategory(plan.getContractLine().getCategory());
        }
        item.setMaterialName(materialName(plan));
        item.setBrand(firstNonBlank(plan.getSelectedBrand(),
                plan.getContractLine() == null ? null : plan.getContractLine().getBrand()));
        item.setSpec(firstNonBlank(plan.getSelectedSpec(),
                plan.getContractLine() == null ? null : plan.getContractLine().getSpec()));
        item.setModel(plan.getSelectedModel());
        item.setUnit(plan.getUnit() == null ? null : plan.getUnit().getName());
        item.setNetQuantity(plan.getNetQuantity());
        item.setLossRate(plan.getLossRate());
        item.setLossQuantity(plan.getLossQuantity());
        item.setQuantity(plan.getQuantity());
        item.setRequiredAt(plan.getRequiredAt());
        item.setArrivedAt(plan.getArrivedAt());
        item.setStatus(plan.getStatus() == null ? null : plan.getStatus().name());
        item.setStatusName(plan.getStatus() == null ? null : plan.getStatus().getName());

        MaterialSelection selection = plan.getSelection();
        if (selection != null) {
            item.setSelectionId(selection.getSelectionId());
            item.setSelectionStatus(selection.getStatus() == null ? null : selection.getStatus().name());
            item.setSelectionStatusName(selection.getStatus() == null ? null : selection.getStatus().getName());
        }

        BigDecimal ordered;
        BigDecimal received;
        // 优先按采购明细汇总（支持按供应商合并下单与一条材料拆多供应商）
        if (!items.isEmpty()) {
            ordered = items.stream()
                    .filter(row -> row.getStatus() != null && COUNTED.contains(row.getStatus()))
                    .map(row -> value(row.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            received = items.stream()
                    .map(row -> value(row.getReceivedQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            ordered = purchases.stream()
                    .filter(order -> order.getStatus() != null && COUNTED.contains(order.getStatus()))
                    .map(order -> value(order.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            received = purchases.stream()
                    .map(order -> value(order.getReceivedQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        item.setOrderedQuantity(ordered);
        item.setReceivedQuantity(received);
        // 一条计划可拆多供应商；到场操作取最后一条有效采购明细，避免多明细采购单无法指定验收对象。
        items.stream()
                .filter(row -> row.getStatus() != null && COUNTED.contains(row.getStatus()))
                .filter(row -> row.getPurchaseOrder() != null)
                .max(Comparator.comparing(PurchaseOrderItem::getPurchaseItemId))
                .ifPresent(row -> {
                    PurchaseOrder order = row.getPurchaseOrder();
                    item.setPurchaseId(order.getPurchaseId());
                    item.setPurchaseItemId(row.getPurchaseItemId());
                    item.setPurchaseNo(order.getNo());
                    item.setSupplierName(order.getSupplierName());
                });
        if (item.getPurchaseId() == null) {
            purchases.stream()
                    .filter(order -> order.getStatus() != null && COUNTED.contains(order.getStatus()))
                    .max(Comparator.comparing(PurchaseOrder::getPurchaseId))
                    .ifPresent(order -> {
                        item.setPurchaseId(order.getPurchaseId());
                        item.setPurchaseNo(order.getNo());
                        item.setSupplierName(order.getSupplierName());
                    });
        }

        boolean arrivedDone = plan.getStatus() == MaterialPlanStatus.Arrived;
        boolean delayed = !arrivedDone && plan.getRequiredAt() != null
                && plan.getRequiredAt().isBefore(LocalDate.now());
        item.setDelayed(delayed);
        item.setNextAction(nextAction(plan.getStatus(), selection));
        return item;
    }

    private boolean matchGroup(MaterialPlanWorkbenchItem item, String group) {
        if (group == null || group.isBlank() || "all".equalsIgnoreCase(group)) {
            return true;
        }
        return switch (group) {
            case "ready" -> "ReadyToOrder".equals(item.getStatus());
            case "ordered" -> "Ordered".equals(item.getStatus()) || "PartiallyOrdered".equals(item.getStatus());
            case "arrival" -> "Ordered".equals(item.getStatus())
                    || "PartiallyArrived".equals(item.getStatus());
            case "delayed" -> item.isDelayed();
            default -> true;
        };
    }

    private String nextAction(MaterialPlanStatus status, MaterialSelection selection) {
        if (status == null) {
            return "确认材料状态";
        }
        return switch (status) {
            case Planned -> selection == null || selection.getStatus() != SelectionStatus.Selected
                    ? "待设计师选材" : "从主材作业单下单";
            case ReadyToOrder -> "从主材作业单下单";
            case Requested, Approved -> "从主材作业单下单";
            case PartiallyOrdered -> "继续下单尾量";
            case Ordered -> "待到货确认";
            case PartiallyArrived -> "待到货确认尾量";
            case Arrived -> "已完成";
            case Closed, Cancelled -> "已关闭";
        };
    }

    private String materialName(MaterialPlan plan) {
        if (plan.getCatalogItem() != null && plan.getCatalogItem().getName() != null) {
            return plan.getCatalogItem().getName();
        }
        if (plan.getContractLine() != null && plan.getContractLine().getName() != null) {
            return plan.getContractLine().getName();
        }
        return plan.getQuoteItem() == null ? null : plan.getQuoteItem().getName();
    }

    private List<Project> visibleProjects(Employee current) {
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        return switch (current.getRole()) {
            case Boss, Finance -> projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId);
            case StoreManager -> current.getStore() == null || current.getStore().getStoreId() == null
                    ? List.of()
                    : projectRepository.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(
                            comId, current.getStore().getStoreId());
            case TeamLeader -> current.getTeam() == null || current.getTeam().getTeamId() == null
                    ? List.of()
                    : projectRepository.findByCompany_ComIdAndCustomer_Team_TeamIdOrderByProjectIdDesc(
                            comId, current.getTeam().getTeamId());
            case Designer -> projectRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByProjectIdDesc(comId, empId);
            case Pm -> projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, empId);
            case Marketer -> projectRepository.findByCompany_ComIdAndQuote_CreatedBy_EmpIdOrderByProjectIdDesc(comId, empId);
            default -> List.of();
        };
    }

    private boolean visible(Employee current, Project project) {
        if (project == null || project.getCompany() == null || current.getCompany() == null) {
            return false;
        }
        if (!project.getCompany().getComId().equals(current.getCompany().getComId())) {
            return false;
        }
        return switch (current.getRole()) {
            case Boss, Finance -> true;
            case StoreManager -> project.getCustomer() != null && project.getCustomer().getStore() != null
                    && current.getStore() != null
                    && current.getStore().getStoreId().equals(project.getCustomer().getStore().getStoreId());
            case TeamLeader -> project.getCustomer() != null && project.getCustomer().getTeam() != null
                    && current.getTeam() != null
                    && current.getTeam().getTeamId().equals(project.getCustomer().getTeam().getTeamId());
            case Designer -> project.getDesigner() != null
                    && current.getEmpId().equals(project.getDesigner().getEmpId());
            case Pm -> project.getManager() != null
                    && current.getEmpId().equals(project.getManager().getEmpId());
            default -> false;
        };
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
