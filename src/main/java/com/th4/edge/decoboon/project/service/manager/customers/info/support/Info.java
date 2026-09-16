package com.th4.edge.decoboon.project.service.manager.customers.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectManagerAccessContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 项目经理负责项目关联客户详情实现。 */
@Note("项目经理关联客户详情")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area",
                        "budget", "status", "owner", "remark", "wechatAdded", "wechatNo", "wechatAddedAt",
                        "visitCount", "lastVisitAt", "lastFollowAt", "nextFollowAt", "pendingMeasureAt",
                        "measureAt", "measureRequestedAt", "handedOffAt", "designer", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/manager/customer/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.manager.customers.info.Info {

    @AutoInject
    @NotNull
    private Long customerId;

    @User
    private Employee employee;

    private final ProjectManagerAccessContext accessContext;

    public Info(ProjectManagerAccessContext accessContext) {
        this.accessContext = accessContext;
    }

    @Override
    public Customer call(Customer request) {
        return accessContext.requireCustomer(customerId, employee);
    }
}
