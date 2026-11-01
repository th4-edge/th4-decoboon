package com.th4.edge.decoboon.finance.service.settlements.update.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 项目结算状态更新实现。 */
@Note("项目结算状态更新")
@Properties(allow = {
        @AllowProperties(model = Settlement.class,
                properties = {"settlementId", "project", "contractAmount", "changeAmount", "deductionAmount",
                        "rewardAmount", "finalAmount", "retentionRate", "retentionAmount", "retentionReleaseAt",
                        "retentionReleasedAt", "receivableAmount", "receivedAmount",
                        "payableAmount", "paidAmount", "materialAmount", "salaryAmount", "projectAmount", "expenseAmount", "totalCost",
                        "estimatedCost", "actualCost", "manualIncomeAmount", "manualCostAmount", "profitAmount", "costComplete", "costIncompleteCount", "grossMargin", "status", "confirmedBy", "confirmedAt", "closedAt", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/settlement/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.settlements.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long settlementId;
    @AutoInject
    @NotNull
    private SettlementStatus status;
    @AutoInject
    private String remark;

    private final SettlementRepository settlementRepository;
    private final ProjectRepository projectRepository;
    private final FinanceAccessContext accessContext;
    private final FinanceReportContext reportContext;

    public Update(SettlementRepository settlementRepository,
                  ProjectRepository projectRepository,
                  FinanceAccessContext accessContext,
                  FinanceReportContext reportContext) {
        this.settlementRepository = settlementRepository;
        this.projectRepository = projectRepository;
        this.accessContext = accessContext;
        this.reportContext = reportContext;
    }

    @Override
    @Transactional
    public Settlement call(Settlement request) {
        Employee current = accessContext.requireEditor(employee);
        Specification<Settlement> byId = (root, query, builder) ->
                builder.equal(root.get("settlementId"), settlementId);
        Settlement settlement = settlementRepository.findOne(
                        byId.and(accessContext.visible(current, Settlement.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "结算单不存在或无权访问")));
        Project project = settlement.getProject();
        if (status == SettlementStatus.Confirmed || status == SettlementStatus.Closed) {
            reportContext.snapshot(current, project, settlement, current);
        }
        if (status == SettlementStatus.Confirmed && settlement.getStatus() == SettlementStatus.Draft) {
            if (project.getStatus() != ProjectStatus.Accepting
                    && project.getStatus() != ProjectStatus.Finished) {
                throw new DefinedException(new DefaultErrorCode("400", "项目验收阶段或完工后才能确认结算"));
            }
            if (!Boolean.TRUE.equals(settlement.getCostComplete())) {
                throw new DefinedException(new DefaultErrorCode("400", "项目最终成本未完整，不能确认结算"));
            }
            settlement.setStatus(SettlementStatus.Confirmed);
            settlement.setConfirmedBy(current);
            settlement.setConfirmedAt(LocalDate.now());
        } else if (status == SettlementStatus.Closed) {
            if (!reportContext.receivableCleared(project.getProjectId(), current.getCompany().getComId())) {
                throw new DefinedException(new DefaultErrorCode("400", "应收未全部收清，不能关闭结算"));
            }
            if (!reportContext.payableCleared(project.getProjectId(), current.getCompany().getComId())) {
                throw new DefinedException(new DefaultErrorCode("400", "应付未全部付清，不能关闭结算"));
            }
            if (project.getStatus() != ProjectStatus.Finished
                    && project.getStatus() != ProjectStatus.Accepting) {
                throw new DefinedException(new DefaultErrorCode("400", "项目完工或验收完成后才能关闭结算"));
            }
            settlement.setStatus(settlement.getRetentionAmount() != null
                    && settlement.getRetentionAmount().signum() > 0
                    ? SettlementStatus.RetentionPending : SettlementStatus.Closed);
            settlement.setClosedAt(LocalDate.now());
            project.setStatus(ProjectStatus.Settled);
            if (project.getActualEndDate() == null) project.setActualEndDate(LocalDate.now());
            projectRepository.save(project);
        } else {
            throw new DefinedException(new DefaultErrorCode("400", "不支持的结算状态流转"));
        }
        if (remark != null) settlement.setRemark(remark);
        return settlementRepository.save(settlement);
    }
}
