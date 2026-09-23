package com.th4.edge.decoboon.customer.service.acceptance.update.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.AcceptanceRectificationType;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主验收确认实现，重复提交同一结果时保持幂等。 */
@Note("业主验收确认")
@Properties(allow = {
        @AllowProperties(model = Acceptance.class,
                properties = {"acceptanceId", "project", "property", "stage", "status", "conclusion", "issues",
                        "evidenceUrls", "rectificationOwner", "rectificationDueDate", "rectificationType",
                        "rectificationIssueImages", "confirmedAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "community", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status", "progress"})
})
@Service("customer/acceptance/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.customer.service.acceptance.update.Update {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long acceptanceId;

    @AutoInject
    @NotNull
    private Boolean confirmed;

    @AutoInject
    @Length(max = 1024)
    private String conclusion;

    /** 驳回时业主填写的问题清单。 */
    @AutoInject
    private java.util.List<String> issues;

    @AutoInject
    private AcceptanceRectificationType rectificationType;

    @AutoInject
    private java.util.List<String> rectificationIssueImages;

    private final CustomerAccessContext accessContext;
    private final AcceptanceRepository acceptanceRepository;
    private final ProjectContext projectContext;
    private final ProjectStageRepository stageRepository;

    public Update(CustomerAccessContext accessContext,
                  AcceptanceRepository acceptanceRepository,
                  ProjectContext projectContext,
                  ProjectStageRepository stageRepository) {
        this.accessContext = accessContext;
        this.acceptanceRepository = acceptanceRepository;
        this.projectContext = projectContext;
        this.stageRepository = stageRepository;
    }

    @Override
    @Transactional
    public Acceptance call(Acceptance request) {
        Credentials credential = accessContext.requireCredential(principal);
        Acceptance candidate = acceptanceRepository.findForCustomerAccess(
                        acceptanceId, credential.getCreId(), CustomerAccessStatus.Active)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "验收单不存在或无权访问")));
        // 与提交/复验统一按“阶段 -> 验收单”顺序加锁，避免提交与业主确认并发时交叉等待。
        stageRepository.findLockedByStageId(candidate.getStage().getStageId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目阶段不存在")));
        Acceptance acceptance = acceptanceRepository.findLockedForCustomerAccess(
                        acceptanceId, credential.getCreId(), CustomerAccessStatus.Active)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "验收单不存在或无权访问")));
        AcceptStatus target = Boolean.TRUE.equals(confirmed) ? AcceptStatus.Confirmed : AcceptStatus.Rejected;
        if (acceptance.getStatus() != AcceptStatus.Pending) {
            if (acceptance.getStatus() == target) {
                return acceptance;
            }
            throw new DefinedException(new DefaultErrorCode("400", "验收单已处理"));
        }

        Employee rectificationOwner = null;
        if (!Boolean.TRUE.equals(confirmed)) {
            if (rectificationType == null) {
                throw new DefinedException(new DefaultErrorCode("400", "请选择整改类型"));
            }
            // 业主只提交问题、图片和类型；责任人固定为当前项目项目经理，截止日期由服务端计算。
            rectificationOwner = acceptance.getProject() == null
                    ? null : acceptance.getProject().getManager();
            if (rectificationOwner == null) {
                throw new DefinedException(new DefaultErrorCode("400", "当前项目未指定项目经理，无法提交整改要求"));
            }
        }
        return projectContext.confirmAcceptance(acceptance, credential, confirmed, conclusion,
                issues, rectificationOwner, null, rectificationType, rectificationIssueImages);
    }
}
