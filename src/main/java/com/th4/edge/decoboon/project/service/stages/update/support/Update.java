package com.th4.edge.decoboon.project.service.stages.update.support;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.components.lock.DistributedLock;

import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 项目经理开启阶段，自动同步项目状态并发布对业主可见动态。 */
@Note("项目阶段推进")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "name", "stageCode", "sort", "weight", "acceptanceMode", "status", "progress",
                        "plannedStart", "plannedEnd", "actualStart", "actualEnd", "createTime",
                        "notApplicableReason", "notApplicableBy", "notApplicableAt"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/stage/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.stages.update.Update {

    @AutoInject
    @NotNull
    private Long stageId;

    @AutoInject
    private StageStatus status;

    @AutoInject
    private String content;

    @AutoInject
    private String reason;

    @User
    private Employee employee;

    private final ProjectStageRepository stageRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectContext projectContext;
    private final ProjectRepository projectRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final MeasureRepository measureRepository;
    private final ProjectContractRepository contractRepository;
    private final CarryoverLockContext carryoverLockContext;

    private final DistributedLock distributedLock;

    public Update(ProjectStageRepository stageRepository,
                  ProjectTaskRepository taskRepository,
                  ProjectContext projectContext,
                  ProjectRepository projectRepository,
                  ProjectReportRepository reportRepository,
                  ProjectAccessContext accessContext,
                  ProjectPermissionContext permissionContext,
                  MeasureRepository measureRepository,
                  ProjectContractRepository contractRepository,
                  CarryoverLockContext carryoverLockContext,
                DistributedLock distributedLock) {
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.projectContext = projectContext;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.measureRepository = measureRepository;
        this.contractRepository = contractRepository;
        this.carryoverLockContext = carryoverLockContext;
        this.distributedLock = distributedLock;
    }

    @Override
    @Transactional
    public ProjectStage call(ProjectStage request) {
        Employee current = accessContext.requireEmployee(employee);
        // 行级排他锁把同一阶段的并发开工串行化：后到请求拿到锁后读到已提交状态，
        // 走幂等分支返回同一事实，避免死锁与 undefinedErrorCode。
        String lockKey = "stage-open:" + current.getCompany().getComId() + ":" + stageId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "项目阶段正在处理中，请稍后重试"));
        }
        try {
        ProjectStage stage = stageRepository.findByStageId(stageId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在")));
        // 锁内统一做数据范围校验：不可见一律 404，不泄露对象存在性。
        accessContext.requireVisible(stage.getProject(), current);
        // 阶段开工：企业负责人 / 店长（限本门店数据范围）/ 项目经理
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm
                && current.getRole() != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人、店长或项目经理可以开启施工阶段"));
        }
        StageStatus target = status == null ? StageStatus.Doing : status;
        // GAP-039 / DEC-057：空阶段先分类，本项不涉及与撤销走独立分支。
        if (target == StageStatus.NotApplicable) {
            return markNotApplicable(stage, current);
        }
        if (target == StageStatus.Todo && StageStatus.NotApplicable.equals(stage.getStatus())) {
            return restoreApplicable(stage, current);
        }
        if (target != StageStatus.Doing) {
            throw new DefinedException(new DefaultErrorCode("400", "不支持的阶段操作"));
        }
        if (StageStatus.NotApplicable.equals(stage.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "该阶段已判定为本项不涉及，请先撤销判定再开工"));
        }
        // 幂等：重复开工返回同一最终事实，不再写动态、任务或覆盖实际开工时间。
        if (stage.getStatus() == StageStatus.Doing) {
            return snapshot(stage);
        }
        if (stage.getStatus() != StageStatus.Todo) {
            throw new DefinedException(new DefaultErrorCode("400", "当前阶段状态不能开工"));
        }
        // GAP-039 / DEC-057：非本项不涉及阶段必须有合同施工任务，空阶段不得开工。
        if (taskRepository.findByStage(stage).isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "该阶段无合同施工任务，请先核对报价与合同施工行，或判定为本项不涉及"));
        }
        Project project = stage.getProject();
        if (project.getStatus() == ProjectStatus.Finished || project.getStatus() == ProjectStatus.Settled) {
            throw new DefinedException(new DefaultErrorCode("400", "项目已完工结算，不能再开启阶段"));
        }
        if (!effectiveContract(project)) {
            throw new DefinedException(new DefaultErrorCode("400", "合同未生效，不能进入施工阶段"));
        }
        if (project.getManager() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请先指定项目经理再开工"));
        }
        if (project.getStartDate() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请先确认项目开工日期"));
        }
        if (project.getPlanEndDate() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请先确认项目计划完工日期"));
        }
        if (project.getPlanEndDate().isBefore(project.getStartDate())) {
            throw new DefinedException(new DefaultErrorCode("400", "计划完工日期不能早于开工日期"));
        }
        var property = project.getProperty();
        boolean measured = property != null && property.getLatestMeasureAt() != null;
        if (!measured && property != null) {
            measured = measureRepository.findByPropertyOrderByMeasureTimeDesc(property).stream()
                    .anyMatch(item -> item.getStatus() == MeasureStatus.Completed);
        }
        if (!measured) {
            throw new DefinedException(new DefaultErrorCode("400", "请先完成量房并录入量房结论再开工"));
        }

        stage.setStatus(StageStatus.Doing);
        stage.setActualStart(stage.getActualStart() == null ? LocalDate.now() : stage.getActualStart());
        // 历史坑：这里原先把项目状态无条件写成 Ongoing，会把"阶段待验收（Accepting）/停工（Suspended）"
        // 悄悄降级成"施工中"，出现"待验收"和"施工中"同时成立的矛盾（DEC-009 决策二）。
        // 只允许"待开工 → 施工中"这一种推进，其余状态交给 refreshProject/验收流程维护。
        if (project.getStatus() == ProjectStatus.Preparing) {
            project.setStatus(ProjectStatus.Ongoing);
        }
        projectRepository.save(project);
        ProjectStage saved = stageRepository.save(stage);

        if (!hasStageOpenReport(saved)) {
            ProjectReport report = new ProjectReport();
            report.setCompany(saved.getCompany());
            report.setProject(saved.getProject());
            report.setProperty(saved.getProperty());
            permissionContext.apply(report, saved.getProject());
            report.setStage(saved);
            report.setType(ReportType.StageOpen);
            report.setTitle("阶段开工 · " + saved.getName());
            report.setContent(content == null || content.isBlank() ? saved.getName() + "已正式开工" : content);
            report.setAuthor(current);
            report.setAuthorRole(current.getRole());
            report.setVisibleToCustomer(true);
            reportRepository.save(report);
        }
        if (carryoverLockContext != null) carryoverLockContext.lock(saved.getProject(), CarryoverBusinessType.Stage, current);
        return snapshot(saved);
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    /**
     * GAP-039 / DEC-057：把未开始且无施工任务的阶段判定为「本项不涉及」。
     * 判定留痕（原因、判定人、时间），并从项目进度分母与阶段权重剔除。
     */
    private ProjectStage markNotApplicable(ProjectStage stage, Employee current) {
        if (StageStatus.NotApplicable.equals(stage.getStatus())) {
            return snapshot(stage);
        }
        if (stage.getStatus() != StageStatus.Todo) {
            throw new DefinedException(new DefaultErrorCode("400", "只有未开始的阶段可以判定为本项不涉及"));
        }
        if (reason == null || reason.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写本项不涉及的判定依据"));
        }
        stage.setStatus(StageStatus.NotApplicable);
        stage.setNotApplicableReason(reason.trim());
        stage.setNotApplicableBy(current);
        stage.setNotApplicableAt(LocalDateTime.now());
        ProjectStage saved = stageRepository.save(stage);
        projectContext.refreshProject(saved.getProject());
        return snapshot(saved);
    }

    /** GAP-039 / DEC-057：撤销「本项不涉及」判定，阶段回到未开始，由报价/合同施工行补齐后重排。 */
    private ProjectStage restoreApplicable(ProjectStage stage, Employee current) {
        if (!StageStatus.NotApplicable.equals(stage.getStatus())) {
            return snapshot(stage);
        }
        stage.setStatus(StageStatus.Todo);
        stage.setNotApplicableReason(null);
        stage.setNotApplicableBy(null);
        stage.setNotApplicableAt(null);
        ProjectStage saved = stageRepository.save(stage);
        projectContext.refreshProject(saved.getProject());
        return snapshot(saved);
    }

    /** 开工前置：项目必须绑定已生效合同（Effective 及之后的履约状态均视为已生效）。 */
    private boolean effectiveContract(Project project) {
        if (project == null || project.getProjectId() == null || project.getCompany() == null) {
            return false;
        }
        ProjectContract contract = contractRepository
                .findByProject_ProjectIdAndCompany_ComId(project.getProjectId(), project.getCompany().getComId())
                .orElse(null);
        if (contract == null || contract.getStatus() == null) {
            return false;
        }
        return contract.getStatus() != ContractStatus.Draft
                && contract.getStatus() != ContractStatus.PendingSign
                && contract.getStatus() != ContractStatus.Signed;
    }

    /** 阶段开工动态是否已存在，防止重复写动态。 */
    private boolean hasStageOpenReport(ProjectStage stage) {
        if (stage.getStageId() == null) {
            return false;
        }
        return reportRepository.count(((Specification<ProjectReport>) (root, query, builder) -> builder.and(
                builder.equal(root.get("stage").get("stageId"), stage.getStageId()),
                builder.equal(root.get("type"), ReportType.StageOpen)))) > 0;
    }

    /**
     * 返回脱离持久化上下文的阶段快照，避免事务提交后序列化懒加载关联导致
     * "数据库已提交但接口返回失败"的部分成功假象。
     */
    private ProjectStage snapshot(ProjectStage stage) {
        ProjectStage view = new ProjectStage();
        view.setStageId(stage.getStageId());
        view.setName(stage.getName());
        view.setSort(stage.getSort());
        view.setWeight(stage.getWeight());
        view.setStatus(stage.getStatus());
        view.setProgress(stage.getProgress());
        view.setPlannedStart(stage.getPlannedStart());
        view.setPlannedEnd(stage.getPlannedEnd());
        view.setActualStart(stage.getActualStart());
        view.setActualEnd(stage.getActualEnd());
        view.setNotApplicableReason(stage.getNotApplicableReason());
        view.setNotApplicableBy(stage.getNotApplicableBy());
        view.setNotApplicableAt(stage.getNotApplicableAt());
        view.setCreateTime(stage.getCreateTime());
        Project project = stage.getProject();
        if (project != null) {
            Project projectView = new Project();
            projectView.setProjectId(project.getProjectId());
            projectView.setNo(project.getNo());
            projectView.setName(project.getName());
            projectView.setSiteAddr(project.getSiteAddr());
            view.setProject(projectView);
        }
        return view;
    }
}
