package com.th4.edge.decoboon.carryover.service.adjustment.save.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverAdjustmentType;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverAdjustmentContext;
import com.th4.edge.decoboon.carryover.model.CarryoverAdjustment;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Note("保存承接调整")
@Properties(allow = {
        @AllowProperties(model = CarryoverAdjustment.class,
                properties = {"adjustmentId", "type", "beforeAmount", "deltaAmount", "afterAmount",
                        "reason", "createdBy", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.carryover.model.CarryoverProject.class,
                properties = {"itemId", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/adjustment/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.adjustment.save.Save {

    @User
    private Employee employee;

    @AutoInject @NotNull private Long itemId;
    @AutoInject @NotNull private CarryoverAdjustmentType type;
    @AutoInject @NotNull private BigDecimal afterAmount;
    @AutoInject @NotEmpty @Length(max = 500) private String reason;

    private final CarryoverAccessContext accessContext;
    private final CarryoverAdjustmentContext adjustmentContext;

    public Save(CarryoverAccessContext accessContext, CarryoverAdjustmentContext adjustmentContext) {
        this.accessContext = accessContext;
        this.adjustmentContext = adjustmentContext;
    }

    @Override
    @Transactional
    public CarryoverAdjustment call(CarryoverAdjustment request) {
        Employee current = accessContext.requireBoss(employee);
        return adjustmentContext.adjust(current, itemId, type, afterAmount, reason);
    }
}
