package com.th4.edge.decoboon.risk.service.log.support;

import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
import com.th4.edge.decoboon.risk.model.RiskLogView;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 风险处置时间线实现。 */
@Note("风险处置时间线")
@Properties(allow = {
        @AllowProperties(model = RiskLogView.class,
                properties = {"logId", "action", "actionLabel", "fromStatusLabel", "toStatusLabel",
                        "operatorName", "note", "evidenceUrls", "occurredAt"})
})
@Service("risk/item/log/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Log implements com.th4.edge.decoboon.risk.service.log.Log {

    @User
    private Employee employee;

    @AutoInject
    private Long riskId;


    private final RiskDispositionContext context;

    public Log(RiskDispositionContext context) { this.context = context; }

    @Override
    @Transactional(readOnly = true)
    public List<RiskLogView> call(RiskDispositionRequest request) {
        return context.logs(employee, riskId);
    }
}
