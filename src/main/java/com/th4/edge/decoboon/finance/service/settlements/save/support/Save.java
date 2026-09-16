package com.th4.edge.decoboon.finance.service.settlements.save.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 项目结算生成与重算实现。 */
@Note("项目结算生成")
@Properties(allow = {
        @AllowProperties(model = Settlement.class,
                properties = {"settlementId", "project", "contractAmount", "changeAmount", "deductionAmount",
                        "rewardAmount", "finalAmount", "retentionRate", "retentionAmount", "retentionReleaseAt",
                        "retentionReleasedAt", "receivableAmount", "receivedAmount",
                        "payableAmount", "paidAmount", "materialAmount", "salaryAmount", "projectAmount", "expenseAmount", "totalCost",
                        "profitAmount", "grossMargin", "status", "confirmedBy", "confirmedAt", "closedAt", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/settlement/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.settlements.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long projectId;
    @AutoInject
    private String remark;
    @AutoInject
    private BigDecimal deductionAmount;
    @AutoInject
    private BigDecimal rewardAmount;
    @AutoInject
    private BigDecimal retentionRate;
    @AutoInject
    private java.time.LocalDate retentionReleaseAt;

    private final SettlementRepository settlementRepository;
    private final FinanceAccessContext accessContext;
    private final FinanceReportContext reportContext;
    private final ProjectContractRepository contractRepository;

    public Save(SettlementRepository settlementRepository,
                FinanceAccessContext accessContext,
                FinanceReportContext reportContext,
                ProjectContractRepository contractRepository) {
        this.settlementRepository = settlementRepository;
        this.accessContext = accessContext;
        this.reportContext = reportContext;
        this.contractRepository = contractRepository;
    }

    @Override
    @Transactional
    public Settlement call(Settlement request) {
        Employee current = accessContext.requireEditor(employee);
        Project project = accessContext.resolveProject(current, projectId);
        Settlement settlement = settlementRepository.findByProject_ProjectIdAndCompany_ComId(
                        projectId, current.getCompany().getComId())
                .orElseGet(Settlement::new);
        if (settlement.getStatus() == SettlementStatus.Closed) {
            throw new DefinedException(new DefaultErrorCode("400", "已关闭结算不能重算"));
        }
        settlement.setStatus(SettlementStatus.Draft);
        if (remark != null) settlement.setRemark(remark);
        Settlement snapshot = reportContext.snapshot(current, project, settlement, current);
        BigDecimal deduction = value(deductionAmount);
        BigDecimal reward = value(rewardAmount);
        BigDecimal rate = retentionRate == null ? BigDecimal.valueOf(3) : retentionRate;
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "质保金比例必须在 0% 到 5% 之间"));
        }
        if (rate.signum() > 0 && rate.compareTo(BigDecimal.valueOf(3)) < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "质保金比例不能低于 3%"));
        }
        ProjectContract contract = contractRepository.findByProject(project).orElse(null);
        BigDecimal changeAmount = contract == null ? BigDecimal.ZERO
                : value(contract.getCurrentAmount()).subtract(value(contract.getBaselineAmount()));
        BigDecimal finalAmount = value(snapshot.getContractAmount()).add(reward).subtract(deduction);
        BigDecimal retentionAmount = finalAmount.multiply(rate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        snapshot.setChangeAmount(changeAmount);
        snapshot.setDeductionAmount(deduction);
        snapshot.setRewardAmount(reward);
        snapshot.setFinalAmount(finalAmount);
        snapshot.setRetentionRate(rate);
        snapshot.setRetentionAmount(retentionAmount);
        snapshot.setRetentionReleaseAt(retentionReleaseAt);
        return settlementRepository.save(snapshot);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
