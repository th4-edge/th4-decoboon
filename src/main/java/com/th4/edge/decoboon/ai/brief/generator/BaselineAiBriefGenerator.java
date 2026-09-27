package com.th4.edge.decoboon.ai.brief.generator;

import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 规则基线经营简报生成器。
 *
 * <p>无大模型时仍可验证事实、权限、页面和用量链路；只描述确定性数字，不推断人员责任和业务因果。</p>
 */
@Component
public class BaselineAiBriefGenerator implements AiBriefGenerator {

    @Override
    public String generate(AiBriefInput input) {
        BossDashboard dashboard = input.dashboard();
        FinanceSummary finance = input.finance();
        BusinessAlertReport alerts = input.alerts();
        String period = input.month() == null ? "本期" : input.month().toString().substring(0, 7);

        StringBuilder text = new StringBuilder(period).append("经营概况：");
        DashboardMetric signed = dashboard == null ? null : dashboard.getSignedAmount();
        if (signed == null || signed.getValue() == null) {
            text.append("签约额暂不可确认");
        } else {
            text.append("签约额").append(wan(signed.getValue())).append("万元");
            if (signed.getMonthOverMonthRate() != null) {
                text.append("，环比").append(rate(signed.getMonthOverMonthRate()));
            }
        }

        if (finance != null) {
            text.append("；应收未收").append(wan(finance.getOutstandingAmount())).append("万元");
            if (finance.getOverdueAmount() != null && finance.getOverdueAmount().signum() > 0) {
                text.append("，其中逾期").append(wan(finance.getOverdueAmount())).append("万元");
            }
        }

        if (alerts != null) {
            text.append("；当前接入预警").append(alerts.getTotalCount() == null ? 0 : alerts.getTotalCount()).append("项");
            if (alerts.getHighCount() != null && alerts.getHighCount() > 0) {
                text.append("，其中高风险").append(alerts.getHighCount()).append("项");
            }
        }
        text.append("。");
        return text.toString();
    }

    @Override
    public String version() {
        return "baseline-v1";
    }

    private String wan(BigDecimal value) {
        if (value == null) return "—";
        return value.divide(BigDecimal.valueOf(10000), 1, RoundingMode.HALF_UP).toPlainString();
    }

    private String rate(BigDecimal value) {
        if (value == null) return "—";
        String sign = value.signum() > 0 ? "增长" : value.signum() < 0 ? "下降" : "持平";
        return sign + value.abs().setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
