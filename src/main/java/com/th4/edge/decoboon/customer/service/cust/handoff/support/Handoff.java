package com.th4.edge.decoboon.customer.service.cust.handoff.support;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
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
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    @AutoInject private Long propertyId;
    @AutoInject @NotNull private Long designerId;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final DataScopeContext dataScopeContext;
    private final NoticeContext noticeContext;
    private final PropertyContext propertyContext;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final DesignRepository designRepository;
    private final RequirementRepository requirementRepository;

    public Handoff(CustomerRepository customerRepository,
                   EmployeeRepository employeeRepository,
                   DataScopeContext dataScopeContext,
                   NoticeContext noticeContext,
                   PropertyContext propertyContext,
                   PropertyRepository propertyRepository,
                   MeasureRepository measureRepository,
                   DesignRepository designRepository,
                   RequirementRepository requirementRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.dataScopeContext = dataScopeContext;
        this.noticeContext = noticeContext;
        this.propertyContext = propertyContext;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.designRepository = designRepository;
        this.requirementRepository = requirementRepository;
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
            throw new DefinedException(new DefaultErrorCode("403", "仅团队负责人、店长或企业负责人可以指定设计师"));
        }
        // DEC-066（2026-10-12 用户裁决）：新客户（待首联）允许直接指定设计师——自然到店客户由店长/团队负责人
        // 当场派单，首次跟进由接手的设计师完成；状态不随派单推进。仅"已流失"客户必须先恢复再指派。
        if (customer.getStatus() == CustStatus.Lost) {
            throw new DefinedException(new DefaultErrorCode("400", "客户已流失，请先恢复客户后再指定设计师"));
        }
        if (current.getRole() == EmployeeRole.StoreManager
                && (current.getStore() == null || customer.getStore() == null
                || !current.getStore().getStoreId().equals(customer.getStore().getStoreId()))) {
            throw new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问"));
        }
        Employee designer = employeeRepository.findByEmpIdAndCompany_ComId(
                designerId, current.getCompany().getComId());
        if (designer == null || designer.getStatus() != EmployeeStatus.Active) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择本企业在职员工"));
        }
        if (current.getRole() == EmployeeRole.StoreManager
                && (designer.getStore() == null || current.getStore() == null
                || !designer.getStore().getStoreId().equals(current.getStore().getStoreId()))) {
            throw new DefinedException(new DefaultErrorCode("400", "仅可指定本门店在职员工"));
        }
        if (current.getRole() == EmployeeRole.TeamLeader
                && (designer.getTeam() == null || current.getTeam() == null
                || !designer.getTeam().getTeamId().equals(current.getTeam().getTeamId()))) {
            throw new DefinedException(new DefaultErrorCode("400", "仅可指定本团队在职员工"));
        }
        Employee previous = customer.getDesigner();
        if (propertyId != null) {
            propertyContext.resolve(propertyId, customer, current);
        }
        List<Property> properties = propertyRepository
                .findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(current.getCompany().getComId(), customer.getCustId());
        if (properties.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "客户尚未建立房屋档案，不能指定设计师"));
        }
        boolean changing = previous != null && !previous.getEmpId().equals(designer.getEmpId());
        customer.setDesigner(designer);
        customer.setHandedOffAt(LocalDateTime.now());
        Customer saved = customerRepository.save(customer);

        for (Property property : properties) {
            propertyContext.assignDesigner(property, designer);
            for (Measure measure : measureRepository.findByPropertyOrderByMeasureTimeDesc(property)) {
                if (measure.getDesigner() != null && !changing) continue;
                if (changing && measure.getStatus() != MeasureStatus.Scheduled) continue;
                measure.setDesigner(designer);
                measureRepository.save(measure);
            }
        }

        for (Design design : designRepository
                .findByCompany_ComIdAndCustomer_CustIdOrderByVersionDesc(current.getCompany().getComId(), customer.getCustId())) {
            if (design.getStatus() == com.th4.edge.decoboon.design.configure.DesignStatus.Archived) continue;
            design.setOwner(designer);
            design.setStore(designer.getStore());
            design.setTeam(designer.getTeam());
            designRepository.save(design);
        }
        for (Requirement requirement : requirementRepository
                .findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(current.getCompany().getComId(), customer.getCustId())) {
            requirement.setOwner(designer);
            requirement.setStore(designer.getStore());
            requirement.setTeam(designer.getTeam());
            requirementRepository.save(requirement);
        }

        if (!designer.getEmpId().equals(previous == null ? null : previous.getEmpId())) {
            noticeContext.designerSubmitted(saved, current, designer);
        }
        return saved;
    }
}
