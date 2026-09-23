package com.th4.edge.decoboon.statistic.service.risk.report.support;

import com.th4.edge.decoboon.statistic.context.RiskCenterContext;
import com.th4.edge.decoboon.statistic.model.RiskCenterQuery;
import com.th4.edge.decoboon.statistic.model.RiskCenterReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 风险中心读侧报表实现。 */
@Note("风险中心报表")
@Properties(allow = {
        @AllowProperties(model = RiskCenterReport.class,
                properties = {"asOf", "totalCount", "highCount", "mediumCount", "lowCount",
                        "typeDistribution", "responsibilityDistribution", "items"}),
        @AllowProperties(model = RiskCenterReport.RiskTypeDistribution.class,
                properties = {"key", "label", "description", "count", "highCount"}),
        @AllowProperties(model = RiskCenterReport.RiskResponsibility.class,
                properties = {"name", "count", "highCount"}),
        @AllowProperties(model = RiskCenterReport.RiskItemView.class,
                properties = {"key", "type", "typeLabel", "level", "levelLabel", "title", "message",
                        "projectName", "customerName", "ownerName", "amount", "dueDate",
                        "timeText", "timeTone", "nextAction",
                        "riskId", "status", "statusLabel", "disposerName", "dueAt", "dueText", "dueTone",
                        "roundNo", "recurrenceCount"})
})
@Service("statistic/risk/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.risk.report.Report {

    @User
    private Employee employee;

    private final RiskCenterContext riskCenterContext;

    public Report(RiskCenterContext riskCenterContext) {
        this.riskCenterContext = riskCenterContext;
    }

    /**
     * 注意：本方法不是只读事务——读取清单时会把实时预警与处置单对齐
     * （新预警建单、级别升级重开、同级复现累计、条件消失自动消解）。
     */
    @Override
    @Transactional
    public RiskCenterReport call(RiskCenterQuery request) {
        return riskCenterContext.report(employee, request);
    }
}
