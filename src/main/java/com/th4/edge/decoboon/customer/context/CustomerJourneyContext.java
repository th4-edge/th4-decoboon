package com.th4.edge.decoboon.customer.context;

import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.project.model.AcceptanceRound;
import com.th4.edge.decoboon.common.support.DateTimeText;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.common.support.SelectionText;
import com.th4.edge.decoboon.common.support.CraftText;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.data.enums.Enumerable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 业主项目全生命周期旅程装配。 */
@Component
public class CustomerJourneyContext {

    private final MeasureRepository measureRepository;
    private final DesignRepository designRepository;
    private final SpaceRepository spaceRepository;
    private final DesignItemRepository designItemRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final ProjectContractRepository contractRepository;
    private final ContractChangeRepository changeRepository;
    private final ContractChangeItemRepository changeItemRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final MaterialSelectionRepository selectionRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectReportRepository reportRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final AcceptanceRoundRepository acceptanceRoundRepository;
    private final ReceivableRepository receivableRepository;
    private final CustomerFeedbackRepository feedbackRepository;

    public CustomerJourneyContext(MeasureRepository measureRepository,
                                  DesignRepository designRepository,
                                  SpaceRepository spaceRepository,
                                  DesignItemRepository designItemRepository,
                                  QuoteRepository quoteRepository,
                                  QuoteLineRepository quoteLineRepository,
                                  ProjectContractRepository contractRepository,
                                  ContractChangeRepository changeRepository,
                                  ContractChangeItemRepository changeItemRepository,
                                  MaterialPlanRepository materialPlanRepository,
                                  MaterialSelectionRepository selectionRepository,
                                  ProjectStageRepository stageRepository,
                                  ProjectReportRepository reportRepository,
                                  AcceptanceRepository acceptanceRepository,
                                  AcceptanceRoundRepository acceptanceRoundRepository,
                                  ReceivableRepository receivableRepository,
                                  CustomerFeedbackRepository feedbackRepository) {
        this.measureRepository = measureRepository;
        this.designRepository = designRepository;
        this.spaceRepository = spaceRepository;
        this.designItemRepository = designItemRepository;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.contractRepository = contractRepository;
        this.changeRepository = changeRepository;
        this.changeItemRepository = changeItemRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.selectionRepository = selectionRepository;
        this.stageRepository = stageRepository;
        this.reportRepository = reportRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.acceptanceRoundRepository = acceptanceRoundRepository;
        this.receivableRepository = receivableRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public CustomerJourney build(Project project) {
        Long comId = project.getCompany().getComId();
        Long projectId = project.getProjectId();
        Long propertyId = project.getProperty().getPropertyId();
        List<Measure> measures = measureRepository.findByPropertyOrderByMeasureTimeDesc(project.getProperty());
        List<Design> designs = propertyId == null ? List.of()
                : designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(comId, List.of(propertyId));
        List<Quote> quotes = quoteRepository.findByProperty_PropertyIdOrderByQuoteIdDesc(propertyId).stream()
                .filter(item -> item.getCompany() != null && Objects.equals(item.getCompany().getComId(), comId)).toList();
        ProjectContract contract = contractRepository.findByProject(project).orElse(null);
        List<ContractChange> changes = changeRepository.findByProject_ProjectIdAndCompany_ComIdOrderByChangeIdDesc(projectId, comId);
        List<MaterialPlan> plans = materialPlanRepository.findByProject(project);
        List<MaterialSelection> selections = selectionRepository.findByCompany_ComIdAndProject_ProjectIdOrderByVersionDesc(comId, projectId);
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        List<Acceptance> acceptances = acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(projectId);
        List<ProjectReport> reports = reportRepository
                .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                        comId, projectId, PageRequest.of(0, 12)).getContent();
        List<Receivable> receivables = receivableRepository.findByProjectOrderByDueDateAsc(project);
        List<CustomerFeedback> feedbacks = feedbackRepository.findByProject_ProjectIdAndCompany_ComIdOrderByFeedbackIdDesc(projectId, comId);

        CustomerJourney journey = new CustomerJourney();
        journey.setProjectId(projectId);
        journey.setProjectNo(project.getNo());
        journey.setProjectName(project.getName());
        // P1（XA-20260924-001）：业主端不信任可能漂移的 project.status 列。
        // 事实口径：**存在待业主确认的验收单 ⇔ 待验收**；否则按阶段是否已开工推导，
        // 避免出现"项目待验收 / 验收整改待确认 0 / 整改中 1"这种自相矛盾的业主可见结论。
        ProjectStatus ownerStatus = resolveOwnerStatus(project, stages, acceptances);
        journey.setProjectStatus(ownerStatus.name());
        journey.setProjectStatusName(ownerStatus.getName());
        journey.setProgress(project.getProgress());
        // 历史坑：Project.currentStageName 是需要回填的派生列（只有项目总览/分页投影会临时赋值），
        // 旅程直接读它会拿到 null，前端再兜底成"待开工"，与真实的"待验收"自相矛盾（DEC-009 决策二）。
        // 这里一律按阶段事实现场推导，保证"当前阶段"与项目状态不打架。
        journey.setCurrentStageName(currentStageName(stages));
        journey.setPropertyAddress(project.getProperty().getAddress());
        journey.setAsOf(LocalDateTime.now());
        journey.setSections(List.of(
                measureSection(measures),
                designSection(designs),
                quoteSection(quotes),
                contractSection(contract, changes),
                changeSection(changes),
                materialSection(plans, selections),
                constructionSection(project, stages, reports),
                acceptanceSection(acceptances),
                paymentSection(receivables),
                feedbackSection(feedbacks)
        ));

        List<CustomerJourney.Action> actions = collectActions(acceptances, changes, selections, feedbacks);
        journey.setActions(actions);
        journey.setPendingActionCount(actions.size());
        journey.setOpenFeedbackCount((int) feedbacks.stream()
                .filter(item -> item.getStatus() != CustomerFeedbackStatus.Closed).count());
        return journey;
    }

    private CustomerJourney.Section measureSection(List<Measure> values) {
        CustomerJourney.Section section = section("measure", "量房", "房屋测量与现场条件");
        if (values.isEmpty()) {
            section.setStatus("pending"); section.setStatusName("待量房");
            section.setSummary("尚未形成量房记录");
            return section;
        }
        Measure latest = values.get(0);
        section.setStatus(latest.getStatus() == null ? "done" : latest.getStatus().name());
        section.setStatusName(latest.getStatus() == null ? "已完成" : latest.getStatus().getName());
        section.setSummary("最近量房 " + date(latest.getMeasureTime()));
        section.getFacts().add(fact("count", "量房记录", String.valueOf(values.size()), "count"));
        section.getFacts().add(fact("latest", "最近时间", date(latest.getMeasureTime()), "date"));
        section.setItems(values.stream().limit(10).map(this::measureItem).toList());
        return section;
    }

    private CustomerJourney.Section designSection(List<Design> values) {
        CustomerJourney.Section section = section("design", "设计", "需求、方案与设计版本");
        if (values.isEmpty()) {
            section.setStatus("pending"); section.setStatusName("待设计");
            section.setSummary("尚未形成设计方案");
            return section;
        }
        Design latest = values.get(0);
        section.setStatus(latest.getStatus() == null ? null : latest.getStatus().name());
        section.setStatusName(latest.getStatus() == null ? null : latest.getStatus().getName());
        section.setSummary(latest.getName());
        section.getFacts().add(fact("count", "方案数量", String.valueOf(values.size()), "count"));
        section.getFacts().add(fact("version", "当前版本", "V" + value(latest.getVersion(), 1), "text"));
        if (latest.getStyle() != null) section.getFacts().add(fact("style", "设计风格", latest.getStyle(), "text"));
        section.setItems(values.stream().limit(10).map(this::designItem).toList());
        return section;
    }

    private CustomerJourney.Section quoteSection(List<Quote> values) {
        CustomerJourney.Section section = section("quote", "报价", "客户确认的价格与版本");
        if (values.isEmpty()) {
            section.setStatus("pending"); section.setStatusName("待报价");
            section.setSummary("尚未形成客户报价");
            return section;
        }
        Quote latest = values.get(0);
        section.setStatus(latest.getStatus() == null ? null : latest.getStatus().name());
        section.setStatusName(latest.getStatus() == null ? null : latest.getStatus().getName());
        section.setSummary("当前版本报价 " + money(latest.getAmount()));
        section.getFacts().add(fact("count", "报价版本", String.valueOf(values.stream().filter(q -> q.getStatus() != QuoteStatus.Superseded).count()), "count"));
        section.getFacts().add(fact("amount", "当前金额", money(latest.getAmount()), "money"));
        section.getFacts().add(fact("version", "版本", "V" + value(latest.getVersion(), 1), "text"));
        section.setItems(values.stream().limit(10).map(this::quoteItem).toList());
        return section;
    }

    private CustomerJourney.Section contractSection(ProjectContract contract, List<ContractChange> changes) {
        CustomerJourney.Section section = section("contract", "合同", "签约、生效与合同金额");
        if (contract == null) {
            section.setStatus("pending"); section.setStatusName("待签约");
            section.setSummary("尚未生成合同基线");
            return section;
        }
        section.setStatus(contract.getStatus() == null ? null : contract.getStatus().name());
        section.setStatusName(contract.getStatus() == null ? null : contract.getStatus().getName());
        section.setSummary("合同 " + contract.getNo());
        section.getFacts().add(fact("signed", "签约金额", money(contract.getSignedAmount() == null ? contract.getBaselineAmount() : contract.getSignedAmount()), "money"));
        section.getFacts().add(fact("current", "当前金额", money(contract.getCurrentAmount()), "money"));
        section.getFacts().add(fact("signedAt", "签约日期", date(contract.getSignedAt()), "date"));
        section.getItems().add(contractItem(contract));
        return section;
    }

    private CustomerJourney.Section changeSection(List<ContractChange> values) {
        CustomerJourney.Section section = section("change", "增减项", "范围、金额与生效状态");
        // DEC-068：Pending=待企业审批（业主端显示“审批中”）；CustomerConfirmed=已生效；
        // Approved=历史遗留“待客户确认”，按未生效处理。业主确认不再作为系统事实。
        long processing = values.stream().filter(item -> item.getStatus() == ContractChangeStatus.Pending
                || item.getStatus() == ContractChangeStatus.Approved).count();
        BigDecimal amount = values.stream().filter(item -> item.getStatus() == ContractChangeStatus.CustomerConfirmed)
                .map(ContractChange::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        section.setStatus(processing > 0 ? "pending" : values.isEmpty() ? "none" : "done");
        section.setStatusName(processing > 0 ? "审批中" : values.isEmpty() ? "暂无变更" : "已生效");
        section.setSummary(processing > 0 ? processing + " 项变更审批中" : "共 " + values.size() + " 项变更");
        section.getFacts().add(fact("total", "变更总数", String.valueOf(values.size()), "count"));
        section.getFacts().add(fact("pending", "审批中", String.valueOf(processing), "count"));
        section.getFacts().add(fact("confirmed", "已生效金额", money(amount), "money"));

        // 效率：明细与签认证据各一次批量查询，最多 2 次额外 SQL，避免逐单 N+1。
        List<Long> changeIds = values.stream().map(ContractChange::getChangeId).filter(Objects::nonNull).toList();
        Map<Long, List<ContractChangeItem>> itemMap = new LinkedHashMap<>();
        Map<Long, List<String>> imageMap = new LinkedHashMap<>();
        if (!changeIds.isEmpty()) {
            for (ContractChangeItem item : changeItemRepository.findByChange_ChangeIdInOrderByChangeItemIdAsc(changeIds)) {
                if (item.getChange() == null || item.getChange().getChangeId() == null) continue;
                itemMap.computeIfAbsent(item.getChange().getChangeId(), key -> new ArrayList<>()).add(item);
            }
            for (Object[] row : changeRepository.findImageRowsByChangeIds(changeIds)) {
                if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
                Long changeId = ((Number) row[0]).longValue();
                imageMap.computeIfAbsent(changeId, key -> new ArrayList<>()).add(String.valueOf(row[1]));
            }
        }
        section.setItems(values.stream().limit(20)
                .map(change -> changeItem(change,
                        itemMap.getOrDefault(change.getChangeId(), List.of()),
                        imageMap.getOrDefault(change.getChangeId(), List.of())))
                .toList());
        return section;
    }

    private CustomerJourney.Section materialSection(List<MaterialPlan> plans, List<MaterialSelection> selections) {
        CustomerJourney.Section section = section("material", "材料", "选材、下单与到场");
        long ordered = plans.stream().filter(this::ordered).count();
        long arrived = plans.stream().filter(item -> item.getStatus() == MaterialPlanStatus.Arrived).count();
        section.setStatus(arrived == plans.size() && !plans.isEmpty() ? "done" : ordered > 0 ? "doing" : "pending");
        section.setStatusName(arrived == plans.size() && !plans.isEmpty() ? "已到场" : ordered > 0 ? "采购中" : "待选材");
        section.setSummary(plans.isEmpty() ? "尚未形成材料计划" : "材料计划 " + plans.size() + " 项，已到场 " + arrived + " 项");
        section.getFacts().add(fact("plans", "材料计划", String.valueOf(plans.size()), "count"));
        section.getFacts().add(fact("ordered", "已下单", String.valueOf(ordered), "count"));
        section.getFacts().add(fact("arrived", "已到场", String.valueOf(arrived), "count"));
        if (!selections.isEmpty()) {
            MaterialSelection latest = selections.get(0);
            section.getFacts().add(fact("selection", "选材版本", "V" + value(latest.getVersion(), 1), "text"));
            section.getFacts().add(fact("selectionStatus", "选材状态", latest.getStatus() == null ? "—" : latest.getStatus().getName(), "text"));
        }
        section.setItems(new ArrayList<>(plans.stream().limit(30).map(this::materialItem).toList()));
        section.getItems().addAll(selections.stream().limit(30).map(this::selectionItem).toList());
        return section;
    }

    private CustomerJourney.Section constructionSection(Project project, List<ProjectStage> stages, List<ProjectReport> reports) {
        CustomerJourney.Section section = section("construction", "施工", "阶段、进度与现场证据");
        long done = stages.stream().filter(item -> item.getStatus() == StageStatus.Done).count();
        section.setStatus(project.getStatus() == null ? null : project.getStatus().name());
        section.setStatusName(project.getStatus() == null ? null : project.getStatus().getName());
        section.setSummary(project.getCurrentStageName() == null ? "施工准备中" : "当前阶段：" + project.getCurrentStageName());
        section.getFacts().add(fact("progress", "整体进度", value(project.getProgress(), 0) + "%", "percent"));
        section.getFacts().add(fact("stages", "已完成阶段", done + "/" + stages.size(), "text"));
        section.getFacts().add(fact("reports", "现场更新", String.valueOf(reports.size()), "count"));
        section.setItems(new ArrayList<>(stages.stream().map(this::stageItem).toList()));
        section.getItems().addAll(reports.stream().limit(12).map(this::reportItem).toList());
        return section;
    }

    private CustomerJourney.Section acceptanceSection(List<Acceptance> values) {
        CustomerJourney.Section section = section("acceptance", "验收与整改", "阶段验收、问题和复验");
        long pending = values.stream().filter(item -> item.getStatus() == AcceptStatus.Pending).count();
        long rejected = values.stream().filter(item -> item.getStatus() == AcceptStatus.Rejected).count();
        long confirmed = values.stream().filter(item -> item.getStatus() == AcceptStatus.Confirmed).count();
        Acceptance latest = values.stream().findFirst().orElse(null);
        section.setStatus(rejected > 0 ? "rectifying" : pending > 0 ? "pending" : confirmed > 0 ? "done" : "none");
        section.setStatusName(rejected > 0 ? "整改中" : pending > 0 ? "待确认" : confirmed > 0 ? "已确认" : "暂无验收");
        section.setSummary(rejected > 0 ? rejected + " 个阶段待整改" : pending > 0 ? pending + " 个阶段等待您确认" : "已完成 " + confirmed + " 个阶段验收");
        section.getFacts().add(fact("pending", "待确认", String.valueOf(pending), "count"));
        section.getFacts().add(fact("rejected", "整改中", String.valueOf(rejected), "count"));
        section.getFacts().add(fact("confirmed", "已确认", String.valueOf(confirmed), "count"));
        if (latest != null && latest.getRectificationDueDate() != null) {
            section.getFacts().add(fact("dueDate", "最近整改截止", date(latest.getRectificationDueDate()), "date"));
        }
        section.setItems(values.stream().limit(20).map(this::acceptanceItem).toList());
        return section;
    }

    private CustomerJourney.Section paymentSection(List<Receivable> values) {
        // 业主口径：装企的"应收/已收/未收"在这里换算成业主的"应付/已付/待付"。
        CustomerJourney.Section section = section("payment", "付款", "按合同节点的付款进度");
        BigDecimal total = values.stream().map(Receivable::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = values.stream().map(Receivable::getReceivedAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = total.subtract(received);
        if (outstanding.signum() < 0) outstanding = BigDecimal.ZERO;
        Receivable next = values.stream().filter(item -> item.getDueDate() != null && outstanding(item)).findFirst().orElse(null);
        section.setStatus(outstanding.signum() == 0 && !values.isEmpty() ? "done" : "pending");
        section.setStatusName(outstanding.signum() == 0 && !values.isEmpty() ? "已付清" : "待付款");
        section.setSummary("已付 " + money(received) + "，待付 " + money(outstanding));
        section.getFacts().add(fact("total", "应付总额", money(total), "money"));
        section.getFacts().add(fact("received", "已付", money(received), "money"));
        section.getFacts().add(fact("outstanding", "待付", money(outstanding), "money"));
        if (next != null) {
            section.getFacts().add(fact("next", "下一节点", next.getName() + " · " + date(next.getDueDate()), "text"));
        }
        section.setItems(values.stream().map(this::paymentItem).toList());
        return section;
    }

    private CustomerJourney.Section feedbackSection(List<CustomerFeedback> values) {
        CustomerJourney.Section section = section("feedback", "意见与投诉", "提出问题并跟踪处理结果");
        long open = values.stream().filter(item -> item.getStatus() != CustomerFeedbackStatus.Closed).count();
        CustomerFeedback latest = values.stream().findFirst().orElse(null);
        section.setStatus(open > 0 ? "doing" : values.isEmpty() ? "none" : "done");
        section.setStatusName(open > 0 ? "处理中" : values.isEmpty() ? "暂无记录" : "已关闭");
        section.setSummary(latest == null ? "您可以随时提出意见或投诉" : latest.getTitle());
        section.getFacts().add(fact("total", "反馈总数", String.valueOf(values.size()), "count"));
        section.getFacts().add(fact("open", "处理中", String.valueOf(open), "count"));
        if (latest != null) section.getFacts().add(fact("latestStatus", "最近状态", latest.getStatus().getName(), "text"));
        section.setItems(values.stream().limit(30).map(this::feedbackItem).toList());
        return section;
    }

    private CustomerJourney.Item measureItem(Measure measure) {
        CustomerJourney.Item item = item("measure-" + measure.getMeasureId(), measure.getStatus() == null ? "量房记录" : measure.getStatus().getName());
        item.setSubtitle("上门量房留痕");
        item.setStatus(measure.getStatus() == null ? null : measure.getStatus().name());
        item.setStatusName(measure.getStatus() == null ? null : measure.getStatus().getName());
        String time = date(measure.getMeasureTime() == null ? measure.getCompletedAt() : measure.getMeasureTime());
        item.setDate(time);
        item.setBusinessType("Measure"); item.setBusinessId(measure.getMeasureId());
        if (measure.getSitePhotos() != null) {
            item.getImages().addAll(measure.getSitePhotos());
            int index = 0;
            for (String url : measure.getSitePhotos()) {
                item.getMedia().add(new CustomerJourney.Media(
                        "measure-" + measure.getMeasureId() + "-site-" + (index++), url, "Photo",
                        "现场原样照片", time, null));
            }
        }
        if (measure.getStructureImages() != null) {
            int index = 0;
            for (String url : measure.getStructureImages()) {
                item.getMedia().add(new CustomerJourney.Media(
                        "measure-" + measure.getMeasureId() + "-structure-" + (index++), url, "Structure",
                        "原始结构图", time, null));
            }
        }
        if (measure.getDesigner() != null) item.getFacts().add(fact("designer", "量房设计师", name(measure.getDesigner()), "text"));
        if (measure.getOriginalStructure() != null) item.getFacts().add(fact("structure", "原始结构", measure.getOriginalStructure(), "text"));
        if (measure.getDimensions() != null) item.getFacts().add(fact("dimensions", "尺寸记录", measure.getDimensions(), "text"));
        if (measure.getCustomerRequirements() != null) item.getLines().add("需求：" + measure.getCustomerRequirements());
        if (measure.getDesignerConclusion() != null) item.getLines().add("结论：" + measure.getDesignerConclusion());
        return item;
    }

    private CustomerJourney.Item designItem(Design design) {
        CustomerJourney.Item item = item("design-" + design.getDesignId(), design.getName());
        item.setSubtitle("设计方案 V" + value(design.getVersion(), 1));
        item.setStatus(design.getStatus() == null ? null : design.getStatus().name());
        item.setStatusName(design.getStatus() == null ? null : design.getStatus().getName());
        item.setBusinessType("Design"); item.setBusinessId(design.getDesignId());
        String time = date(design.getUpdateTime());
        // 图纸/效果图：CAD 预览、原始结构、拆改、砌筑、优化、现场照片，按用途标注。
        addDrawing(item, design.getDesignId(), design.getCadPreviewUrl(), "Cad", "CAD 平面图", time, null);
        addDrawing(item, design.getDesignId(), design.getCadUrl(), "Cad", "CAD 源文件", time, null);
        addDrawing(item, design.getDesignId(), design.getCoverUrl(), "Render", "方案效果图", time, null);
        addDrawing(item, design.getDesignId(), design.getOriginalPlanUrl(), "Original", "原始结构图", time, null);
        addDrawing(item, design.getDesignId(), design.getDemolitionUrl(), "Demolition", "拆改图", time, null);
        addDrawing(item, design.getDesignId(), design.getMasonryUrl(), "Masonry", "砌筑图", time, null);
        addDrawing(item, design.getDesignId(), design.getOptimizationUrl(), "Optimization", "优化方案图", time, null);
        addDrawings(item, design.getDesignId(), design.getOriginalPlanImages(), "Original", "原始结构图", time, null);
        addDrawings(item, design.getDesignId(), design.getSitePhotos(), "SitePhoto", "现场照片", time, null);
        addDrawings(item, design.getDesignId(), design.getSurveyPhotos(), "SurveyPhoto", "量房照片", time, null);

        List<Space> spaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        List<DesignItem> designItems = designItemRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId());
        for (Space space : spaces) {
            CustomerJourney.Region region = new CustomerJourney.Region();
            region.setKey("region-" + space.getSpaceId());
            region.setName(space.getName());
            region.setType(space.getType() == null ? null : space.getType().getName());
            region.setLength(decimal(space.getLength()));
            region.setWidth(decimal(space.getWidth()));
            region.setHeight(decimal(space.getHeight()));
            region.setArea(decimal(space.getArea()));
            region.setPerimeter(decimal(space.getPerimeter()));
            // DEC-062：未录入为 null；历史推算值标记为「估算」，业主侧只作参考提示，不作为工程量依据。
            region.setAreaSource(space.getAreaSource() == null ? null : space.getAreaSource().getName());
            region.setRemark(space.getRemark());
            if (space.getImages() != null) region.getImages().addAll(space.getImages());
            long count = designItems.stream()
                    .filter(d -> d.getSpace() != null && space.getSpaceId() != null
                            && space.getSpaceId().equals(d.getSpace().getSpaceId()))
                    .count();
            region.setItemCount((int) count);
            item.getRegions().add(region);
        }
        item.setPriceGroups(designItemGroups(designItems));
        if (design.getStyle() != null) item.getFacts().add(fact("style", "设计风格", design.getStyle(), "text"));
        if (design.getArea() != null) item.getFacts().add(fact("area", "套内面积", decimal(design.getArea()) + "㎡", "text"));
        if (design.getLayout() != null) item.getFacts().add(fact("layout", "户型", design.getLayout(), "text"));
        if (design.getDescription() != null) item.getLines().add(design.getDescription());
        return item;
    }

    private CustomerJourney.Item quoteItem(Quote quote) {
        CustomerJourney.Item item = item("quote-" + quote.getQuoteId(), quote.getNo());
        item.setSubtitle("客户报价 V" + value(quote.getVersion(), 1));
        item.setStatus(quote.getStatus() == null ? null : quote.getStatus().name());
        item.setStatusName(quote.getStatus() == null ? null : quote.getStatus().getName());
        item.setAmount(money(quote.getAmount()));
        item.setDate(date(quote.getAcceptedAt() == null ? quote.getSubmittedAt() : quote.getAcceptedAt()));
        List<QuoteLine> lines = quoteLineRepository.findByQuote(quote);
        item.setPriceGroups(quoteGroups(lines));
        if (quote.getSubtotal() != null) item.getFacts().add(fact("subtotal", "清单小计", money(quote.getSubtotal()), "money"));
        if (quote.getTaxAmount() != null) item.getFacts().add(fact("tax", "税金", money(quote.getTaxAmount()), "money"));
        item.getFacts().add(fact("amount", "报价总额", money(quote.getAmount()), "money"));
        if (quote.getDiscount() != null && quote.getDiscount().compareTo(BigDecimal.ONE) < 0) {
            item.getFacts().add(fact("discount", "折扣", quote.getDiscount().stripTrailingZeros().toPlainString(), "text"));
        }
        return item;
    }

    /** 设计分项按区域分组，作为方案“每个区域做了哪些设计”的结构化清单。 */
    private List<CustomerJourney.PriceGroup> designItemGroups(List<DesignItem> items) {
        java.util.LinkedHashMap<String, CustomerJourney.PriceGroup> groups = new java.util.LinkedHashMap<>();
        for (DesignItem designItem : items) {
            String regionName = designItem.getSpace() == null || designItem.getSpace().getName() == null
                    ? "综合项目" : designItem.getSpace().getName();
            String key = designItem.getSpace() == null ? "group-none" : "group-" + designItem.getSpace().getSpaceId();
            CustomerJourney.PriceGroup group = groups.computeIfAbsent(key, k -> {
                CustomerJourney.PriceGroup g = new CustomerJourney.PriceGroup();
                g.setKey(k); g.setName(regionName);
                return g;
            });
            CustomerJourney.PriceRow row = new CustomerJourney.PriceRow();
            row.setKey("design-item-" + designItem.getItemId());
            row.setName(designItem.getName());
            row.setCategory(designItem.getCategory());
            row.setBrand(designItem.getBrand());
            row.setSpec(designItem.getSpec());
            row.setUnit(designItem.getUnit() == null ? null : designItem.getUnit().getName());
            row.setQuantity(decimal(designItem.getQuantity()));
            row.setType(designItem.getType() == null ? null : designItem.getType().getName());
            // 施工工艺/验收标准是业主判断"这套活儿怎么做"的核心信息，随行下发；
            // 分项自身快照优先（签约口径），历史行缺快照时兜底材料库主数据。
            CatalogItem catalog = designItem.getCatalogItem();
            row.setCraft(CraftText.firstNonBlank(designItem.getCraft(),
                    catalog == null ? null : catalog.getCraft()));
            row.setCriterion(CraftText.firstNonBlank(designItem.getCriterion(),
                    catalog == null ? null : catalog.getCriterion()));
            group.getRows().add(row);
        }
        return new ArrayList<>(groups.values());
    }

    /** 报价行按区域分组，业主可逐区域核对“每个区域有哪些项目”。 */
    private List<CustomerJourney.PriceGroup> quoteGroups(List<QuoteLine> lines) {
        java.util.LinkedHashMap<String, CustomerJourney.PriceGroup> groups = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, BigDecimal> totals = new java.util.LinkedHashMap<>();
        for (QuoteLine line : lines) {
            String regionName = line.getSpace() == null || line.getSpace().getName() == null
                    ? "综合项目" : line.getSpace().getName();
            String key = line.getSpace() == null ? "group-none" : "group-" + line.getSpace().getSpaceId();
            CustomerJourney.PriceGroup group = groups.computeIfAbsent(key, k -> {
                CustomerJourney.PriceGroup g = new CustomerJourney.PriceGroup();
                g.setKey(k); g.setName(regionName);
                return g;
            });
            CustomerJourney.PriceRow row = new CustomerJourney.PriceRow();
            row.setKey("quote-line-" + line.getItemId());
            row.setName(line.getName());
            row.setCategory(line.getCategory());
            row.setBrand(line.getBrand());
            row.setSpec(line.getSpec());
            row.setUnit(line.getUnit() == null ? null : line.getUnit().getName());
            row.setQuantity(decimal(line.getQuantity()));
            row.setPrice(money(line.getPrice()));
            row.setAmount(money(line.getAmount()));
            row.setRole(line.getRole() == null ? null : line.getRole().getName());
            row.setType(line.getType() == null ? null : line.getType().getName());
            // 报价行快照 → 材料库主数据 → 设计分项快照，任一命中即可。
            CatalogItem catalog = line.getCatalogItem();
            DesignItem designItem = line.getDesignItem();
            row.setCraft(CraftText.firstNonBlank(line.getCraft(),
                    catalog == null ? null : catalog.getCraft(),
                    designItem == null ? null : designItem.getCraft()));
            row.setCriterion(CraftText.firstNonBlank(
                    designItem == null ? null : designItem.getCriterion(),
                    catalog == null ? null : catalog.getCriterion()));
            group.getRows().add(row);
            totals.merge(key, line.getAmount() == null ? BigDecimal.ZERO : line.getAmount(), BigDecimal::add);
        }
        for (CustomerJourney.PriceGroup group : groups.values()) {
            BigDecimal total = totals.getOrDefault(group.getKey(), BigDecimal.ZERO);
            group.setTotal(money(total));
            group.setSummary(group.getRows().size() + " 项 · 小计 " + money(total));
        }
        return new ArrayList<>(groups.values());
    }

    private void addDrawing(CustomerJourney.Item item, Long designId, String url, String kind,
                            String title, String time, String room) {
        if (url == null || url.isBlank()) return;
        item.getMedia().add(new CustomerJourney.Media(
                "design-" + designId + "-" + kind + "-" + item.getMedia().size(), url, kind, title, time, room));
    }

    private void addDrawings(CustomerJourney.Item item, Long designId, List<String> urls, String kind,
                             String title, String time, String room) {
        if (urls == null) return;
        int index = 0;
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;
            item.getMedia().add(new CustomerJourney.Media(
                    "design-" + designId + "-" + kind + "-" + (index++), url, kind, title, time, room));
        }
    }

    private String name(com.th4.edge.tenant.user.model.Employee employee) {
        return employee == null ? null : employee.getName();
    }

    private String decimal(BigDecimal value) {
        if (value == null) return null;
        return value.stripTrailingZeros().toPlainString();
    }

    private CustomerJourney.Item contractItem(ProjectContract contract) {
        CustomerJourney.Item item = item("contract-" + contract.getContractId(), contract.getNo());
        item.setSubtitle("合同履约额度");
        item.setStatus(contract.getStatus() == null ? null : contract.getStatus().name());
        item.setStatusName(contract.getStatus() == null ? null : contract.getStatus().getName());
        item.setAmount(money(contract.getCurrentAmount()));
        item.setDate(date(contract.getSignedAt()));
        return item;
    }

    private CustomerJourney.Item changeItem(ContractChange change,
                                            List<ContractChangeItem> items,
                                            List<String> evidenceImages) {
        CustomerJourney.Item item = item("change-" + change.getChangeId(), change.getNo());
        boolean effective = change.getStatus() == ContractChangeStatus.CustomerConfirmed;
        item.setSubtitle(change.getReason());
        item.setStatus(change.getStatus() == null ? null : change.getStatus().name());
        item.setStatusName(effective ? "已生效" : "审批中");
        // DEC-068 业主端两态：待审批不返回金额，避免未生效金额被业主当作已定的钱；
        // 已生效才返回变更金额与生效时间。
        item.setAmount(effective ? money(change.getAmount()) : null);
        item.setBusinessType("ContractChange");
        item.setBusinessId(change.getChangeId());
        item.setDate(effective ? date(change.getApprovedAt()) : null);
        if (effective) {
            item.getFacts().add(fact("approvedAt", "生效时间", date(change.getApprovedAt()), "date"));
        } else {
            item.getFacts().add(fact("pendingNotice", "说明",
                    "审批通过后自动更新合同金额与施工、材料计划", "text"));
        }
        items.stream().limit(100).forEach(detail -> item.getLines().add(
                detail.getName() + " · 原数量 " + detail.getOldQuantity() + " → " + detail.getNewQuantity()
                        + " · 金额变化 " + money(detail.getAmountDelta())));
        List<String> images = new ArrayList<>(evidenceImages == null ? List.of() : evidenceImages);
        if (change.getSignatureUrl() != null && !change.getSignatureUrl().isBlank()) {
            images.add(change.getSignatureUrl());
        }
        item.setImages(images.stream().filter(value -> value != null && !value.isBlank()).distinct().toList());
        // DEC-068：业主只读可见变更结果（原因、明细、状态、已生效金额），
        // 不暴露成本、成本变化、内部审批意见与内部备注；业主端零可编辑控件。
        return item;
    }

    private CustomerJourney.Item materialItem(MaterialPlan plan) {
        CustomerJourney.Item item = item("material-" + plan.getPlanId(), plan.getSelection() == null ? "材料计划" : plan.getSelection().getTitle());
        item.setSubtitle("材料计划");
        item.setStatus(plan.getStatus() == null ? null : plan.getStatus().name());
        item.setStatusName(plan.getStatus() == null ? null : plan.getStatus().getName());
        item.setDate(date(plan.getArrivedAt() == null ? plan.getRequiredAt() : plan.getArrivedAt()));
        item.getFacts().add(fact("quantity", "数量", plan.getQuantity() + unit(plan.getUnit()), "text"));
        if (plan.getSelectedBrand() != null) item.getFacts().add(fact("brand", "品牌", plan.getSelectedBrand(), "text"));
        if (plan.getSelectedSpec() != null) item.getFacts().add(fact("spec", "规格", plan.getSelectedSpec(), "text"));
        return item;
    }

    private CustomerJourney.Item selectionItem(MaterialSelection selection) {
        CustomerJourney.Item item = item("selection-" + selection.getSelectionId(), selection.getTitle());
        item.setSubtitle("主材选材 V" + value(selection.getVersion(), 1));
        item.setStatus(selection.getStatus() == null ? null : selection.getStatus().name());
        item.setStatusName(selection.getStatus() == null ? null : selection.getStatus().getName());
        item.setDate(date(selection.getUpdateTime()));
        // 历史坑：这里原本直接把 selectionJson 塞进 lines，业主端会把
        // {"color":"...","spec":"..."} 一长串原样渲染出来。选材结果必须是"标签 + 值"的结构化字段，
        // JSON 只用于回显表单，可读文本仅作兜底。
        List<SelectionText.Field> fields = SelectionText.fields(selection.getContent(), selection.getSelectionJson());
        for (SelectionText.Field field : fields) {
            if (field.label() == null || field.label().isBlank()) {
                item.getLines().add(field.value());
            } else {
                item.getFacts().add(fact(field.key(), field.label(), field.value(), "text"));
            }
        }
        if (fields.isEmpty() && selection.getContent() != null && !selection.getContent().isBlank()) {
            item.getLines().add(selection.getContent());
        }
        return item;
    }

    private CustomerJourney.Item stageItem(ProjectStage stage) {
        CustomerJourney.Item item = item("stage-" + stage.getStageId(), stage.getName());
        item.setSubtitle("施工阶段");
        item.setStatus(stage.getStatus() == null ? null : stage.getStatus().name());
        item.setStatusName(stage.getStatus() == null ? null : stage.getStatus().getName());
        item.setDate(date(stage.getPlannedStart()) + "~" + date(stage.getPlannedEnd()));
        item.getFacts().add(fact("progress", "进度", value(stage.getProgress(), 0) + "%", "percent"));
        return item;
    }

    private CustomerJourney.Item reportItem(ProjectReport report) {
        CustomerJourney.Item item = item("report-" + report.getReportId(), report.getTitle());
        item.setSubtitle(report.getContent());
        item.setDate(date(report.getCreateTime()));
        item.setBusinessType("Report"); item.setBusinessId(report.getReportId());
        if (report.getImages() != null) item.getImages().addAll(report.getImages());
        return item;
    }

    private CustomerJourney.Item acceptanceItem(Acceptance acceptance) {
        CustomerJourney.Item item = item("acceptance-" + acceptance.getAcceptanceId(), "阶段验收单");
        item.setSubtitle(acceptance.getStage() == null ? null : acceptance.getStage().getName());
        item.setStatus(acceptance.getStatus() == null ? null : acceptance.getStatus().name());
        item.setStatusName(acceptance.getStatus() == null ? null : acceptance.getStatus().getName());
        item.setDate(date(acceptance.getConfirmedAt() == null ? acceptance.getCreateTime() : acceptance.getConfirmedAt()));
        item.setBusinessType("Acceptance"); item.setBusinessId(acceptance.getAcceptanceId());
        if (acceptance.getIssues() != null) item.getLines().addAll(acceptance.getIssues());
        // XA-20260924-006：整改闭环对业主必须透明——谁负责整改、什么时候到期、这是第几轮
        Employee rectifyOwner = acceptance.getRectificationOwner();
        if (rectifyOwner != null) {
            item.getFacts().add(fact("rectifyOwner", "整改责任人", rectifyOwner.getName(), "text"));
        }
        if (acceptance.getRectificationType() != null) {
            item.getFacts().add(fact("rectifyType", "整改类型",
                    acceptance.getRectificationType().getName(), "text"));
        }
        if (acceptance.getRectificationDueDate() != null) {
            item.getFacts().add(fact("dueDate", "整改截止", date(acceptance.getRectificationDueDate()), "date"));
        }
        Integer roundNo = acceptanceRoundRepository.findFirstByAcceptanceOrderByRoundNoDesc(acceptance)
                .map(AcceptanceRound::getRoundNo).orElse(null);
        if (roundNo != null) {
            item.getFacts().add(fact("round", "整改轮次", "第 " + roundNo + " 轮", "text"));
        }
        return item;
    }

    private CustomerJourney.Item paymentItem(Receivable receivable) {
        CustomerJourney.Item item = item("payment-" + receivable.getReceivableId(), receivable.getName());
        item.setSubtitle(receivable.getStageName());
        item.setStatus(receivable.getStatus() == null ? null : receivable.getStatus().name());
        item.setStatusName(paymentStatusName(receivable.getStatus()));
        BigDecimal amount = receivable.getAmount() == null ? BigDecimal.ZERO : receivable.getAmount();
        BigDecimal paid = receivable.getReceivedAmount() == null ? BigDecimal.ZERO : receivable.getReceivedAmount();
        BigDecimal unpaid = amount.subtract(paid);
        if (unpaid.signum() < 0) unpaid = BigDecimal.ZERO;
        item.setAmount(money(receivable.getAmount()));
        item.setDate(date(receivable.getDueDate()));
        item.getFacts().add(fact("received", "已付", money(paid), "money"));
        item.getFacts().add(fact("outstanding", "待付", money(unpaid), "money"));
        return item;
    }

    private CustomerJourney.Item feedbackItem(CustomerFeedback feedback) {
        CustomerJourney.Item item = item("feedback-" + feedback.getFeedbackId(), feedback.getTitle());
        item.setSubtitle(feedback.getType() == null ? null : feedback.getType().getName());
        item.setStatus(feedback.getStatus() == null ? null : feedback.getStatus().name());
        item.setStatusName(feedback.getStatus() == null ? null : feedback.getStatus().getName());
        item.setDate(date(feedback.getDueAt()));
        item.setBusinessType("Feedback"); item.setBusinessId(feedback.getFeedbackId());
        item.getLines().add(feedback.getContent());
        if (feedback.getResponse() != null) item.getLines().add("处理回复：" + feedback.getResponse());
        return item;
    }

    /**
     * 付款节点状态：业主端用"付款"口径。
     *
     * <p>{@code ReceivableStatus.getName()} 是装企口径（待收/部分收款/已收清），
     * 员工端财务页面继续用它；业主旅程只读投影必须换算成待付/部分付款/已付清。</p>
     */
    private String paymentStatusName(ReceivableStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case Pending -> "待付款";
            case Partial -> "部分付款";
            case Received -> "已付清";
            case Overdue -> "已逾期";
            case Closed -> "已关闭";
        };
    }

    /**
     * 业主可见项目状态。
     *
     * <p>优先级：终态（已完工/已结算）保留事实；否则**以是否存在 Pending 验收单**判定"待验收"，
     * 再按阶段是否已开工判定"施工中/待开工"。停工状态保留。</p>
     */
    private ProjectStatus resolveOwnerStatus(Project project, List<ProjectStage> stages, List<Acceptance> acceptances) {
        ProjectStatus current = project.getStatus() == null ? ProjectStatus.Preparing : project.getStatus();
        if (current == ProjectStatus.Finished || current == ProjectStatus.Settled) {
            return current;
        }
        if (current == ProjectStatus.Suspended) {
            return current;
        }
        boolean pendingAcceptance = acceptances.stream()
                .anyMatch(item -> item.getStatus() == AcceptStatus.Pending);
        if (pendingAcceptance) {
            return ProjectStatus.Accepting;
        }
        boolean anyStarted = stages.stream().anyMatch(stage ->
                stage.getStatus() == StageStatus.Doing || stage.getStatus() == StageStatus.Done);
        if (anyStarted) {
            return ProjectStatus.Ongoing;
        }
        boolean allDone = !stages.isEmpty() && stages.stream().allMatch(stage -> stage.getStatus() == StageStatus.Done);
        return allDone ? ProjectStatus.Finished : ProjectStatus.Preparing;
    }

    /**
     * 当前阶段：取正在施工 / 待验收的阶段，没有则取第一个阶段。
     *
     * <p>口径与 {@code ProjectOverviewContext.currentStageName}、{@code project/project/page} 保持一致。</p>
     */
    private String currentStageName(List<ProjectStage> stages) {
        return stages.stream()
                .filter(stage -> stage.getStatus() == StageStatus.Doing || stage.getStatus() == StageStatus.Accepting)
                .findFirst()
                .or(() -> stages.stream().findFirst())
                .map(ProjectStage::getName)
                .orElse(null);
    }

    private String unit(Enumerable<?> unit) {
        return unit == null ? "" : " " + unit.getName();
    }

    private CustomerJourney.Item item(String key, String title) {
        CustomerJourney.Item item = new CustomerJourney.Item();
        item.setKey(key); item.setTitle(title);
        return item;
    }

    private List<CustomerJourney.Action> collectActions(List<Acceptance> acceptances,
                                                        List<ContractChange> changes,
                                                        List<MaterialSelection> selections,
                                                        List<CustomerFeedback> feedbacks) {
        List<CustomerJourney.Action> actions = new ArrayList<>();
        acceptances.stream().filter(item -> item.getStatus() == AcceptStatus.Pending).findFirst().ifPresent(item ->
                actions.add(new CustomerJourney.Action("acceptance-" + item.getAcceptanceId(), "CONFIRM_ACCEPTANCE",
                        "确认阶段验收", "阶段已提交验收，请您确认或提出整改要求", "AcceptanceStage",
                        item.getStage() == null ? null : item.getStage().getStageId())));
        feedbacks.stream().filter(item -> item.getStatus() != CustomerFeedbackStatus.Closed).findFirst().ifPresent(item ->
                actions.add(new CustomerJourney.Action("feedback-" + item.getFeedbackId(), "VIEW_FEEDBACK",
                        "跟踪我的意见", item.getTitle(), "CustomerFeedback", item.getFeedbackId())));
        actions.add(new CustomerJourney.Action("feedback-new", "SUBMIT_FEEDBACK",
                "提出意见或投诉", "现场发现问题时可直接反馈给项目团队", "CustomerFeedback", null));
        return actions;
    }

    private boolean ordered(MaterialPlan plan) {
        return plan.getStatus() == MaterialPlanStatus.PartiallyOrdered
                || plan.getStatus() == MaterialPlanStatus.Ordered
                || plan.getStatus() == MaterialPlanStatus.PartiallyArrived
                || plan.getStatus() == MaterialPlanStatus.Arrived;
    }

    private boolean outstanding(Receivable item) {
        return item.getStatus() != com.th4.edge.decoboon.finance.configure.ReceivableStatus.Received
                && item.getStatus() != com.th4.edge.decoboon.finance.configure.ReceivableStatus.Closed;
    }

    private CustomerJourney.Section section(String key, String title, String subtitle) {
        CustomerJourney.Section section = new CustomerJourney.Section();
        section.setKey(key); section.setTitle(title); section.setSubtitle(subtitle);
        return section;
    }
    private CustomerJourney.Fact fact(String key, String label, String value, String valueType) {
        return new CustomerJourney.Fact(key, label, value, valueType);
    }
    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    /**
     * 时间文案。
     *
     * <p>历史坑：实体的审计时间用的是框架 {@code DateTime}（@Embedded，未覆写 toString），
     * 直接 {@code toString()} 会输出 {@code com.th4.edge.framework.data.date.model.DateTime@1a2b}。
     * 业主端「项目旅程 · 施工」里"阶段开工 · 开工交底"这条现场汇报的日期就是这么漏出来的。
     * 这里统一把框架 DateTime / 14 位时间数字收敛成 {@code yyyy-MM-dd HH:mm:ss}。</p>
     */
    private String date(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof DateTime dateTime) {
            return DateTimeText.format(dateTime);
        }
        // LocalDateTime/LocalDate 的 toString 会带 T（2026-09-10T05:03:07）；业主端一律收敛成可读格式
        if (value instanceof java.time.LocalDateTime dateTime) {
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (value instanceof java.time.LocalDate date) {
            return date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        String text = value.toString();
        return text.matches("\\d{14}") ? DateTimeText.formatPacked(Long.parseLong(text)) : text;
    }

    private int value(Integer value, int fallback) { return value == null ? fallback : value; }
}
