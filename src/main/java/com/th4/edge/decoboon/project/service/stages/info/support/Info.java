package com.th4.edge.decoboon.project.service.stages.info.support;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Note("项目阶段详情")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "name", "stageCode", "sort", "weight", "status", "progress",
                        "plannedStart", "plannedEnd", "actualStart", "actualEnd", "createTime"}),
        // 阶段页只需要项目摘要；property 会带出门店/团队/负责人循环对象，曾生成 240KB+ 非法 JSON。
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr" })
})
@Service("project/stage/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.stages.info.Info {
    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long stageId;
    private final ProjectStageRepository repository;
    private final ProjectAccessContext accessContext;

    public Info(ProjectStageRepository repository, ProjectAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    public ProjectStage call(ProjectStage request) {
        ProjectStage stage = repository.findOne(((Specification<ProjectStage>) (root, query, builder) -> builder.equal(root.get("stageId"), stageId)).and(accessContext.visibleChild(employee, ProjectStage.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在")));
        accessContext.requireVisible(stage.getProject(), employee);
        return stage;
    }
}
