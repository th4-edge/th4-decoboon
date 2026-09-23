package com.th4.edge.decoboon.risk.service.record.support;

import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskDispositionRequest;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;

import java.util.List;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** 处置记录实现：urgent=true 记为催办，否则记为处置说明。 */
@Note("风险处置记录")
@Properties(allow = {
        @AllowProperties(model = RiskItem.class,
                properties = {"riskId", "alertKey", "status", "owner", "dueAt", "roundNo", "recurrenceCount"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("risk/item/record")
@Scope(ScopeTypes.PROTOTYPE)
public class Record implements com.th4.edge.decoboon.risk.service.record.Record {

    @User
    private Employee employee;

    @AutoInject
    private Long riskId;

    @AutoInject
    private String note;

    @AutoInject
    private List<String> evidenceUrls;

    @AutoInject
    private Boolean urgent;


    private final RiskDispositionContext context;

    public Record(RiskDispositionContext context) { this.context = context; }

    @Override
    @Transactional
    public RiskItem call(RiskDispositionRequest request) {
        if (Boolean.TRUE.equals(urgent)) {
            return context.urge(employee, riskId, note);
        }
        return context.record(employee, riskId, note, request.getEvidenceUrls());
    }
}
