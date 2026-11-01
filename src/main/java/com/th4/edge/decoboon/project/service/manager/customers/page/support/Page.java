package com.th4.edge.decoboon.project.service.manager.customers.page.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.context.ProjectManagerAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** 项目经理负责项目关联客户分页实现。 */
@Note("项目经理关联客户分页")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area",
                        "budget", "status", "owner", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount",
                        "lastVisitAt", "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt",
                        "measureRequestedAt", "handedOffAt", "designer", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/manager/customer/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.manager.customers.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private String keyword;

    @AutoInject
    private CustStatus status;

    private final ProjectManagerAccessContext accessContext;
    private final CustomerRepository customerRepository;

    public Page(ProjectManagerAccessContext accessContext, CustomerRepository customerRepository) {
        this.accessContext = accessContext;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Customer> call(Customer request) {
        Employee operator = accessContext.requireManager(employee);
        Long comId = operator.getCompany().getComId();
        Long managerId = operator.getEmpId();
        Specification<Customer> specification = (root, query, builder) -> {
            var predicate = builder.equal(root.get("company").get("comId"), comId);
            if (operator.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.Boss) {
                return predicate;
            }
            var subquery = query.subquery(Long.class);
            var project = subquery.from(Project.class);
            subquery.select(project.get("projectId"))
                    .where(builder.equal(project.get("customer"), root),
                            builder.equal(project.get("manager").get("empId"), managerId));
            return builder.and(predicate, builder.exists(subquery));
        };
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        if (!kw.isBlank()) {
            String pattern = "%" + kw.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("telNo")), pattern),
                    builder.like(builder.lower(root.get("community")), pattern),
                    builder.like(builder.lower(root.get("addr")), pattern)));
        }
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return customerRepository.findAll(
                specification, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "custId")));
    }
}
