package com.th4.edge.decoboon.project.service.manager.customers.page.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectManagerAccessContext;
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

/** 项目经理负责项目关联客户分页实现。 */
@Note("项目经理关联客户分页")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area",
                        "budget", "status", "owner", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount",
                        "lastVisitAt", "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt",
                        "measureRequestedAt", "handedOffAt", "designer", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/manager/customer/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.manager.customers.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private String keyword;

    @AutoInject
    private CustStatus status;

    private final ProjectManagerAccessContext accessContext;

    public Page(ProjectManagerAccessContext accessContext) {
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Customer> call(Customer request) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Customer> customers = accessContext.managedCustomers(employee).stream()
                .filter(customer -> status == null || customer.getStatus() == status)
                .filter(customer -> kw.isBlank()
                        || contains(customer.getName(), kw)
                        || contains(customer.getTelNo(), kw)
                        || contains(customer.getCommunity(), kw)
                        || contains(customer.getAddr(), kw))
                .sorted(Comparator.comparing(Customer::getCustId).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        int from = Math.min(page * pageSize, customers.size());
        int to = Math.min(from + pageSize, customers.size());
        return new PageImpl<>(customers.subList(from, to), PageRequest.of(page, pageSize), customers.size());
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
    }
}
