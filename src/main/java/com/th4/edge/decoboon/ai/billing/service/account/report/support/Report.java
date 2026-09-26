package com.th4.edge.decoboon.ai.billing.service.account.report.support;

import com.th4.edge.decoboon.ai.billing.context.AiBillingReportContext;
import com.th4.edge.decoboon.ai.billing.model.AiAccountReport;
import com.th4.edge.decoboon.ai.billing.model.AiResourceLedger;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("AI资源账户摘要")
@Properties(allow = {
        @AllowProperties(model = AiAccountReport.class,
                properties = {"totalGranted", "availableUnits", "reservedUnits", "consumedUnits",
                        "refundedUnits", "adjustedUnits", "balanceVersion", "status",
                        "formalEnabled", "recentLedgers"}),
        @AllowProperties(model = AiResourceLedger.class,
                properties = {"ledgerId", "type", "changeUnits", "balanceBefore", "balanceAfter",
                        "rateCardVersion", "reason", "occurredAt", "allocationStore", "call"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = AiUsageCall.class, properties = {"callId", "capabilityCode", "businessModule"})
})
@Service("ai/account/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.ai.billing.service.account.report.Report {

    @User
    private Employee employee;

    private final AiBillingReportContext reportContext;

    public Report(AiBillingReportContext reportContext) {
        this.reportContext = reportContext;
    }

    @Override
    @Transactional(readOnly = true)
    public AiAccountReport call(Employee request) {
        return reportContext.report(employee);
    }
}
