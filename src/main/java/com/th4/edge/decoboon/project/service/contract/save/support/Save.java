package com.th4.edge.decoboon.project.service.contract.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;

/** 从客户确认报价创建合同草稿或待签合同。 */
@Note("创建合同基线")
@Description("报价确认后先形成合同，不直接创建项目")
@Properties(allow = {
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "project", "quote", "customer", "property", "baselineAmount",
                        "signedAmount", "currentAmount", "status", "signedAt", "startDate", "planEndDate", "effectiveAt",
                        "paymentTerms", "createdBy", "remark", "receivableCount", "receivableAmount",
                        "receivedAmount", "pendingChangeCount", "signedCost", "costComplete", "paymentPlan",
                        "constructionPlan", "createTime"}),
        @AllowProperties(model = ContractPaymentPlan.class,
                properties = {"planId", "sourceCode", "sourceVersion", "name", "totalAmount", "status", "nodes"}),
        @AllowProperties(model = ContractPaymentNode.class,
                properties = {"nodeId", "code", "name", "ratio", "amount", "type", "triggerType", "triggerStageCode",
                        "triggerStageName", "offsetDays", "dueDate", "sort"}),
        @AllowProperties(model = ContractConstructionPlan.class,
                properties = {"planId", "sourceCode", "sourceVersion", "name", "startDate", "endDate", "status", "stages"}),
        @AllowProperties(model = ContractConstructionStage.class,
                properties = {"stageId", "code", "name", "sort", "defaultDays", "plannedStart", "plannedEnd",
                        "acceptanceMode", "description"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status", "property" }),
        @AllowProperties(model = Quote.class, properties = {"quoteId", "no", "title", "amount", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "address", "area", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/contract/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.contract.save.Save {

    @AutoInject @NotNull private Long quoteId;
    @AutoInject private LocalDate signedAt;
    @AutoInject private BigDecimal signedAmount;
    @AutoInject private LocalDate startDate;
    @AutoInject private LocalDate planEndDate;
    @AutoInject private String paymentTerms;
    @AutoInject private String paymentPlanCode;
    @AutoInject private String constructionPlanCode;
    @AutoInject private String remark;
    @User private Employee employee;

    private final ProjectAccessContext accessContext;
    private final ProjectContractContext contractContext;

    public Save(ProjectAccessContext accessContext, ProjectContractContext contractContext) {
        this.accessContext = accessContext;
        this.contractContext = contractContext;
    }

    @Override
    @Transactional
    public ProjectContract call(ProjectContract request) {
        Employee current = accessContext.requireEmployee(employee);
        // 现行能力矩阵只有 Boss 拥有 contract:write；Designer 不因页面可点而获得签约权。
        // 合同签署/生效的最终角色矩阵仍为 BLOCKED，确认前按最小权限失败关闭。
        Quote quote;
        try {
            quote = accessContext.requireQuote(quoteId, current);
        } catch (DefinedException notVisible) {
            // 同企业但超出数据范围：无 contract:write 的角色按能力语义返回 403（矩阵要求），
            // 跨企业或不存在仍保持 404，避免泄露对象存在性。
            if (current.getRole() != EmployeeRole.Boss && accessContext.sameCompanyQuote(quoteId, current)) {
                throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人可以处理合同签约"));
            }
            throw notVisible;
        }
        if (current.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人可以处理合同签约"));
        }
        if (quote.getStatus() != QuoteStatus.Accepted) {
            throw new DefinedException(new DefaultErrorCode("400", "只有客户已确认的报价可以生成合同"));
        }
        if (signedAt != null && (signedAmount == null || signedAmount.signum() <= 0)) {
            throw new DefinedException(new DefaultErrorCode("400", "签约时必须填写最终签约价格"));
        }
        ProjectContract contract = contractContext.createFromQuote(
                quote, current, signedAt, signedAmount, startDate, planEndDate, remark,
                paymentPlanCode, constructionPlanCode);
        if (paymentTerms != null) {
            contract.setPaymentTerms(paymentTerms);
        }
        return contractContext.enrich(contract);
    }
}
