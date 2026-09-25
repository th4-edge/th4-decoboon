package com.th4.edge.decoboon.carryover.service.batch.info.support;

import com.th4.edge.decoboon.carryover.context.CarryoverBatchContext;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("承接任务详情")
@Properties(allow = @AllowProperties(model = CarryoverBatch.class,
        properties = {"batchId", "asOfDate", "status", "scopeConfirmedAt", "completedAt", "version", "createTime"}))
@Service("carryover/batch/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.carryover.service.batch.info.Info {

    @User
    private Employee employee;

    private final CarryoverAccessContext accessContext;
    private final CarryoverBatchContext batchContext;

    public Info(CarryoverAccessContext accessContext, CarryoverBatchContext batchContext) {
        this.accessContext = accessContext;
        this.batchContext = batchContext;
    }

    @Override
    @Transactional(readOnly = true)
    public CarryoverBatch call(CarryoverBatch request) {
        return batchContext.info(accessContext.requireEditor(employee));
    }
}
