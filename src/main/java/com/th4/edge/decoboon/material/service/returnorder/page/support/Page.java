package com.th4.edge.decoboon.material.service.returnorder.page.support;

import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.MaterialReturn;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.MaterialReturnRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/** 材料退货单分页：已到货扣减的冲减事实，供财务与验收查看。 */
@Note("材料退货单分页")
@Description("数据范围内材料退货单：金额冲减应付与已确认成本，DEC-MP-02 强制走退货单")
@Properties(allow = {
        @AllowProperties(model = MaterialReturn.class,
                properties = {"materialReturnId", "no", "quantity", "unit", "unitPrice", "amount",
                        "status", "returnedAt", "reason", "remark", "createTime",
                        "project", "materialPlan", "purchaseOrder", "supplier"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = MaterialPlan.class,
                properties = {"planId", "quantity", "unit", "status", "sourceType", "selectedBrand", "selectedSpec"}),
        @AllowProperties(model = PurchaseOrder.class, properties = {"purchaseId", "no", "supplierName", "status"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "name", "contactName", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/return/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.material.service.returnorder.page.Page {

    @User private Employee employee;
    @AutoInject private Long projectId;

    private final ProjectAccessContext accessContext;
    private final MaterialReturnRepository repository;

    public Page(ProjectAccessContext accessContext, MaterialReturnRepository repository) {
        this.accessContext = accessContext;
        this.repository = repository;
    }

    @Override
    public List<MaterialReturn> call(MaterialReturn request) {
        Employee current = accessContext.requireEmployee(employee);
        if (projectId != null) {
            Project project = accessContext.requireProject(projectId, current);
            return repository.findByProject_ProjectIdOrderByMaterialReturnIdDesc(project.getProjectId());
        }
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("400", "请指定项目后查询退货单"));
        }
        return repository.findByCompany_ComIdOrderByMaterialReturnIdDesc(current.getCompany().getComId());
    }
}
