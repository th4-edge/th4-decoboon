package com.th4.edge.decoboon.customer.service.feedback.page.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 业主意见投诉列表实现。 */
@Note("业主意见投诉列表")
@Properties(allow = {
        @AllowProperties(model = CustomerFeedback.class, properties = {
                "feedbackId", "project", "property", "stage", "kind", "type", "status", "title", "content",
                "images", "handler", "response", "respondedAt", "dueAt", "closedAt", "satisfaction", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "community", "building", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/feedback/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.feedback.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject private Long projectId;
    @AutoInject private Integer size;

    private final CustomerAccessContext accessContext;
    private final CustomerFeedbackRepository feedbackRepository;

    public Page(CustomerAccessContext accessContext, CustomerFeedbackRepository feedbackRepository) {
        this.accessContext = accessContext;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFeedback> call(CustomerFeedback request) {
        Credentials credential = accessContext.requireCredential(principal);
        List<CustomerFeedback> values = feedbackRepository.findByCredential_CreIdOrderByFeedbackIdDesc(credential.getCreId());
        if (projectId != null) {
            accessContext.requireProject(credential, projectId);
            values = values.stream().filter(item -> projectId.equals(item.getProject().getProjectId())).toList();
        }
        int limit = size == null || size < 1 ? 50 : Math.min(size, 100);
        return values.stream().limit(limit).toList();
    }
}
