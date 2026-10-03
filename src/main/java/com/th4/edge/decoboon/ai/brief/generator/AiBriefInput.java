package com.th4.edge.decoboon.ai.brief.generator;

import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;

import java.time.LocalDate;

/** 经营简报生成器输入。只包含已有经营事实，不包含客户正文和附件。 */
public record AiBriefInput(
        LocalDate month,
        BossDashboard dashboard,
        FinanceSummary finance,
        BusinessAlertReport alerts
) {
}
