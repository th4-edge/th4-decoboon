package com.th4.edge.decoboon.customer.service.portrait.save.support;

import com.th4.edge.decoboon.customer.configure.PortraitCompetitor;
import com.th4.edge.decoboon.customer.configure.PortraitConcern;
import com.th4.edge.decoboon.customer.configure.PortraitDecisionRole;
import com.th4.edge.decoboon.customer.configure.PortraitEco;
import com.th4.edge.decoboon.customer.configure.PortraitHouseCondition;
import com.th4.edge.decoboon.customer.configure.PortraitHousehold;
import com.th4.edge.decoboon.customer.configure.PortraitPayment;
import com.th4.edge.decoboon.customer.configure.PortraitPurpose;
import com.th4.edge.decoboon.customer.configure.PortraitStartWindow;
import com.th4.edge.decoboon.customer.configure.PortraitStyle;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 维护客户画像实现（按客户 upsert，全部字段可空）。 */
@Note("维护客户画像")
@Properties(allow = {
        @AllowProperties(model = CustomerPortrait.class, properties = {
                "portraitId", "customer", "decisionRole", "purpose", "houseCondition",
                "startWindow", "style", "eco", "household", "payment", "concerns", "competitor", "updatedBy"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer-portrait/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.portrait.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    private PortraitDecisionRole decisionRole;
    @AutoInject
    private PortraitPurpose purpose;
    @AutoInject
    private PortraitHouseCondition houseCondition;
    @AutoInject
    private PortraitStartWindow startWindow;
    @AutoInject
    private PortraitStyle style;
    @AutoInject
    private PortraitEco eco;
    @AutoInject
    private PortraitHousehold household;
    @AutoInject
    private PortraitPayment payment;
    @AutoInject
    private List<PortraitConcern> concerns;
    @AutoInject
    private PortraitCompetitor competitor;

    /** 需要显式清空的字段名（与前端表单字段同名），用于把已选值恢复为未填写。 */
    @AutoInject
    private List<String> clearFields;

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final CustomerRepository customerRepository;
    private final CustomerPortraitRepository portraitRepository;

    public Save(TenantContext tenantContext,
                DataScopeContext dataScopeContext,
                CustomerRepository customerRepository,
                CustomerPortraitRepository portraitRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.customerRepository = customerRepository;
        this.portraitRepository = portraitRepository;
    }

    @Override
    @Transactional
    public CustomerPortrait call(CustomerPortrait request) {
        Employee current = tenantContext.requireEmployee(employee);
        requireMarketingRole(current);
        Long comId = current.getCompany().getComId();
        Customer customer = resolveCustomer(current, customerId);
        CustomerPortrait portrait = portraitRepository
                .findByCompany_ComIdAndCustomer_CustId(comId, customer.getCustId())
                .orElseGet(() -> {
                    CustomerPortrait created = new CustomerPortrait();
                    created.setCompany(current.getCompany());
                    created.setCustomer(customer);
                    return created;
                });
        if (decisionRole != null) portrait.setDecisionRole(decisionRole);
        if (purpose != null) portrait.setPurpose(purpose);
        if (houseCondition != null) portrait.setHouseCondition(houseCondition);
        if (startWindow != null) portrait.setStartWindow(startWindow);
        if (style != null) portrait.setStyle(style);
        if (eco != null) portrait.setEco(eco);
        if (household != null) portrait.setHousehold(household);
        if (payment != null) portrait.setPayment(payment);
        if (concerns != null) portrait.setConcerns(new ArrayList<>(concerns));
        if (competitor != null) portrait.setCompetitor(competitor);
        if (clearFields != null) {
            for (String field : clearFields) {
                if (field == null) continue;
                switch (field) {
                    case "decisionRole" -> portrait.setDecisionRole(null);
                    case "purpose" -> portrait.setPurpose(null);
                    case "houseCondition" -> portrait.setHouseCondition(null);
                    case "startWindow" -> portrait.setStartWindow(null);
                    case "style" -> portrait.setStyle(null);
                    case "eco" -> portrait.setEco(null);
                    case "household" -> portrait.setHousehold(null);
                    case "payment" -> portrait.setPayment(null);
                    case "competitor" -> portrait.setCompetitor(null);
                    case "concerns" -> portrait.setConcerns(new ArrayList<>());
                    default -> { }
                }
            }
        }
        portrait.setUpdatedBy(current);
        return portraitRepository.save(portrait);
    }

    /** 在数据范围内加载客户；设计师可额外访问指派给自己的客户。 */
    private Customer resolveCustomer(Employee current, Long customerId) {
        Long comId = current.getCompany().getComId();
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Customer> byId = (root, query, builder) -> builder.equal(root.get("custId"), customerId);
        Customer customer = customerRepository.findOne(byId.and(DataScopeSpecification.visible(Customer.class, scope))).orElse(null);
        if (customer == null && current.getRole() == EmployeeRole.Designer) {
            customer = customerRepository.findByCompany_ComIdAndDesigner_EmpId(comId, current.getEmpId()).stream()
                    .filter(item -> customerId.equals(item.getCustId()))
                    .findFirst()
                    .orElse(null);
        }
        if (customer == null) {
            throw new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问"));
        }
        return customer;
    }

    /** 允许维护画像的角色：企业负责人、店长、团队负责人、业务员、设计师。 */
    private void requireMarketingRole(Employee current) {
        EmployeeRole role = current.getRole();
        boolean allowed = role == EmployeeRole.Boss || role == EmployeeRole.StoreManager
                || role == EmployeeRole.TeamLeader || role == EmployeeRole.Marketer
                || role == EmployeeRole.Designer;
        if (!allowed) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色无权维护客户标签或画像"));
        }
    }
}
