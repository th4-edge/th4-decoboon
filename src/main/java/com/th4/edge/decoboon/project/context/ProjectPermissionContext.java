package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectOwned;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

/**
 * 项目域经营归属计算。
 *
 * <p>经营 owner 始终以房屋负责人为第一来源，客户负责人仅在房屋负责人缺失时回退。
 * 子对象只复制该快照，项目经理、设计师和财务（含履约协同）通过项目参与关系另外授权。</p>
 */
@Note("项目域经营归属")
@Component
public class ProjectPermissionContext {

    public Employee resolveOwner(Property property, Customer customer) {
        if (property != null && property.getOwner() != null) {
            return property.getOwner();
        }
        return customer == null ? null : customer.getOwner();
    }

    public void apply(Project project) {
        if (project == null) {
            return;
        }
        apply(project, project.getProperty(), project.getCustomer());
    }

    public void apply(ProjectOwned target, Project project) {
        if (target == null || project == null) {
            return;
        }
        apply(target, project.getProperty(), project.getCustomer());
        target.setCompany(project.getCompany() == null ? target.getCompany() : project.getCompany());
    }

    public void apply(ProjectOwned target, Property property, Customer customer) {
        if (target == null) {
            return;
        }
        Company company = property == null ? null : property.getCompany();
        if (company == null && customer != null) {
            company = customer.getCompany();
        }
        Employee owner = resolveOwner(property, customer);
        target.setCompany(company);
        target.setStore(property == null ? null : property.getStore());
        target.setTeam(property == null ? null : property.getTeam());
        target.setOwner(owner);
    }
}
