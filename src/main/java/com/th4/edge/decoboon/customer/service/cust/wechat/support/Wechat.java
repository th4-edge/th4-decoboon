package com.th4.edge.decoboon.customer.service.cust.wechat.support;

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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 微信添加事件实现。 */
@Note("记录客户微信")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount", "lastVisitAt", "status"})
})
@Service("customer/customer/wechat")
@Scope(ScopeTypes.PROTOTYPE)
public class Wechat implements com.th4.edge.decoboon.customer.service.cust.wechat.Wechat {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long customerId;
    @AutoInject
    @Length(max = 64)
    private String wechatNo;

    private final CustomerRepository customerRepository;
    private final DataScopeContext dataScopeContext;

    public Wechat(CustomerRepository customerRepository, DataScopeContext dataScopeContext) {
        this.customerRepository = customerRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional
    public Customer call(Customer request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        var scope = dataScopeContext.resolve(current);
        Specification<Customer> byId = (root, query, builder) -> builder.equal(root.get("custId"), customerId);
        Customer customer = customerRepository.findOne(byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
        customer.setWechatAdded(true);
        customer.setWechatNo(wechatNo == null || wechatNo.isBlank() ? null : wechatNo.trim());
        if (customer.getWechatAddedAt() == null) customer.setWechatAddedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }
}
