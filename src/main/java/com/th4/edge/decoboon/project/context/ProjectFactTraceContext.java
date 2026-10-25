package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.common.configure.FactVisibility;
import com.th4.edge.decoboon.common.model.BusinessFactContract;
import com.th4.edge.decoboon.common.model.BusinessFactNode;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.GoodsReceipt;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectFactTrace;
import com.th4.edge.decoboon.project.model.ProjectFactTraceContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.GoodsReceiptRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 项目 PHASE 事实追踪的只读聚合适配。 */
@Note("项目事实追踪上下文")
@Component
public class ProjectFactTraceContext {

    private final ProjectContractRepository contractRepository;
    private final DesignRepository designRepository;
    private final QuoteRepository quoteRepository;
    private final MeasureRepository measureRepository;
    private final MeasureRegionRepository measureRegionRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectReportRepository reportRepository;
    private final MaterialSelectionRepository selectionRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final ContractChangeRepository changeRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final ReceivableRepository receivableRepository;
    private final ReceiptRepository receiptRepository;
    private final PayableRepository payableRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final SettlementRepository settlementRepository;
    private final ProjectCostContext costContext;

    public ProjectFactTraceContext(ProjectContractRepository contractRepository,
                                   DesignRepository designRepository,
                                   QuoteRepository quoteRepository,
                                   MeasureRepository measureRepository,
                                   MeasureRegionRepository measureRegionRepository,
                                   ProjectStageRepository stageRepository,
                                   ProjectTaskRepository taskRepository,
                                   ProjectReportRepository reportRepository,
                                   MaterialSelectionRepository selectionRepository,
                                   MaterialPlanRepository materialPlanRepository,
                                   PurchaseOrderRepository purchaseRepository,
                                   GoodsReceiptRepository goodsReceiptRepository,
                                   ContractChangeRepository changeRepository,
                                   AcceptanceRepository acceptanceRepository,
                                   ReceivableRepository receivableRepository,
                                   ReceiptRepository receiptRepository,
                                   PayableRepository payableRepository,
                                   PaymentRepository paymentRepository,
                                   CustomerFeedbackRepository feedbackRepository,
                                   SettlementRepository settlementRepository,
                                   ProjectCostContext costContext) {
        this.contractRepository = contractRepository;
        this.designRepository = designRepository;
        this.quoteRepository = quoteRepository;
        this.measureRepository = measureRepository;
        this.measureRegionRepository = measureRegionRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.reportRepository = reportRepository;
        this.selectionRepository = selectionRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.purchaseRepository = purchaseRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.changeRepository = changeRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.receivableRepository = receivableRepository;
        this.receiptRepository = receiptRepository;
        this.payableRepository = payableRepository;
        this.paymentRepository = paymentRepository;
        this.feedbackRepository = feedbackRepository;
        this.settlementRepository = settlementRepository;
        this.costContext = costContext;
    }

    @Transactional(readOnly = true)
    public ProjectFactTrace build(Project project, boolean includeInternalCost) {
        ProjectFactTrace trace = new ProjectFactTrace();
        trace.setProjectId(project.getProjectId());
        trace.setProjectNo(project.getNo());
        trace.setProjectName(project.getName());
        trace.setAsOf(LocalDateTime.now());

        sourceFacts(project, trace);
        preSaleFacts(project, trace);
        signingFacts(project, trace);
        fulfillmentFacts(project, trace, includeInternalCost);
        changeFacts(project, trace);
        acceptanceFacts(project, trace);
        financeFacts(project, trace, includeInternalCost);
        afterSalesFacts(project, trace);
        trace.setNodes(ProjectFactTraceContract.ordered(trace.getNodes()));
        ProjectFactTraceContract.validate(trace);
        return trace;
    }

    private void sourceFacts(Project project, ProjectFactTrace trace) {
        if (project.getCustomer() != null) {
            trace.getNodes().add(node(project, FactType.Customer, "Customer", project.getCustomer().getCustId(), 1,
                    null, null, auditTime(project.getCustomer().getCreateTime()), null,
                    project.getCustomer().getName(), "客户经营对象", null, null, null, null,
                    FactVisibility.OwnerVisible, "customer:" + project.getCustomer().getCustId()));
        }
        if (project.getProperty() != null) {
            trace.getNodes().add(node(project, FactType.Property, "Property", project.getProperty().getPropertyId(), 1,
                    project.getProperty().getStatus() == null ? null : project.getProperty().getStatus().name(),
                    project.getProperty().getStatus() == null ? null : project.getProperty().getStatus().getName(),
                    auditTime(project.getProperty().getCreateTime()), null,
                    project.getProperty().getCommunity(), project.getProperty().getAddress(), null, null, null, null,
                    FactVisibility.OwnerVisible, "property:" + project.getProperty().getPropertyId()));
        }
        String origin = project.getBusinessOrigin() == com.th4.edge.decoboon.common.configure.BusinessOrigin.Carryover
                ? BusinessFactContract.CARRYOVER_SOURCE : "正常业务";
        trace.getNodes().add(node(project, FactType.ProjectOrigin, "Project", project.getProjectId(), 1,
                project.getBusinessOrigin() == null ? null : project.getBusinessOrigin().name(), origin,
                auditTime(project.getCreateTime()), project.getOwner() == null ? null : project.getOwner().getName(),
                "项目来源", origin, null, null, null, null, FactVisibility.OwnerVisible,
                "project:" + project.getProjectId()));
    }

    private void preSaleFacts(Project project, ProjectFactTrace trace) {
        Measure measure = latestCompletedMeasure(project);
        if (measure == null) {
            gap(trace, "measure-missing", FactType.Measure, "量房", "无系统量房记录", true);
        } else {
            long regionCount = 0;
            long measuredCount = 0;
            if (project.getCompany() != null && measure.getMeasureId() != null) {
                List<MeasureRegion> regions = measureRegionRepository
                        .findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(
                                project.getCompany().getComId(), measure.getMeasureId());
                regionCount = regions.size();
                measuredCount = regions.stream()
                        .filter(region -> region.getAreaSource() == AreaSource.Measured && region.getArea() != null)
                        .count();
            }
            String actor = measure.getExecutor() != null ? measure.getExecutor().getName()
                    : measure.getDesigner() == null ? null : measure.getDesigner().getName();
            trace.getNodes().add(node(project, FactType.Measure, "Measure", measure.getMeasureId(), 1,
                    measure.getStatus() == null ? null : measure.getStatus().name(),
                    measure.getStatus() == null ? null : measure.getStatus().getName(),
                    measure.getCompletedAt(), actor, "量房记录",
                    "区域 " + regionCount + " · 实测 " + measuredCount + " · 待补 " + (regionCount - measuredCount),
                    null, BigDecimal.valueOf(regionCount), null, null, FactVisibility.OwnerVisible,
                    "measure:" + measure.getMeasureId()));
        }

        Quote quote = project.getQuote();
        Design boundDesign = quote == null ? null : quote.getDesign();
        List<Design> designs = designHistory(project, boundDesign);
        if (designs.isEmpty()) {
            gap(trace, "design-missing", FactType.Design, "设计方案", "无设计方案来源", false);
        } else {
            String nodeKey = project.getProperty() == null
                    ? "Design:Design:project:" + project.getProjectId()
                    : "Design:Design:property:" + project.getProperty().getPropertyId();
            for (Design design : designs) {
                boolean current = boundDesign != null && Objects.equals(boundDesign.getDesignId(), design.getDesignId())
                        && design.getStatus() != com.th4.edge.decoboon.design.configure.DesignStatus.Archived;
                BusinessFactNode fact = node(project, FactType.Design, "Design", design.getDesignId(), design.getVersion(),
                        design.getStatus() == null ? null : design.getStatus().name(),
                        design.getStatus() == null ? null : design.getStatus().getName(),
                        auditTime(design.getCreateTime()), design.getOwner() == null ? null : design.getOwner().getName(),
                        design.getName(), "设计方案 V" + value(design.getVersion()), null, null, null, null,
                        FactVisibility.OwnerVisible, "design:" + design.getDesignId());
                fact.setRecordedAt(auditTime(design.getCreateTime()));
                addFact(trace, fact, nodeKey, current, "历史/替代设计方案");
            }
        }
        if (quote == null) {
            gap(trace, "quote-missing", FactType.Quote, "报价", "无合同绑定报价", true);
        } else {
            String nodeKey = quoteFamilyKey(project, quote, boundDesign);
            for (Quote candidate : quoteHistory(project, quote, boundDesign)) {
                boolean current = Objects.equals(quote.getQuoteId(), candidate.getQuoteId())
                        && candidate.getStatus() != QuoteStatus.Superseded
                        && candidate.getStatus() != QuoteStatus.Rejected;
                BusinessFactNode fact = node(project, FactType.Quote, "Quote", candidate.getQuoteId(),
                        candidate.getVersion(),
                        candidate.getStatus() == null ? null : candidate.getStatus().name(),
                        candidate.getStatus() == null ? null : candidate.getStatus().getName(),
                        candidate.getAcceptedAt() == null ? candidate.getSubmittedAt() : candidate.getAcceptedAt(),
                        candidate.getAcceptedBy() == null ? null : candidate.getAcceptedBy().getName(),
                        candidate.getNo(), "报价 V" + value(candidate.getVersion()), candidate.getAmount(), null, null,
                        null, FactVisibility.OwnerVisible, "quote:" + candidate.getQuoteId());
                fact.setRecordedAt(auditTime(candidate.getCreateTime()));
                addFact(trace, fact, nodeKey, current, "历史/替代报价");
            }
        }
    }

    private List<Design> designHistory(Project project, Design boundDesign) {
        List<Design> designs = new ArrayList<>();
        if (project.getCompany() != null && project.getProperty() != null) {
            designs.addAll(designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(
                    project.getCompany().getComId(), List.of(project.getProperty().getPropertyId())));
        }
        if (boundDesign != null && designs.stream().noneMatch(item -> item != null
                && Objects.equals(item.getDesignId(), boundDesign.getDesignId()))) {
            designs.add(boundDesign);
        }
        designs.removeIf(Objects::isNull);
        designs.sort(Comparator.comparing(Design::getVersion, Comparator.nullsLast(Integer::compareTo)).reversed()
                .thenComparing(Design::getDesignId, Comparator.nullsLast(Long::compareTo).reversed()));
        return designs;
    }

    private List<Quote> quoteHistory(Project project, Quote boundQuote, Design boundDesign) {
        List<Quote> quotes = new ArrayList<>();
        if (boundDesign != null && boundDesign.getDesignId() != null) {
            quotes.addAll(quoteRepository.findByDesign_DesignIdOrderByVersionDesc(boundDesign.getDesignId()));
        } else if (project.getCompany() != null && project.getProperty() != null) {
            quotes.addAll(quoteRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByQuoteIdDesc(
                    project.getCompany().getComId(), project.getProperty().getPropertyId()));
        }
        if (quotes.stream().noneMatch(item -> item != null
                && Objects.equals(item.getQuoteId(), boundQuote.getQuoteId()))) {
            quotes.add(boundQuote);
        }
        quotes.removeIf(Objects::isNull);
        quotes.sort(Comparator.comparing(Quote::getVersion, Comparator.nullsLast(Integer::compareTo)).reversed()
                .thenComparing(Quote::getQuoteId, Comparator.nullsLast(Long::compareTo).reversed()));
        return quotes;
    }

    private String quoteFamilyKey(Project project, Quote boundQuote, Design boundDesign) {
        if (boundDesign != null && boundDesign.getDesignId() != null) {
            return "Quote:Quote:design:" + boundDesign.getDesignId();
        }
        if (project.getProperty() != null && project.getProperty().getPropertyId() != null) {
            return "Quote:Quote:property:" + project.getProperty().getPropertyId();
        }
        return "Quote:Quote:project:" + project.getProjectId() + ":quote:" + boundQuote.getQuoteId();
    }

    private void signingFacts(Project project, ProjectFactTrace trace) {
        Quote quote = project.getQuote();
        if (quote != null && quote.getAcceptedAt() != null) {
            trace.getNodes().add(node(project, FactType.QuoteAcceptance, "Quote", quote.getQuoteId(), quote.getVersion(),
                    "Accepted", "已接受", quote.getAcceptedAt(),
                    quote.getAcceptedBy() == null ? null : quote.getAcceptedBy().getName(),
                    "报价接受登记", "客户报价已登记接受", null, null, null, null,
                    FactVisibility.OwnerVisible, "quote:" + quote.getQuoteId()));
        }
        ProjectContract contract = contractRepository.findDetailedByProject(project).orElse(null);
        if (contract == null) {
            gap(trace, "contract-missing", FactType.Contract, "合同", "无合同绑定关系", true);
        } else {
            trace.getNodes().add(node(project, FactType.Contract, "Contract", contract.getContractId(), 1,
                    contract.getStatus() == null ? null : contract.getStatus().name(),
                    contract.getStatus() == null ? null : contract.getStatus().getName(),
                    contract.getEffectiveAt() == null ? contract.getSignedAt() == null ? null : contract.getSignedAt().atStartOfDay() : contract.getEffectiveAt(),
                    contract.getCreatedBy() == null ? null : contract.getCreatedBy().getName(),
                    contract.getNo(), "合同当前金额", contract.getCurrentAmount() == null ? contract.getBaselineAmount() : contract.getCurrentAmount(),
                    null, null, null, FactVisibility.OwnerVisible, "contract:" + contract.getContractId()));
        }
    }

    private void fulfillmentFacts(Project project, ProjectFactTrace trace, boolean includeInternalCost) {
        trace.getNodes().add(node(project, FactType.Project, "Project", project.getProjectId(), 1,
                project.getStatus() == null ? null : project.getStatus().name(),
                project.getStatus() == null ? null : project.getStatus().getName(),
                auditTime(project.getCreateTime()), project.getManager() == null ? null : project.getManager().getName(),
                project.getName(), "项目当前进度", null, null, project.getProgress(), null,
                FactVisibility.OwnerVisible, "project:" + project.getProjectId()));

        List<ProjectStage> stages = stageRepository.findDetailedByProjectOrderBySort(project);
        for (ProjectStage stage : stages) {
            trace.getNodes().add(node(project, FactType.Stage, "ProjectStage", stage.getStageId(), 1,
                    stage.getStatus() == null ? null : stage.getStatus().name(),
                    stage.getStatus() == null ? null : stage.getStatus().getName(),
                    stageDate(stage), stage.getOwner() == null ? null : stage.getOwner().getName(),
                    stage.getName(), "施工阶段", null, null, stage.getProgress(), null,
                    FactVisibility.OwnerVisible, "project-stage:" + stage.getStageId()));
        }

        for (ProjectTask task : taskRepository.findDetailedByProject(project)) {
            trace.getNodes().add(node(project, FactType.Task, "ProjectTask", task.getTaskId(), 1,
                    task.getStatus() == null ? null : task.getStatus().name(),
                    task.getStatus() == null ? null : task.getStatus().getName(),
                    taskDate(task), task.getOwner() == null ? null : task.getOwner().getName(),
                    task.getName(), "施工任务", null, task.getQuantity(), task.getProgress(), null,
                    FactVisibility.OwnerVisible, "project-task:" + task.getTaskId()));
        }

        if (project.getCompany() != null) {
            List<ProjectReport> reports = reportRepository
                    .findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(
                            project.getCompany().getComId(), project.getProjectId(), PageRequest.of(0, 20))
                    .getContent();
            for (ProjectReport report : reports) {
                trace.getNodes().add(node(project, FactType.SiteReport, "ProjectReport", report.getReportId(), 1,
                        report.getType() == null ? null : report.getType().name(),
                        report.getType() == null ? null : report.getType().getName(),
                        auditTime(report.getCreateTime()), report.getAuthor() == null ? null : report.getAuthor().getName(),
                        report.getTitle(), reportSummary(report),
                        null, null, null, null,
                        Boolean.TRUE.equals(report.getVisibleToCustomer()) ? FactVisibility.OwnerVisible : FactVisibility.Internal,
                        "project-report:" + report.getReportId()));
            }
        }

        if (project.getCompany() != null) {
            List<MaterialSelection> selections = selectionRepository
                    .findByCompany_ComIdAndProject_ProjectIdOrderByVersionDesc(
                            project.getCompany().getComId(), project.getProjectId());
            for (MaterialSelection selection : selections) {
                String nodeKey = selection.getSourceContractLine() == null
                        ? "MaterialSelection:MaterialSelection:selection:" + selection.getSelectionId()
                        : "MaterialSelection:MaterialSelection:line:"
                        + selection.getSourceContractLine().getLineId();
                boolean current = isCurrentSelection(selection, selections);
                BusinessFactNode fact = node(project, FactType.MaterialSelection, "MaterialSelection",
                        selection.getSelectionId(), selection.getVersion(),
                        selection.getStatus() == null ? null : selection.getStatus().name(),
                        selection.getStatus() == null ? null : selection.getStatus().getName(),
                        auditTime(selection.getCreateTime()),
                        selection.getDesigner() == null ? null : selection.getDesigner().getName(),
                        selection.getTitle(), "主材选材 V" + value(selection.getVersion()), null, null, null, null,
                        FactVisibility.OwnerVisible, "material-selection:" + selection.getSelectionId());
                fact.setRecordedAt(auditTime(selection.getCreateTime()));
                addFact(trace, fact, nodeKey, current, "历史选材版本");
            }
        }

        List<MaterialPlan> plans = materialPlanRepository.findByProject(project);
        if (!plans.isEmpty()) {
            MaterialPlan latest = plans.stream()
                    .max(Comparator.comparing(MaterialPlan::getPlanId, Comparator.nullsLast(Long::compareTo)))
                    .orElse(plans.get(plans.size() - 1));
            trace.getNodes().add(node(project, FactType.MaterialPlan, "MaterialPlan", latest.getPlanId(), 1,
                    latest.getStatus() == null ? null : latest.getStatus().name(),
                    latest.getStatus() == null ? null : latest.getStatus().getName(),
                    auditTime(latest.getCreateTime()), latest.getOwner() == null ? null : latest.getOwner().getName(),
                    "材料计划", plans.size() + " 项材料计划", null, latest.getQuantity(), null, null,
                    FactVisibility.OwnerVisible, "material-plan:" + latest.getPlanId()));
        }

        List<PurchaseOrder> purchases = purchaseRepository.findByProjectOrderByPurchaseIdDesc(project);
        if (!purchases.isEmpty()) {
            String nodeKey = "Purchase:PurchaseOrder:project:" + project.getProjectId();
            PurchaseOrder current = purchases.stream()
                    .filter(order -> !isCancelled(order.getStatus()))
                    .findFirst().orElse(null);
            if (current != null) {
                BusinessFactNode fact = node(project, FactType.Purchase, "PurchaseOrder", current.getPurchaseId(), 1,
                        current.getStatus() == null ? null : current.getStatus().name(),
                        current.getStatus() == null ? null : current.getStatus().getName(),
                        current.getOrderedAt() == null ? auditTime(current.getCreateTime()) : current.getOrderedAt().atStartOfDay(),
                        current.getCreatedBy() == null ? null : current.getCreatedBy().getName(),
                        "采购单", purchases.size() + " 张采购单", includeInternalCost ? current.getAmount() : null,
                        current.getQuantity(), null, null, FactVisibility.Internal,
                        "purchase:" + current.getPurchaseId());
                fact.setRecordedAt(auditTime(current.getCreateTime()));
                addFact(trace, fact, nodeKey, true, null);
            }
            PurchaseOrder cancelled = purchases.stream()
                    .filter(order -> isCancelled(order.getStatus()))
                    .findFirst().orElse(null);
            if (cancelled != null) {
                BusinessFactNode fact = node(project, FactType.Purchase, "PurchaseOrder", cancelled.getPurchaseId(), 1,
                        cancelled.getStatus() == null ? null : cancelled.getStatus().name(),
                        cancelled.getStatus() == null ? null : cancelled.getStatus().getName(),
                        cancelled.getOrderedAt() == null ? auditTime(cancelled.getCreateTime()) : cancelled.getOrderedAt().atStartOfDay(),
                        cancelled.getCreatedBy() == null ? null : cancelled.getCreatedBy().getName(),
                        "采购单", "已取消/驳回采购", includeInternalCost ? cancelled.getAmount() : null,
                        cancelled.getQuantity(), null, null, FactVisibility.Internal,
                        "purchase:" + cancelled.getPurchaseId());
                fact.setRecordedAt(auditTime(cancelled.getCreateTime()));
                addFact(trace, fact, nodeKey, false, "历史/失效采购单");
            }
        }

        List<GoodsReceipt> receipts = goodsReceiptRepository
                .findByPurchase_Project_ProjectIdOrderByReceiptIdDesc(project.getProjectId());
        if (!receipts.isEmpty()) {
            GoodsReceipt latest = receipts.get(0);
            trace.getNodes().add(node(project, FactType.GoodsReceipt, "GoodsReceipt", latest.getReceiptId(), 1,
                    null, null, latest.getArrivedAt(),
                    latest.getInspector() == null ? null : latest.getInspector().getName(),
                    "材料到场", receipts.size() + " 次到场", null, latest.getQuantity(), null, null,
                    FactVisibility.OwnerVisible, "goods-receipt:" + latest.getReceiptId()));
        }
    }

    private void changeFacts(Project project, ProjectFactTrace trace) {
        if (project.getCompany() == null) return;
        List<ContractChange> changes = changeRepository
                .findByProject_ProjectIdAndCompany_ComIdOrderByChangeIdDesc(project.getProjectId(), project.getCompany().getComId());
        for (ContractChange change : changes) {
            boolean effective = change.getStatus() == ContractChangeStatus.CustomerConfirmed;
            boolean current = change.getStatus() != ContractChangeStatus.Rejected
                    && change.getStatus() != ContractChangeStatus.Cancelled;
            BusinessFactNode fact = node(project, FactType.ContractChange, "ContractChange", change.getChangeId(), 1,
                    change.getStatus() == null ? null : change.getStatus().name(),
                    change.getStatus() == null ? null : change.getStatus().getName(),
                    effective && change.getApprovedAt() != null ? change.getApprovedAt() : auditTime(change.getCreateTime()),
                    change.getOwner() == null ? null : change.getOwner().getName(),
                    change.getNo(), change.getReason(), effective ? change.getAmount() : null, null, null, null,
                    FactVisibility.OwnerVisible, "contract-change:" + change.getChangeId());
            fact.setRecordedAt(auditTime(change.getCreateTime()));
            addFact(trace, fact, "ContractChange:ContractChange:" + change.getChangeId(), current, "已驳回/已取消变更");
        }
    }

    private void acceptanceFacts(Project project, ProjectFactTrace trace) {
        List<Acceptance> acceptances = acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(project.getProjectId());
        for (Acceptance acceptance : acceptances) {
            boolean current = acceptance.getStatus() != AcceptStatus.Rejected;
            BusinessFactNode fact = node(project, FactType.Acceptance, "Acceptance", acceptance.getAcceptanceId(), 1,
                    acceptance.getStatus() == null ? null : acceptance.getStatus().name(),
                    acceptance.getStatus() == null ? null : acceptance.getStatus().getName(),
                    acceptance.getConfirmedAt() == null ? auditTime(acceptance.getCreateTime()) : acceptance.getConfirmedAt(),
                    acceptance.getOwner() == null ? null : acceptance.getOwner().getName(),
                    acceptance.getStage() == null ? "阶段验收" : acceptance.getStage().getName() + "验收",
                    acceptance.getConclusion(), null, null, null, null,
                    FactVisibility.OwnerVisible, "acceptance:" + acceptance.getAcceptanceId());
            fact.setRecordedAt(auditTime(acceptance.getCreateTime()));
            addFact(trace, fact, "Acceptance:Acceptance:" + acceptance.getAcceptanceId(), current, "历史验收轮次");
        }
    }

    private void financeFacts(Project project, ProjectFactTrace trace, boolean includeInternalCost) {
        if (project.getCompany() == null) return;
        Long comId = project.getCompany().getComId();
        List<Receivable> receivables = receivableRepository.findByProjectOrderByDueDateAsc(project);
        if (!receivables.isEmpty()) {
            Receivable latest = receivables.get(receivables.size() - 1);
            BigDecimal amount = sum(receivables, Receivable::getAmount);
            BigDecimal received = sum(receivables, Receivable::getReceivedAmount);
            BigDecimal outstanding = amount.subtract(received).max(BigDecimal.ZERO);
            trace.getNodes().add(node(project, FactType.Receivable, "Receivable", latest.getReceivableId(), 1,
                    latest.getStatus() == null ? null : latest.getStatus().name(),
                    latest.getStatus() == null ? null : latest.getStatus().getName(),
                    auditTime(latest.getCreateTime()), null, "应收计划",
                    receivables.size() + " 个节点 · 待收 " + money(outstanding), amount, null, null, null,
                    FactVisibility.OwnerVisible, "receivable:" + latest.getReceivableId()));
        }
        List<Receipt> receipts = receiptRepository.findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(comId, project.getProjectId());
        if (!receipts.isEmpty()) {
            String nodeKey = "Receipt:Receipt:project:" + project.getProjectId();
            List<Receipt> active = receipts.stream()
                    .filter(receipt -> receipt.getStatus() == com.th4.edge.decoboon.finance.configure.ReceiptStatus.Active)
                    .toList();
            List<Receipt> reversed = receipts.stream()
                    .filter(receipt -> receipt.getStatus() == com.th4.edge.decoboon.finance.configure.ReceiptStatus.Reversed)
                    .toList();
            if (!active.isEmpty()) {
                Receipt latest = active.get(0);
                BusinessFactNode fact = node(project, FactType.Receipt, "Receipt", latest.getReceiptId(), 1,
                        latest.getStatus() == null ? null : latest.getStatus().name(),
                        latest.getStatus() == null ? null : latest.getStatus().getName(),
                        atStartOfDay(latest.getReceivedAt()),
                        latest.getCreatedBy() == null ? null : latest.getCreatedBy().getName(),
                        "收款", "累计 " + money(sum(active, Receipt::getAmount)), sum(active, Receipt::getAmount),
                        null, null, null, FactVisibility.OwnerVisible, "receipt:" + latest.getReceiptId());
                fact.setRecordedAt(auditTime(latest.getCreateTime()));
                addFact(trace, fact, nodeKey, true, null);
            }
            if (!reversed.isEmpty()) {
                Receipt latest = reversed.get(0);
                BusinessFactNode fact = node(project, FactType.Receipt, "Receipt", latest.getReceiptId(), 1,
                        latest.getStatus() == null ? null : latest.getStatus().name(),
                        latest.getStatus() == null ? null : latest.getStatus().getName(),
                        atStartOfDay(latest.getReceivedAt()),
                        latest.getCreatedBy() == null ? null : latest.getCreatedBy().getName(),
                        "收款冲正", "历史冲正 " + money(sum(reversed, Receipt::getAmount)),
                        sum(reversed, Receipt::getAmount), null, null, null,
                        FactVisibility.OwnerVisible, "receipt:" + latest.getReceiptId());
                fact.setRecordedAt(auditTime(latest.getCreateTime()));
                addFact(trace, fact, nodeKey, false, "已冲正历史收款，不参与当前累计");
            }
        }

        if (includeInternalCost) {
            List<Payable> payables = payableRepository.findByProjectOrderByDueDateAsc(project);
            if (!payables.isEmpty()) {
                Payable latest = payables.get(payables.size() - 1);
                trace.getNodes().add(node(project, FactType.Payable, "Payable", latest.getPayableId(), 1,
                        latest.getStatus() == null ? null : latest.getStatus().name(),
                        latest.getStatus() == null ? null : latest.getStatus().getName(),
                        auditTime(latest.getCreateTime()), null, "应付",
                        payables.size() + " 个应付节点", sum(payables, Payable::getAmount), null, null, null,
                        FactVisibility.Internal, "payable:" + latest.getPayableId()));
            }
            List<Payment> payments = paymentRepository.findByCompany_ComIdAndProject_ProjectIdOrderByPaidAtDesc(comId, project.getProjectId());
            if (!payments.isEmpty()) {
                Payment latest = payments.get(0);
                trace.getNodes().add(node(project, FactType.Payment, "Payment", latest.getPaymentId(), 1,
                        null, null, atStartOfDay(latest.getPaidAt()),
                        latest.getCreatedBy() == null ? null : latest.getCreatedBy().getName(),
                        "付款", "累计 " + money(sum(payments, Payment::getAmount)), sum(payments, Payment::getAmount),
                        null, null, null, FactVisibility.Internal, "payment:" + latest.getPaymentId()));
            }
            ProjectCostContext.CostSummary summary = costContext.summarize(project);
            if (summary == null) {
                gap(trace, "cost-missing", FactType.Cost, "项目成本", "成本汇总暂不可确认", false);
            } else {
                boolean complete = summary.incompleteItemCount() == 0;
                BusinessFactNode fact = node(project, FactType.Cost, "ProjectCost", project.getProjectId(), 1,
                        complete ? "Complete" : "Incomplete", complete ? "成本完整" : "成本未完整",
                        LocalDateTime.now(), null, "项目成本",
                        complete ? "实际成本已完整" : "存在未确认成本", summary.actualCost(), null, null, complete,
                        FactVisibility.Internal, "project-cost:" + project.getProjectId());
                addFact(trace, fact, "Cost:ProjectCost:project:" + project.getProjectId(), true, null);
            }
            profitSettlementFact(project, trace, comId);
        }
    }

    private void profitSettlementFact(Project project, ProjectFactTrace trace, Long comId) {
        Settlement settlement = settlementRepository.findByProject_ProjectIdAndCompany_ComId(
                project.getProjectId(), comId).orElse(null);
        if (settlement == null) return;
        // costComplete 是结算实体的瞬态字段；回读时只以持久化的利润值判断该快照是否完整。
        boolean complete = settlement.getProfitAmount() != null;
        LocalDateTime businessTime = settlement.getConfirmedAt() == null
                ? settlement.getClosedAt() == null ? null : settlement.getClosedAt().atStartOfDay()
                : settlement.getConfirmedAt().atStartOfDay();
        String summary = complete
                ? "利润 " + money(settlement.getProfitAmount()) + " · 毛利率 " + percent(settlement.getGrossMargin())
                : "结算快照成本未完整，利润/毛利率暂不可确认";
        BusinessFactNode fact = node(project, FactType.ProfitSettlement, "Settlement",
                settlement.getSettlementId(), 1,
                settlement.getStatus() == null ? null : settlement.getStatus().name(),
                settlement.getStatus() == null ? null : settlement.getStatus().getName(),
                businessTime,
                settlement.getConfirmedBy() == null ? null : settlement.getConfirmedBy().getName(),
                "利润与结算", summary, settlement.getFinalAmount(), null, null, complete,
                FactVisibility.Internal, "settlement:" + settlement.getSettlementId());
        fact.setRecordedAt(auditTime(settlement.getCreateTime()));
        fact.setProfitAmount(settlement.getProfitAmount());
        fact.setGrossMargin(settlement.getGrossMargin());
        if (!complete) {
            fact.setUncertainty("结算快照成本未完整，利润/毛利率暂不可确认");
        }
        addFact(trace, fact, "ProfitSettlement:Settlement:project:" + project.getProjectId(), true, null);
    }

    private void afterSalesFacts(Project project, ProjectFactTrace trace) {
        if (project.getActualEndDate() != null) {
            trace.getNodes().add(node(project, FactType.Delivery, "Project", project.getProjectId(), 1,
                    "Delivered", "已交付", project.getActualEndDate().atStartOfDay(),
                    project.getManager() == null ? null : project.getManager().getName(),
                    "项目交付", "实际完工日期", null, null, 100, null,
                    FactVisibility.OwnerVisible, "project:" + project.getProjectId()));
        }
        if (project.getCompany() != null) {
            List<CustomerFeedback> feedbacks = feedbackRepository
                    .findByProject_ProjectIdAndCompany_ComIdOrderByFeedbackIdDesc(project.getProjectId(), project.getCompany().getComId());
            for (CustomerFeedback feedback : feedbacks) {
                trace.getNodes().add(node(project, FactType.Feedback, "CustomerFeedback", feedback.getFeedbackId(), 1,
                        feedback.getStatus() == null ? null : feedback.getStatus().name(),
                        feedback.getStatus() == null ? null : feedback.getStatus().getName(),
                        auditTime(feedback.getCreateTime()), null,
                        feedback.getTitle(), feedback.getContent(), null, null, null, null,
                        FactVisibility.OwnerVisible, "feedback:" + feedback.getFeedbackId()));
            }
        }
    }

    private BusinessFactNode node(Project project, FactType type, String objectType, Long objectId, Integer version,
                                  String status, String statusName, LocalDateTime businessTime, String actor,
                                  String title, String summary, BigDecimal amount, BigDecimal quantity,
                                  Integer progress, Boolean costComplete, FactVisibility visibility, String navigationPath) {
        BusinessFactNode node = new BusinessFactNode();
        node.setNodeId(type.name() + "-" + objectType + "-" + objectId);
        node.setNodeKey(type.name() + ":" + objectType + ":" + objectId + ":" + version);
        node.setStage(type.getStage());
        node.setFactType(type);
        node.setObjectType(objectType);
        node.setObjectId(objectId);
        node.setProjectId(project.getProjectId());
        node.setVersion(version == null ? 1 : version);
        node.setCurrent(true);
        node.setStatus(status);
        node.setStatusName(statusName);
        node.setBusinessTime(businessTime);
        node.setRecordedAt(LocalDateTime.now());
        node.setActorName(actor);
        node.setTitle(title);
        node.setSummary(summary);
        node.setAmount(amount);
        node.setQuantity(quantity);
        node.setProgress(progress);
        node.setCostComplete(costComplete);
        node.setVisibility(visibility);
        node.setNavigationPath(navigationPath);
        node.setReturnPath("project:" + project.getProjectId());
        node.setReturnAnchor("project-facts");
        node.setSourceLabel("项目关联事实");
        return node;
    }

    private void addFact(ProjectFactTrace trace, BusinessFactNode fact, String nodeKey,
                         boolean current, String historyReason) {
        fact.setNodeKey(nodeKey);
        fact.setCurrent(current);
        if (!current && (fact.getUncertainty() == null || fact.getUncertainty().isBlank())) {
            fact.setUncertainty(historyReason == null || historyReason.isBlank()
                    ? "历史/失效事实" : historyReason);
        }
        trace.getNodes().add(fact);
    }

    private boolean isCurrentSelection(MaterialSelection selection, List<MaterialSelection> selections) {
        if (selection.getSourceContractLine() == null || selection.getSourceContractLine().getLineId() == null) {
            return true;
        }
        Long lineId = selection.getSourceContractLine().getLineId();
        int maxVersion = selections.stream()
                .filter(item -> item.getSourceContractLine() != null
                        && Objects.equals(item.getSourceContractLine().getLineId(), lineId))
                .map(MaterialSelection::getVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(value(selection.getVersion()));
        return value(selection.getVersion()) == maxVersion;
    }

    private boolean isCancelled(PurchaseStatus status) {
        return status == PurchaseStatus.Rejected || status == PurchaseStatus.Cancelled;
    }

    private void gap(ProjectFactTrace trace, String nodeKey, FactType type, String label, String reason, boolean required) {
        ProjectFactTrace.SourceGap gap = new ProjectFactTrace.SourceGap();
        gap.setNodeKey(nodeKey);
        gap.setStage(type.getStage());
        gap.setFactType(type);
        gap.setLabel(label);
        gap.setReason(reason);
        gap.setRequired(required);
        trace.getSourceGaps().add(gap);
    }

    private Measure latestCompletedMeasure(Project project) {
        if (project.getCompany() == null || project.getProperty() == null) return null;
        return measureRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByMeasureTimeDesc(
                        project.getCompany().getComId(), project.getProperty().getPropertyId()).stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Completed)
                .sorted(Comparator.comparing(Measure::getCompletedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(Measure::getMeasureId, Comparator.nullsLast(Long::compareTo).reversed()))
                .findFirst().orElse(null);
    }

    private LocalDateTime auditTime(DateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private LocalDateTime stageDate(ProjectStage stage) {
        if (stage.getActualEnd() != null) return stage.getActualEnd().atStartOfDay();
        if (stage.getActualStart() != null) return stage.getActualStart().atStartOfDay();
        return auditTime(stage.getCreateTime());
    }

    private LocalDateTime taskDate(ProjectTask task) {
        if (task.getActualEnd() != null) return task.getActualEnd().atStartOfDay();
        if (task.getActualStart() != null) return task.getActualStart().atStartOfDay();
        return auditTime(task.getCreateTime());
    }

    private String reportSummary(ProjectReport report) {
        String content = report.getContent() == null ? "" : report.getContent().replaceAll("\\s+", " ").trim();
        return content.length() > 120 ? content.substring(0, 120) + "…" : content;
    }

    private <T> BigDecimal sum(List<T> values, java.util.function.Function<T, BigDecimal> getter) {
        return values.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String money(BigDecimal value) {
        return value == null ? "暂不可确认" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String percent(BigDecimal value) {
        return value == null ? "暂不可确认" : value.stripTrailingZeros().toPlainString() + "%";
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}
