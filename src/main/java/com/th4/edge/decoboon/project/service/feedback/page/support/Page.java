package com.th4.edge.decoboon.project.service.feedback.page.support;

import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 员工项目业主意见投诉列表实现。 */
@Note("项目业主意见投诉")
@Properties(allow = {
        @AllowProperties(model = CustomerFeedback.class, properties = {
                "feedbackId", "project", "property", "stage", "kind", "type", "status", "title", "content",
                "images", "handler", "response", "respondedAt", "dueAt", "closedAt", "satisfaction", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "community", "building", "roomNo", "address"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/feedback/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.feedback.page.Page {

    @User private Employee employee;
    @AutoInject private Long projectId;

    private final ProjectAccessContext accessContext;
    private final CustomerFeedbackRepository feedbackRepository;

    public Page(ProjectAccessContext accessContext, CustomerFeedbackRepository feedbackRepository) {
        this.accessContext = accessContext;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerFeedback> call(CustomerFeedback request) {
        accessContext.requireEmployee(employee);
        // 与 contract-change / acceptance / rectify 等其它项目子对象一致：数据范围之外的「项目参与人」也能看到本项目事实。
        Specification<CustomerFeedback> spec = accessContext.visibleChild(employee, CustomerFeedback.class, "project");
        if (projectId != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("project").get("projectId"), projectId));
        }
        return feedbackRepository.findAll(spec);
    }
}
