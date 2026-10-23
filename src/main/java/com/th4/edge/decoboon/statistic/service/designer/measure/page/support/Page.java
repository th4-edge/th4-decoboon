package com.th4.edge.decoboon.statistic.service.designer.measure.page.support;

import com.th4.edge.decoboon.statistic.context.DesignerMeasureTaskContext;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureQuery;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureTask;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设计师待量房任务分页实现。 */
@Note("设计师待量房任务分页")
@Properties(allow = {
        @AllowProperties(model = DesignerMeasureTask.class, properties = {
                "taskKey", "customerId", "customerName", "telNo", "customerStatus", "customerStatusName",
                "propertyId", "propertyLabel", "community", "building", "roomNo", "address", "area", "layout",
                "propertyStatus", "propertyStatusName", "measureId", "measureStatus", "scheduledAt", "completedAt",
                "scheduleStateKey", "scheduleStateName", "stateKey", "stateLabel", "riskLevel", "riskReason",
                "mainActionKey", "mainActionLabel", "nextActionKey", "nextActionLabel",
                "secondaryActionKey", "secondaryActionLabel", "completionHint", "route", "allowed", "blockReason",
                "sitePhotoCount", "structureImageCount", "attachmentCount", "requirementCount", "designCount", "priority"
        })
})
@Service("statistic/designer/measure/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.statistic.service.designer.measure.page.Page {

    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String keyword;
    /** all / overdue / today / scheduled / notScheduled / handoff */
    @AutoInject
    private String state;

    private final TenantContext tenantContext;
    private final DesignerMeasureTaskContext taskContext;

    public Page(TenantContext tenantContext, DesignerMeasureTaskContext taskContext) {
        this.tenantContext = tenantContext;
        this.taskContext = taskContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DesignerMeasureTask> call(@User Employee employee) {
        Employee operator = tenantContext.requireInternalRole(employee, EmployeeRole.Designer);
        if (operator.getCompany() == null || operator.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        DesignerMeasureQuery request = new DesignerMeasureQuery();
        request.setCurrent(current);
        request.setSize(size);
        request.setKeyword(keyword);
        request.setState(state);
        return taskContext.page(operator, request);
    }
}
