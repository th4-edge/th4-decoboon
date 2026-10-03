package com.th4.edge.decoboon.customer.service.cust.measure.support;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.context.CustomerContext;
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
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
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
    private final CustomerContext customerContext;
    private final NoticeContext noticeContext;
    private final PropertyContext propertyContext;
    private final MeasureRepository measureRepository;

    public Measure(CustomerRepository customerRepository,
                   DataScopeContext dataScopeContext,
                   CustomerContext customerContext,
                   NoticeContext noticeContext,
                   PropertyContext propertyContext,
                   MeasureRepository measureRepository) {
        this.customerRepository = customerRepository;
        this.dataScopeContext = dataScopeContext;
        this.customerContext = customerContext;
        this.noticeContext = noticeContext;
        this.propertyContext = propertyContext;
        this.measureRepository = measureRepository;
    }

    @Override
    @Transactional
    public Customer call(Customer request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        Customer customer = customerContext.requireVisible(customerId, current);
        if (customer.getStatus() == CustStatus.New) {
            throw new DefinedException(new DefaultErrorCode("400", "请先完成首次跟进，再约定量房"));
        }
        if (!EnumSet.of(CustStatus.Following, CustStatus.PendingMeasure).contains(customer.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "当前客户阶段不能重新约定量房"));
        }
        Long resolvedPropertyId = targetPropertyId != null ? targetPropertyId : propertyId;
        Property property = propertyContext.resolve(resolvedPropertyId, customer, current);
        Employee responsibleDesigner = resolveResponsibleDesigner(property, customer, current);
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
        if (customer.getDesigner() == null && responsibleDesigner != null) {
            customer.setDesigner(responsibleDesigner);
        }
        if (customer.getPendingMeasureAt() == null) customer.setPendingMeasureAt(LocalDateTime.now());
        customer.setMeasureRequestedAt(LocalDateTime.now());
        Customer saved = customerRepository.save(customer);
        if (property.getDesigner() == null && responsibleDesigner != null) {
            property.setDesigner(responsibleDesigner);
        }

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

    /** 设计师录入的客户默认由本人承接设计，无需再次指派。 */
    private Employee resolveResponsibleDesigner(Property property, Customer customer, Employee current) {
        if (property.getDesigner() != null) return property.getDesigner();
        if (customer.getDesigner() != null) return customer.getDesigner();
        if (current.getRole() != EmployeeRole.Designer) return null;
        if (sameEmployee(property.getOwner(), current)
                || sameEmployee(property.getSalesman(), current)
                || sameEmployee(customer.getOwner(), current)) {
            return current;
        }
        return null;
    }

    private boolean sameEmployee(Employee left, Employee right) {
        return left != null && right != null && left.getEmpId() != null
                && left.getEmpId().equals(right.getEmpId());
    }
}
