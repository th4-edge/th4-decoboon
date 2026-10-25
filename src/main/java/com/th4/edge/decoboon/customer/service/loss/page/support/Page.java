package com.th4.edge.decoboon.customer.service.loss.page.support;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.decoboon.customer.repository.CustomerLossRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 客户流失历史分页 Provider。 */
@Note("客户流失历史")
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
@Service("customer/loss/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.loss.page.Page {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerContext customerContext;
    private final CustomerLossRepository lossRepository;

    public Page(CustomerContext customerContext, CustomerLossRepository lossRepository) {
        this.customerContext = customerContext;
        this.lossRepository = lossRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CustomerLoss> call(CustomerLoss request) {
        Customer customer = customerContext.requireVisible(customerId, employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return lossRepository.findByCompany_ComIdAndCustomer_CustIdOrderByLossIdDesc(
                customer.getCompany().getComId(), customer.getCustId(), PageRequest.of(page, pageSize));
    }
}
