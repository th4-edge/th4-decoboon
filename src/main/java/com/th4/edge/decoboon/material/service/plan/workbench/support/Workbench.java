package com.th4.edge.decoboon.material.service.plan.workbench.support;


import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.decoboon.material.context.MaterialPlanWorkbenchContext;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbench;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 材料计划工作台：按项目聚合待下单/在途/待到场材料，供财务逐条下单与 PM/设计师到场确认。
 */
@Note("材料计划工作台")
@Description("数据范围内按项目聚合材料计划：待下单、部分下单、待到货、逾期与下一步动作")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.decoboon.material.model.MaterialPlanWorkbench.class,
                properties = {"projectId", "projectNo", "projectName", "projectStatusName", "propertyAddress", "customerName", "designerName", "managerName", "totalItems", "readyToOrder", "partiallyOrdered", "pendingArrival", "arrived", "delayed", "items"}),
        @AllowProperties(model = com.th4.edge.decoboon.material.model.MaterialPlanWorkbenchItem.class,
                properties = {"planId", "projectId", "projectName", "propertyAddress", "sourceType", "sourceTypeName", "spaceName", "category", "materialName", "brand", "spec", "model", "unit", "netQuantity", "lossRate", "lossQuantity", "quantity", "orderedQuantity", "receivedQuantity", "requiredAt", "arrivedAt", "status", "statusName", "selectionId", "selectionStatus", "selectionStatusName", "purchaseId", "purchaseItemId", "purchaseNo", "supplierName", "delayed", "nextAction"})
})
@Service("material/plan/workbench")
@Scope(ScopeTypes.PROTOTYPE)
public class Workbench implements com.th4.edge.decoboon.material.service.plan.workbench.Workbench {

    @User private Employee employee;
    @AutoInject private Long projectId;
    @AutoInject private String group;

    private final ProjectAccessContext accessContext;
    private final MaterialPlanWorkbenchContext workbenchContext;

    public Workbench(ProjectAccessContext accessContext,
                     MaterialPlanWorkbenchContext workbenchContext) {
        this.accessContext = accessContext;
        this.workbenchContext = workbenchContext;
    }

    @Override
    public List<MaterialPlanWorkbench> call(MaterialPlanWorkbench request) {
        Employee current = accessContext.requireEmployee(employee);
        return workbenchContext.summarize(current, projectId, group);
    }
}
