package com.th4.edge.decoboon.content.service.employeecontent.review.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.content.configure.EmployeeContentStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** 审核员工成片实现。 */
@Note("审核员工成片")
@Properties(allow = {
        @AllowProperties(model = EmployeeContent.class,
        properties = {"contentId", "status", "reviewStatus", "rejectionReason", "reviewedBy", "reviewedAt", "renderedUrl", "coverUrl"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name", "role", "status"})
})
@Service("content/employee-content/review")
@Scope(ScopeTypes.PROTOTYPE)
public class Review implements com.th4.edge.decoboon.content.service.employeecontent.review.Review {
    @User private Employee employee;
    @AutoInject @NotNull private Long contentId;
    @AutoInject @NotNull private Boolean approved;
    @AutoInject private String reason;
    private final EmployeeContentRepository contentRepository;
    private final ContentAccessContext accessContext;
    private final FileSignatureContext signatureContext;
    public Review(EmployeeContentRepository contentRepository, ContentAccessContext accessContext,
                  FileSignatureContext signatureContext) {
        this.contentRepository = contentRepository; this.accessContext = accessContext; this.signatureContext = signatureContext;
    }
    @Override @Transactional public EmployeeContent call(EmployeeContent request) {
        Employee me = accessContext.requireManager(employee);
        EmployeeContent content = accessContext.requireEmployeeContent(contentId, me);
        if (content.getStatus() != EmployeeContentStatus.PendingReview) {
            throw new DefinedException(new DefaultErrorCode("409", "当前内容不在待审核状态"));
        }
        content.setReviewStatus(Boolean.TRUE.equals(approved) ? "Approved" : "Rejected");
        content.setStatus(Boolean.TRUE.equals(approved) ? EmployeeContentStatus.ReadyToPublish : EmployeeContentStatus.Rejected);
        content.setRejectionReason(Boolean.TRUE.equals(approved) ? null : (reason == null ? "审核驳回" : reason));
        content.setReviewedBy(me);
        content.setReviewedAt(LocalDateTime.now());
        EmployeeContent saved = contentRepository.save(content);
        if (saved.getRenderedFilePath() != null) saved.setRenderedUrl(signatureContext.sign(saved.getRenderedFilePath()));
        if (saved.getCoverFilePath() != null) saved.setCoverUrl(signatureContext.sign(saved.getCoverFilePath()));
        return saved;
    }
}
