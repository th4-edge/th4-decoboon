package com.th4.edge.decoboon.project.service.feedback.update.support;

import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 员工处理业主意见投诉实现。 */
@Note("处理业主意见投诉")
@Properties(allow = {
        @AllowProperties(model = CustomerFeedback.class, properties = {
                "feedbackId", "project", "stage", "kind", "type", "status", "title", "content", "images",
                "handler", "response", "respondedAt", "dueAt", "closedAt", "satisfaction", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/feedback/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.feedback.update.Update {

    @User private Employee employee;
    @AutoInject @NotNull private Long feedbackId;
    @AutoInject private CustomerFeedbackStatus status;
    @AutoInject private String response;

    private final DataScopeContext dataScopeContext;
    private final CustomerFeedbackRepository feedbackRepository;

    public Update(DataScopeContext dataScopeContext, CustomerFeedbackRepository feedbackRepository) {
        this.dataScopeContext = dataScopeContext;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional
    public CustomerFeedback call(CustomerFeedback request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.requireScope(current);
        Specification<CustomerFeedback> byId = (root, query, builder) -> builder.equal(root.get("feedbackId"), feedbackId);
        CustomerFeedback feedback = feedbackRepository.findOne(
                        byId.and(DataScopeSpecification.visible(CustomerFeedback.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "意见或投诉不存在或无权访问")));
        if (status == null) {
            throw new DefinedException(new DefaultErrorCode("400", "处理状态不能为空"));
        }
        feedback.setStatus(status);
        feedback.setHandler(current);
        if (response != null) feedback.setResponse(response.trim());
        if (feedback.getRespondedAt() == null) feedback.setRespondedAt(LocalDateTime.now());
        if (status == CustomerFeedbackStatus.Closed) feedback.setClosedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }
}
