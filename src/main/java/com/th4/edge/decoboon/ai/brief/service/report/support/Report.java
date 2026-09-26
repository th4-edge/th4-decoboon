package com.th4.edge.decoboon.ai.brief.service.report.support;

import com.th4.edge.decoboon.ai.brief.context.AiBossBriefContext;
import com.th4.edge.decoboon.ai.brief.model.AiBriefQuery;
import com.th4.edge.decoboon.ai.brief.model.AiBriefReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI 经营参谋 P0 实现。仅 BOSS 可生成和查看。 */
@Note("AI经营简报")
@Properties(allow = {
        @AllowProperties(model = AiBriefReport.class,
                properties = {"month", "asOf", "summary", "facts", "actions", "costStatus", "usage", "generatorVersion"}),
        @AllowProperties(model = AiBriefReport.AiBriefFact.class,
                properties = {"category", "label", "value", "note", "tone"}),
        @AllowProperties(model = AiBriefReport.AiBriefAction.class,
                properties = {"level", "title", "reason", "ownerName", "dueText", "targetRoute", "businessType", "businessId"}),
        @AllowProperties(model = AiBriefReport.AiBriefCostStatus.class,
                properties = {"complete", "incompleteProjectCount", "message"}),
        @AllowProperties(model = AiBriefReport.AiBriefUsage.class,
                properties = {"callId", "resourceUnits", "shadowBillableUnits", "rateCardVersion"})
})
@Service("ai/brief/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.ai.brief.service.report.Report {

    @User
    private Employee employee;

    private final AiBossBriefContext briefContext;

    public Report(AiBossBriefContext briefContext) {
        this.briefContext = briefContext;
    }

    @Override
    @Transactional
    public AiBriefReport call(AiBriefQuery request) {
        return briefContext.report(employee, request);
    }
}
