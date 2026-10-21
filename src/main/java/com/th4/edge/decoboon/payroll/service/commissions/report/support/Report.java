package com.th4.edge.decoboon.payroll.service.commissions.report.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.CommissionReportContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.FinanceCommissionReport;
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

/** 员工提成筛选汇总实现。 */
@Note("员工提成汇总")
@Properties(allow = @AllowProperties(model = FinanceCommissionReport.class,
        properties = {"count", "commissionAmount", "accruedAmount", "releasedAmount", "paidAmount", "asOf"}))
@Service("payroll/commission/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.payroll.service.commissions.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long storeId;
    @org.springframework.beans.factory.annotation.Autowired
    private StoreFilterContext storeFilterContext;

    private final PayrollAccessContext accessContext;
    private final CommissionReportContext reportContext;

    public Report(PayrollAccessContext accessContext, CommissionReportContext reportContext) {
        this.accessContext = accessContext;
        this.reportContext = reportContext;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceCommissionReport call(CommissionItem request) {
        Employee current = accessContext.requireManager(employee);
        Long storeFilter = storeFilterContext.resolve(current, storeId);
        CommissionReportContext.CommissionAggregate aggregate = reportContext.aggregate(current, storeFilter);
        FinanceCommissionReport report = new FinanceCommissionReport();
        report.setCount(aggregate.count());
        report.setCommissionAmount(aggregate.commissionAmount());
        report.setAccruedAmount(aggregate.accruedAmount());
        report.setReleasedAmount(aggregate.releasedAmount());
        report.setPaidAmount(aggregate.paidAmount());
        report.setAsOf(aggregate.asOf());
        return report;
    }
}
