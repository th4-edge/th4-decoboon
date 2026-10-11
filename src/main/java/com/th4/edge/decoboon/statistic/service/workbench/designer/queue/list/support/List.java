package com.th4.edge.decoboon.statistic.service.workbench.designer.queue.list.support;

import com.th4.edge.decoboon.statistic.context.DesignerWorkQueueContext;
import com.th4.edge.decoboon.statistic.model.WorkQueueItem;
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

/** 设计师今日主线队列实现。 */
@Note("设计师今日主线队列")
@Properties(allow = {
        @AllowProperties(model = WorkQueueItem.class, properties = {
                "queueKey", "objectType", "objectId", "customerId", "propertyId", "projectId",
                "title", "subtitle", "stateKey", "stateLabel", "ownerName", "nextActionKey",
                "nextActionLabel", "route", "priority", "dueAt", "stayDays", "riskLevel", "allowed", "blockReason"
        })
})
@Service("statistic/workbench/designer/queue/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.statistic.service.workbench.designer.queue.list.List {

    private final TenantContext tenantContext;
    private final DesignerWorkQueueContext queueContext;

    public List(TenantContext tenantContext, DesignerWorkQueueContext queueContext) {
        this.tenantContext = tenantContext;
        this.queueContext = queueContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<WorkQueueItem> call(@User Employee employee) {
        Employee current = tenantContext.requireInternalRole(employee, EmployeeRole.Designer); // DEC-032 / BR-028：BOSS 是内部岗位能力超集，企业内部能力入口按超集放行（ANS-BIZ-20261011-002）
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        return queueContext.build(current);
    }
}
