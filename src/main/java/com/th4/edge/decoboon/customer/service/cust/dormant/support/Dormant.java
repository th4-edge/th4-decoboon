package com.th4.edge.decoboon.customer.service.cust.dormant.support;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerDormantReason;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.context.CustomerDormantContext;
import com.th4.edge.decoboon.customer.model.Customer;
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
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设置客户休眠 Provider。 */
@Note("设置休眠客户")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "status", "statusBeforeDormant", "dormantReason", "dormantReasonText",
                        "dormantAt", "dormantBy", "restoredAt", "restoredBy", "nextFollowAt", "owner", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/customer/dormant")
@Scope(ScopeTypes.PROTOTYPE)
public class Dormant implements com.th4.edge.decoboon.customer.service.cust.dormant.Dormant {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    @NotNull
    private CustomerDormantReason reasonCode;

    @AutoInject
    @Length(max = 200)
    private String reasonText;

    @AutoInject
    private CustStatus expectedStatus;

    @AutoInject
    @Length(max = 96)
    private String requestId;

    private final CustomerContext customerContext;
    private final CustomerDormantContext dormantContext;
    private final DataScopeContext dataScopeContext;
    private final DistributedLock distributedLock;

    public Dormant(CustomerContext customerContext,
                   CustomerDormantContext dormantContext,
                   DataScopeContext dataScopeContext,
                   DistributedLock distributedLock) {
        this.customerContext = customerContext;
        this.dormantContext = dormantContext;
        this.dataScopeContext = dataScopeContext;
        this.distributedLock = distributedLock;
    }

    @Override
    @Transactional
    public Customer call(Customer request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        dormantContext.requireWritableRole(current);
        Customer customer = customerContext.requireVisible(customerId, current);
        String lockKey = lockKey(current, customerId);
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "该客户正在被处理，请稍后重试"));
        }
        try {
            return dormantContext.setDormant(customer, current, reasonCode, reasonText, expectedStatus, requestId);
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private String lockKey(Employee employee, Long customerId) {
        return "customer-status:" + employee.getCompany().getComId() + ":" + customerId;
    }
}
