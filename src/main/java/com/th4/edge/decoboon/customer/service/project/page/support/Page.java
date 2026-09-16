package com.th4.edge.decoboon.customer.service.project.page.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 仅返回当前微信身份显式授权的项目。 */
@Note("业主项目分页")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "area", "status", "progress",
                        "startDate", "planEndDate", "actualEndDate", "currentStageName", "property", "customer",
                        "designer", "manager", "support"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class,
                properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/project/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.project.page.Page {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerAccessContext accessContext;

    public Page(CustomerAccessContext accessContext) {
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Project> call(Project request) {
        Credentials credential = accessContext.requireCredential(principal);
        List<Project> projects = accessContext.activeAccesses(credential).stream()
                .map(access -> access.getProject())
                .distinct()
                .sorted(Comparator.comparing(Project::getProjectId).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, projects.size());
        int to = Math.min(from + pageSize, projects.size());
        return new PageImpl<>(projects.subList(from, to), PageRequest.of(page, pageSize), projects.size());
    }
}
