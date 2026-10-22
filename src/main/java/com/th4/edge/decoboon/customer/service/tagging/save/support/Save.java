package com.th4.edge.decoboon.customer.service.tagging.save.support;

import com.th4.edge.decoboon.customer.configure.TagScope;
import com.th4.edge.decoboon.customer.configure.TagStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.decoboon.customer.model.CustomerTagLink;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerTagLinkRepository;
import com.th4.edge.decoboon.customer.repository.CustomerTagRepository;
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

/** 打人工标签实现。 */
@Note("给客户打人工标签")
@Properties(allow = {
        @AllowProperties(model = CustomerTagLink.class, properties = {"linkId", "customer", "tag", "createdBy", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = CustomerTag.class, properties = {"tagId", "name", "category", "scope", "color", "status", "sort"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer-tag-link/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.tagging.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    @NotNull
    private Long tagId;

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final CustomerRepository customerRepository;
    private final CustomerTagRepository tagRepository;
    private final CustomerTagLinkRepository linkRepository;

    public Save(TenantContext tenantContext,
                DataScopeContext dataScopeContext,
                CustomerRepository customerRepository,
                CustomerTagRepository tagRepository,
                CustomerTagLinkRepository linkRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.customerRepository = customerRepository;
        this.tagRepository = tagRepository;
        this.linkRepository = linkRepository;
    }

    @Override
    @Transactional
    public CustomerTagLink call(CustomerTagLink request) {
        Employee current = tenantContext.requireEmployee(employee);
        requireMarketingRole(current);
        Long comId = current.getCompany().getComId();
        Customer customer = resolveCustomer(current, customerId);
        CustomerTag tag = tagRepository.findByCompany_ComIdAndTagId(comId, tagId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "标签不存在或无权访问")));
        if (tag.getScope() == TagScope.System) {
            throw new DefinedException(new DefaultErrorCode("403", "系统标签由系统自动装配，无需人工绑定"));
        }
        if (tag.getStatus() != TagStatus.Enabled) {
            throw new DefinedException(new DefaultErrorCode("409", "标签已停用，无法绑定"));
        }
        return linkRepository.findByCompany_ComIdAndCustomer_CustIdAndTag_TagId(comId, customer.getCustId(), tag.getTagId())
                .orElseGet(() -> {
                    CustomerTagLink link = new CustomerTagLink();
                    link.setCompany(current.getCompany());
                    link.setCustomer(customer);
                    link.setTag(tag);
                    link.setCreatedBy(current);
                    return linkRepository.save(link);
                });
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

    /** 允许打标签 / 维护画像的角色：企业负责人、店长、财务、团队负责人、业务员、设计师。 */
    private void requireMarketingRole(Employee current) {
        EmployeeRole role = current.getRole();
        boolean allowed = role == EmployeeRole.Boss || role == EmployeeRole.StoreManager || role == EmployeeRole.Finance
                || role == EmployeeRole.TeamLeader || role == EmployeeRole.Marketer
                || role == EmployeeRole.Designer;
        if (!allowed) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色无权维护客户标签或画像"));
        }
    }
}
