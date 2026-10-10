package com.th4.edge.decoboon.customer.service.cust.update.support;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.configure.CustomerStatusChangeType;
import com.th4.edge.decoboon.customer.configure.Gender;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerStatusLogRepository;
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
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户编辑 Provider。
 *
 * <p>先按当前角色的数据范围加载客户，只编辑业务字段；客户状态由后续状态流转服务维护。
 * 企业负责人和店长可以明确转交客户，店长只能转交给本门店人员，转交时同步刷新 store/team 归属快照。</p>
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

    /** 前端候选负责人；仅企业负责人和店长可明确转交，其他角色沿用原负责人。 */
    @AutoInject
    private Long ownerId;

    /** 负责人实际发生变化时必须填写，并写入客户变更日志。 */
    @AutoInject
    @Length(max = 200)
    private String reason;

    private final CustomerRepository customerRepository;
    private final CustomerContext customerContext;
    private final DataScopeContext dataScopeContext;
    private final CustomerStatusLogRepository statusLogRepository;

    public Update(CustomerRepository customerRepository,
                  CustomerContext customerContext,
                  DataScopeContext dataScopeContext,
                  CustomerStatusLogRepository statusLogRepository) {
        this.customerRepository = customerRepository;
        this.customerContext = customerContext;
        this.dataScopeContext = dataScopeContext;
        this.statusLogRepository = statusLogRepository;
    }

    @Override
    @Transactional
    public Customer call(Customer request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        Customer customer = customerContext.requireVisible(customerId, current);

        if (name != null) {
            customer.setName(name);
        }
        if (telNo != null) {
            if (customer.getLastFollowAt() != null && !telNo.equals(customer.getTelNo())) {
                throw new DefinedException(new DefaultErrorCode("403", "客户已完成首联跟进，手机号不允许修改"));
            }
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
        Employee previousOwner = customer.getOwner();
        boolean ownerChanged = previousOwner == null || owner.getEmpId() == null
                || !owner.getEmpId().equals(previousOwner.getEmpId());
        String normalizedReason = reason == null ? null : reason.trim();
        if (ownerChanged && (normalizedReason == null || normalizedReason.isEmpty())) {
            throw new DefinedException(new DefaultErrorCode("400", "变更客户负责人必须填写原因"));
        }
        customerContext.applyOwnership(customer, owner);
        Customer saved = customerRepository.save(customer);
        if (ownerChanged) {
            CustomerStatusLog log = new CustomerStatusLog();
            log.setCompany(current.getCompany());
            log.setCustomer(saved);
            log.setFromStatus(saved.getStatus());
            log.setToStatus(saved.getStatus());
            log.setChangeType(CustomerStatusChangeType.OwnerChange);
            log.setReasonText(normalizedReason);
            log.setOperator(current);
            log.setOperatorRole(current.getRole());
            log.setChangedAt(LocalDateTime.now());
            statusLogRepository.save(log);
        }
        return saved;
    }
}
