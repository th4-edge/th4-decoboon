package com.th4.edge.decoboon.payroll.service.commissions.save.support;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** 新增手工比例提成并按签约计提。 */
@Note("提成计提新增")
@Properties(allow = {
        @AllowProperties(model = CommissionItem.class,
                properties = {"commissionId", "employee", "project", "contractAmount", "manualRate", "commissionAmount",
                        "accruedAmount", "releasedAmount", "paidAmount", "status", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"})
})
@Service("payroll/commission/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.payroll.service.commissions.save.Save {
    @User private Employee employee;
    @AutoInject @NotNull private Long empId;
    @AutoInject @NotNull private Long projectId;
    @AutoInject @NotNull private BigDecimal contractAmount;
    @AutoInject @NotNull private BigDecimal manualRate;
    @AutoInject private BigDecimal commissionAmount;
    @AutoInject private String remark;
    private final CommissionItemRepository commissionRepository;
    private final PayableRepository payableRepository;
    private final PayrollAccessContext accessContext;
    public Save(CommissionItemRepository commissionRepository, PayableRepository payableRepository,
                PayrollAccessContext accessContext) {
        this.commissionRepository = commissionRepository;
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
    }
    @Override @Transactional
    public CommissionItem call(@ModelSaveInject CommissionItem request) {
        Employee current = accessContext.requireEditor(employee);
        Employee target = accessContext.resolveEmployee(current, empId);
        Project project = accessContext.resolveProject(current, projectId);
        BigDecimal amount = commissionAmount == null
                ? contractAmount.multiply(manualRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : commissionAmount.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) throw new com.th4.edge.framework.exception.DefinedException(
                new com.th4.edge.framework.exception.DefaultErrorCode("400", "提成金额必须大于 0"));
        accessContext.applyOwnership(request, current, target, project);
        request.setContractAmount(contractAmount.setScale(2, RoundingMode.HALF_UP));
        request.setManualRate(manualRate);
        request.setCommissionAmount(amount);
        request.setAccruedAmount(amount);
        request.setReleasedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        request.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        request.setStatus(CommissionStatus.Accrued);
        request.setRemark(remark);
        CommissionItem saved = commissionRepository.save(request);

        Payable payable = new Payable();
        payable.setBusinessOrigin(BusinessOrigin.New);
        payable.setCompany(request.getCompany());
        payable.setStore(request.getStore());
        payable.setTeam(request.getTeam());
        payable.setOwner(target);
        payable.setProject(project);
        payable.setProperty(project.getProperty());
        payable.setName(project.getName() + " 员工提成");
        payable.setType(PayableType.EmployeeCommission);
        payable.setPayeeEmployee(target);
        payable.setPayeeName(target.getName());
        payable.setAmount(amount);
        payable.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payable.setDueDate(LocalDate.now());
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(Boolean.FALSE);
        payable.setSourceType("CommissionItem");
        payable.setSourceId(saved.getCommissionId());
        payable.setRemark("签约计提");
        payable.setCreatedBy(current);
        payableRepository.save(payable);
        return saved;
    }
}
