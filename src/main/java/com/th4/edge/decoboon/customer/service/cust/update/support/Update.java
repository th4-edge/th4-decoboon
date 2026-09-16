package com.th4.edge.decoboon.customer.service.cust.update.support;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.configure.Gender;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 客户编辑 Provider。
 *
 * <p>先按当前角色的数据范围加载客户，只编辑业务字段；客户状态由后续状态流转服务维护。
 * 非 BOSS 不能转交客户，BOSS 转交时同步刷新 store/team 归属快照。</p>
 */
@Note("客户编辑")
@Description("按当前角色数据范围编辑客户基础信息和负责人归属")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area",
                        "budget", "status", "owner", "remark", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount", "lastVisitAt",
                        "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt", "measureRequestedAt", "handedOffAt", "designer", "createTime"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name"})
})
@Service("customer/customer/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.customer.service.cust.update.Update {

    /** 框架注入：当前登录员工。 */
    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    @Length(min = 1, max = 32)
    private String name;

    @AutoInject
    @Length(min = 1, max = 20)
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
    @Length(max = 512)
    private String remark;

    /** 前端候选负责人；仅 BOSS 生效，非 BOSS 转交请求会被服务端忽略。 */
    @AutoInject
    private Long ownerId;

    private final CustomerRepository customerRepository;
    private final CustomerContext customerContext;
    private final DataScopeContext dataScopeContext;

    public Update(CustomerRepository customerRepository,
                  CustomerContext customerContext,
                  DataScopeContext dataScopeContext) {
        this.customerRepository = customerRepository;
        this.customerContext = customerContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional
    public Customer call(Customer request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);

        Specification<Customer> byId = (root, query, builder) ->
                builder.equal(root.get("custId"), customerId);
        Customer customer = customerRepository.findOne(
                        byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "客户不存在或无权访问")));

        if (name != null) {
            customer.setName(name);
        }
        if (telNo != null) {
            customer.setTelNo(telNo);
        }
        if (community != null) {
            customer.setCommunity(community);
        }
        if (addr != null) {
            customer.setAddr(addr);
        }
        if (area != null) {
            customer.setArea(area);
        }
        if (budget != null) {
            customer.setBudget(budget);
        }
        if (gender != null) {
            customer.setGender(gender);
        }
        if (source != null) {
            customer.setSource(source);
        }
        if (remark != null) {
            customer.setRemark(remark);
        }
        Employee owner = customerContext.resolveOwner(current, ownerId, customer.getOwner());
        customerContext.applyOwnership(customer, owner);
        return customerRepository.save(customer);
    }
}
