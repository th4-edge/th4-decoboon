package com.th4.edge.decoboon.customer.service.cust.measure.support;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
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
import java.util.EnumSet;

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
    @AutoInject @NotNull private Long propertyId;

    @AutoInject private Long targetPropertyId;
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
        if (customer.getStatus() == CustStatus.New) {
            throw new DefinedException(new DefaultErrorCode("400", "请先完成首次跟进，再约定量房"));
        }
        if (!EnumSet.of(CustStatus.Following, CustStatus.PendingMeasure).contains(customer.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "当前客户阶段不能重新约定量房"));
        }
        Long resolvedPropertyId = targetPropertyId != null ? targetPropertyId : propertyId;
        Property property = propertyContext.resolve(resolvedPropertyId, customer, current);
        if (property.getLatestMeasureAt() != null
                || EnumSet.of(PropertyStatus.Measured, PropertyStatus.Designing, PropertyStatus.Quoted,
                PropertyStatus.Contracted, PropertyStatus.Serving, PropertyStatus.Completed).contains(property.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "该房屋已完成量房，不能重复预约"));
        }
        if (measureAt.isBefore(LocalDateTime.now())) {
            throw new DefinedException(new DefaultErrorCode("400", "约定量房时间不能早于当前时间"));
        }
        customer.setMeasureAt(measureAt);
        customer.setStatus(CustStatus.PendingMeasure);
        if (customer.getPendingMeasureAt() == null) customer.setPendingMeasureAt(LocalDateTime.now());
        customer.setMeasureRequestedAt(LocalDateTime.now());
        Customer saved = customerRepository.save(customer);

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
        // 量房设计师是设计责任事实：只接受已指派的设计师，未指派时保持为空，
        // 绝不把当前操作人（业务员/设计师）无条件写成 Measure.designer。
        scheduled.setDesigner(property.getDesigner() != null ? property.getDesigner() : saved.getDesigner());
        scheduled.setScheduledAt(measureAt);
        scheduled.setCompletedAt(null);
        scheduled.setMeasureTime(measureAt);
        scheduled.setStatus(MeasureStatus.Scheduled);
        measureRepository.save(scheduled);

        noticeContext.measureRequested(saved, current);
        return saved;
    }
}
