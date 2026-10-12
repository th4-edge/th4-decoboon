package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.common.context.StoreAttributionContext.AttributedStore;
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

    /** 门店归属唯一写入解析器（`DEC-070`）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.common.context.StoreAttributionContext storeAttributionContext;


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

    /**
     * 合同 / 项目 / 采购单 / 任务 / 验收轮 / 材料扣减等**共用写入入口**（`DEC-070` §2）。
     *
     * <p>门店改为**客户门店快照**（单一事实源）：客户门店为空时按「负责人门店 → 单门店企业默认 → 阻断」
     * 解析（{@code StoreAttributionContext}）。team 与 owner 语义保持不变（本批只统一门店归属）。</p>
     */
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
        // DEC-070 不回溯：门店只在**首次写入**（创建时快照）确定；已有门店的对象（含历史对象在编辑路径被再次
        // 应用归属）保持原门店不变，组织变动（换负责人 / 换门店 / 门店停用）不回写历史。
        if (target.getStore() == null) {
            target.setStore(snapshotStore(customer, property));
        }
        target.setTeam(property == null ? null : property.getTeam());
        target.setOwner(owner);
    }

    /** 客户门店快照解析（统一解析器；不新增字段、不回溯历史对象）。 */
    private com.th4.edge.tenant.user.model.Store snapshotStore(Customer customer, Property property) {
        if (customer == null) {
            return property == null ? null : property.getStore();
        }
        AttributedStore attributed = storeAttributionContext.forCustomerSnapshot(customer, resolveOwner(null, customer),
                property == null ? null : property.getStore());
        return attributed.store();
    }
}
