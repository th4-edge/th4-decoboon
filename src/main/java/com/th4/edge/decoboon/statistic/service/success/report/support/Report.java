package com.th4.edge.decoboon.statistic.service.success.report.support;

import com.th4.edge.decoboon.statistic.context.SuccessProgramContext;
import com.th4.edge.decoboon.statistic.model.RoleActivation;
import com.th4.edge.decoboon.statistic.model.SuccessMilestone;
import com.th4.edge.decoboon.statistic.model.SuccessProgramReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业负责人用户成功看板实现。 */
@Note("用户成功与流失风险")
@Properties(allow = {
        @AllowProperties(model = SuccessProgramReport.class,
                properties = {"companyName", "startDate", "daysSinceCreated", "activationStatus",
                        "activationScore", "roleActivationRate", "riskLevel", "nextAction",
                        "nextActionLabel", "nextActionPath", "riskReasons", "milestones",
                        "roleActivations", "refreshedAt"}),
        @AllowProperties(model = SuccessMilestone.class,
                properties = {"key", "label", "description", "targetDay", "status", "count",
                        "actionLabel", "actionPath"}),
        @AllowProperties(model = RoleActivation.class,
                properties = {"role", "label", "configuredCount", "activeCount", "activated", "evidence"})
})
@Service("statistic/success/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.success.report.Report {

    @User
    private Employee employee;

    private final SuccessProgramContext successProgramContext;

    public Report(SuccessProgramContext successProgramContext) {
        this.successProgramContext = successProgramContext;
    }

    @Override
    @Transactional(readOnly = true)
    public SuccessProgramReport call(Employee request) {
        return successProgramContext.report(employee);
    }
}
