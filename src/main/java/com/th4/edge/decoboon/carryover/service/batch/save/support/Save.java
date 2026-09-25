package com.th4.edge.decoboon.carryover.service.batch.save.support;

import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverBatchContext;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
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

import java.time.LocalDate;

@Note("保存承接任务")
@Properties(allow = @AllowProperties(model = CarryoverBatch.class,
        properties = {"batchId", "asOfDate", "status", "scopeConfirmedAt", "completedAt", "version", "createTime"}))
@Service("carryover/batch/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.batch.save.Save {

    @User
    private Employee employee;

    @AutoInject
    private Long batchId;

    @AutoInject
    private LocalDate asOfDate;

    private final CarryoverAccessContext accessContext;
    private final CarryoverBatchContext batchContext;

    public Save(CarryoverAccessContext accessContext, CarryoverBatchContext batchContext) {
        this.accessContext = accessContext;
        this.batchContext = batchContext;
    }

    @Override
    @Transactional
    public CarryoverBatch call(@ModelSaveInject CarryoverBatch request) {
        Employee current = accessContext.requireBoss(employee);
        request.setBatchId(batchId);
        request.setAsOfDate(asOfDate);
        return batchContext.save(current, request);
    }
}
