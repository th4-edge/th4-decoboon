package com.th4.edge.decoboon.customer.service.cust.info.support;

import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * 客户详情 Provider。
 */
@Note("客户详情")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr",
                        "area", "budget", "status", "owner", "remark", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount", "lastVisitAt",
                        "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt", "measureRequestedAt", "handedOffAt", "designer", "createTime"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name"})
})
@Service("customer/customer/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.cust.info.Info {

    @AutoInject
    @NotNull
    private Long customerId;

    /** 框架注入：当前登录员工。 */
    @User
    private Employee employee;

    private final CustomerRepository customerRepository;
    private final DataScopeContext dataScopeContext;

    public Info(CustomerRepository customerRepository,
                DataScopeContext dataScopeContext) {
        this.customerRepository = customerRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Customer call(Customer request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Customer> byId = (root, query, builder) ->
                builder.equal(root.get("custId"), customerId);
        return customerRepository.findOne(
                        byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
    }
}
