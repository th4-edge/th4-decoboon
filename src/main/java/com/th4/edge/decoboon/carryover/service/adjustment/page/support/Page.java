package com.th4.edge.decoboon.carryover.service.adjustment.page.support;

import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.model.CarryoverAdjustment;
import com.th4.edge.decoboon.carryover.repository.CarryoverAdjustmentRepository;
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

@Note("承接调整记录")
@Properties(allow = {
        @AllowProperties(model = CarryoverAdjustment.class,
                properties = {"adjustmentId", "item", "type", "beforeAmount", "deltaAmount", "afterAmount",
                        "reason", "createdBy", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.carryover.model.CarryoverProject.class,
                properties = {"itemId", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/adjustment/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.carryover.service.adjustment.page.Page {

    @User
    private Employee employee;

    @AutoInject @NotNull private Long itemId;

    private final CarryoverAccessContext accessContext;
    private final CarryoverAdjustmentRepository adjustmentRepository;

    public Page(CarryoverAccessContext accessContext, CarryoverAdjustmentRepository adjustmentRepository) {
        this.accessContext = accessContext;
        this.adjustmentRepository = adjustmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarryoverAdjustment> call(CarryoverAdjustment request) {
        Employee current = accessContext.requireBoss(employee);
        accessContext.requireItem(current, itemId);
        return adjustmentRepository.findByItem_ItemIdOrderByAdjustmentIdDesc(itemId);
    }
}
