package com.th4.edge.decoboon.customer.service.follows.save.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.FollowWay;
import com.th4.edge.decoboon.customer.context.CustomerContext;
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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 新增客户跟进实现。 */
@Note("新增客户跟进")
@Properties(allow = {
        @AllowProperties(model = CustomerFollow.class,
                properties = {"followId", "customer", "emp", "way", "content", "nextFollowAt", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer-follow/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.follows.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    private FollowWay way;

    @AutoInject
    @NotEmpty
    @Length(max = 512)
    private String content;

    @AutoInject
    private String nextFollowAt;

    private final CustomerFollowRepository followRepository;
    private final CustomerRepository customerRepository;
    private final CustomerContext customerContext;
    private final DataScopeContext dataScopeContext;

    public Save(CustomerFollowRepository followRepository,
                CustomerRepository customerRepository,
                CustomerContext customerContext,
                DataScopeContext dataScopeContext) {
        this.followRepository = followRepository;
        this.customerRepository = customerRepository;
        this.customerContext = customerContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional
    public CustomerFollow call(CustomerFollow request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        Customer customer = customerContext.requireVisible(customerId, current);

        CustomerFollow follow = new CustomerFollow();
        follow.setCompany(current.getCompany());
        follow.setStore(customer.getStore());
        follow.setTeam(customer.getTeam());
        follow.setCustomer(customer);
        follow.setEmp(current);
        follow.setWay(way == null ? FollowWay.Other : way);
        follow.setContent(content);
        LocalDateTime next = parseDate(nextFollowAt);
        follow.setNextFollowAt(next);
        customer.setLastFollowAt(LocalDateTime.now());
        customer.setNextFollowAt(next);
        if (customer.getStatus() == CustStatus.New) {
            customer.setStatus(CustStatus.Following);
        }
        customerRepository.save(customer);
        return followRepository.save(follow);
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDateTime parsed = value.length() == 10
                    ? LocalDate.parse(value).atStartOfDay()
                    : LocalDateTime.parse(value.replace(' ', 'T'));
            if (parsed.toLocalDate().isBefore(LocalDate.now())) {
                throw new DefinedException(new DefaultErrorCode("400", "下次跟进时间不能早于今天"));
            }
            return parsed;
        } catch (java.time.format.DateTimeParseException error) {
            throw new DefinedException(new DefaultErrorCode("400", "下次跟进时间格式不正确"));
        }
    }
}
