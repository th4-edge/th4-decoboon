package com.th4.edge.decoboon.project.context;

import com.th4.edge.components.lock.DistributedLocked;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.collab.context.SubscribeMessageContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.context.DesignLifecycleContext;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.AcceptanceRectificationType;
import com.th4.edge.decoboon.material.context.MaterialCategoryContext;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialSourceType;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.decoboon.project.configure.PlanDeviationStatus;
import com.th4.edge.decoboon.project.repository.ProjectPlanDeviationRepository;
import com.th4.edge.decoboon.project.model.AcceptanceRound;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 项目施工闭环上下文。
 *
 * <p>负责把已接受报价展开为项目、阶段、施工任务和材料计划，并在任务、材料、
 * 验收状态变化后统一刷新项目汇总。所有业务状态变化收敛在这里，实体只承载数据。</p>
 */
@Component
public class ProjectContext {

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository quoteItemRepository;
    private final ProjectRepository projectRepository;
    private final ProjectContractRepository projectContractRepository;
    private final ContractLineRepository contractLineRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectPlanDeviationRepository deviationRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ReceivableRepository receivableRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final AcceptanceRoundRepository acceptanceRoundRepository;
    private final CustomerRepository customerRepository;
    private final PropertyContext propertyContext;
    private final PropertyRepository propertyRepository;
    private final ProjectPermissionContext permissionContext;
    private final UserIdentityContext userIdentityContext;
    private final NoticeContext noticeContext;
    private final SubscribeMessageContext subscribeMessageContext;
    private final ProjectCostContext projectCostContext;
    private final ContractPlanContext contractPlanContext;
    private final MaterialPlanContext materialPlanContext;
    private final MaterialCategoryContext materialCategoryContext;
    private final DesignLifecycleContext designLifecycleContext;

    public ProjectContext(QuoteRepository quoteRepository,
                          QuoteLineRepository quoteItemRepository,
                          ProjectRepository projectRepository,
                          ProjectStageRepository stageRepository,
                          ProjectTaskRepository taskRepository,
                          ProjectPlanDeviationRepository deviationRepository,
                          MaterialPlanRepository materialPlanRepository,
                          ReceivableRepository receivableRepository,
                          AcceptanceRepository acceptanceRepository,
                          AcceptanceRoundRepository acceptanceRoundRepository,
                          CustomerRepository customerRepository,
                          PropertyContext propertyContext,
                          PropertyRepository propertyRepository,
                          ProjectPermissionContext permissionContext,
                          ProjectContractRepository projectContractRepository,
                          ContractLineRepository contractLineRepository,
                          UserIdentityContext userIdentityContext,
                          NoticeContext noticeContext,
                          SubscribeMessageContext subscribeMessageContext,
                          ProjectCostContext projectCostContext,
                          ContractPlanContext contractPlanContext,
                          MaterialPlanContext materialPlanContext,
                          MaterialCategoryContext materialCategoryContext,
                          DesignLifecycleContext designLifecycleContext) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.deviationRepository = deviationRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.receivableRepository = receivableRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.acceptanceRoundRepository = acceptanceRoundRepository;
        this.customerRepository = customerRepository;
        this.propertyContext = propertyContext;
        this.propertyRepository = propertyRepository;
        this.permissionContext = permissionContext;
        this.projectContractRepository = projectContractRepository;
        this.contractLineRepository = contractLineRepository;
        this.userIdentityContext = userIdentityContext;
        this.noticeContext = noticeContext;
        this.subscribeMessageContext = subscribeMessageContext;
        this.projectCostContext = projectCostContext;
        this.contractPlanContext = contractPlanContext;
        this.materialPlanContext = materialPlanContext;
        this.materialCategoryContext = materialCategoryContext;
        this.designLifecycleContext = designLifecycleContext;
    }

    /**
     * 由客户接受的报价生成完整施工计划。
     *
     * @param quoteId 报价索引
     * @param draft 项目基础信息，至少包含名称和工地地址
     * @return 已生成阶段、任务和材料计划的项目
     */
    @DistributedLocked(key = "#root.quoteProjectLockKey(#quote)")
    @Transactional
    public Project createFromQuote(Quote quote, Project draft) {
        if (quote == null || quote.getQuoteId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价不能为空"));
        }
        ProjectContract contract = projectContractRepository.findByQuote_QuoteId(quote.getQuoteId())
                .filter(item -> item.getStatus() == com.th4.edge.decoboon.project.configure.ContractStatus.Effective)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "请先完成合同签约并生效，再创建项目")));
        return createFromEffectiveContract(contract, draft);
    }

    @DistributedLocked(key = "#root.contractProjectLockKey(#contract)")
    @Transactional
    public Project createFromEffectiveContract(ProjectContract contract, Project draft) {
        if (contract == null || contract.getContractId() == null
                || contract.getStatus() != com.th4.edge.decoboon.project.configure.ContractStatus.Effective) {
            throw new DefinedException(new DefaultErrorCode("400", "只有已生效合同可以创建项目"));
        }
        if (contract.getProject() != null) {
            generateReceivablePlan(contract, contract.getProject());
            return contract.getProject();
        }
        Project project = createProjectForContract(contract, draft);
        contract.setProject(project);
        projectContractRepository.save(contract);
        generateReceivablePlan(contract, project);
        return project;
    }

    private Project createProjectForContract(ProjectContract contract, Project draft) {
        Quote quote = contract.getQuote();
        if (projectRepository.existsByQuote(quote)) {
            throw new DefinedException(new DefaultErrorCode("400", "该报价已经生成项目"));
        }
        Property property = quote.getProperty();
        if (property == null || property.getPropertyId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价缺少客户房屋，请先建立并选择房屋档案"));
        }
        property = propertyRepository.findByPropertyIdAndCompany_ComId(
                        property.getPropertyId(), quote.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋不存在")));
        if (projectRepository.existsByCompany_ComIdAndProperty_PropertyId(
                quote.getCompany().getComId(), property.getPropertyId())) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已经生成唯一项目，不能重复立项"));
        }
        if (quoteRepository.existsByProperty_PropertyIdAndStatusInAndQuoteIdNot(
                property.getPropertyId(), List.of(QuoteStatus.Accepted, QuoteStatus.Converted), quote.getQuoteId())) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已有其他客户确认报价"));
        }
        supersedeOtherQuotes(property, quote.getQuoteId());
        if (draft.getCompany() == null || quote.getCompany() == null
                || !quote.getCompany().getComId().equals(draft.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "项目企业与报价企业不一致"));
        }

        Project project = new Project();
        project.setNo("P" + System.currentTimeMillis());
        project.setName(draft.getName());
        project.setCompany(quote.getCompany());
        project.setProperty(property);
        project.setCustomer(property.getCustomer());
        permissionContext.apply(project);
        project.setQuote(quote);
        project.setDesigner(draft.getDesigner() == null ? quote.getCreatedBy() : draft.getDesigner());
        project.setManager(draft.getManager());
        project.setSiteAddr(draft.getSiteAddr());
        project.setArea(property.getArea() == null || property.getArea().signum() == 0 ? quote.getArea() : property.getArea());
        project.setContractAmount(contract.getCurrentAmount() == null
                ? quote.getAmount() : contract.getCurrentAmount());
        BigDecimal signedCost = BigDecimal.ZERO;
        for (ContractLine line : contractLineRepository.findByContractOrderBySortAsc(contract)) {
            signedCost = signedCost.add(line.getCost() == null ? BigDecimal.ZERO : line.getCost());
        }
        project.setCost(signedCost);
        project.setStartDate(draft.getStartDate());
        project.setPlanEndDate(draft.getPlanEndDate());
        project.setStatus(ProjectStatus.Preparing);
        project.setProgress(0);
        projectRepository.save(project);

        List<ContractLine> contractLines = contractLineRepository.findByContractOrderBySortAsc(contract);
        ContractConstructionPlan constructionPlan = contractPlanContext.constructionPlan(contract);
        if (constructionPlan != null && !constructionPlan.getStages().isEmpty()) {
            generateFromConstructionPlan(project, constructionPlan, contractLines);
        } else {
            generateFromContract(project, contractLines);
            scheduleStages(project);
        }
        refreshProject(project);
        Quote converted = quote;
        converted.setStatus(QuoteStatus.Converted);
        converted.setConvertedAt(java.time.LocalDateTime.now());
        quoteRepository.save(converted);
        // 项目立项说明方案已经落地施工，同步推进设计方案状态，保证业主端不再显示待谈单。
        designLifecycleContext.markSelected(converted.getDesign());
        property.setStatus(com.th4.edge.decoboon.property.configure.PropertyStatus.Serving);
        propertyRepository.save(property);
        property.getCustomer().setStatus(CustStatus.Serving);
        customerRepository.save(property.getCustomer());
        return project;
    }

    /**
     * 推进施工任务状态并刷新项目进度。
     */
    @Transactional
    public ProjectTask updateTask(Long taskId, Long companyId, TaskStatus status, List<String> evidenceUrls) {
        ProjectTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "施工任务不存在")));
        requireTenant(task.getCompany(), companyId);
        if (status == null) {
            throw new DefinedException(new DefaultErrorCode("400", "任务状态不能为空"));
        }
        task.setStatus(status);
        if (TaskStatus.Doing.equals(status)) {
            task.setProgress(Math.max(task.getProgress(), 10));
            task.setActualStart(LocalDate.now());
        } else if (TaskStatus.Accepting.equals(status)) {
            task.setProgress(90);
        } else if (TaskStatus.Done.equals(status)) {
            if (evidenceUrls == null || evidenceUrls.isEmpty()) {
                throw new DefinedException(new DefaultErrorCode("400", "任务验收前必须上传现场照片或视频"));
            }
            task.setProgress(100);
            task.setActualEnd(LocalDate.now());
        } else if (TaskStatus.Todo.equals(status)) {
            task.setProgress(0);
        }
        if (evidenceUrls != null) {
            task.setEvidenceUrls(new ArrayList<>(evidenceUrls));
        }
        ProjectTask saved = taskRepository.save(task);
        refreshProject(task.getProject());
        return saved;
    }

    /**
     * 推进材料计划状态。
     */
    @Transactional
    public MaterialPlan updateMaterialPlan(Long planId, Long companyId, MaterialPlanStatus status, LocalDate arrivedAt) {
        MaterialPlan plan = materialPlanRepository.findById(planId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在")));
        requireTenant(plan.getCompany(), companyId);
        if (status == null) {
            throw new DefinedException(new DefaultErrorCode("400", "材料状态不能为空"));
        }
        plan.setStatus(status);
        if (MaterialPlanStatus.Arrived.equals(status) || MaterialPlanStatus.Closed.equals(status)) {
            plan.setArrivedAt(arrivedAt == null ? LocalDate.now() : arrivedAt);
        }
        MaterialPlan saved = materialPlanRepository.save(plan);
        refreshProject(plan.getProject());
        return saved;
    }

    /**
     * 阶段施工完成后提交验收（第 1 轮）。
     */
    @DistributedLocked(key = "'stage-acceptance:' + #companyId + ':' + #stageId")
    @Transactional
    public Acceptance submitAcceptance(Long companyId, Long projectId, Long stageId, Employee employee,
                                       String conclusion, List<String> issues, List<String> evidenceUrls) {
        return submitAcceptance(companyId, projectId, stageId, employee, conclusion, issues, evidenceUrls,
                null, null, null, null);
    }

    @Transactional
    public Acceptance submitAcceptance(Long companyId, Long projectId, Long stageId, Employee employee,
                                       String conclusion, List<String> issues, List<String> evidenceUrls,
                                       Employee rectificationOwner, LocalDate rectificationDueDate) {
        return submitAcceptance(companyId, projectId, stageId, employee, conclusion, issues, evidenceUrls,
                rectificationOwner, rectificationDueDate, null, null);
    }

    /**
     * 提交验收或提交复验：不合格复用同一阶段与同一张验收单，每次提交追加一条轮次（append-only）。
     *
     * <p>业务口径（用户 2026-09-21 裁决）：整改阶段级、循环不限次数、原验收记录保留并追加结论、
     * 只做质量事实不碰账。第 2 轮起必须携带整改说明与整改后照片。</p>
     */
    @Transactional
    public Acceptance submitAcceptance(Long companyId, Long projectId, Long stageId, Employee employee,
                                       String conclusion, List<String> issues, List<String> evidenceUrls,
                                       Employee rectificationOwner, LocalDate rectificationDueDate,
                                       String rectificationNote, List<String> rectificationEvidenceUrls) {
        // 同一阶段的所有提交/复验先锁定阶段事实，再读取验收单；并发后到请求
        // 会排队并命中 Pending 幂等分支，不再触发唯一键冲突或未定义错误。
        ProjectStage stage = stageRepository.findByStageId(stageId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在")));
        requireTenant(stage.getCompany(), companyId);
        if (!stage.getProject().getProjectId().equals(projectId)) {
            throw new DefinedException(new DefaultErrorCode("400", "阶段不属于当前项目"));
        }

        Acceptance acceptance = acceptanceRepository.findFirstByStageOrderByAcceptanceIdDesc(stage).orElse(null);
        if (acceptance != null && AcceptStatus.Confirmed.equals(acceptance.getStatus())) {
            // 复验通过后原验收记录保留，不再接受新轮次。
            throw new DefinedException(new DefaultErrorCode("400", "该阶段验收已通过"));
        }
        if (acceptance != null && AcceptStatus.Pending.equals(acceptance.getStatus())) {
            // 重复点击或并发后到请求返回已有待确认事实，不新增轮次。
            return acceptance;
        }

        List<ProjectTask> tasks = taskRepository.findByStage(stage);
        boolean unfinished = tasks.stream().anyMatch(task -> !TaskStatus.Done.equals(task.getStatus()));
        if (unfinished) {
            throw new DefinedException(new DefaultErrorCode("400", "阶段内仍有未完成施工任务"));
        }
        if (evidenceUrls == null || evidenceUrls.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "提交阶段验收前必须上传现场照片或视频"));
        }

        int roundNo = acceptance == null ? 1
                : acceptanceRoundRepository.findFirstByAcceptanceOrderByRoundNoDesc(acceptance)
                        .map(round -> round.getRoundNo() + 1).orElse(1);
        if (roundNo > 1 && (rectificationNote == null || rectificationNote.isBlank()
                || rectificationEvidenceUrls == null || rectificationEvidenceUrls.isEmpty())) {
            throw new DefinedException(new DefaultErrorCode("400", "提交复验必须填写整改说明并上传整改后照片"));
        }

        if (acceptance == null) {
            acceptance = new Acceptance();
            acceptance.setCompany(stage.getProject().getCompany());
            acceptance.setProperty(stage.getProject().getProperty());
            acceptance.setProject(stage.getProject());
            permissionContext.apply(acceptance, stage.getProject());
            acceptance.setStage(stage);
        }
        acceptance.setSubmittedBy(employee);
        acceptance.setStatus(AcceptStatus.Pending);
        acceptance.setConclusion(conclusion);
        acceptance.setIssues(issues == null ? null : new ArrayList<>(issues));
        acceptance.setEvidenceUrls(evidenceUrls == null ? null : new ArrayList<>(evidenceUrls));
        acceptance.setRectificationOwner(rectificationOwner);
        acceptance.setRectificationDueDate(rectificationDueDate);
        acceptance.setRectificationType(null);
        acceptance.setRectificationIssueImages(null);
        // 新一轮重新提交：清空单上的结论痕迹，历史结论保留在轮次记录中。
        acceptance.setConfirmedAt(null);
        acceptance.setConfirmedBy(null);
        Acceptance saved = acceptanceRepository.save(acceptance);

        AcceptanceRound round = new AcceptanceRound();
        round.setCompany(saved.getCompany());
        round.setStore(saved.getStore());
        round.setTeam(saved.getTeam());
        round.setOwner(saved.getOwner());
        round.setAcceptance(saved);
        round.setProject(saved.getProject());
        round.setProperty(saved.getProperty());
        round.setStage(stage);
        round.setRoundNo(roundNo);
        round.setResult(AcceptStatus.Pending);
        round.setRectificationNote(rectificationNote);
        round.setRectificationEvidenceUrls(
                rectificationEvidenceUrls == null ? null : new ArrayList<>(rectificationEvidenceUrls));
        round.setEvidenceUrls(evidenceUrls == null ? null : new ArrayList<>(evidenceUrls));
        round.setIssues(issues == null ? null : new ArrayList<>(issues));
        round.setConclusion(conclusion);
        round.setSubmittedBy(employee);
        round.setSubmittedAt(LocalDateTime.now());
        acceptanceRoundRepository.save(round);

        stage.setStatus(StageStatus.Accepting);
        stageRepository.save(stage);
        stage.getProject().setStatus(ProjectStatus.Accepting);
        projectRepository.save(stage.getProject());
        refreshProject(saved.getProject());
        // 验收提交后由项目侧统一触达业主；触发时机与站内事实一致，避免出现
        // “站内有验收单但业主没收到提醒”的半套状态。
        subscribeMessageContext.ownerAcceptancePending(saved);
        return saved;
    }

    /**
     * 确认或驳回验收单。
     */
    @Transactional
    public Acceptance confirmAcceptance(Long acceptanceId, Long companyId, boolean confirmed, String conclusion) {
        Acceptance acceptance = acceptanceRepository.findById(acceptanceId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "验收单不存在")));
        requireTenant(acceptance.getCompany(), companyId);
        return confirmAcceptance(acceptance, (Credentials) null, confirmed, conclusion);
    }

    /** 使用已加载且在权限范围内命中的验收单执行状态推进。 */
    @Transactional
    public Acceptance confirmAcceptance(Acceptance acceptance, Credentials operator,
                                        boolean confirmed, String conclusion) {
        return confirmAcceptance(acceptance, operator, confirmed, conclusion, null);
    }

    /**
     * 业主确认或驳回：更新验收单当前状态，并把本轮结论追加回当前轮次（历史轮次不可修改）。
     */
    @Transactional
    public Acceptance confirmAcceptance(Acceptance acceptance, Credentials operator,
                                        boolean confirmed, String conclusion, List<String> issues) {
        return confirmAcceptance(acceptance, operator, confirmed, conclusion, issues, null, null);
    }

    /**
     * 兼容既有确认调用；业主驳回的完整事实由下面的类型化重载承载。
     */
    @Transactional
    public Acceptance confirmAcceptance(Acceptance acceptance, Credentials operator,
                                        boolean confirmed, String conclusion, List<String> issues,
                                        Employee rectificationOwner, LocalDate rectificationDueDate) {
        return confirmAcceptance(acceptance, operator, confirmed, conclusion, issues,
                rectificationOwner, rectificationDueDate, null, null);
    }

    /**
     * 业主确认或驳回。驳回必须带问题清单、整改类型和项目经理；截止日期由服务端按整改类型计算，
     * 不接受前端传入的截止日期。已处理的同结果请求由 Provider 在锁定验收单后直接幂等返回。
     */
    @Transactional
    public Acceptance confirmAcceptance(Acceptance acceptance, Credentials operator,
                                        boolean confirmed, String conclusion, List<String> issues,
                                        Employee rectificationOwner, LocalDate ignoredDueDate,
                                        AcceptanceRectificationType rectificationType,
                                        List<String> rectificationIssueImages) {
        if (acceptance == null) {
            throw new DefinedException(new DefaultErrorCode("404", "验收单不存在"));
        }
        if (!AcceptStatus.Pending.equals(acceptance.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "验收单已处理"));
        }
        List<String> normalizedIssues = normalizeIssues(issues);
        if (!confirmed && normalizedIssues.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "业主驳回必须填写问题清单"));
        }
        if (!confirmed && (rectificationOwner == null || rectificationType == null)) {
            throw new DefinedException(new DefaultErrorCode("400", "业主驳回必须指定整改责任人和整改类型"));
        }
        LocalDate rectificationDueDate = confirmed ? null
                : LocalDate.now().plusDays(rectificationType.getDueDays());
        List<String> normalizedIssueImages = normalizeImageUrls(rectificationIssueImages);
        acceptance.setStatus(confirmed ? AcceptStatus.Confirmed : AcceptStatus.Rejected);
        acceptance.setConclusion(conclusion == null ? acceptance.getConclusion() : conclusion);
        if (!confirmed) {
            acceptance.setIssues(new ArrayList<>(normalizedIssues));
            acceptance.setRectificationOwner(rectificationOwner);
            acceptance.setRectificationDueDate(rectificationDueDate);
            acceptance.setRectificationType(rectificationType);
            acceptance.setRectificationIssueImages(normalizedIssueImages.isEmpty()
                    ? null : new ArrayList<>(normalizedIssueImages));
        }
        acceptance.setConfirmedAt(LocalDateTime.now());
        acceptance.setConfirmedBy(operator);
        if (confirmed) {
            ProjectStage stage = acceptance.getStage();
            stage.setStatus(StageStatus.Done);
            stage.setProgress(100);
            stage.setActualEnd(LocalDate.now());
            stageRepository.save(stage);
        } else {
            ProjectStage stage = acceptance.getStage();
            stage.setStatus(StageStatus.Doing);
            stage.setActualEnd(null);
            stageRepository.save(stage);
        }
        Acceptance saved = acceptanceRepository.save(acceptance);

        acceptanceRoundRepository.findFirstByAcceptanceOrderByRoundNoDesc(saved).ifPresent(round -> {
            round.setResult(confirmed ? AcceptStatus.Confirmed : AcceptStatus.Rejected);
            round.setConclusion(conclusion == null ? round.getConclusion() : conclusion);
            if (!confirmed) {
                round.setIssues(new ArrayList<>(normalizedIssues));
                round.setRectificationOwner(rectificationOwner);
                round.setRectificationDueDate(rectificationDueDate);
                round.setRectificationType(rectificationType);
                round.setRectificationIssueImages(normalizedIssueImages.isEmpty()
                        ? null : new ArrayList<>(normalizedIssueImages));
            }
            round.setConfirmedBy(operator);
            round.setConfirmedAt(LocalDateTime.now());
            acceptanceRoundRepository.save(round);
        });

        refreshProject(acceptance.getProject());
        noticeContext.notifyAcceptanceDecision(saved);
        return saved;
    }

    private List<String> normalizeImageUrls(List<String> images) {
        if (images == null) {
            return List.of();
        }
        List<String> normalized = images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (normalized.size() > 9) {
            throw new DefinedException(new DefaultErrorCode("400", "整改问题图片最多上传 9 张"));
        }
        return normalized;
    }

    private List<String> normalizeIssues(List<String> issues) {
        if (issues == null) {
            return List.of();
        }
        return issues.stream()
                .filter(issue -> issue != null && !issue.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    /** 员工验收入口从已认证员工显式解析微信身份，记录操作人。 */
    @Transactional
    public Acceptance confirmAcceptance(Acceptance acceptance, Employee operator,
                                        boolean confirmed, String conclusion) {
        Credentials credential = operator == null ? null : userIdentityContext.requireWx(operator);
        return confirmAcceptance(acceptance, credential, confirmed, conclusion);
    }

    /**
     * 以排期为准重算阶段与项目计划（用户 2026-09-23 裁决）。
     *
     * <p>阶段计划 = 该阶段施工任务 plannedStart 的最小值 / plannedEnd 的最大值；项目计划 = 全部阶段的最值；
     * 没有排期任务的阶段保留原有计划窗口；冲突时以排期为准并回写合同施工计划快照。</p>
     */
    @Transactional
    public Project recalculatePlanFromSchedule(Project project) {
        if (project == null || project.getProjectId() == null) {
            return project;
        }
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        if (stages.isEmpty()) {
            return project;
        }
        java.time.LocalDate projectStart = null;
        java.time.LocalDate projectEnd = null;
        for (ProjectStage stage : stages) {
            List<ProjectTask> tasks = taskRepository.findByStage(stage);
            java.time.LocalDate start = tasks.stream().map(ProjectTask::getPlannedStart)
                    .filter(Objects::nonNull).min(java.time.LocalDate::compareTo).orElse(null);
            java.time.LocalDate end = tasks.stream().map(ProjectTask::getPlannedEnd)
                    .filter(Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
            if (start != null) {
                stage.setPlannedStart(start);
            }
            if (end != null) {
                stage.setPlannedEnd(end);
            }
            if (stage.getPlannedStart() != null
                    && (projectStart == null || stage.getPlannedStart().isBefore(projectStart))) {
                projectStart = stage.getPlannedStart();
            }
            if (stage.getPlannedEnd() != null
                    && (projectEnd == null || stage.getPlannedEnd().isAfter(projectEnd))) {
                projectEnd = stage.getPlannedEnd();
            }
        }
        if (projectStart != null) {
            project.setStartDate(projectStart);
        }
        if (projectEnd != null) {
            project.setPlanEndDate(projectEnd);
        }
        stageRepository.saveAll(stages);
        Project saved = projectRepository.save(project);
        projectContractRepository
                .findByProject_ProjectIdAndCompany_ComId(saved.getProjectId(), saved.getCompany().getComId())
                .ifPresent(contract -> contractPlanContext.syncConstructionPlanFromProject(contract, stages));
        return saved;
    }

    /**
     * 排期驱动：仅刷新项目“预计完工日”，不回写合同基准完工日、不回写合同施工计划快照。
     *
     * <p>DEC-048：合同基准计划锁定；任务排期属于执行层，只产生预计完工；
     * 预计完工日超过合同基准完工日时由 ProjectPlanDeviation 承载“计划偏差”待确认。
     * 在跑项目承接（BR-010）继续使用 {@link #recalculatePlanFromSchedule(Project)}。</p>
     */
    @Transactional
    public Project refreshExpectedEndFromSchedule(Project project) {
        if (project == null || project.getProjectId() == null) {
            return project;
        }
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        if (stages.isEmpty()) {
            return project;
        }
        java.time.LocalDate expectedEnd = null;
        for (ProjectStage stage : stages) {
            List<ProjectTask> tasks = taskRepository.findByStage(stage);
            java.time.LocalDate end = tasks.stream().map(ProjectTask::getPlannedEnd)
                    .filter(Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
            if (end != null && (expectedEnd == null || end.isAfter(expectedEnd))) {
                expectedEnd = end;
            }
        }
        if (expectedEnd != null) {
            project.setExpectedEndDate(expectedEnd);
        }
        Project saved = projectRepository.save(project);
        maybeCreatePlanDeviation(saved, expectedEnd);
        return saved;
    }

    /**
     * DEC-051：任何超窗（预计完工日 &gt; 合同基准完工日）都生成计划偏差单；
     * 同一项目已有待确认偏差时不重复生成。合同基准完工日以合同主档为准。
     */
    @Transactional
    public ProjectPlanDeviation maybeCreatePlanDeviation(Project project, LocalDate expectedEnd) {
        if (project == null || project.getProjectId() == null || expectedEnd == null) {
            return null;
        }
        ProjectContract contract = projectContractRepository
                .findByProject_ProjectIdAndCompany_ComId(project.getProjectId(), project.getCompany().getComId())
                .orElse(null);
        if (contract == null || contract.getPlanEndDate() == null) {
            return null;
        }
        if (!expectedEnd.isAfter(contract.getPlanEndDate())) {
            return null;
        }
        if (deviationRepository.existsByProject_ProjectIdAndStatus(
                project.getProjectId(), PlanDeviationStatus.Pending)) {
            return null;
        }
        ProjectPlanDeviation deviation = new ProjectPlanDeviation();
        deviation.setProject(project);
        deviation.setProperty(project.getProperty());
        deviation.setContract(contract);
        permissionContext.apply(deviation, project);
        deviation.setBaseEndDate(contract.getPlanEndDate());
        deviation.setExpectedEndDate(expectedEnd);
        deviation.setDeviationDays((int) ChronoUnit.DAYS.between(contract.getPlanEndDate(), expectedEnd));
        deviation.setStatus(PlanDeviationStatus.Pending);
        deviation.setResponsible(project.getManager() == null ? project.getOwner() : project.getManager());
        deviation.setReceivableRecalculated(Boolean.FALSE);
        deviation.setVisibleToCustomer(Boolean.TRUE);
        return deviationRepository.save(deviation);
    }

    /**
     * 按阶段权重将项目总工期拆分到各施工阶段。
     */
    @Transactional
    public void scheduleStages(Project project) {
        if (project == null || project.getStartDate() == null || project.getPlanEndDate() == null) {
            return;
        }
        if (project.getPlanEndDate().isBefore(project.getStartDate())) {
            throw new DefinedException(new DefaultErrorCode("400", "计划完工日期不能早于开工日期"));
        }
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        if (stages.isEmpty()) {
            return;
        }
        long totalDays = ChronoUnit.DAYS.between(project.getStartDate(), project.getPlanEndDate()) + 1;
        if (totalDays < stages.size()) {
            throw new DefinedException(new DefaultErrorCode("400", "项目总工期不能少于施工阶段数量"));
        }
        BigDecimal totalWeight = stages.stream()
                .map(ProjectStage::getWeight)
                .filter(weight -> weight != null && weight.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate cursor = project.getStartDate();
        long allocated = 0;
        for (int index = 0; index < stages.size(); index++) {
            ProjectStage stage = stages.get(index);
            long stageDays;
            if (index == stages.size() - 1) {
                stageDays = Math.max(1, totalDays - allocated);
            } else if (totalWeight.signum() > 0 && stage.getWeight() != null && stage.getWeight().signum() > 0) {
                stageDays = Math.max(1, Math.round(totalDays * stage.getWeight().doubleValue() / totalWeight.doubleValue()));
            } else {
                stageDays = Math.max(1, totalDays / stages.size());
            }
            long remainingStages = stages.size() - index - 1;
            long availableDays = totalDays - allocated;
            long cappedDays = index == stages.size() - 1
                    ? availableDays
                    : Math.max(1, Math.min(stageDays, availableDays - remainingStages));
            stage.setPlannedStart(cursor);
            stage.setPlannedEnd(cursor.plusDays(cappedDays - 1));
            allocated += cappedDays;
            cursor = stage.getPlannedEnd().plusDays(1);
            stageRepository.save(stage);
        }
    }

    /**
     * 刷新阶段和项目进度、成本及汇总状态。
     */
    @Transactional
    public Project refreshProject(Project project) {
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedProgress = BigDecimal.ZERO;
        BigDecimal weightedRemainingProgress = BigDecimal.ZERO;
        boolean allDone = !stages.isEmpty();
        boolean anyStarted = false;
        boolean anyAccepting = false;

        for (ProjectStage stage : stages) {
            List<ProjectTask> tasks = taskRepository.findByStage(stage);
            int rawProgress = averageTaskProgress(tasks);
            int progress = blendProgress(stage.getBaselineProgress(), rawProgress);
            Acceptance latestAcceptance = acceptanceRepository
                    .findFirstByStageOrderByAcceptanceIdDesc(stage)
                    .orElse(null);
            AcceptStatus acceptanceStatus = latestAcceptance == null ? null : latestAcceptance.getStatus();
            stage.setProgress(progress);
            if (AcceptStatus.Pending == acceptanceStatus) {
                stage.setStatus(StageStatus.Accepting);
                anyAccepting = true;
            } else if (AcceptStatus.Rejected == acceptanceStatus) {
                stage.setStatus(StageStatus.Doing);
            } else if (AcceptStatus.Confirmed == acceptanceStatus) {
                stage.setStatus(StageStatus.Done);
                stage.setActualEnd(stage.getActualEnd() == null ? LocalDate.now() : stage.getActualEnd());
            } else if (progress == 100 && !tasks.isEmpty()) {
                stage.setStatus(StageStatus.Accepting);
                anyAccepting = true;
            } else if (progress > 0 || stage.getActualStart() != null || StageStatus.Doing.equals(stage.getStatus())) {
                // 历史坑：只看任务进度会把"刚开始、任务还没报进度"的阶段刷回 Todo。
                // actualStart / 既有 Doing 都是"已开工"的真实事实，必须保留。
                stage.setStatus(StageStatus.Doing);
                stage.setActualStart(stage.getActualStart() == null ? LocalDate.now() : stage.getActualStart());
            } else {
                stage.setStatus(StageStatus.Todo);
            }
            BigDecimal weight = stage.getWeight() == null ? BigDecimal.ZERO : stage.getWeight();
            totalWeight = totalWeight.add(weight);
            weightedProgress = weightedProgress.add(weight.multiply(BigDecimal.valueOf(progress)));
            weightedRemainingProgress = weightedRemainingProgress.add(weight.multiply(BigDecimal.valueOf(rawProgress)));
            allDone = allDone && StageStatus.Done.equals(stage.getStatus());
            anyStarted = anyStarted || !StageStatus.Todo.equals(stage.getStatus());
            stageRepository.save(stage);
        }

        int calculatedProgress;
        if (project.getBaselineProgress() != null && totalWeight.signum() > 0) {
            int remainingProgress = weightedRemainingProgress
                    .divide(totalWeight, 0, RoundingMode.HALF_UP)
                    .intValue();
            calculatedProgress = blendProgress(project.getBaselineProgress(), remainingProgress);
        } else {
            calculatedProgress = totalWeight.signum() == 0 ? 0
                    : weightedProgress.divide(totalWeight, 0, RoundingMode.HALF_UP).intValue();
        }
        // 防高估：计划内仍有阶段未完成（包括无任务阶段）时，项目整体不得显示 100%。
        // 阶段权重可以按任务/成本动态计算，但 100% 必须同时满足“所有阶段 Done”。
        calculatedProgress = capProjectProgress(allDone, calculatedProgress);
        project.setProgress(calculatedProgress);
        project.setCost(calculateProjectCost(project));
        if (allDone) {
            project.setStatus(ProjectStatus.Finished);
            project.setActualEndDate(LocalDate.now());
        } else if (anyAccepting) {
            project.setStatus(ProjectStatus.Accepting);
        } else if (anyStarted) {
            project.setStatus(ProjectStatus.Ongoing);
        } else if (!ProjectStatus.Accepting.equals(project.getStatus())) {
            project.setStatus(ProjectStatus.Preparing);
        }
        return projectRepository.save(project);
    }

    /** 接管基线进度只作为起点：后续剩余任务从基线继续增长，不能把进度算回零。 */
    static int blendProgress(Integer baselineProgress, int remainingProgress) {
        int baseline = baselineProgress == null ? 0 : Math.max(0, Math.min(100, baselineProgress));
        int remaining = Math.max(0, Math.min(100, remainingProgress));
        return Math.min(100, baseline + Math.round((100 - baseline) * remaining / 100f));
    }

    static int capProjectProgress(boolean allDone, int calculatedProgress) {
        int bounded = Math.max(0, Math.min(100, calculatedProgress));
        return !allDone && bounded == 100 ? 99 : bounded;
    }

    private void supersedeOtherQuotes(Property property, Long winnerQuoteId) {
        List<Quote> history = quoteRepository.findByProperty_PropertyIdOrderByQuoteIdDesc(property.getPropertyId()).stream()
                .filter(item -> !item.getQuoteId().equals(winnerQuoteId))
                .filter(item -> item.getStatus() == QuoteStatus.Draft
                        || item.getStatus() == QuoteStatus.Submitted
                        || item.getStatus() == QuoteStatus.Accepted)
                .peek(item -> item.setStatus(QuoteStatus.Superseded))
                .toList();
        if (!history.isEmpty()) quoteRepository.saveAll(history);
    }

    private void generateReceivablePlan(ProjectContract contract, Project project) {
        if (contract == null || project == null
                || !receivableRepository.findByProjectOrderByDueDateAsc(project).isEmpty()) {
            return;
        }
        ContractPaymentPlan paymentPlan = contractPlanContext.paymentPlan(contract);
        if (paymentPlan != null && !paymentPlan.getNodes().isEmpty()) {
            Map<String, ProjectStage> stages = new java.util.HashMap<>();
            for (ProjectStage stage : stageRepository.findByProjectOrderBySort(project)) {
                if (stage.getStageCode() != null) stages.put(stage.getStageCode(), stage);
            }
            for (ContractPaymentNode node : paymentPlan.getNodes()) {
                ProjectStage stage = node.getTriggerStageCode() == null
                        ? null : stages.get(node.getTriggerStageCode());
                createReceivable(contract, project, node, stage);
            }
            return;
        }
        generateLegacyReceivablePlan(contract, project);
    }

    private void generateLegacyReceivablePlan(ProjectContract contract, Project project) {
        BigDecimal total = contract.getCurrentAmount() == null ? BigDecimal.ZERO : contract.getCurrentAmount();
        if (total.signum() <= 0) return;
        BigDecimal first = total.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal progress = total.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal last = total.subtract(first).subtract(progress);
        createReceivable(contract, project, "合同签约款", ReceivableType.First, first,
                contract.getSignedAt() == null ? LocalDate.now() : contract.getSignedAt(), 0);
        createReceivable(contract, project, "项目开工款", ReceivableType.Progress, progress,
                contract.getStartDate() == null ? LocalDate.now().plusDays(30) : contract.getStartDate(), 1);
        createReceivable(contract, project, "竣工尾款", ReceivableType.Final, last,
                contract.getPlanEndDate() == null ? LocalDate.now().plusDays(90) : contract.getPlanEndDate(), 2);
    }

    private void createReceivable(ProjectContract contract, Project project,
                                  ContractPaymentNode node, ProjectStage stage) {
        Receivable receivable = new Receivable();
        receivable.setCompany(project.getCompany());
        receivable.setProperty(project.getProperty());
        receivable.setProject(project);
        receivable.setContract(contract);
        receivable.setStage(stage);
        receivable.setStageCode(stage == null ? node.getTriggerStageCode() : stage.getStageCode());
        receivable.setTriggerType(node.getTriggerType());
        receivable.setCustomer(project.getCustomer());
        receivable.setOwner(project.getProperty().getOwner());
        receivable.setStore(project.getProperty().getStore());
        receivable.setTeam(project.getProperty().getTeam());
        receivable.setName(node.getName());
        receivable.setStageName(stage == null ? node.getTriggerStageName() : stage.getName());
        receivable.setType(node.getType());
        receivable.setAmount(node.getAmount());
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(node.getDueDate());
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setInvoiceRequired(Boolean.FALSE);
        receivable.setRemark("合同付款计划快照：" + node.getPlan().getName() + " / " + node.getCode());
        receivable.setCreatedBy(contract.getCreatedBy());
        Receivable saved = receivableRepository.save(receivable);
        node.setReceivable(saved);
    }

    private void createReceivable(ProjectContract contract, Project project, String name,
                                  ReceivableType type, BigDecimal amount, LocalDate dueDate, int sort) {
        Receivable receivable = new Receivable();
        receivable.setCompany(project.getCompany());
        receivable.setProperty(project.getProperty());
        receivable.setProject(project);
        receivable.setContract(contract);
        receivable.setCustomer(project.getCustomer());
        receivable.setOwner(project.getProperty().getOwner());
        receivable.setStore(project.getProperty().getStore());
        receivable.setTeam(project.getProperty().getTeam());
        receivable.setName(name);
        receivable.setStageName(name.replace("款", ""));
        receivable.setType(type);
        receivable.setAmount(amount);
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setDueDate(dueDate);
        receivable.setStatus(ReceivableStatus.Pending);
        receivable.setInvoiceRequired(Boolean.FALSE);
        receivable.setRemark("合同生效后按标准 30% / 40% / 30% 生成，可由财务调整");
        receivable.setCreatedBy(contract.getCreatedBy());
        receivableRepository.save(receivable);
    }

    private void generateFromContract(Project project, List<ContractLine> contractLines) {
        Map<String, ProjectStage> stages = new LinkedHashMap<>();
        Map<String, BigDecimal> stageCosts = new LinkedHashMap<>();
        for (ContractLine item : contractLines) {
            if (item.getType() == ItemType.Construction) {
                appendConstruction(project, item, stages, stageCosts);
            } else if (item.getType() == ItemType.Material) {
                appendMaterial(project, item);
            }
        }
        assignDesignStageWeights(stages, stageCosts);
        stageRepository.saveAll(stages.values());
    }

    private void generateFromConstructionPlan(Project project, ContractConstructionPlan plan,
                                              List<ContractLine> contractLines) {
        Map<String, ProjectStage> stages = new LinkedHashMap<>();
        Map<String, BigDecimal> stageCosts = new LinkedHashMap<>();
        for (ContractConstructionStage snapshot : plan.getStages()) {
            ProjectStage stage = new ProjectStage();
            stage.setCompany(project.getCompany());
            stage.setProperty(project.getProperty());
            stage.setProject(project);
            permissionContext.apply(stage, project);
            stage.setName(snapshot.getName());
            stage.setStageCode(snapshot.getCode());
            stage.setSort(snapshot.getSort());
            stage.setWeight(BigDecimal.ZERO);
            stage.setStatus(StageStatus.Todo);
            stage.setProgress(0);
            stage.setPlannedStart(snapshot.getPlannedStart());
            stage.setPlannedEnd(snapshot.getPlannedEnd());
            stageRepository.save(stage);
            snapshot.setProjectStage(stage);
            stages.put(snapshot.getCode(), stage);
        }
        for (ContractLine item : contractLines) {
            if (item.getType() == ItemType.Material) {
                appendMaterial(project, item);
            } else if (item.getType() == ItemType.Construction) {
                ProjectStage stage = stageFor(item, stages);
                appendConstructionTask(project, item, stage, stageCosts);
            }
        }
        assignPlanStageWeights(stages, stageCosts);
    }

    private ProjectStage stageFor(ContractLine item, Map<String, ProjectStage> stages) {
        String text = ((item.getCategory() == null ? "" : item.getCategory()) + " "
                + (item.getName() == null ? "" : item.getName())).toLowerCase();
        String code;
        if (text.contains("水电") || text.contains("强电") || text.contains("弱电")) code = "WATER_ELECTRIC";
        else if (text.contains("泥") || text.contains("瓦") || text.contains("防水") || text.contains("贴砖")) code = "MASONRY";
        else if (text.contains("木") || text.contains("吊顶")) code = "CARPENTRY";
        else if (text.contains("油漆") || text.contains("涂料") || text.contains("墙面")) code = "PAINTING";
        else if (text.contains("安装") || text.contains("设备")) code = "INSTALL";
        else if (text.contains("拆除")) code = "DEMOLITION";
        else if (text.contains("竣") || text.contains("清洁")) code = "COMPLETION";
        else code = "PREPARE";
        ProjectStage exact = stages.get(code);
        if (exact != null) return exact;
        if (code.equals("CARPENTRY") && stages.containsKey("MASONRY")) return stages.get("MASONRY");
        if (code.equals("INSTALL") && stages.containsKey("FINISH")) return stages.get("FINISH");
        return stages.values().stream().findFirst().orElseThrow(
                () -> new DefinedException(new DefaultErrorCode("400", "施工计划没有可用阶段")));
    }

    private void appendConstructionTask(Project project, ContractLine item, ProjectStage stage,
                                        Map<String, BigDecimal> stageCosts) {
        CatalogItem catalogItem = item.getSourceQuoteLine() == null ? null : item.getSourceQuoteLine().getCatalogItem();
        ProjectTask task = new ProjectTask();
        task.setCompany(project.getCompany());
        task.setProperty(project.getProperty());
        task.setProject(project);
        permissionContext.apply(task, project);
        task.setOwner(resolveDefaultTaskOwner(project));
        task.setStage(stage);
        task.setContractLine(item);
        task.setQuoteItem(item.getSourceQuoteLine());
        task.setCatalogItem(catalogItem);
        task.setName(item.getName());
        task.setZone(item.getSpaceName());
        task.setCraft(catalogItem == null ? null : catalogItem.getCraft());
        task.setSpec(item.getSpec());
        task.setQuantity(item.getQuantity());
        task.setUnit(item.getUnit());
        task.setSalePrice(item.getAmount());
        task.setCost(item.getCost());
        task.setStatus(TaskStatus.Todo);
        task.setProgress(0);
        task.setPlannedStart(stage.getPlannedStart());
        task.setPlannedEnd(stage.getPlannedEnd());
        taskRepository.save(task);
        stageCosts.merge(stage.getStageCode(), item.getCost() == null ? BigDecimal.ZERO : item.getCost(), BigDecimal::add);
    }

    private void assignPlanStageWeights(Map<String, ProjectStage> stages,
                                        Map<String, BigDecimal> stageCosts) {
        BigDecimal total = stageCosts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        for (ProjectStage stage : stages.values()) {
            BigDecimal cost = stageCosts.getOrDefault(stage.getStageCode(), BigDecimal.ZERO);
            BigDecimal weight = total.signum() == 0 ? BigDecimal.ONE
                    : cost.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            stage.setWeight(weight);
        }
    }

    private void appendConstruction(Project project, ContractLine item,
                                    Map<String, ProjectStage> stages,
                                    Map<String, BigDecimal> stageCosts) {
        CatalogItem catalogItem = item.getSourceQuoteLine().getCatalogItem();
        String category = item.getCategory() == null || item.getCategory().isBlank()
                ? "其他施工" : item.getCategory();
        String key = "category-" + category;
        ProjectStage stage = stages.get(key);
        if (stage == null) {
            stage = new ProjectStage();
            stage.setCompany(project.getCompany());
            stage.setProperty(project.getProperty());
            stage.setProject(project);
            permissionContext.apply(stage, project);
            stage.setName(category);
            stage.setSort(stages.size());
            stage.setWeight(BigDecimal.ZERO);
            stage.setStatus(StageStatus.Todo);
            stage.setProgress(0);
            stage.setPlannedStart(project.getStartDate());
            stage.setPlannedEnd(project.getPlanEndDate());
            stageRepository.save(stage);
            stages.put(key, stage);
        }
        ProjectTask task = new ProjectTask();
        task.setCompany(project.getCompany());
        task.setProperty(project.getProperty());
        task.setProject(project);
        permissionContext.apply(task, project);
        task.setOwner(resolveDefaultTaskOwner(project));
        task.setStage(stage);
        task.setContractLine(item);
        task.setQuoteItem(item.getSourceQuoteLine());
        task.setCatalogItem(catalogItem);
        task.setName(item.getName());
        task.setZone(item.getSpaceName());
        task.setCraft(catalogItem == null ? null : catalogItem.getCraft());
        task.setSpec(item.getSpec());
        task.setQuantity(item.getQuantity());
        task.setUnit(item.getUnit());
        task.setSalePrice(item.getAmount());
        task.setCost(item.getCost());
        task.setStatus(TaskStatus.Todo);
        task.setProgress(0);
        task.setPlannedStart(project.getStartDate());
        task.setPlannedEnd(project.getPlanEndDate());
        taskRepository.save(task);
        stageCosts.merge(key, item.getCost() == null ? BigDecimal.ZERO : item.getCost(), BigDecimal::add);
    }

    private void appendMaterial(Project project, ContractLine item) {
        MaterialPlan plan = new MaterialPlan();
        plan.setCompany(project.getCompany());
        plan.setProperty(project.getProperty());
        plan.setProject(project);
        permissionContext.apply(plan, project);
        plan.setContract(item.getContract());
        plan.setContractLine(item);
        plan.setQuoteItem(item.getSourceQuoteLine());
        plan.setCatalogItem(item.getSourceQuoteLine().getCatalogItem());
        plan.setSourceType(MaterialSourceType.Selection);
        plan.setSelectedBrand(item.getBrand());
        plan.setSelectedSpec(item.getSpec());
        plan.setQuantity(item.getQuantity());
        plan.setNetQuantity(item.getQuantity());
        plan.setUnit(item.getUnit());
        plan.setUnitPrice(item.getSourceQuoteLine().getPrice());
        plan.setCost(item.getCost());
        plan.setStatus(MaterialPlanStatus.Planned);
        plan.setRequiredAt(project.getStartDate());
        // 主材分类损耗率继承（DEC-MP-09）
        materialPlanContext.applyLossRate(plan,
                materialCategoryContext.lossRate(project.getCompany(), item.getCategory()));
        materialPlanContext.applyDerived(plan);
        materialPlanRepository.save(plan);
    }

    /**
     * 默认施工任务负责人：优先项目经理，只有未指定项目经理时才回退项目经营负责人。
     * 经营 owner 与施工任务负责人是不同语义，禁止通过 ProjectPermissionContext.apply 直接复制。
     */
    private Employee resolveDefaultTaskOwner(Project project) {
        return project.getManager() == null ? project.getOwner() : project.getManager();
    }

    private void generateFromQuote(Project project, List<QuoteLine> quoteItems) {
        Map<String, ProjectStage> stages = new LinkedHashMap<>();
        Map<String, BigDecimal> stageCosts = new LinkedHashMap<>();
        for (QuoteLine item : quoteItems) {
            if (item.getType() == ItemType.Construction) {
                appendConstruction(project, item, stages, stageCosts);
            } else if (item.getType() == ItemType.Material) {
                appendMaterial(project, item);
            }
        }
        assignDesignStageWeights(stages, stageCosts);
        stageRepository.saveAll(stages.values());
    }

    private void appendConstruction(Project project, QuoteLine item,
                                    Map<String, ProjectStage> stages,
                                    Map<String, BigDecimal> stageCosts) {
        CatalogItem catalogItem = item.getCatalogItem();
        String category = item.getCategory() == null || item.getCategory().isBlank()
                ? "其他施工" : item.getCategory();
        String key = "category-" + category;
        ProjectStage stage = stages.get(key);
        if (stage == null) {
            stage = new ProjectStage();
            stage.setCompany(project.getCompany());
            stage.setProperty(project.getProperty());
            stage.setProject(project);
            permissionContext.apply(stage, project);
            stage.setName(category);
            stage.setSort(stages.size());
            stage.setWeight(BigDecimal.ZERO);
            stage.setStatus(StageStatus.Todo);
            stage.setProgress(0);
            stage.setPlannedStart(project.getStartDate());
            stage.setPlannedEnd(project.getPlanEndDate());
            stageRepository.save(stage);
            stages.put(key, stage);
        }

        ProjectTask task = new ProjectTask();
        task.setCompany(project.getCompany());
        task.setProperty(project.getProperty());
        task.setProject(project);
        permissionContext.apply(task, project);
        task.setOwner(resolveDefaultTaskOwner(project));
        task.setStage(stage);
        task.setQuoteItem(item);
        task.setCatalogItem(catalogItem);
        task.setName(item.getName());
        task.setZone(item.getSpace() == null ? null : item.getSpace().getName());
        task.setQuantity(item.getQuantity());
        task.setUnit(item.getUnit());
        task.setSalePrice(item.getAmount());
        task.setCost(item.getCost());
        task.setStatus(TaskStatus.Todo);
        task.setProgress(0);
        task.setPlannedStart(project.getStartDate());
        task.setPlannedEnd(project.getPlanEndDate());
        taskRepository.save(task);
        stageCosts.merge(key, item.getCost() == null ? BigDecimal.ZERO : item.getCost(), BigDecimal::add);
    }

    private void appendMaterial(Project project, QuoteLine item) {
        CatalogItem catalogItem = item.getCatalogItem();
        if (catalogItem == null) {
            return;
        }
        MaterialPlan plan = new MaterialPlan();
        plan.setCompany(project.getCompany());
        plan.setProperty(project.getProperty());
        plan.setProject(project);
        permissionContext.apply(plan, project);
        plan.setQuoteItem(item);
        plan.setCatalogItem(catalogItem);
        plan.setSourceType(MaterialSourceType.Selection);
        plan.setSelectedBrand(catalogItem.getBrand());
        plan.setSelectedSpec(catalogItem.getSpec());
        plan.setQuantity(item.getQuantity());
        plan.setNetQuantity(item.getQuantity());
        plan.setUnit(item.getUnit());
        plan.setUnitPrice(item.getPrice());
        plan.setCost(item.getCost());
        plan.setStatus(MaterialPlanStatus.Planned);
        plan.setRequiredAt(project.getStartDate());
        // 主材分类损耗率继承（DEC-MP-09）
        materialPlanContext.applyLossRate(plan,
                materialCategoryContext.lossRate(project.getCompany(), catalogItem.getCategory()));
        materialPlanContext.applyDerived(plan);
        materialPlanRepository.save(plan);
    }

    private void assignDesignStageWeights(Map<String, ProjectStage> stages,
                                          Map<String, BigDecimal> stageCosts) {
        BigDecimal total = stageCosts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        for (ProjectStage stage : stages.values()) {
            String key = "category-" + stage.getName();
            BigDecimal cost = stageCosts.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal weight = total.signum() == 0 ? BigDecimal.ONE
                    : cost.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            stage.setWeight(weight);
        }
    }

    private int averageTaskProgress(List<ProjectTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        return tasks.stream()
                .map(ProjectTask::getProgress)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .average()
                .stream()
                .mapToInt(value -> (int) Math.round(value))
                .findFirst()
                .orElse(0);
    }

    private BigDecimal calculateProjectCost(Project project) {
        return projectCostContext.summarize(project).actualCost();
    }

    private void requireTenant(com.th4.edge.tenant.user.model.Company company, Long companyId) {
        if (company == null || companyId == null || !company.getComId().equals(companyId)) {
            throw new DefinedException(new DefaultErrorCode("403", "无权访问其他企业的项目数据"));
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 立项与同一房屋下的报价确认、签约共用同一把房屋锁（替代原房屋行级悲观锁）。
     * key 不可解析时退化为对象维度，保证非空锁键且由业务方法返回既有错误口径。
     */
    public String quoteProjectLockKey(Quote quote) {
        if (quote == null) {
            return "property-project:unknown";
        }
        Property property = quote.getProperty();
        if (property == null || property.getPropertyId() == null) {
            return "property-project:quote:" + quote.getQuoteId();
        }
        return "property:" + resolveComId(property, quote.getCompany()) + ":" + property.getPropertyId();
    }

    public String contractProjectLockKey(ProjectContract contract) {
        if (contract == null) {
            return "property-project:unknown";
        }
        Property property = contract.getProperty();
        if (property == null && contract.getQuote() != null) {
            property = contract.getQuote().getProperty();
        }
        if (property == null || property.getPropertyId() == null) {
            return "property-project:contract:" + contract.getContractId();
        }
        return "property:" + resolveComId(property, contract.getCompany()) + ":" + property.getPropertyId();
    }

    private Long resolveComId(Property property, com.th4.edge.tenant.user.model.Company fallback) {
        if (property != null && property.getCompany() != null) {
            return property.getCompany().getComId();
        }
        return fallback == null ? null : fallback.getComId();
    }
}
