package com.th4.edge.decoboon.carryover.service.project.save.support;

import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverProjectContext;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("保存承接项目")
@Properties(allow = @AllowProperties(model = CarryoverProject.class,
        properties = {"itemId", "status", "version", "customerName", "customerPhone", "community", "address", "area",
                "contractNo", "signedAmount", "currentAmount", "signedAt", "startDate", "planEndDate",
                "projectName", "projectStatus", "currentStageName", "progress", "remainingWork", "currentIssue",
                "receivableOutstanding", "receivableDueDate", "receivableSourceNo", "receivableConfirmed",
                "payableOutstanding", "payableDueDate", "payablePayeeName", "payableSourceNo", "payableConfirmed",
                "costTotal", "costStatus", "costMissingNote", "legacyId", "legacyProjectNo", "legacyAccountNote", "createTime"}))
@Service("carryover/project/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.project.save.Save {

    @User
    private Employee employee;

    @AutoInject
    private Long itemId;

    @AutoInject
    private Long batchId;

    @AutoInject
    private Long ownerId;

    @AutoInject
    private Long managerId;

    @AutoInject
    private Long designerId;

    private final CarryoverAccessContext accessContext;
    private final CarryoverProjectContext projectContext;

    public Save(CarryoverAccessContext accessContext, CarryoverProjectContext projectContext) {
        this.accessContext = accessContext;
        this.projectContext = projectContext;
    }

    @Override
    @Transactional
    public CarryoverProject call(@ModelSaveInject CarryoverProject request) {
        Employee current = accessContext.requireBoss(employee);
        request.setItemId(itemId);
        if (batchId != null) {
            CarryoverBatch batch = new CarryoverBatch();
            batch.setBatchId(batchId);
            request.setBatch(batch);
        }
        request.setOwner(employeeRef(ownerId));
        request.setManager(employeeRef(managerId));
        request.setDesigner(employeeRef(designerId));
        return projectContext.save(current, request);
    }

    private Employee employeeRef(Long empId) {
        if (empId == null) return null;
        Employee employee = new Employee();
        employee.setEmpId(empId);
        return employee;
    }
}
