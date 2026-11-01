package com.th4.edge.decoboon.collab.service.notices.update.support;

import com.th4.edge.decoboon.collab.model.Notice;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.collab.repository.NoticeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Note("通知已读")
@Properties(allow = {
        @AllowProperties(model = Notice.class,
                properties = {"id", "project", "customer", "quote", "type", "title", "content", "read", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "property" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo", "community", "addr", "status"}),
        @AllowProperties(model = Quote.class, properties = {"quoteId", "no", "status", "amount", "area"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class, properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"})
})
@Service("collab/notice/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.collab.service.notices.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long noticeId;
    @AutoInject
    private Boolean read;

    private final NoticeRepository noticeRepository;
    private final DataScopeContext dataScopeContext;

    public Update(NoticeRepository noticeRepository, DataScopeContext dataScopeContext) {
        this.noticeRepository = noticeRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Notice call(Notice request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        Notice notice = noticeRepository.findByNoticeIdAndReceiver_EmpId(noticeId, current.getEmpId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "通知不存在或无权访问")));
        notice.setRead(read == null || read);
        return noticeRepository.save(notice);
    }
}
