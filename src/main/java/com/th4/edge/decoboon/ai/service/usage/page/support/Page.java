package com.th4.edge.decoboon.ai.service.usage.page.support;

import com.th4.edge.decoboon.ai.context.AiUsageReportContext;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.model.AiUsageQuery;
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

/** AI 调用明细分页实现。P0 仅 BOSS 可读。 */
@Note("AI调用明细")
@Properties(allow = {
        @AllowProperties(model = AiUsageCall.class,
                properties = {"callId", "businessModule", "capabilityCode", "sceneCode", "businessType",
                        "businessId", "providerCode", "modelCode", "modelVersion", "capabilityVersion",
                        "rateCardVersion", "inputUnits", "outputUnits", "resourceUnits",
                        "shadowBillableUnits", "refundedUnits", "estimated", "status", "billingStatus",
                        "failureCode", "startedAt", "completedAt", "durationMs", "allocationType",
                        "allocationStore", "businessStore", "employeeStore", "employee"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("ai/usage/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.ai.service.usage.page.Page {

    @User
    private Employee employee;

    private final AiUsageReportContext reportContext;

    public Page(AiUsageReportContext reportContext) {
        this.reportContext = reportContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AiUsageCall> call(AiUsageQuery request) {
        return reportContext.page(employee, request);
    }
}
