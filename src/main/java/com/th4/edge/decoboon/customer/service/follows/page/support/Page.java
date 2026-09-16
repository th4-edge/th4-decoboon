package com.th4.edge.decoboon.customer.service.follows.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** 客户跟进分页实现。 */
@Note("客户跟进分页")
@Properties(allow = {
        @AllowProperties(model = CustomerFollow.class,
                properties = {"followId", "customer", "emp", "way", "content", "nextFollowAt", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer-follow/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.follows.page.Page {

    @AutoInject
    private Long customerId;

    @User
    private Employee employee;

    private final CustomerFollowRepository followRepository;
    private final CustomerRepository customerRepository;
    private final DataScopeContext dataScopeContext;

    public Page(CustomerFollowRepository followRepository,
                CustomerRepository customerRepository,
                DataScopeContext dataScopeContext) {
        this.followRepository = followRepository;
        this.customerRepository = customerRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public org.springframework.data.domain.Page<CustomerFollow> call(CustomerFollow request) {
        if (customerId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "客户索引不能为空"));
        }
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Customer> byId = (root, query, builder) ->
                builder.equal(root.get("custId"), customerId);
        customerRepository.findOne(byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
        return followRepository.findByCustomer_CustIdOrderByCreateTime_DateTimeDesc(
                customerId, PageRequest.of(0, 200));
    }
}
