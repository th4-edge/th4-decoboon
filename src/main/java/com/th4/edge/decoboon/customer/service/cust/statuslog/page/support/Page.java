package com.th4.edge.decoboon.customer.service.cust.statuslog.page.support;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.decoboon.customer.repository.CustomerStatusLogRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 客户状态变更日志分页实现。 */
@Note("客户状态变更日志分页")
@Properties(allow = {
        @AllowProperties(model = CustomerStatusLog.class,
                properties = {"logId", "fromStatus", "toStatus", "changeType", "reasonCode", "reasonText",
                        "operator", "operatorRole", "changedAt", "customer"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/customer/status-log/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.cust.statuslog.page.Page {

    @AutoInject
    private Long customerId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @User
    private Employee employee;

    private final DataScopeContext dataScopeContext;
    private final CustomerContext customerContext;
    private final CustomerStatusLogRepository logRepository;

    public Page(DataScopeContext dataScopeContext,
                CustomerContext customerContext,
                CustomerStatusLogRepository logRepository) {
        this.dataScopeContext = dataScopeContext;
        this.customerContext = customerContext;
        this.logRepository = logRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CustomerStatusLog> call(CustomerStatusLog request) {
        if (customerId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "客户索引不能为空"));
        }
        Employee currentEmployee = dataScopeContext.requireEmployee(employee);
        customerContext.requireVisible(customerId, currentEmployee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return logRepository.findByCompany_ComIdAndCustomer_CustIdOrderByLogIdDesc(
                currentEmployee.getCompany().getComId(), customerId, PageRequest.of(page, pageSize));
    }
}
