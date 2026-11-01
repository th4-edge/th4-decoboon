package com.th4.edge.decoboon.content.service.publication.backfill.support;

import com.th4.edge.decoboon.content.configure.ContentPlatform;
import com.th4.edge.decoboon.content.configure.ContentTaskStatus;
import com.th4.edge.decoboon.content.configure.EmployeeContentStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.decoboon.content.repository.ContentTaskRepository;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.decoboon.content.repository.PublicationRecordRepository;
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

/** 回填员工发布记录实现。 */
@Note("回填员工发布记录")
@Properties(allow = {
        @AllowProperties(model = PublicationRecord.class,
        properties = {"publicationId", "platform", "postUrl", "platformPostId", "publishedAt", "backfillEvidencePath", "status", "createTime", "updateTime", "employeeContent", "task", "employee"}),
        @AllowProperties(model = com.th4.edge.decoboon.content.model.ContentTask.class, properties = {"taskId", "title", "goal", "targetAudience", "platform", "deadline", "assigneeScope", "status", "publishedAt", "closedAt", "claimedByMe", "myContentStatus", "myContentId"}),
        @AllowProperties(model = com.th4.edge.decoboon.content.model.EmployeeContent.class, properties = {"contentId", "status", "startedAt", "submittedAt", "reviewStatus", "publishStatus", "rejectionReason", "selectedAssetIds", "caption", "renderedFilePath", "coverFilePath", "renderJobId", "renderError", "reviewedAt", "renderedUrl", "coverUrl"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Employee.class, properties = {"empId", "name", "role", "status"})
})
@Service("content/publication/backfill")
@Scope(ScopeTypes.PROTOTYPE)
public class Backfill implements com.th4.edge.decoboon.content.service.publication.backfill.Backfill {
    @User private Employee employee;
    @AutoInject @NotNull private Long contentId;
    @AutoInject private ContentPlatform platform;
    @AutoInject private String postUrl;
    @AutoInject private String platformPostId;
    @AutoInject private LocalDateTime publishedAt;
    @AutoInject private String backfillEvidencePath;
    private final PublicationRecordRepository publicationRepository;
    private final EmployeeContentRepository contentRepository;
    private final ContentTaskRepository taskRepository;
    private final ContentAccessContext accessContext;
    public Backfill(PublicationRecordRepository publicationRepository, EmployeeContentRepository contentRepository,
                    ContentTaskRepository taskRepository, ContentAccessContext accessContext) {
        this.publicationRepository = publicationRepository; this.contentRepository = contentRepository;
        this.taskRepository = taskRepository; this.accessContext = accessContext;
    }
    @Override @Transactional public PublicationRecord call(PublicationRecord request) {
        Employee me = accessContext.requireEmployee(employee);
        EmployeeContent content = accessContext.requireEmployeeContent(contentId, me);
        if (!content.getEmployee().getEmpId().equals(me.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("403", "只能回填本人的发布记录"));
        }
        if (content.getStatus() != EmployeeContentStatus.ReadyToPublish
                && content.getStatus() != EmployeeContentStatus.Published
                && content.getStatus() != EmployeeContentStatus.Backfilled) {
            throw new DefinedException(new DefaultErrorCode("409", "内容尚未通过审核，不能回填发布"));
        }
        if ((postUrl == null || postUrl.isBlank()) && (backfillEvidencePath == null || backfillEvidencePath.isBlank())) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写作品链接或上传发布截图"));
        }
        PublicationRecord record = publicationRepository.findFirstByEmployeeContent_ContentIdOrderByPublicationIdDesc(contentId)
                .orElseGet(PublicationRecord::new);
        record.setCompany(me.getCompany());
        record.setEmployeeContent(content);
        record.setTask(content.getTask());
        record.setEmployee(me);
        record.setPlatform(platform == null ? ContentPlatform.Douyin : platform);
        record.setPostUrl(postUrl == null ? null : postUrl.trim());
        record.setPlatformPostId(platformPostId == null ? null : platformPostId.trim());
        record.setPublishedAt(publishedAt == null ? LocalDateTime.now() : publishedAt);
        record.setBackfillEvidencePath(backfillEvidencePath == null ? null : backfillEvidencePath.trim());
        record.setStatus("Backfilled");
        content.setStatus(EmployeeContentStatus.Backfilled);
        content.setPublishStatus("Backfilled");
        contentRepository.save(content);
        PublicationRecord saved = publicationRepository.save(record);
        boolean allDone = contentRepository.findByTask_TaskIdOrderByContentIdAsc(content.getTask().getTaskId()).stream()
                .allMatch(item -> item.getStatus() == EmployeeContentStatus.Backfilled);
        if (allDone && content.getTask().getStatus() != ContentTaskStatus.Closed) {
            content.getTask().setStatus(ContentTaskStatus.Completed);
            taskRepository.save(content.getTask());
        }
        return saved;
    }
}
