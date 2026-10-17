package com.th4.edge.decoboon.blueprint.service.check.report.support;

import com.th4.edge.decoboon.material.context.MaterialHealthContext;
import com.th4.edge.decoboon.material.model.MaterialHealthIssue;
import com.th4.edge.decoboon.material.model.MaterialHealthQuery;
import com.th4.edge.decoboon.material.model.MaterialHealthReport;
import com.th4.edge.decoboon.material.model.MaterialHealthRow;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 项目材料业务体检报告实现。 */
@Note("项目材料业务体检报告")
@Properties(allow = {
        @AllowProperties(model = MaterialHealthReport.class,
                properties = {"projectId", "projectNo", "projectName", "projectStatusName", "propertyAddress",
                        "customerName", "designerName", "managerName", "hasMaterialPlan", "totalItems",
                        "selectedItems", "unselectedItems", "readyToOrder", "partiallyOrdered", "pendingArrival",
                        "partiallyArrived", "arrived", "delayed", "purchaseCount", "unpricedPurchaseCount",
                        "missingSupplierCount", "costComplete", "costIncompleteCount", "materialCost",
                        "estimatedCost", "actualCost", "profitAmount", "costLabel", "stageKey", "stageLabel",
                        "stageSummary", "nextAction", "settlementStatus", "issues", "rows"}),
        @AllowProperties(model = MaterialHealthIssue.class,
                properties = {"id", "label", "severity", "count", "nextAction", "evidence", "refs"}),
        @AllowProperties(model = MaterialHealthRow.class,
                properties = {"planId", "spaceName", "category", "materialName", "status", "statusName",
                        "selectionStatus", "selectionStatusName", "quantity", "orderedQuantity", "receivedQuantity",
                        "requiredAt", "supplierName", "purchaseNo", "delayed", "nextAction"})
})
@Service("blueprint/check/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.blueprint.service.check.report.Report {

    @User
    private Employee employee;

    @AutoInject
    private Long projectId;

    private final MaterialHealthContext healthContext;

    public Report(MaterialHealthContext healthContext) {
        this.healthContext = healthContext;
    }

    @Override
    public MaterialHealthReport call(MaterialHealthQuery request) {
        if (projectId == null) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("400", "项目索引不能为空"));
        }
        return healthContext.report(employee, projectId);
    }
}
