package com.th4.edge.decoboon.content.service.employeecontent.info.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.decoboon.content.repository.PublicationRecordRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 员工内容详情实现。 */
@Note("员工内容详情")
@Properties(allow = {
        @AllowProperties(model = EmployeeContent.class,
                properties = {"contentId", "status", "startedAt", "submittedAt", "reviewStatus", "publishStatus",
                        "rejectionReason", "selectedAssetIds", "caption", "renderedFilePath", "coverFilePath", "renderJobId",
                        "renderError", "reviewedBy", "reviewedAt", "renderedUrl", "coverUrl", "latestPublication",
                        "createTime", "updateTime", "task", "employee"}),
        @AllowProperties(model = ContentTask.class, properties = {"taskId", "title", "status", "deadline", "materialIds", "requiredClaims", "forbiddenClaims", "template"}),
        @AllowProperties(model = ContentTemplate.class, properties = {"templateId", "name", "format", "durationSec", "scriptSchema"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = PublicationRecord.class, properties = {"publicationId", "platform", "postUrl", "publishedAt", "backfillEvidencePath", "status"})
})
@Service("content/employee-content/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.content.service.employeecontent.info.Info {
    @User private Employee employee;
    @AutoInject @NotNull private Long contentId;
    private final ContentAccessContext accessContext;
    private final PublicationRecordRepository publicationRepository;
    private final FileSignatureContext signatureContext;
    public Info(ContentAccessContext accessContext, PublicationRecordRepository publicationRepository,
                FileSignatureContext signatureContext) {
        this.accessContext = accessContext; this.publicationRepository = publicationRepository; this.signatureContext = signatureContext;
    }
    @Override public EmployeeContent call(EmployeeContent request) {
        EmployeeContent item = accessContext.requireEmployeeContent(contentId, employee);
        if (item.getRenderedFilePath() != null) item.setRenderedUrl(signatureContext.sign(item.getRenderedFilePath()));
        if (item.getCoverFilePath() != null) item.setCoverUrl(signatureContext.sign(item.getCoverFilePath()));
        item.setLatestPublication(publicationRepository.findFirstByEmployeeContent_ContentIdOrderByPublicationIdDesc(item.getContentId()).orElse(null));
        return item;
    }
}
