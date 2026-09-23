package com.th4.edge.decoboon.customer.service.feedback.save.support;

import com.th4.edge.decoboon.customer.configure.CustomerFeedbackKind;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackType;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 业主意见投诉提交实现。 */
@Note("业主意见投诉提交")
@Properties(allow = {
        @AllowProperties(model = CustomerFeedback.class, properties = {
                "feedbackId", "project", "property", "stage", "kind", "type", "status", "title", "content",
                "images", "handler", "response", "respondedAt", "dueAt", "closedAt", "satisfaction", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "community", "building", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/feedback/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.feedback.save.Save {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject @NotNull private Long projectId;
    @AutoInject private Long stageId;
    @AutoInject @NotNull private CustomerFeedbackKind kind;
    @AutoInject @NotNull private CustomerFeedbackType type;
    @AutoInject @NotEmpty private String title;
    @AutoInject @NotEmpty private String content;
    @AutoInject private List<String> images;

    private final CustomerAccessContext accessContext;
    private final CustomerFeedbackRepository feedbackRepository;
    private final ProjectStageRepository stageRepository;

    public Save(CustomerAccessContext accessContext,
                CustomerFeedbackRepository feedbackRepository,
                ProjectStageRepository stageRepository) {
        this.accessContext = accessContext;
        this.feedbackRepository = feedbackRepository;
        this.stageRepository = stageRepository;
    }

    @Override
    @Transactional
    public CustomerFeedback call(CustomerFeedback request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        Employee handler = project.getManager();
        if (handler == null) {
            throw new DefinedException(new DefaultErrorCode("400", "当前项目未指定项目经理，无法提交意见或投诉"));
        }
        CustomerFeedback feedback = new CustomerFeedback();
        feedback.setCompany(project.getCompany());
        feedback.setStore(project.getStore());
        feedback.setTeam(project.getTeam());
        feedback.setOwner(handler);
        feedback.setProject(project);
        feedback.setProperty(project.getProperty());
        feedback.setCustomer(project.getCustomer());
        feedback.setCredential(credential);
        feedback.setKind(kind);
        feedback.setType(type);
        feedback.setStatus(CustomerFeedbackStatus.Submitted);
        feedback.setTitle(title.trim());
        feedback.setContent(content.trim());
        feedback.setImages(normalizeImages(images));
        feedback.setHandler(handler);
        feedback.setDueAt(LocalDateTime.now().plusDays(
                kind == CustomerFeedbackKind.Complaint ? 1 : 3));
        if (stageId != null) {
            ProjectStage stage = stageRepository.findById(stageId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "阶段不存在或无权访问")));
            if (stage.getProject() == null || !projectId.equals(stage.getProject().getProjectId())) {
                throw new DefinedException(new DefaultErrorCode("404", "阶段不存在或无权访问"));
            }
            feedback.setStage(stage);
        }
        return feedbackRepository.save(feedback);
    }

    private List<String> normalizeImages(List<String> values) {
        if (values == null) return new ArrayList<>();
        List<String> normalized = values.stream().filter(v -> v != null && !v.isBlank()).distinct().toList();
        if (normalized.size() > 9) {
            throw new DefinedException(new DefaultErrorCode("400", "意见投诉图片最多上传 9 张"));
        }
        return new ArrayList<>(normalized);
    }
}
