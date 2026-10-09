package com.th4.edge.decoboon.content.service.leadtouch.list.support;

import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.model.LeadTouch;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.decoboon.content.repository.LeadTouchRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 内容线索触点列表实现。 */
@Note("内容线索触点列表")
@Properties(allow = {
        @AllowProperties(model = LeadTouch.class, properties = {"touchId", "customer", "employeeContent", "publication", "task", "employee", "attributionType", "confidence", "isPrimary", "evidence", "touchedAt", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo", "source", "status"}),
        @AllowProperties(model = EmployeeContent.class, properties = {"contentId", "status", "caption"}),
        @AllowProperties(model = PublicationRecord.class, properties = {"publicationId", "platform", "postUrl", "publishedAt", "status"}),
        @AllowProperties(model = ContentTask.class, properties = {"taskId", "title", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("content/lead-touch/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.content.service.leadtouch.list.List {
    @User private Employee employee;
    @AutoInject private Long customerId;
    @AutoInject private Long contentId;
    private final LeadTouchRepository repository;
    private final CustomerRepository customerRepository;
    private final ContentAccessContext accessContext;
    public List(LeadTouchRepository repository, CustomerRepository customerRepository, ContentAccessContext accessContext) {
        this.repository=repository; this.customerRepository=customerRepository; this.accessContext=accessContext;
    }
    @Override public java.util.List<LeadTouch> call(LeadTouch request) {
        Employee me = accessContext.requireEmployee(employee);
        java.util.List<LeadTouch> rows;
        if (customerId != null) {
            customerRepository.findByCustIdAndCompany_ComId(customerId, me.getCompany().getComId())
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
            rows = repository.findByCustomer_CustIdAndCompany_ComIdOrderByTouchIdDesc(customerId, me.getCompany().getComId());
        } else if (contentId != null) {
            accessContext.requireEmployeeContent(contentId, me);
            rows = repository.findByEmployeeContent_ContentIdOrderByTouchIdDesc(contentId);
        } else {
            rows = repository.findByCompany_ComIdOrderByTouchIdDesc(me.getCompany().getComId());
        }
        return rows.stream().filter(item -> canView(me, item)).toList();
    }

    private boolean canView(Employee me, LeadTouch touch) {
        Employee owner = touch.getEmployee();
        if (owner == null) return false;
        if (me.getRole() == EmployeeRole.Boss) return true;
        if (me.getRole() == EmployeeRole.StoreManager) {
            return me.getStore() != null && owner.getStore() != null
                    && me.getStore().getStoreId().equals(owner.getStore().getStoreId());
        }
        if (me.getRole() == EmployeeRole.TeamLeader) {
            return me.getTeam() != null && owner.getTeam() != null
                    && me.getTeam().getTeamId().equals(owner.getTeam().getTeamId());
        }
        return me.getEmpId().equals(owner.getEmpId());
    }
}
