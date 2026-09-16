package com.th4.edge.decoboon.customer.service.cust.measure.support;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Note("约定量房")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "addr", "community", "status",
                        "owner", "designer", "measureAt", "measureRequestedAt", "pendingMeasureAt", "handedOffAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer/measure")
@Scope(ScopeTypes.PROTOTYPE)
public class Measure implements com.th4.edge.decoboon.customer.service.cust.measure.Measure {
    @User private Employee employee;
    @AutoInject @NotNull private Long customerId;
    @AutoInject private Long propertyId;
    @AutoInject @NotNull private LocalDateTime measureAt;
    private final CustomerRepository customerRepository;
    private final DataScopeContext dataScopeContext;
    private final NoticeContext noticeContext;
    private final PropertyContext propertyContext;
    private final MeasureRepository measureRepository;

    public Measure(CustomerRepository customerRepository,
                   DataScopeContext dataScopeContext,
                   NoticeContext noticeContext,
                   PropertyContext propertyContext,
                   MeasureRepository measureRepository) {
        this.customerRepository = customerRepository;
        this.dataScopeContext = dataScopeContext;
        this.noticeContext = noticeContext;
        this.propertyContext = propertyContext;
        this.measureRepository = measureRepository;
    }

    @Override
    @Transactional
    public Customer call(Customer request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        var scope = dataScopeContext.resolve(current);
        Specification<Customer> byId = (root, query, builder) ->
                builder.equal(root.get("custId"), customerId);
        Customer customer = customerRepository.findOne(
                        byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
        if (customer.getAddr() == null || customer.getAddr().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请先补充房屋地址再约定量房"));
        }
        if (customer.getStatus() == CustStatus.Lost
                || customer.getStatus() == CustStatus.Signed
                || customer.getStatus() == CustStatus.Serving
                || customer.getStatus() == CustStatus.Finished) {
            throw new DefinedException(new DefaultErrorCode("400", "当前客户阶段不能重新约定量房"));
        }
        if (measureAt.isBefore(LocalDateTime.now())) {
            throw new DefinedException(new DefaultErrorCode("400", "约定量房时间不能早于当前时间"));
        }
        customer.setMeasureAt(measureAt);
        customer.setStatus(CustStatus.PendingMeasure);
        if (customer.getPendingMeasureAt() == null) customer.setPendingMeasureAt(LocalDateTime.now());
        customer.setMeasureRequestedAt(LocalDateTime.now());
        Customer saved = customerRepository.save(customer);
        Property property = propertyId == null
                ? propertyContext.ensureForCustomer(saved, current)
                : propertyContext.resolve(propertyId, saved, current);

        com.th4.edge.decoboon.property.model.Measure scheduled = measureRepository
                .findByPropertyOrderByMeasureTimeDesc(property).stream()
                .filter(item -> item.getStatus() == MeasureStatus.Scheduled)
                .findFirst()
                .orElseGet(com.th4.edge.decoboon.property.model.Measure::new);
        scheduled.setCompany(property.getCompany());
        scheduled.setStore(property.getStore());
        scheduled.setTeam(property.getTeam());
        scheduled.setOwner(property.getOwner() == null ? current : property.getOwner());
        scheduled.setCustomer(saved);
        scheduled.setProperty(property);
        scheduled.setDesigner(saved.getDesigner() == null ? current : saved.getDesigner());
        scheduled.setMeasureTime(measureAt);
        scheduled.setStatus(MeasureStatus.Scheduled);
        measureRepository.save(scheduled);

        noticeContext.measureRequested(saved, current);
        return saved;
    }
}
