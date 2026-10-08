package com.th4.edge.decoboon.customer.service.cust.resume.support;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.customer.configure.CustStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恢复休眠客户 Provider。 */
@Note("恢复休眠客户")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "status", "statusBeforeDormant", "dormantReason", "dormantReasonText",
                        "dormantAt", "dormantBy", "restoredAt", "restoredBy", "nextFollowAt", "owner", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/customer/resume")
@Scope(ScopeTypes.PROTOTYPE)
public class Resume implements com.th4.edge.decoboon.customer.service.cust.resume.Resume {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    private CustStatus restoreStatus;

    @AutoInject
    private String nextFollowAt;

    @AutoInject
    @Length(max = 96)
    private String requestId;

    private final CustomerContext customerContext;
    private final CustomerDormantContext dormantContext;
    private final DataScopeContext dataScopeContext;
    private final DistributedLock distributedLock;

    public Resume(CustomerContext customerContext,
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
        String lockKey = "customer-status:" + current.getCompany().getComId() + ":" + customerId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "该客户正在被处理，请稍后重试"));
        }
        try {
            return dormantContext.restore(customer, current, restoreStatus, parseDate(nextFollowAt), requestId);
        } finally {
            distributedLock.unlock(lockKey);
        }
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
