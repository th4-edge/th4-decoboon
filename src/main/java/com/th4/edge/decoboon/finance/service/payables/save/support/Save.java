package com.th4.edge.decoboon.finance.service.payables.save.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 应付单创建实现。 */
@Note("应付单创建")
@Properties(allow = {
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "materialPlan", "name", "type", "payeeEmployee", "payeePartner", "payeeName",
                        "period", "amount", "paidAmount", "dueDate", "status", "urgent", "sourceNo", "remark",
                        "createdBy", "createTime", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = MaterialPlan.class,
                properties = {"materialPlanId", "name", "quantity", "unit", "cost", "status", "requiredAt", "property" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
        ,@AllowProperties(model = Partner.class, properties = {"partnerId", "type", "name", "telNo"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class, properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"})
})
@Service("finance/payable/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.payables.save.Save {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private Long materialPlanId;
    @AutoInject
    @NotEmpty
    private String name;
    @AutoInject
    private PayableType type;
    @AutoInject
    private Long payeeEmployeeId;
    @AutoInject
    private Long payeePartnerId;
    @AutoInject
    private String payeeName;
    @AutoInject
    private String period;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    @NotNull
    private LocalDate dueDate;
    @AutoInject
    private Boolean urgent;
    @AutoInject
    private String sourceNo;
    @AutoInject
    private String remark;

    private final PayableRepository payableRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final EmployeeRepository employeeRepository;
    private final PartnerRepository partnerRepository;
    private final FinanceAccessContext accessContext;

    public Save(PayableRepository payableRepository,
                MaterialPlanRepository materialPlanRepository,
                EmployeeRepository employeeRepository,
                PartnerRepository partnerRepository,
                FinanceAccessContext accessContext) {
        this.payableRepository = payableRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.employeeRepository = employeeRepository;
        this.partnerRepository = partnerRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Payable call(Payable request) {
        Employee current = accessContext.requireEditor(employee);
        BigDecimal total = accessContext.value(amount);
        if (total.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "应付金额必须大于 0"));
        }
        Project project = projectId == null ? null : accessContext.resolveProject(current, projectId);
        PayableType payableType = type == null ? PayableType.Other : type;
        Employee payee = null;
        if (payeeEmployeeId != null) {
            payee = employeeRepository.findByEmpIdAndCompany_ComId(payeeEmployeeId, current.getCompany().getComId());
            if (payee == null) {
                throw new DefinedException(new DefaultErrorCode("400", "收款人员工不属于当前企业"));
            }
        }
        if (payableType == PayableType.Salary && payee == null
                && (payeeName == null || payeeName.isBlank())) {
            throw new DefinedException(new DefaultErrorCode("400", "工人工资必须填写收款工人"));
        }
        MaterialPlan materialPlan = null;
        if (materialPlanId != null) {
            materialPlan = materialPlanRepository.findById(materialPlanId)
                    .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在")));
            if (project != null && !materialPlan.getProject().getProjectId().equals(project.getProjectId())) {
                throw new DefinedException(new DefaultErrorCode("400", "材料计划不属于当前项目"));
            }
        }
        Partner payeePartner = null;
        if (payeePartnerId != null) {
            payeePartner = partnerRepository.findById(payeePartnerId)
                    .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "收款合作方不存在")));
        }

        request.setProject(project);
        request.setProperty(project == null ? null : project.getProperty());
        request.setMaterialPlan(materialPlan);
        request.setName(name.trim());
        request.setType(payableType);
        request.setPayeeEmployee(payee);
        request.setPayeePartner(payeePartner);
        request.setPayeeName(payee != null ? payee.getName()
                : payeePartner != null ? payeePartner.getName() : payeeName);
        request.setPeriod(period);
        request.setAmount(total);
        request.setPaidAmount(BigDecimal.ZERO);
        request.setDueDate(dueDate);
        request.setUrgent(Boolean.TRUE.equals(urgent));
        request.setSourceNo(sourceNo);
        request.setRemark(remark);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        accessContext.refreshPayableStatus(request);
        return payableRepository.save(request);
    }
}
