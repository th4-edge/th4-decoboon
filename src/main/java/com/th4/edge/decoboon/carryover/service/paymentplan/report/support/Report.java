package com.th4.edge.decoboon.carryover.service.paymentplan.report.support;

import com.th4.edge.decoboon.carryover.context.CarryoverPaymentPlanContext;
import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlan;
import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlanReport;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 剩余收付款计划查询实现。 */
@Note("剩余收付款计划查询")
@Properties(allow = {
        @AllowProperties(model = CarryoverPaymentPlanReport.class,
                properties = {"itemId", "receivableOutstanding", "payableOutstanding",
                        "plannedReceivable", "plannedPayable", "unplannedReceivable", "unplannedPayable", "plans"}),
        @AllowProperties(model = CarryoverPaymentPlan.class,
                properties = {"planId", "direction", "amount", "plannedDate", "counterparty", "remark", "sort"}),
        @AllowProperties(model = CarryoverProject.class, properties = {"itemId"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/payment-plan/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.carryover.service.paymentplan.report.Report {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long itemId;

    private final CarryoverPaymentPlanContext context;

    public Report(CarryoverPaymentPlanContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = true)
    public CarryoverPaymentPlanReport call(CarryoverPaymentPlan request) {
        return context.report(employee, itemId);
    }
}
