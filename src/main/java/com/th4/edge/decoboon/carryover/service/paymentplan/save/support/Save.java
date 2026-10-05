package com.th4.edge.decoboon.carryover.service.paymentplan.save.support;

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

import java.util.List;

/** 剩余收付款计划整批保存实现。 */
@Note("保存剩余收付款计划")
@Properties(allow = {
        @AllowProperties(model = CarryoverPaymentPlanReport.class,
                properties = {"itemId", "receivableOutstanding", "payableOutstanding",
                        "plannedReceivable", "plannedPayable", "unplannedReceivable", "unplannedPayable", "plans"}),
        @AllowProperties(model = CarryoverPaymentPlan.class,
                properties = {"planId", "direction", "amount", "plannedDate", "counterparty", "remark", "sort"}),
        @AllowProperties(model = CarryoverProject.class, properties = {"itemId"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/payment-plan/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.paymentplan.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long itemId;
    @AutoInject
    private List<CarryoverPaymentPlan> plans;

    private final CarryoverPaymentPlanContext context;

    public Save(CarryoverPaymentPlanContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public CarryoverPaymentPlanReport call(CarryoverPaymentPlan request) {
        return context.save(employee, itemId, plans);
    }
}
