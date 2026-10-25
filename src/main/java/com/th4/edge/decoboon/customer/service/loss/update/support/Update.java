package com.th4.edge.decoboon.customer.service.loss.update.support;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.context.CustomerLossContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.decoboon.customer.repository.CustomerLossRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恢复客户流失记录 Provider。 */
@Note("恢复客户流失")
@Properties(allow = {
        @AllowProperties(model = CustomerLoss.class,
                properties = {"lossId", "scope", "stage", "fromStatus", "reasonCode", "reasonText",
                        "competitorName", "lostAt", "lostBy", "operatorRole", "restoredAt", "restoredBy",
                        "restoreStatus", "restoreNote", "customer", "property"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/loss/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.customer.service.loss.update.Update {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long lossId;

    @AutoInject
    private CustStatus restoreStatus;

    @AutoInject
    private String nextFollowAt;

    @AutoInject
    @Length(max = 500)
    private String restoreNote;

    @AutoInject
    @Length(max = 96)
    private String requestId;

    private final CustomerLossRepository lossRepository;
    private final CustomerContext customerContext;
    private final CustomerLossContext lossContext;
    private final DataScopeContext dataScopeContext;
    private final DistributedLock distributedLock;
    private final TransactionTemplate transactionTemplate;

    public Update(CustomerLossRepository lossRepository,
                  CustomerContext customerContext,
                  CustomerLossContext lossContext,
                  DataScopeContext dataScopeContext,
                  DistributedLock distributedLock,
                  PlatformTransactionManager transactionManager) {
        this.lossRepository = lossRepository;
        this.customerContext = customerContext;
        this.lossContext = lossContext;
        this.dataScopeContext = dataScopeContext;
        this.distributedLock = distributedLock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public CustomerLoss call(CustomerLoss request) {
        // 锁前只做无状态定位，不把业务实体载入后续事务的一级缓存。
        Employee current = dataScopeContext.requireEmployee(employee);
        lossContext.requireWritableRole(current);
        CustomerLoss located = lossRepository.findByCompany_ComIdAndLossId(
                        current.getCompany().getComId(), lossId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "流失记录不存在或无权访问")));
        if (located.getCustomer() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "流失记录客户不存在"));
        }
        Long customerId = located.getCustomer().getCustId();
        String lockKey = "customer-loss:" + current.getCompany().getComId() + ":" + customerId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "该客户正在被处理，请稍后重试"));
        }
        try {
            CustomerLoss saved = transactionTemplate.execute(status -> {
                CustomerLoss loss = lossRepository.findByCompany_ComIdAndLossId(
                                current.getCompany().getComId(), lossId)
                        .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "流失记录不存在或无权访问")));
                Customer customer = customerContext.requireVisible(customerId, current);
                return lossContext.restore(loss, customer, current, restoreStatus,
                        parseDate(nextFollowAt), restoreNote, requestId);
            });
            if (saved == null) {
                throw new DefinedException(new DefaultErrorCode("500", "客户流失恢复失败"));
            }
            return saved;
        } finally {
            // TransactionTemplate 返回时事务已经提交或回滚，锁才允许释放。
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
