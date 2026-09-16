package com.th4.edge.decoboon.project.service.manager.customerfollows.page.support;

import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** 项目经理关联客户跟进分页实现。 */
@Note("项目经理关联客户跟进分页")
@Properties(allow = {
        @AllowProperties(model = CustomerFollow.class,
                properties = {"followId", "customer", "emp", "way", "content", "nextFollowAt", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class,
                properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/manager/customer-follow/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.manager.customerfollows.page.Page {

    @AutoInject
    @NotNull
    private Long customerId;

    @User
    private Employee employee;

    private final ProjectManagerAccessContext accessContext;
    private final CustomerFollowRepository followRepository;

    public Page(ProjectManagerAccessContext accessContext,
                CustomerFollowRepository followRepository) {
        this.accessContext = accessContext;
        this.followRepository = followRepository;
    }

    @Override
    public org.springframework.data.domain.Page<CustomerFollow> call(CustomerFollow request) {
        accessContext.requireCustomer(customerId, employee);
        return followRepository.findByCustomer_CustIdOrderByCreateTime_DateTimeDesc(
                customerId, PageRequest.of(0, 100));
    }
}
