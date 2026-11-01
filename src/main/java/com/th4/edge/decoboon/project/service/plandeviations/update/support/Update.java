package com.th4.edge.decoboon.project.service.plandeviations.update.support;

import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.project.configure.PlanDeviationStatus;
import com.th4.edge.decoboon.project.configure.PlanDeviationStrategy;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.collab.context.SubscribeMessageContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectPlanDeviationRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划偏差处理实现。
 *
 * <p>DEC-051：确认/驳回限 Boss/本门店店长；顺延方式由发起人在发起时选择，确认时不得为空；
 * 客户原因强制走合同工期变更（RoutedToChange）；确认后更新合同基准完工日并留痕。</p>
 */
@Note("计划偏差处理")
@Properties(allow = {
        @AllowProperties(model = ProjectPlanDeviation.class,
                properties = {"deviationId", "project", "contract", "baseEndDate", "expectedEndDate", "deviationDays",
                        "reasonType", "reason", "scheduleStrategy", "responsible", "dueDate", "status",
                        "confirmedBy", "confirmedAt", "newBaseEndDate", "receivableRecalculated",
                        "visibleToCustomer", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ProjectContract.class, properties = {"contractId", "no", "status", "signedAmount", "currentAmount", "startDate", "planEndDate"})
})
@Service("project/plan-deviation/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.plandeviations.update.Update {

    @AutoInject
    private Long deviationId;

    @AutoInject
    private PlanDeviationStatus status;

    @AutoInject
    private PlanDeviationStrategy scheduleStrategy;

    @AutoInject
    private String reason;

    @User
    private Employee employee;

    private final ProjectPlanDeviationRepository deviationRepository;
    private final ProjectContractRepository contractRepository;
    private final ProjectReportRepository reportRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final ContractPlanContext contractPlanContext;
    private final ProjectContext projectContext;
    private final SubscribeMessageContext subscribeMessageContext;

    public Update(ProjectPlanDeviationRepository deviationRepository,
                  ProjectContractRepository contractRepository,
                  ProjectReportRepository reportRepository,
                  ProjectAccessContext accessContext,
                  ProjectPermissionContext permissionContext,
                  ContractPlanContext contractPlanContext,
                  ProjectContext projectContext,
                  SubscribeMessageContext subscribeMessageContext) {
        this.deviationRepository = deviationRepository;
        this.contractRepository = contractRepository;
        this.reportRepository = reportRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.contractPlanContext = contractPlanContext;
        this.projectContext = projectContext;
        this.subscribeMessageContext = subscribeMessageContext;
    }

    @Override
    @Transactional
    public ProjectPlanDeviation call(ProjectPlanDeviation request) {
        Employee current = accessContext.requireEmployee(employee);
        if (deviationId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "偏差单索引不能为空"));
        }
        ProjectPlanDeviation deviation = deviationRepository.findById(deviationId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "计划偏差不存在或无权访问")));
        Project project = deviation.getProject();
        accessContext.requireVisible(project, current);
        // DEC-051 DEV-01：确认/驳回限 Boss 或本门店店长。
        accessContext.requireMemberEditor(project, current);
        if (deviation.getStatus() != PlanDeviationStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "计划偏差已处理，不能重复确认"));
        }
        PlanDeviationStatus target = status == null ? PlanDeviationStatus.Confirmed : status;
        if (target == PlanDeviationStatus.Confirmed) {
            if (scheduleStrategy == null) {
                throw new DefinedException(new DefaultErrorCode("400", "请先选择顺延方式"));
            }
            deviation.setScheduleStrategy(scheduleStrategy);
            LocalDate newBaseEnd = scheduleStrategy == PlanDeviationStrategy.Extend
                    ? deviation.getExpectedEndDate()
                    : deviation.getBaseEndDate();
            deviation.setNewBaseEndDate(newBaseEnd);
            applyBaseline(deviation, newBaseEnd, reason);
            // DEC-048 T-08：基准完工日变更后重算未收款应收的到期日。
            int recalculated = deviation.getContract() == null
                    ? 0 : projectContext.recalculateReceivableDueDates(deviation.getContract());
            deviation.setReceivableRecalculated(true);
            deviation.setStatus(PlanDeviationStatus.Confirmed);
            deviation.setConfirmedBy(current);
            deviation.setConfirmedAt(LocalDateTime.now());
            ProjectPlanDeviation saved = deviationRepository.save(deviation);
            subscribeMessageContext.ownerPlanDeviationConfirmed(saved);
            writeReport(saved, current, "计划偏差已确认",
                    "合同基准完工日调整为 " + newBaseEnd + "（方式：" + scheduleStrategy.getName()
                            + "，重算应收 " + recalculated + " 条）");
            return saved;
        }
        if (target == PlanDeviationStatus.Rejected) {
            deviation.setStatus(PlanDeviationStatus.Rejected);
            deviation.setReason(reason == null ? deviation.getReason() : reason);
            deviation.setConfirmedBy(current);
            deviation.setConfirmedAt(LocalDateTime.now());
            ProjectPlanDeviation saved = deviationRepository.save(deviation);
            writeReport(saved, current, "计划偏差已驳回", "保持原合同基准完工日 " + deviation.getBaseEndDate());
            return saved;
        }
        if (target == PlanDeviationStatus.RoutedToChange) {
            // DEC-051 DEV-05：客户原因强制走合同工期变更，不在此处顺延基准。
            deviation.setStatus(PlanDeviationStatus.RoutedToChange);
            deviation.setConfirmedBy(current);
            deviation.setConfirmedAt(LocalDateTime.now());
            deviation.setReason(reason == null ? deviation.getReason() : reason);
            ProjectPlanDeviation saved = deviationRepository.save(deviation);
            subscribeMessageContext.ownerPlanDeviationConfirmed(saved);
            writeReport(saved, current, "计划偏差转合同工期变更",
                    "客户原因超窗，须走合同工期变更；原合同基准完工日 " + deviation.getBaseEndDate());
            return saved;
        }
        throw new DefinedException(new DefaultErrorCode("400", "不支持的偏差处理动作"));
    }

    private void applyBaseline(ProjectPlanDeviation deviation, LocalDate newBaseEnd, String reason) {
        ProjectContract contract = deviation.getContract();
        if (contract == null) {
            return;
        }
        contract.setPlanEndDate(newBaseEnd);
        Project project = deviation.getProject();
        if (project != null) {
            project.setPlanEndDate(newBaseEnd);
        }
        contractRepository.save(contract);
        contractPlanContext.updateBaselineEndDate(contract, newBaseEnd);
        if (reason != null && !reason.isBlank()) {
            deviation.setReason(reason);
        }
    }

    private void writeReport(ProjectPlanDeviation deviation, Employee operator, String title, String content) {
        Project project = deviation.getProject();
        if (project == null) {
            return;
        }
        ProjectReport report = new ProjectReport();
        report.setProject(project);
        report.setProperty(project.getProperty());
        permissionContext.apply(report, project);
        report.setType(ReportType.Notice);
        report.setTitle(title);
        report.setContent(content + "（操作人：" + operator.getName() + "）");
        report.setAuthor(operator);
        report.setAuthorRole(operator.getRole());
        // DEC-051 DEV-04：偏差对象对业主可见；确认结果同步通知业主。
        report.setVisibleToCustomer(Boolean.TRUE);
        reportRepository.save(report);
    }
}
