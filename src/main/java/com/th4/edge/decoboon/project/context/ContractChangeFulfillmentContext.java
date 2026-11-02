package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.common.support.CraftText;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.material.context.MaterialCategoryContext;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialSourceType;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.MaterialReturn;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户已确认合同变更到履约对象的展开上下文。
 *
 * <p>未开始/未采购范围直接调整；已经开始或已经采购的范围保留历史事实，
 * 通过差异任务、差异材料计划或现场动态记录新增影响。</p>
 */
@Note("合同变更履约联动")
@Component
public class ContractChangeFulfillmentContext {

    private final ContractChangeItemRepository itemRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectTaskRepository taskRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectPermissionContext permissionContext;
    private final MaterialPlanContext materialPlanContext;
    private final MaterialDeductionContext materialDeductionContext;
    private final MaterialCategoryContext materialCategoryContext;

    public ContractChangeFulfillmentContext(ContractChangeItemRepository itemRepository,
                                            ProjectStageRepository stageRepository,
                                            ProjectTaskRepository taskRepository,
                                            MaterialPlanRepository materialPlanRepository,
                                            ProjectReportRepository reportRepository,
                                            ProjectPermissionContext permissionContext,
                                            MaterialPlanContext materialPlanContext,
                                            MaterialDeductionContext materialDeductionContext,
                                            MaterialCategoryContext materialCategoryContext) {
        this.itemRepository = itemRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.reportRepository = reportRepository;
        this.permissionContext = permissionContext;
        this.materialPlanContext = materialPlanContext;
        this.materialDeductionContext = materialDeductionContext;
        this.materialCategoryContext = materialCategoryContext;
    }

    @Transactional
    public void apply(ContractChange change, Employee operator) {
        if (change == null || change.getProject() == null) {
            return;
        }
        List<ContractChangeItem> items = itemRepository
                .findByChange_ChangeIdOrderByChangeItemIdAsc(change.getChangeId());
        if (items.isEmpty()) {
            items = change.getItems() == null ? List.of() : change.getItems();
        }
        Project project = change.getProject();
        List<ProjectTask> tasks = new ArrayList<>(taskRepository.findByProject(project));
        List<MaterialPlan> materials = new ArrayList<>(materialPlanRepository.findByProject(project));
        Map<String, ProjectStage> stages = new LinkedHashMap<>();
        for (ProjectStage stage : stageRepository.findByProjectOrderBySort(project)) {
            stages.put(stage.getName(), stage);
        }

        for (ContractChangeItem item : items) {
            ItemType itemType = resolveItemType(item);
            if (itemType == ItemType.Construction) {
                applyConstruction(change, item, project, tasks, stages, operator);
            } else if (itemType == ItemType.Material) {
                applyMaterial(change, item, project, materials, operator);
            } else if (itemType == ItemType.Custom
                    || itemType == ItemType.Soft
                    || itemType == ItemType.Appliance) {
                applyConstruction(change, item, project, tasks, stages, operator);
                applyMaterial(change, item, project, materials, operator);
            } else if (itemType == ItemType.General) {
                applyConstruction(change, item, project, tasks, stages, operator);
            }
        }
    }

    private void applyConstruction(ContractChange change, ContractChangeItem item, Project project,
                                   List<ProjectTask> tasks, Map<String, ProjectStage> stages,
                                   Employee operator) {
        ProjectTask existing = findTask(tasks, item.getSourceLine());
        ContractChangeType type = item.getChangeType();
        if (type == ContractChangeType.Remove) {
            if (existing == null) {
                report(change, project, operator, item, "取消范围未找到原履约任务，已记录差异");
            } else if (existing.getStatus() == TaskStatus.Todo) {
                existing.setStatus(TaskStatus.Cancelled);
                existing.setProgress(0);
                taskRepository.save(existing);
            } else {
                report(change, project, operator, item, "任务已执行，保留原履约事实并记录取消差异");
            }
            return;
        }

        if (type == ContractChangeType.Replace) {
            if (existing != null && existing.getStatus() == TaskStatus.Todo) {
                existing.setStatus(TaskStatus.Cancelled);
                taskRepository.save(existing);
            } else if (existing != null) {
                report(change, project, operator, item, "原任务已执行，保留原履约事实并生成替代任务");
            }
            ProjectTask replacement = newTask(change, item, project, stages, operator);
            replacement.setQuantity(quantityForReplacement(item));
            replacement.setSalePrice(fullSaleAmount(item));
            replacement.setCost(fullCost(item));
            taskRepository.save(replacement);
            tasks.add(replacement);
            return;
        }

        if ((type == ContractChangeType.Quantity || type == ContractChangeType.Price)
                && existing != null && existing.getStatus() == TaskStatus.Todo) {
            if (item.getNewQuantity() != null && item.getNewQuantity().signum() >= 0) {
                existing.setQuantity(item.getNewQuantity());
            }
            existing.setSalePrice(value(existing.getSalePrice()).add(value(item.getAmountDelta())));
            existing.setCost(value(existing.getCost()).add(value(item.getCostDelta())));
            taskRepository.save(existing);
            return;
        }

        ProjectTask task = newTask(change, item, project, stages, operator);
        task.setQuantity(type == ContractChangeType.Add ? quantityForReplacement(item) : quantityDelta(item));
        task.setSalePrice(value(item.getAmountDelta()));
        task.setCost(value(item.getCostDelta()));
        taskRepository.save(task);
        tasks.add(task);
    }

    private ProjectTask newTask(ContractChange change, ContractChangeItem item, Project project,
                                Map<String, ProjectStage> stages, Employee operator) {
        ContractLine source = item.getSourceLine();
        String category = targetCategory(item);
        if (category == null || category.isBlank()) {
            category = source == null || source.getCategory() == null || source.getCategory().isBlank()
                    ? "合同变更" : source.getCategory();
        }
        ProjectStage stage = stages.get(category);
        if (stage == null) {
            stage = new ProjectStage();
            stage.setProject(project);
            stage.setProperty(project.getProperty());
            permissionContext.apply(stage, project);
            stage.setName(category);
            stage.setSort(stages.size());
            stage.setWeight(BigDecimal.ONE);
            stage.setStatus(StageStatus.Todo);
            stage.setProgress(0);
            stage.setPlannedStart(project.getStartDate());
            stage.setPlannedEnd(project.getPlanEndDate());
            stage = stageRepository.save(stage);
            stages.put(category, stage);
        }
        if (StageStatus.NotApplicable.equals(stage.getStatus())) {
            // DEC-057：增减项为「本项不涉及」阶段补回履约内容时，自动恢复为未开始。
            stage.setStatus(StageStatus.Todo);
            stage.setNotApplicableReason(null);
            stage.setNotApplicableBy(null);
            stage.setNotApplicableAt(null);
            stage = stageRepository.save(stage);
        }

        ProjectTask task = new ProjectTask();
        task.setProject(project);
        task.setProperty(project.getProperty());
        permissionContext.apply(task, project);
        task.setStage(stage);
        task.setContractLine(source);
        task.setChangeItem(item);
        task.setName(change.getNo() + " · " + item.getName());
        task.setZone(source == null ? null : source.getSpaceName());
        // 新增/替换优先使用目标报价项目快照；历史调量才回退原合同行快照。
        task.setCraft(item.getTargetCraft() != null ? item.getTargetCraft() : sourceCraft(source));
        task.setSpec(item.getTargetSpec() != null ? item.getTargetSpec() : source == null ? null : source.getSpec());
        task.setUnit(item.getTargetUnit() != null ? item.getTargetUnit() : source == null ? Unit.Area : source.getUnit());
        task.setStatus(TaskStatus.Todo);
        task.setProgress(0);
        task.setPriority("normal");
        task.setConceal(Boolean.FALSE);
        task.setCreator(operator);
        task.setOwner(project.getManager() == null ? project.getOwner() : project.getManager());
        task.setPlannedStart(project.getStartDate());
        task.setPlannedEnd(project.getPlanEndDate());
        return task;
    }

    private void applyMaterial(ContractChange change, ContractChangeItem item, Project project,
                               List<MaterialPlan> materials, Employee operator) {
        MaterialPlan existing = findMaterial(materials, item.getSourceLine());
        ContractChangeType type = item.getChangeType();
        if (type == ContractChangeType.Remove) {
            if (existing == null) {
                report(change, project, operator, item, "取消范围未找到原材料计划，已记录差异");
                return;
            }
            // DEC-MP-02：已到货走退货单、已下单调整采购单、未下单直接冲减（先冲损耗再冲净用量）
            BigDecimal reduceQuantity = value(existing.getQuantity());
            if (reduceQuantity.signum() <= 0) {
                reduceQuantity = value(item.getOldQuantity());
            }
            List<MaterialReturn> returns = materialDeductionContext.reduce(
                    existing, reduceQuantity, "合同变更减项", operator);
            if (!returns.isEmpty()) {
                report(change, project, operator, item,
                        "材料已到货，生成退货单并冲减应付与成本（共 " + returns.size() + " 张）");
            }
            return;
        }

        if (type == ContractChangeType.Replace) {
            if (existing != null && canAdjustMaterial(existing.getStatus())) {
                existing.setStatus(MaterialPlanStatus.Closed);
                materialPlanRepository.save(existing);
            } else if (existing != null) {
                report(change, project, operator, item, "材料已采购，保留原计划并生成替代材料需求");
            }
            MaterialPlan replacement = newMaterial(change, item, project, operator);
            replacement.setSourceType(MaterialSourceType.ChangeAdd);
            replacement.setQuantity(quantityForReplacement(item));
            replacement.setNetQuantity(replacement.getQuantity());
            replacement.setUnitPrice(unitPrice(item, null));
            replacement.setCost(fullCost(item));
            materialPlanContext.applyLossRate(replacement, categoryLoss(project, item));
            materialPlanContext.applyDerived(replacement);
            materialPlanRepository.save(replacement);
            materials.add(replacement);
            return;
        }

        if ((type == ContractChangeType.Quantity || type == ContractChangeType.Price) && existing != null) {
            if (item.getNewQuantity() != null && item.getNewQuantity().signum() >= 0) {
                BigDecimal current = value(existing.getQuantity());
                BigDecimal target = item.getNewQuantity();
                if (target.compareTo(current) < 0) {
                    // 减少数量：已下单/已到货部分按采购调整或退货冲减，未下单部分直接冲减
                    materialDeductionContext.reduce(existing, current.subtract(target),
                            "合同变更减少数量", operator);
                } else if (target.compareTo(current) > 0) {
                    existing.setNetQuantity(value(existing.getNetQuantity()).add(target.subtract(current)));
                    materialPlanContext.applyDerived(existing);
                    materialPlanRepository.save(existing);
                }
            }
            if (item.getNewPrice() != null && item.getNewPrice().signum() >= 0) {
                existing.setUnitPrice(item.getNewPrice());
            }
            existing.setCost(value(existing.getCost()).add(value(item.getCostDelta())));
            materialPlanContext.applyDerived(existing);
            materialPlanRepository.save(existing);
            return;
        }

        MaterialPlan plan = newMaterial(change, item, project, operator);
        plan.setSourceType(type == ContractChangeType.Remove
                ? MaterialSourceType.ChangeReduce : MaterialSourceType.ChangeAdd);
        plan.setQuantity(type == ContractChangeType.Add || type == ContractChangeType.Replace
                ? quantityForReplacement(item) : quantityDelta(item));
        plan.setNetQuantity(plan.getQuantity());
        plan.setUnitPrice(unitPrice(item, existing));
        plan.setCost(value(item.getCostDelta()));
        materialPlanContext.applyLossRate(plan, categoryLoss(project, item));
        materialPlanContext.applyDerived(plan);
        materialPlanRepository.save(plan);
        materials.add(plan);
    }

    /** 按变更来源的主材分类解析默认损耗率（DEC-MP-09）。 */
    private java.math.BigDecimal categoryLoss(Project project, ContractChangeItem item) {
        ContractLine source = item == null ? null : item.getSourceLine();
        String category = targetCategory(item);
        if (category == null || category.isBlank()) {
            category = source == null ? null : source.getCategory();
        }
        return materialCategoryContext.lossRate(project == null ? null : project.getCompany(), category);
    }

    private MaterialPlan newMaterial(ContractChange change, ContractChangeItem item, Project project,
                                     Employee operator) {
        ContractLine source = item.getSourceLine();
        MaterialPlan plan = new MaterialPlan();
        plan.setProject(project);
        plan.setProperty(project.getProperty());
        permissionContext.apply(plan, project);
        plan.setContract(change.getContract());
        plan.setContractLine(source);
        plan.setChangeItem(item);
        plan.setQuoteItem(source == null ? null : source.getSourceQuoteLine());
        plan.setCatalogItem(item.getTargetCatalogItem() != null
                ? item.getTargetCatalogItem()
                : source == null || source.getSourceQuoteLine() == null
                ? null : source.getSourceQuoteLine().getCatalogItem());
        plan.setUnit(item.getTargetUnit() != null ? item.getTargetUnit() : source == null ? Unit.Area : source.getUnit());
        plan.setSelectedBrand(item.getTargetBrand() != null ? item.getTargetBrand() : source == null ? null : source.getBrand());
        plan.setSelectedSpec(item.getTargetSpec() != null ? item.getTargetSpec() : source == null ? null : source.getSpec());
        plan.setSourceType(MaterialSourceType.ChangeAdd);
        plan.setStatus(MaterialPlanStatus.ReadyToOrder);
        plan.setRequiredAt(project.getStartDate() == null ? LocalDate.now() : project.getStartDate());
        return plan;
    }

    private ProjectTask findTask(List<ProjectTask> tasks, ContractLine source) {
        if (source == null || source.getLineId() == null) {
            return null;
        }
        return tasks.stream()
                .filter(task -> task.getContractLine() != null
                        && source.getLineId().equals(task.getContractLine().getLineId()))
                .filter(task -> task.getStatus() != TaskStatus.Cancelled)
                .findFirst()
                .orElse(null);
    }

    private MaterialPlan findMaterial(List<MaterialPlan> materials, ContractLine source) {
        if (source == null || source.getLineId() == null) {
            return null;
        }
        return materials.stream()
                .filter(plan -> plan.getContractLine() != null
                        && source.getLineId().equals(plan.getContractLine().getLineId()))
                .filter(plan -> plan.getStatus() != MaterialPlanStatus.Closed)
                .findFirst()
                .orElse(null);
    }

    private boolean canAdjustMaterial(MaterialPlanStatus status) {
        return status == MaterialPlanStatus.Planned
                || status == MaterialPlanStatus.ReadyToOrder
                || status == MaterialPlanStatus.Requested
                || status == MaterialPlanStatus.Approved;
    }

    private ItemType resolveItemType(ContractChangeItem item) {
        if (item.getTargetType() != null) {
            return item.getTargetType();
        }
        if (item.getItemType() != null) {
            return item.getItemType();
        }
        return item.getSourceLine() == null ? null : item.getSourceLine().getType();
    }

    private BigDecimal quantityForReplacement(ContractChangeItem item) {
        if (item.getTargetQuantity() != null && item.getTargetQuantity().signum() >= 0) {
            return item.getTargetQuantity();
        }
        if (item.getNewQuantity() != null && item.getNewQuantity().signum() >= 0) {
            return item.getNewQuantity();
        }
        if (item.getSourceLine() != null && item.getSourceLine().getQuantity() != null) {
            return item.getSourceLine().getQuantity();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal quantityDelta(ContractChangeItem item) {
        if (item.getNewQuantity() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal old = item.getOldQuantity() == null ? BigDecimal.ZERO : item.getOldQuantity();
        return item.getNewQuantity().subtract(old);
    }

    private BigDecimal fullSaleAmount(ContractChangeItem item) {
        if (item.getTargetAmount() != null) {
            return item.getTargetAmount();
        }
        BigDecimal quantity = quantityForReplacement(item);
        BigDecimal price = item.getNewPrice() == null
                ? (item.getSourceLine() == null ? BigDecimal.ZERO : unitPrice(item, null))
                : item.getNewPrice();
        return quantity.multiply(price);
    }

    private BigDecimal fullCost(ContractChangeItem item) {
        if (item.getTargetCostAmount() != null) {
            return item.getTargetCostAmount();
        }
        if (item.getSourceLine() != null && item.getSourceLine().getCost() != null) {
            return value(item.getSourceLine().getCost()).add(value(item.getCostDelta()));
        }
        return value(item.getCostDelta());
    }

    private BigDecimal unitPrice(ContractChangeItem item, MaterialPlan plan) {
        if (item.getNewPrice() != null) {
            return item.getNewPrice();
        }
        if (plan != null && plan.getUnitPrice() != null) {
            return plan.getUnitPrice();
        }
        ContractLine source = item.getSourceLine();
        if (source == null || source.getSourceQuoteLine() == null) {
            return BigDecimal.ZERO;
        }
        return value(source.getSourceQuoteLine().getPrice());
    }

    private String targetCategory(ContractChangeItem item) {
        return item == null ? null : item.getTargetCategory();
    }

    private String sourceCraft(ContractLine source) {
        QuoteLine craftSource = source == null ? null : source.getSourceQuoteLine();
        return craftSource == null ? null : CraftText.firstNonBlank(craftSource.getCraft(),
                craftSource.getCatalogItem() == null ? null : craftSource.getCatalogItem().getCraft());
    }

    private void report(ContractChange change, Project project, Employee operator,
                        ContractChangeItem item, String content) {
        ProjectReport report = new ProjectReport();
        report.setProject(project);
        permissionContext.apply(report, project);
        report.setProperty(change.getProperty());
        report.setChange(change);
        report.setType(ReportType.Notice);
        report.setTitle("合同变更差异 · " + item.getName());
        report.setContent(content);
        report.setAuthor(operator);
        report.setAuthorRole(operator.getRole());
        report.setVisibleToCustomer(true);
        reportRepository.save(report);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
