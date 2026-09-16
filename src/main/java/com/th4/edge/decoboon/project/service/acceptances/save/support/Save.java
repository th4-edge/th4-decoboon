package com.th4.edge.decoboon.project.service.acceptances.save.support;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 阶段施工完成后提交业主验收。 */
@Note("提交验收")
@Description("阶段施工任务全部完成后提交客户验收")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "stage", "submittedBy", "status", "conclusion",
                        "issues", "evidenceUrls", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"})
})
@Service("project/acceptance/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.acceptances.save.Save {

    @AutoInject
    @NotNull
    private Long stageId;

    @AutoInject
    private String result;

    @AutoInject
    private String content;

    @AutoInject
    private List<String> issues;

    @AutoInject
    private List<String> images;

    @User
    private Employee employee;

    private final ProjectStageRepository stageRepository;
    private final ProjectContext projectContext;
    private final ProjectAccessContext accessContext;

    public Save(ProjectStageRepository stageRepository,
                ProjectContext projectContext,
                ProjectAccessContext accessContext) {
        this.stageRepository = stageRepository;
        this.projectContext = projectContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Acceptance call(Acceptance request) {
        Employee current = accessContext.requireEmployee(employee);
        Specification<com.th4.edge.decoboon.project.model.ProjectStage> byId = (root, query, builder) ->
                builder.equal(root.get("stageId"), stageId);
        var stage = stageRepository.findOne(byId.and(accessContext.visibleChild(
                        employee, com.th4.edge.decoboon.project.model.ProjectStage.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在或无权访问")));
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Pm
                && current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色无权提交阶段验收"));
        }
        String conclusion = content == null || content.isBlank()
                ? ("pass".equalsIgnoreCase(result) ? "验收通过" : "提交验收") : content;
        return projectContext.submitAcceptance(current.getCompany().getComId(), stage.getProject().getProjectId(),
                stageId, current, conclusion, issues, images);
    }
}
