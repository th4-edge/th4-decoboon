package com.th4.edge.decoboon.risk.service.batchassign.support;

import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskBatchAssignRequest;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;

import java.util.List;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** 风险批量指派实现。 */
@Note("风险批量指派")
@Properties
@Service("risk/item/batch-assign")
@Scope(ScopeTypes.PROTOTYPE)
public class BatchAssign implements com.th4.edge.decoboon.risk.service.batchassign.BatchAssign {

    @User
    private Employee employee;

    @AutoInject
    private List<Long> riskIds;

    @AutoInject
    private Long assigneeId;

    @AutoInject
    private String note;


    private final RiskDispositionContext context;

    public BatchAssign(RiskDispositionContext context) { this.context = context; }

    @Override
    @Transactional
    public Integer call(RiskBatchAssignRequest request) {
        return context.batchAssign(employee, riskIds, assigneeId, note);
    }
}
