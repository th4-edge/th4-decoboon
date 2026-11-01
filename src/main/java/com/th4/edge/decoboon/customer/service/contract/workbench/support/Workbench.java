package com.th4.edge.decoboon.customer.service.contract.workbench.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.context.CustomerContractContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ContractWorkbenchContext;
import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业主合同履约档案只读实现。
 *
 * <p>复用员工侧纯读取聚合的事实计算，但使用业主身份授权，并通过显式响应白名单
 * 排除成本、利润、报价和其他内部经营字段。</p>
 */
@Note("业主合同履约档案")
@Properties(allow = {
        @AllowProperties(model = ContractWorkbench.class,
                properties = {"contract", "lineCount", "lineAmount", "receivable", "paymentNodes", "progress", "change", "selection"}),
        @AllowProperties(model = ContractWorkbench.Receivable.class,
                properties = {"receivableTotal", "receivedTotal", "outstandingTotal", "overdueTotal",
                        "overdueCount", "totalCount"}),
        @AllowProperties(model = ContractWorkbench.PaymentNode.class,
                properties = {"nodeId", "code", "name", "ratio", "amount", "receivedAmount",
                        "outstandingAmount", "triggerType", "triggerStageName", "dueDate", "status",
                        "statusName", "receivableId"}),
        @AllowProperties(model = ContractWorkbench.Progress.class,
                properties = {"projectId", "projectNo", "projectName", "status", "statusName", "percent",
                        "currentStageName", "stageTotal", "stageDone", "pendingAcceptanceCount",
                        "startDate", "planEndDate"}),
        @AllowProperties(model = ContractWorkbench.Change.class,
                properties = {"totalCount", "pendingCount", "approvedCount", "confirmedCount",
                        "closedCount", "confirmedAmount"}),
        @AllowProperties(model = ContractWorkbench.Selection.class,
                properties = {"selectionId", "version", "status", "statusName", "title", "summary",
                        "designerName", "confirmerName", "confirmedAt", "totalCount"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount",
                        "status", "signedAt", "startDate", "planEndDate", "effectiveAt", "paymentTerms",
                        "project", "customer", "property"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "status", "progress", "startDate", "planEndDate",
                        "currentStageName", "property", "customer", "designer", "manager"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address",
                        "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/contract/workbench")
@Scope(ScopeTypes.PROTOTYPE)
public class Workbench implements com.th4.edge.decoboon.customer.service.contract.workbench.Workbench {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    private Long projectId;

    @AutoInject
    private Long contractId;

    private final CustomerAccessContext accessContext;
    private final CustomerContractContext contractContext;
    private final ContractWorkbenchContext workbenchContext;

    public Workbench(CustomerAccessContext accessContext,
                     CustomerContractContext contractContext,
                     ContractWorkbenchContext workbenchContext) {
        this.accessContext = accessContext;
        this.contractContext = contractContext;
        this.workbenchContext = workbenchContext;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractWorkbench call(ProjectContract request) {
        Credentials credential = accessContext.requireCredential(principal);
        ProjectContract contract = contractContext.requireContract(credential, projectId, contractId);
        ContractWorkbench view = workbenchContext.build(contract, false);
        // 即使后续响应白名单被误改，也不把成本对象带到业主响应中。
        view.setCost(null);
        return view;
    }
}
