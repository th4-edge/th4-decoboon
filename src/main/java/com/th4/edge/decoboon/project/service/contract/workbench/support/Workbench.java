package com.th4.edge.decoboon.project.service.contract.workbench.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.project.context.ContractWorkbenchContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业负责人「合同履约档案」聚合实现。
 *
 * <p>按 projectId 或 contractId 解析唯一合同，并在服务端完成数据范围校验；
 * 金额、回款、成本、进度、变更与选材全部来自真实领域事实。</p>
 */
@Note("合同履约档案聚合")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "project", "quote", "customer", "property", "baselineAmount",
                        "signedAmount", "currentAmount", "status", "signedAt", "startDate", "planEndDate",
                        "effectiveAt", "paymentTerms", "createdBy", "remark", "receivableCount",
                        "receivableAmount", "receivedAmount", "pendingChangeCount", "signedCost",
                        "costComplete", "paymentPlan", "constructionPlan", "createTime"}),
        @AllowProperties(model = ContractPaymentPlan.class,
                properties = {"planId", "sourceCode", "sourceVersion", "name", "totalAmount", "status", "nodes"}),
        @AllowProperties(model = ContractPaymentNode.class,
                properties = {"nodeId", "code", "name", "ratio", "amount", "type", "triggerType",
                        "triggerStageCode", "triggerStageName", "offsetDays", "dueDate", "sort"}),
        @AllowProperties(model = ContractConstructionPlan.class,
                properties = {"planId", "sourceCode", "sourceVersion", "name", "startDate", "endDate", "status", "stages"}),
        @AllowProperties(model = ContractConstructionStage.class,
                properties = {"stageId", "code", "name", "sort", "defaultDays", "plannedStart", "plannedEnd",
                        "acceptanceMode", "description"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "contractAmount", "status", "property",
                        "customer", "quote", "designer", "manager"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "amount", "status", "version"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address",
                        "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo", "addr"}),
        @AllowProperties(model = MaterialSelection.class,
                properties = {"selectionId", "version", "status", "title", "content", "confirmedAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract/workbench")
@Scope(ScopeTypes.PROTOTYPE)
public class Workbench implements com.th4.edge.decoboon.project.service.contract.workbench.Workbench {

    @AutoInject
    private Long projectId;

    @AutoInject
    private Long contractId;

    @User
    private Employee employee;

    private final ProjectContractRepository contractRepository;
    private final ProjectAccessContext accessContext;
    private final ContractWorkbenchContext workbenchContext;

    public Workbench(ProjectContractRepository contractRepository,
                     ProjectAccessContext accessContext,
                     ContractWorkbenchContext workbenchContext) {
        this.contractRepository = contractRepository;
        this.accessContext = accessContext;
        this.workbenchContext = workbenchContext;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractWorkbench call(ProjectContract request) {
        Employee current = accessContext.requireEmployee(employee);
        ProjectContract contract = resolve(current);
        return workbenchContext.build(contract);
    }

    private ProjectContract resolve(Employee current) {
        if (projectId != null) {
            Project project = accessContext.requireProject(projectId, current);
            return contractRepository.findByProject(project)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "当前项目尚未生成合同基线")));
        }
        if (contractId != null) {
            Specification<ProjectContract> byId =
                    (root, query, builder) -> builder.equal(root.get("contractId"), contractId);
            return contractRepository.findOne(byId.and(accessContext.visibleContract(employee)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同不存在或无权访问")));
        }
        throw new DefinedException(new DefaultErrorCode("400", "请选择项目或合同"));
    }
}
