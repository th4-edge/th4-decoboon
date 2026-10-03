package com.th4.edge.decoboon.customer.service.cust.save.support;

import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.configure.Gender;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 客户创建 Provider。
 *
 * <p>客户必须归属企业，负责人使用 Employee 对象关联，不保存裸 empId。</p>
 */
@Note("客户创建")
@Description("登记客户并绑定企业、负责人和意向房屋信息")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area",
                        "budget", "status", "owner", "remark", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount", "lastVisitAt",
                        "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt", "measureRequestedAt", "handedOffAt", "designer", "createTime"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name"})
})
@Service("customer/customer/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.cust.save.Save {

    /** 框架注入：当前登录员工。 */
    @User
    private Employee employee;

    // 以下字段用于框架调用前的声明式校验；模型本体由 @ModelSaveInject 按请求变量组装。
    @AutoInject
    @NotEmpty
    @Length(max = 32)
    private String name;

    @AutoInject
    @NotEmpty
    @Length(max = 20)
    private String telNo;

    @AutoInject
    @Length(max = 128)
    private String community;

    @AutoInject
    @Length(max = 256)
    private String addr;

    @AutoInject
    private BigDecimal area;

    @AutoInject
    private BigDecimal budget;

    @AutoInject
    private Gender gender;

    @AutoInject
    private CustomerSource source;

    @AutoInject
    private Long ownerId;

    private final CustomerRepository customerRepository;
    private final CustomerContext customerContext;
    private final TenantContext tenantContext;

    public Save(CustomerRepository customerRepository,
                CustomerContext customerContext,
                TenantContext tenantContext) {
        this.customerRepository = customerRepository;
        this.customerContext = customerContext;
        this.tenantContext = tenantContext;
    }

    @Override
    public Customer call(@ModelSaveInject Customer request) {
        Employee current = tenantContext.requireEmployee(employee);
        Employee owner = customerContext.resolveOwner(current, ownerId, current);

        customerContext.applyOwnership(request, owner);
        if (current.getRole() == EmployeeRole.Designer && sameEmployee(owner, current)) {
            // 设计师通过客户录入成为业务负责人时，量房与设计责任默认归本人，无需再次指派。
            request.setDesigner(current);
        }
        if (gender != null) request.setGender(gender);
        if (source != null) request.setSource(source);
        request.setWechatAdded(Boolean.FALSE);
        request.setWechatNo(null);
        request.setBudget(budget);
        request.setStatus(com.th4.edge.decoboon.customer.configure.CustStatus.New);
        request.setVisitCount(0);
        return customerRepository.save(request);
    }

    private boolean sameEmployee(Employee left, Employee right) {
        return left != null && right != null && left.getEmpId() != null
                && left.getEmpId().equals(right.getEmpId());
    }
}
