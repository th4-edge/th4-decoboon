package com.th4.edge.decoboon.project.service.acceptances.update.support;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
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

/** 验收确认实现。 */
@Note("验收确认")
@Description("客户确认或驳回阶段验收单")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "stage", "submittedBy", "status", "conclusion",
                        "issues", "evidenceUrls", "rectificationOwner", "rectificationDueDate", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"})
})
@Service("project/acceptance/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.acceptances.update.Update {

    @AutoInject
    @NotNull
    private Long acceptanceId;

    @AutoInject
    @NotNull
    private Boolean confirmed;

    @AutoInject
    private String conclusion;

    @User
    private Employee employee;

    private final AcceptanceRepository acceptanceRepository;
    private final ProjectContext projectContext;
    private final ProjectAccessContext accessContext;

    public Update(AcceptanceRepository acceptanceRepository,
                  ProjectContext projectContext,
                  ProjectAccessContext accessContext) {
        this.acceptanceRepository = acceptanceRepository;
        this.projectContext = projectContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Acceptance call(Acceptance request) {
        Employee current = accessContext.requireEmployee(employee);
        Acceptance acceptance = acceptanceRepository.findOne(((Specification<Acceptance>) (root, query, builder) -> builder.equal(root.get("acceptanceId"), acceptanceId)).and(accessContext.visibleChild(employee, Acceptance.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "验收单不存在")));
        accessContext.requireVisible(acceptance.getProject(), current);
        // SCN-003：阶段验收由业主本人在授权会话中确认；员工只能提交验收与证据
        // （project/acceptance/save），不得代表业主确认或驳回，线下代录仍为 BLOCKED。
        throw new DefinedException(new DefaultErrorCode("403",
                "业主确认必须由业主本人通过小程序授权会话完成，员工不能代录"));
    }
}
