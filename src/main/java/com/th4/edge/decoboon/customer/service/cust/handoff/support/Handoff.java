package com.th4.edge.decoboon.customer.service.cust.handoff.support;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.context.PropertyContext;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Note("指定设计师")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "addr", "community", "status",
                        "owner", "designer", "measureAt", "measureRequestedAt", "pendingMeasureAt", "handedOffAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer/handoff")
@Scope(ScopeTypes.PROTOTYPE)
public class Handoff implements com.th4.edge.decoboon.customer.service.cust.handoff.Handoff {
    @User private Employee employee;
    @AutoInject @NotNull private Long customerId;
    @AutoInject @NotNull private Long propertyId;
    @AutoInject @NotNull private Long designerId;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final DataScopeContext dataScopeContext;
    private final NoticeContext noticeContext;
    private final PropertyContext propertyContext;
    private final MeasureRepository measureRepository;

    public Handoff(CustomerRepository customerRepository,
                   EmployeeRepository employeeRepository,
                   DataScopeContext dataScopeContext,
                   NoticeContext noticeContext,
                   PropertyContext propertyContext,
                   MeasureRepository measureRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
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
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager
                && current.getRole() != EmployeeRole.TeamLeader) {
            throw new DefinedException(new DefaultErrorCode("403", "仅团队负责人、门店店长或企业负责人可以指定设计师"));
        }
        if (customer.getDesigner() != null) {
            throw new DefinedException(new DefaultErrorCode("400", "客户已指定设计师，不能重复指定"));
        }
        if (customer.getStatus() == CustStatus.New || customer.getStatus() == CustStatus.Lost) {
            throw new DefinedException(new DefaultErrorCode("400", "客户尚未跟进或已流失，不能指定设计师"));
        }
        if (current.getRole() == EmployeeRole.StoreManager
                && (current.getStore() == null || customer.getStore() == null
                || !current.getStore().getStoreId().equals(customer.getStore().getStoreId()))) {
            throw new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问"));
        }
        Employee designer = employeeRepository.findByEmpIdAndCompany_ComId(
                designerId, current.getCompany().getComId());
        if (designer == null || (designer.getRole() != EmployeeRole.Designer && designer.getRole() != EmployeeRole.Boss)) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择本企业设计师或老板本人"));
        }
        if (current.getRole() == EmployeeRole.StoreManager
                && (designer.getStore() == null || current.getStore() == null
                || !designer.getStore().getStoreId().equals(current.getStore().getStoreId()))) {
            throw new DefinedException(new DefaultErrorCode("400", "仅可指定本门店设计师"));
        }
        if (current.getRole() == EmployeeRole.TeamLeader
                && (designer.getTeam() == null || current.getTeam() == null
                || !designer.getTeam().getTeamId().equals(current.getTeam().getTeamId()))) {
            throw new DefinedException(new DefaultErrorCode("400", "仅可指定本团队设计师"));
        }
        var property = propertyContext.resolve(propertyId, customer, current);
        customer.setDesigner(designer);
        customer.setHandedOffAt(LocalDateTime.now());
        Customer saved = customerRepository.save(customer);
        propertyContext.assignDesigner(property, designer);
        // 量房设计师只在此处由管理者显式指定：补齐预约中以及历史遗留的未指派量房记录。
        measureRepository.findByPropertyOrderByMeasureTimeDesc(property).stream()
                .filter(item -> item.getDesigner() == null)
                .forEach(item -> {
                    item.setDesigner(designer);
                    measureRepository.save(item);
                });
        noticeContext.designerSubmitted(saved, current, designer);
        return saved;
    }
}
