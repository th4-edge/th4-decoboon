package com.th4.edge.decoboon.statistic.service.designer.measure.list.support;

import com.th4.edge.decoboon.statistic.context.DesignerMeasureTaskContext;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureQuery;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureTask;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设计师待量房任务实现。 */
@Note("设计师待量房任务")
@Properties(allow = {
        @AllowProperties(model = DesignerMeasureTask.class, properties = {
                "taskKey", "customerId", "customerName", "telNo", "customerStatus", "customerStatusName",
                "propertyId", "propertyLabel", "community", "building", "roomNo", "address", "area", "layout",
                "propertyStatus", "propertyStatusName", "measureId", "measureStatus", "scheduledAt", "completedAt",
                "scheduleStateKey", "scheduleStateName", "stateKey", "stateLabel", "riskLevel", "riskReason", "mainActionKey", "mainActionLabel",
                "nextActionKey", "nextActionLabel", "secondaryActionKey", "secondaryActionLabel",
                "completionHint", "route", "allowed", "blockReason",
                "sitePhotoCount", "structureImageCount", "attachmentCount", "requirementCount", "designCount", "priority"
        })
})
@Service("statistic/designer/measure/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.statistic.service.designer.measure.list.List {

    @User
    private Employee employee;

    private final TenantContext tenantContext;
    private final DesignerMeasureTaskContext taskContext;

    public List(TenantContext tenantContext, DesignerMeasureTaskContext taskContext) {
        this.tenantContext = tenantContext;
        this.taskContext = taskContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DesignerMeasureTask> call(DesignerMeasureQuery request) {
        Employee current = tenantContext.requireInternalRole(employee, EmployeeRole.Designer); // DEC-032 / BR-028：BOSS 是内部岗位能力超集，企业内部能力入口按超集放行（ANS-BIZ-20261011-002）
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        return taskContext.list(current, request == null ? new DesignerMeasureQuery() : request);
    }
}
