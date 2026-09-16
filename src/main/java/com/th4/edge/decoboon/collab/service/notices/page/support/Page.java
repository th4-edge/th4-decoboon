package com.th4.edge.decoboon.collab.service.notices.page.support;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.collab.model.Notice;
import com.th4.edge.decoboon.collab.repository.NoticeRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Note("通知分页")
@Properties(allow = {
        @AllowProperties(model = Notice.class,
                properties = {"id", "project", "customer", "quote", "type", "title", "content", "read", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "property" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo", "community", "addr", "status"}),
        @AllowProperties(model = Quote.class, properties = {"quoteId", "no", "status", "amount", "area", "customer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("collab/notice/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.collab.service.notices.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Boolean read;

    private final NoticeRepository noticeRepository;
    private final NoticeContext noticeContext;
    private final DataScopeContext dataScopeContext;

    public Page(NoticeRepository noticeRepository,
                NoticeContext noticeContext,
                DataScopeContext dataScopeContext) {
        this.noticeRepository = noticeRepository;
        this.noticeContext = noticeContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public org.springframework.data.domain.Page<Notice> call(Notice request) {
        Employee currentEmployee = dataScopeContext.requireEmployee(employee);
        noticeContext.refresh(currentEmployee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, pageSize,
                Sort.by(Sort.Direction.DESC, "createTime.dateTime"));
        if (Boolean.FALSE.equals(read)) {
            return noticeRepository.findByReceiver_EmpIdAndReadFalse(currentEmployee.getEmpId(), pageable);
        }
        return noticeRepository.findByReceiver_EmpId(currentEmployee.getEmpId(), pageable);
    }
}
