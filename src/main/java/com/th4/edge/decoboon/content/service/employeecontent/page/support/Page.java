package com.th4.edge.decoboon.content.service.employeecontent.page.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.content.configure.EmployeeContentStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.decoboon.content.repository.PublicationRecordRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

/** 员工内容分页实现。 */
@Note("员工内容分页")
@Properties(allow = {
        @AllowProperties(model = EmployeeContent.class,
                properties = {"contentId", "status", "startedAt", "submittedAt", "reviewStatus", "publishStatus",
                        "rejectionReason", "selectedAssetIds", "caption", "renderedFilePath", "coverFilePath", "renderJobId",
                        "renderError", "reviewedBy", "reviewedAt", "renderedUrl", "coverUrl", "latestPublication",
                        "createTime", "updateTime", "task", "employee"}),
        @AllowProperties(model = ContentTask.class, properties = {"taskId", "title", "status", "deadline", "template"}),
        @AllowProperties(model = ContentTemplate.class, properties = {"templateId", "name", "format", "durationSec"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = PublicationRecord.class, properties = {"publicationId", "platform", "postUrl", "publishedAt", "backfillEvidencePath", "status"})
})
@Service("content/employee-content/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.content.service.employeecontent.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private EmployeeContentStatus status;
    private final EmployeeContentRepository repository;
    private final PublicationRecordRepository publicationRepository;
    private final ContentAccessContext accessContext;
    private final FileSignatureContext signatureContext;

    public Page(EmployeeContentRepository repository, PublicationRecordRepository publicationRepository,
                ContentAccessContext accessContext, FileSignatureContext signatureContext) {
        this.repository = repository; this.publicationRepository = publicationRepository;
        this.accessContext = accessContext; this.signatureContext = signatureContext;
    }

    @Override
    public org.springframework.data.domain.Page<EmployeeContent> call(EmployeeContent request) {
        Employee me = accessContext.requireEmployee(employee);
        List<EmployeeContent> source = (accessContext.isManager(me)
                ? repository.findByCompany_ComIdOrderByContentIdDesc(me.getCompany().getComId())
                : repository.findByCompany_ComIdAndEmployee_EmpIdOrderByContentIdDesc(me.getCompany().getComId(), me.getEmpId()))
                .stream().filter(item -> status == null || item.getStatus() == status)
                .sorted(Comparator.comparing(EmployeeContent::getContentId, Comparator.nullsLast(Comparator.reverseOrder())))
                .peek(this::hydrate).toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, source.size());
        int to = Math.min(from + pageSize, source.size());
        return new PageImpl<>(source.subList(from, to), PageRequest.of(page, pageSize), source.size());
    }

    private void hydrate(EmployeeContent item) {
        if (item.getRenderedFilePath() != null) item.setRenderedUrl(signatureContext.sign(item.getRenderedFilePath()));
        if (item.getCoverFilePath() != null) item.setCoverUrl(signatureContext.sign(item.getCoverFilePath()));
        item.setLatestPublication(publicationRepository.findFirstByEmployeeContent_ContentIdOrderByPublicationIdDesc(item.getContentId()).orElse(null));
    }
}
