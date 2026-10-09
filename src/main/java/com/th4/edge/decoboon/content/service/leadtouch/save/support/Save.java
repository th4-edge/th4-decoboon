package com.th4.edge.decoboon.content.service.leadtouch.save.support;

import com.th4.edge.decoboon.content.configure.ContentAttributionConfidence;
import com.th4.edge.decoboon.content.configure.ContentAttributionType;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.model.LeadTouch;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.decoboon.content.repository.LeadTouchRepository;
import com.th4.edge.decoboon.content.repository.PublicationRecordRepository;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
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

/** 内容线索归因保存实现。 */
@Note("内容线索归因保存")
@Properties(allow = {
        @AllowProperties(model = LeadTouch.class,
                properties = {"touchId", "customer", "employeeContent", "publication", "task", "employee",
                        "attributionType", "confidence", "isPrimary", "evidence", "touchedAt", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo", "source", "status"}),
        @AllowProperties(model = EmployeeContent.class, properties = {"contentId", "status", "caption"}),
        @AllowProperties(model = PublicationRecord.class, properties = {"publicationId", "platform", "postUrl", "publishedAt", "status"}),
        @AllowProperties(model = ContentTask.class, properties = {"taskId", "title", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("content/lead-touch/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.content.service.leadtouch.save.Save {
    @User private Employee employee;
    @AutoInject @NotNull private Long customerId;
    @AutoInject @NotNull private Long contentId;
    @AutoInject private Long publicationId;
    @AutoInject private ContentAttributionType attributionType;
    @AutoInject private ContentAttributionConfidence confidence;
    @AutoInject private Boolean isPrimary;
    @AutoInject private String evidence;
    private final LeadTouchRepository touchRepository;
    private final CustomerRepository customerRepository;
    private final PublicationRecordRepository publicationRepository;
    private final ContentAccessContext accessContext;
    public Save(LeadTouchRepository touchRepository, CustomerRepository customerRepository,
                PublicationRecordRepository publicationRepository, ContentAccessContext accessContext) {
        this.touchRepository = touchRepository; this.customerRepository = customerRepository;
        this.publicationRepository = publicationRepository; this.accessContext = accessContext;
    }
    @Override @Transactional public LeadTouch call(LeadTouch request) {
        Employee me = accessContext.requireEmployee(employee);
        EmployeeContent content = accessContext.requireEmployeeContent(contentId, me);
        if (!accessContext.isManager(me) && !content.getEmployee().getEmpId().equals(me.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("403", "只能为自己的内容登记线索归因"));
        }
        Customer customer = customerRepository.findByCustIdAndCompany_ComId(customerId, me.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
        PublicationRecord publication;
        if (publicationId != null) {
            publication = publicationRepository.findById(publicationId)
                    .filter(item -> item.getCompany().getComId().equals(me.getCompany().getComId()))
                    .filter(item -> item.getEmployeeContent().getContentId().equals(contentId))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "发布记录不存在或与内容不匹配")));
        } else {
            publication = publicationRepository.findFirstByEmployeeContent_ContentIdOrderByPublicationIdDesc(contentId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("409", "内容尚未回填发布，暂不能登记线索归因")));
        }
        if (!"Backfilled".equals(publication.getStatus()) && !"Published".equals(publication.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("409", "发布记录尚未回填，暂不能登记线索归因"));
        }
        LeadTouch touch = touchRepository.findFirstByCustomer_CustIdAndEmployeeContent_ContentId(customerId, contentId)
                .orElseGet(LeadTouch::new);
        boolean primary = isPrimary == null || Boolean.TRUE.equals(isPrimary);
        ContentAttributionConfidence resolvedConfidence = confidence == null
                ? ContentAttributionConfidence.Medium : confidence;
        if (primary && resolvedConfidence == ContentAttributionConfidence.Low) {
            throw new DefinedException(new DefaultErrorCode("400", "低置信归因不能设为主归因"));
        }
        if (primary) {
            for (LeadTouch other : touchRepository.findByCustomer_CustIdOrderByTouchIdDesc(customerId)) {
                if (other.getIsPrimary()) { other.setIsPrimary(false); touchRepository.save(other); }
            }
        }
        touch.setCompany(me.getCompany()); touch.setCustomer(customer); touch.setEmployeeContent(content);
        touch.setPublication(publication); touch.setTask(content.getTask()); touch.setEmployee(content.getEmployee());
        touch.setAttributionType(attributionType == null ? ContentAttributionType.Manual : attributionType);
        touch.setConfidence(resolvedConfidence);
        touch.setIsPrimary(primary); touch.setEvidence(evidence == null ? null : evidence.trim());
        touch.setTouchedAt(LocalDateTime.now());
        customer.setSource(CustomerSource.Lead);
        customerRepository.save(customer);
        return touchRepository.save(touch);
    }
}
