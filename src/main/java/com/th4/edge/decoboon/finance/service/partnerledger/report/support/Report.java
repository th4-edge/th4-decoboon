package com.th4.edge.decoboon.finance.service.partnerledger.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.PartnerLedgerAggregateContext;
import com.th4.edge.decoboon.finance.model.PartnerLedger;
import com.th4.edge.decoboon.finance.model.PartnerLedgerReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.supply.model.Partner;
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

/** 供应商应付台账。 */
@Note("供应商应付台账")
@Properties(allow = {
        @AllowProperties(model = PartnerLedgerReport.class,
                properties = {"rows", "partnerCount", "payableAmount", "paidAmount", "outstandingAmount", "overdueAmount", "asOf"}),
        @AllowProperties(model = PartnerLedger.class,
                properties = {"partner", "payableAmount", "paidAmount", "outstanding", "overdueAmount",
                        "pendingCount", "nearestDueDate"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "name", "type", "contactName", "telNo", "settlementTerms",
                        "settlementMode", "settlementDays", "cutoffDay", "status"})
})
@Service("finance/partner-ledger/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.partnerledger.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long partnerId;
    @AutoInject
    private String keyword;
    @AutoInject
    private Boolean overdueOnly;
    /** Web 汇总卡片只取标量；App 默认继续返回供应商行。 */
    @AutoInject
    private Boolean summaryOnly;
    @AutoInject
    private Long storeId;

    private final FinanceAccessContext accessContext;
    private final PartnerLedgerAggregateContext aggregateContext;

    public Report(FinanceAccessContext accessContext, PartnerLedgerAggregateContext aggregateContext) {
        this.accessContext = accessContext;
        this.aggregateContext = aggregateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerLedgerReport call(Payable request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(current, storeId);
        PartnerLedgerAggregateContext.PartnerLedgerSummary summary = aggregateContext.summary(
                current, partnerId, keyword, overdueOnly, storeFilter);

        PartnerLedgerReport report = new PartnerLedgerReport();
        if (!Boolean.TRUE.equals(summaryOnly)) {
            report.setRows(aggregateContext.rows(current, partnerId, keyword, overdueOnly, storeFilter));
        }
        report.setPartnerCount(summary.partnerCount());
        report.setPayableAmount(summary.payableAmount());
        report.setPaidAmount(summary.paidAmount());
        report.setOutstandingAmount(summary.outstandingAmount());
        report.setOverdueAmount(summary.overdueAmount());
        report.setAsOf(summary.asOf());
        return report;
    }
}
