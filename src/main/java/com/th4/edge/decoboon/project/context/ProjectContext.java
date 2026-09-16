package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final MaterialPlanRepository materialPlanRepository;
    private final ReceivableRepository receivableRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final CustomerRepository customerRepository;
    private final PropertyContext propertyContext;
    private final PropertyRepository propertyRepository;
    private final ProjectPermissionContext permissionContext;
    private final UserIdentityContext userIdentityContext;
    private final NoticeContext noticeContext;

    public ProjectContext(QuoteRepository quoteRepository,
                          QuoteLineRepository quoteItemRepository,
                          ProjectRepository projectRepository,
                          ProjectStageRepository stageRepository,
                          ProjectTaskRepository taskRepository,
                          MaterialPlanRepository materialPlanRepository,
                          ReceivableRepository receivableRepository,
                          AcceptanceRepository acceptanceRepository,
                          CustomerRepository customerRepository,
                          PropertyContext propertyContext,
                          PropertyRepository propertyRepository,
                          ProjectPermissionContext permissionContext,
                          ProjectContractRepository projectContractRepository,
                          ContractLineRepository contractLineRepository,
                          UserIdentityContext userIdentityContext,
                          NoticeContext noticeContext) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.receivableRepository = receivableRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.customerRepository = customerRepository;
        this.propertyContext = propertyContext;
        this.propertyRepository = propertyRepository;
        this.permissionContext = permissionContext;
        this.projectContractRepository = projectContractRepository;
        this.contractLineRepository = contractLineRepository;
        this.userIdentityContext = userIdentityContext;
        this.noticeContext = noticeContext;
    }

    /**
     * 由客户接受的报价生成完整施工计划。
     *
     * @param quoteId 报价索引
     * @param draft 项目基础信息，至少包含名称和工地地址
     * @return 已生成阶段、任务和材料计划的项目
     */
    @Transactional
    public Project createFromQuote(Long quoteId, Project draft) {
        ProjectContract contract = projectContractRepository.findByQuote_QuoteId(quoteId)
                .filter(item -> item.getStatus() == com.th4.edge.decoboon.project.configure.ContractStatus.Effective)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "请先完成合同签约并生效，再创建项目")));
        return createFromEffectiveContract(contract, draft);
    }

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
        if (property == null) {
            property = propertyContext.ensureForCustomer(quote.getCustomer(), quote.getCreatedBy());
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
        project.setArea(quote.getArea());
        project.setContractAmount(quote.getAmount());
        project.setCost(quote.getCost());
        project.setStartDate(draft.getStartDate());
        project.setPlanEndDate(draft.getPlanEndDate());
        project.setStatus(ProjectStatus.Preparing);
        project.setProgress(0);
        projectRepository.save(project);

        List<ContractLine> contractLines = contractLineRepository.findByContractOrderBySortAsc(contract);
        generateFromContract(project, contractLines);
        refreshProject(project);
        Quote converted = quote;
        converted.setStatus(QuoteStatus.Converted);
        quoteRepository.save(converted);
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
     * 阶段施工完成后提交验收。
     */
    @Transactional
    public Acceptance submitAcceptance(Long companyId, Long projectId, Long stageId, Employee employee,
                                       String conclusion, List<String> issues, List<String> evidenceUrls) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在")));
        requireTenant(stage.getCompany(), companyId);
        if (!stage.getProject().getProjectId().equals(projectId)) {
            throw new DefinedException(new DefaultErrorCode("400", "阶段不属于当前项目"));
        }
        List<ProjectTask> tasks = taskRepository.findByStage(stage);
        boolean unfinished = tasks.stream().anyMatch(task -> !TaskStatus.Done.equals(task.getStatus()));
        if (unfinished) {
            throw new DefinedException(new DefaultErrorCode("400", "阶段内仍有未完成施工任务"));
        }
        if (acceptanceRepository.existsByStageAndStatus(stage, AcceptStatus.Pending)) {
            throw new DefinedException(new DefaultErrorCode("400", "该阶段已有待确认验收单"));
        }

        Acceptance acceptance = new Acceptance();
        acceptance.setCompany(stage.getProject().getCompany());
        acceptance.setProperty(stage.getProject().getProperty());
        acceptance.setProject(stage.getProject());
        permissionContext.apply(acceptance, stage.getProject());
        acceptance.setStage(stage);
        acceptance.setSubmittedBy(employee);
        acceptance.setStatus(AcceptStatus.Pending);
        acceptance.setConclusion(conclusion);
        acceptance.setIssues(issues == null ? null : new ArrayList<>(issues));
        acceptance.setEvidenceUrls(evidenceUrls == null ? null : new ArrayList<>(evidenceUrls));
        stage.setStatus(StageStatus.Accepting);
        stageRepository.save(stage);
        stage.getProject().setStatus(ProjectStatus.Accepting);
        projectRepository.save(stage.getProject());
        Acceptance saved = acceptanceRepository.save(acceptance);
        refreshProject(saved.getProject());
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
        if (acceptance == null) {
            throw new DefinedException(new DefaultErrorCode("404", "验收单不存在"));
        }
        if (!AcceptStatus.Pending.equals(acceptance.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "验收单已处理"));
        }
        acceptance.setStatus(confirmed ? AcceptStatus.Confirmed : AcceptStatus.Rejected);
        acceptance.setConclusion(conclusion == null ? acceptance.getConclusion() : conclusion);
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
        refreshProject(acceptance.getProject());
        noticeContext.notifyAcceptanceDecision(saved);
        return saved;
    }

    /** 员工验收入口从已认证员工显式解析微信身份，记录操作人。 */
    @Transactional
    public Acceptance confirmAcceptance(Acceptance acceptance, Employee operator,
                                        boolean confirmed, String conclusion) {
        Credentials credential = operator == null ? null : userIdentityContext.requireWx(operator);
        return confirmAcceptance(acceptance, credential, confirmed, conclusion);
    }

    /**
     * 刷新阶段和项目进度、成本及汇总状态。
     */
    @Transactional
    public Project refreshProject(Project project) {
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedProgress = BigDecimal.ZERO;
        boolean allDone = !stages.isEmpty();
        boolean anyStarted = false;
        boolean anyAccepting = false;

        for (ProjectStage stage : stages) {
            List<ProjectTask> tasks = taskRepository.findByStage(stage);
            int progress = averageTaskProgress(tasks);
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
            } else if (progress == 100 && !tasks.isEmpty()) {
                stage.setStatus(StageStatus.Done);
                stage.setActualEnd(stage.getActualEnd() == null ? LocalDate.now() : stage.getActualEnd());
            } else if (progress > 0) {
                stage.setStatus(StageStatus.Doing);
                stage.setActualStart(stage.getActualStart() == null ? LocalDate.now() : stage.getActualStart());
            } else {
                stage.setStatus(StageStatus.Todo);
            }
            BigDecimal weight = stage.getWeight() == null ? BigDecimal.ZERO : stage.getWeight();
            totalWeight = totalWeight.add(weight);
            weightedProgress = weightedProgress.add(weight.multiply(BigDecimal.valueOf(progress)));
            allDone = allDone && StageStatus.Done.equals(stage.getStatus());
            anyStarted = anyStarted || progress > 0;
            stageRepository.save(stage);
        }

        project.setProgress(totalWeight.signum() == 0 ? 0
                : weightedProgress.divide(totalWeight, 0, RoundingMode.HALF_UP).intValue());
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
        plan.setQuantity(item.getQuantity());
        plan.setUnit(item.getUnit());
        plan.setUnitPrice(item.getSourceQuoteLine().getPrice());
        plan.setCost(item.getCost());
        plan.setStatus(MaterialPlanStatus.Planned);
        plan.setRequiredAt(project.getStartDate());
        materialPlanRepository.save(plan);
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
        plan.setQuantity(item.getQuantity());
        plan.setUnit(item.getUnit());
        plan.setUnitPrice(item.getPrice());
        plan.setCost(item.getCost());
        plan.setStatus(MaterialPlanStatus.Planned);
        plan.setRequiredAt(project.getStartDate());
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
        BigDecimal taskCost = taskRepository.findByProject(project).stream()
                .filter(task -> task.getStatus() != TaskStatus.Cancelled)
                .map(ProjectTask::getCost)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal materialCost = materialPlanRepository.findByProject(project).stream()
                .filter(plan -> plan.getStatus() != MaterialPlanStatus.Closed)
                .map(MaterialPlan::getCost)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return taskCost.add(materialCost).setScale(2, RoundingMode.HALF_UP);
    }

    private void requireTenant(com.th4.edge.tenant.user.model.Company company, Long companyId) {
        if (company == null || companyId == null || !company.getComId().equals(companyId)) {
            throw new DefinedException(new DefaultErrorCode("403", "无权访问其他企业的项目数据"));
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
