package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 设计转化域归属和权限上下文。
 *
 * <p>所有跨域读取均在同一查询中同时使用主键和数据范围，避免先 findById 再判断权限。</p>
 */
@Note("设计转化上下文")
@Component
public class DesignContext {

    /** 门店归属唯一写入解析器（`DEC-070`）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.common.context.StoreAttributionContext storeAttributionContext;


    private static final Set<EmployeeRole> DESIGN_WRITE_ROLES = EnumSet.of(
            EmployeeRole.Boss,
            EmployeeRole.StoreManager,
            EmployeeRole.TeamLeader,
            EmployeeRole.Designer);

    private final DataScopeContext dataScopeContext;
    private final CustomerRepository customerRepository;
    private final DesignRepository designRepository;
    private final EmployeeRepository employeeRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final PropertyContext propertyContext;

    public DesignContext(DataScopeContext dataScopeContext,
                         CustomerRepository customerRepository,
                         DesignRepository designRepository,
                         EmployeeRepository employeeRepository,
                         QuoteRepository quoteRepository,
                         ProjectRepository projectRepository,
                         PropertyContext propertyContext) {
        this.dataScopeContext = dataScopeContext;
        this.customerRepository = customerRepository;
        this.designRepository = designRepository;
        this.employeeRepository = employeeRepository;
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.propertyContext = propertyContext;
    }

    public Employee requireEmployee(Employee principal) {
        return dataScopeContext.requireEmployee(principal);
    }

    /** 设计新增、复制等写操作的角色边界；数据范围仍由后续对象查询校验。 */
    public Employee requireDesignWriteAccess(Employee principal) {
        Employee current = requireEmployee(principal);
        if (!DESIGN_WRITE_ROLES.contains(current.getRole())) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号无权新增或复制设计方案"));
        }
        return current;
    }

    public Customer requireCustomer(Long customerId, Employee principal) {
        DataScope scope = dataScopeContext.requireScope(principal);
        Specification<Customer> byId = (root, query, builder) ->
                builder.equal(root.get("custId"), customerId);
        Customer customer = customerRepository.findOne(
                        byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElse(null);
        if (customer == null && principal != null) {
            customer = customerRepository.findByCompany_ComIdAndDesigner_EmpId(
                            scope.comId(), principal.getEmpId())
                    .stream()
                    .filter(item -> item.getCustId().equals(customerId))
                    .findFirst()
                    .orElse(null);
        }
        if (customer == null) {
            throw new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问"));
        }
        return customer;
    }

    public Design requireDesign(Long designId, Employee principal) {
        DataScope scope = dataScopeContext.requireScope(principal);
        Specification<Design> byId = (root, query, builder) ->
                builder.equal(root.get("designId"), designId);
        return designRepository.findOne(byId.and(DataScopeSpecification.visible(Design.class, scope)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "设计方案不存在或无权访问")));
    }

    /** 业务员对本客户设计方案的只读范围；不用于任何写操作。 */
    public Specification<Design> visibleReadableDesign(Employee principal) {
        Employee current = requireEmployee(principal);
        if (current.getRole() != EmployeeRole.Marketer) {
            return DataScopeSpecification.visible(Design.class, dataScopeContext.resolve(current));
        }
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("company").get("comId"), comId),
                builder.or(
                        builder.equal(root.get("customer").get("owner").get("empId"), empId),
                        builder.equal(root.get("property").get("salesman").get("empId"), empId)));
    }

    /** 只读加载业务员本客户设计方案，避免扩大写权限。 */
    public Design requireReadableDesign(Long designId, Employee principal) {
        Employee current = requireEmployee(principal);
        if (current.getRole() != EmployeeRole.Marketer) {
            return requireDesign(designId, current);
        }
        Specification<Design> byId = (root, query, builder) ->
                builder.equal(root.get("designId"), designId);
        return designRepository.findOne(byId.and(visibleReadableDesign(current)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "设计方案不存在或无权访问")));
    }

    public Employee resolveOwner(Employee current, Long ownerId, Employee fallback) {
        if (current == null || current.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        Long comId = current.getCompany().getComId();
        Employee owner = fallback == null ? current : fallback;
        if (current.getRole() == EmployeeRole.Boss && ownerId != null) {
            owner = employeeRepository.findByEmpIdAndCompany_ComId(ownerId, comId);
            if (owner == null) {
                throw new DefinedException(new DefaultErrorCode("400", "负责人不属于当前企业"));
            }
        }
        if (owner.getCompany() == null || !comId.equals(owner.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "负责人不属于当前企业"));
        }
        return owner;
    }

    public void apply(Design design, Customer customer, Employee owner) {
        apply(design, customer, owner, propertyContext.ensureForCustomer(customer, owner));
    }

    public void apply(Design design, Customer customer, Employee owner, Property property) {
        requireSameCompany(customer == null ? null : customer.getCompany(), owner == null ? null : owner.getCompany());
        if (property == null || !property.getCustomer().getCustId().equals(customer.getCustId())) {
            throw new DefinedException(new DefaultErrorCode("400", "设计方案必须绑定客户所属房屋"));
        }
        design.setCompany(owner.getCompany());
        // DEC-070：设计门店由「负责人门店」改为「客户门店快照」（单一事实源 = 客户门店）
        design.setStore(storeAttributionContext.forCustomerSnapshot(customer, owner, owner.getStore()).store());
        design.setTeam(owner.getTeam());
        design.setOwner(owner);
        design.setCustomer(customer);
        design.setProperty(property);
    }

    public void apply(Requirement requirement, Customer customer, Employee owner) {
        apply(requirement, customer, owner, propertyContext.ensureForCustomer(customer, owner));
    }

    public void apply(Requirement requirement, Customer customer, Employee owner, Property property) {
        requireSameCompany(customer == null ? null : customer.getCompany(), owner == null ? null : owner.getCompany());
        requirement.setCompany(owner.getCompany());
        // DEC-070：需求单门店同步为客户门店快照
        requirement.setStore(storeAttributionContext.forCustomerSnapshot(customer, owner, owner.getStore()).store());
        requirement.setTeam(owner.getTeam());
        requirement.setOwner(owner);
        requirement.setCustomer(customer);
        requirement.setProperty(property);
    }

    public void apply(Space space, Design design) {
        space.setCompany(design.getCompany());
        space.setStore(design.getStore());
        space.setTeam(design.getTeam());
        space.setOwner(design.getOwner());
        space.setProperty(design.getProperty());
        space.setDesign(design);
    }

    public void apply(DesignItem item, Design design, Space space) {
        item.setCompany(design.getCompany());
        item.setStore(design.getStore());
        item.setTeam(design.getTeam());
        item.setOwner(design.getOwner());
        item.setProperty(design.getProperty());
        item.setDesign(design);
        item.setSpace(space);
    }

    public void apply(Talk talk, Design design) {
        talk.setCompany(design.getCompany());
        talk.setStore(design.getStore());
        talk.setTeam(design.getTeam());
        talk.setOwner(design.getOwner());
        talk.setCustomer(design.getCustomer());
        talk.setProperty(design.getProperty());
        talk.setDesign(design);
    }

    public void apply(TalkSection section, Talk talk) {
        section.setCompany(talk.getCompany());
        section.setStore(talk.getStore());
        section.setTeam(talk.getTeam());
        section.setOwner(talk.getOwner());
        section.setTalk(talk);
    }

    /** 客户一旦存在客户确认报价或项目，则该房屋的设计与报价进入只读状态。 */
    public void requireCustomerOpen(Customer customer) {
        if (customer == null || customer.getCustId() == null || customer.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "客户或房屋信息不完整"));
        }
        Property property = propertyContext.ensureForCustomer(customer, customer.getOwner());
        requirePropertyOpen(property);
    }

    public void requirePropertyOpen(Property property) {
        String blockedReason = propertyWriteBlockedReason(property);
        if (blockedReason != null) {
            throw new DefinedException(new DefaultErrorCode("400", blockedReason));
        }
    }

    /** 服务端唯一可编辑投影：Web/App 只消费该结论，不各自复制状态门禁。 */
    public boolean canEdit(Design design, Employee employee) {
        if (employee == null || employee.getRole() == null
                || !DESIGN_WRITE_ROLES.contains(employee.getRole())) {
            return false;
        }
        if (design == null || design.getStatus() == DesignStatus.Archived) {
            return false;
        }
        return propertyWriteBlockedReason(design.getProperty()) == null;
    }

    public void requireEditable(Design design) {
        if (design.getStatus() == DesignStatus.Archived) {
            throw new DefinedException(new DefaultErrorCode("400", "方案已归档，不能修改"));
        }
        if (design.getProperty() != null) requirePropertyOpen(design.getProperty());
        else requireCustomerOpen(design.getCustomer());
    }

    private String propertyWriteBlockedReason(Property property) {
        if (property == null || property.getPropertyId() == null) {
            return "装修业务必须绑定房屋";
        }
        Long propertyId = property.getPropertyId();
        if (property.getCompany() == null || property.getCompany().getComId() == null) {
            return "房屋所属企业不完整，暂不可确认编辑权限";
        }
        Long comId = property.getCompany().getComId();
        if (quoteRepository.existsByProperty_PropertyIdAndStatusIn(propertyId,
                java.util.List.of(QuoteStatus.Accepted, QuoteStatus.Converted))
                || projectRepository.existsByCompany_ComIdAndProperty_PropertyId(comId, propertyId)) {
            return "该房屋已有客户确认报价或唯一项目，设计方案已锁定；调整请发起合同变更单";
        }
        return null;
    }

    public void requireDesignOwner(Design design, Employee employee) {
        if (employee == null || design.getOwner() == null
                || !design.getOwner().getEmpId().equals(employee.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("404", "设计方案不存在或无权访问"));
        }
    }

    private void requireSameCompany(com.th4.edge.tenant.user.model.Company customerCompany,
                                    com.th4.edge.tenant.user.model.Company ownerCompany) {
        if (customerCompany == null || ownerCompany == null
                || !customerCompany.getComId().equals(ownerCompany.getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "客户、方案和设计师不属于同一企业"));
        }
    }
}
