package com.th4.edge.decoboon.customer.service.feedback.close.support;

import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 业主确认关闭意见或投诉实现。 */
@Note("业主意见投诉关闭")
@Properties(allow = {
        @AllowProperties(model = CustomerFeedback.class, properties = {
                "feedbackId", "kind", "type", "status", "title", "content", "images", "handler", "response",
                "respondedAt", "dueAt", "closedAt", "satisfaction", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/feedback/close")
@Scope(ScopeTypes.PROTOTYPE)
public class Close implements com.th4.edge.decoboon.customer.service.feedback.close.Close {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject @NotNull private Long feedbackId;
    @AutoInject private Integer satisfaction;

    private final CustomerAccessContext accessContext;
    private final CustomerFeedbackRepository feedbackRepository;

    public Close(CustomerAccessContext accessContext, CustomerFeedbackRepository feedbackRepository) {
        this.accessContext = accessContext;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional
    public CustomerFeedback call(CustomerFeedback request) {
        Credentials credential = accessContext.requireCredential(principal);
        CustomerFeedback feedback = feedbackRepository.findByFeedbackIdAndCredential_CreId(feedbackId, credential.getCreId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "意见或投诉不存在或无权访问")));
        accessContext.requireProject(credential, feedback.getProject().getProjectId());
        if (satisfaction != null && (satisfaction < 1 || satisfaction > 5)) {
            throw new DefinedException(new DefaultErrorCode("400", "满意度必须是 1-5"));
        }
        feedback.setStatus(CustomerFeedbackStatus.Closed);
        feedback.setSatisfaction(satisfaction);
        feedback.setClosedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }
}
