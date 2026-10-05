package com.th4.edge.decoboon.finance.service.projects.report.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 单项目财务全景实现。 */
@Note("单项目财务全景")
@Properties(allow = {
        @AllowProperties(model = FinanceProjectReport.class,
                properties = {"project", "contractAmount", "contractRevenue", "receivableAmount", "receivedAmount", "outstandingAmount",
                        "overdueAmount", "payableAmount", "paidAmount", "unpaidAmount", "receiptCount", "paymentCount", "receiptAmount", "paymentAmount",
                        "monthReceiptAmount", "monthPaymentAmount", "urgentPayableAmount", "materialAmount", "materialCost", "salaryAmount",
                        "laborCost", "projectPaymentAmount", "projectCost", "otherPayableAmount", "expenseAllocation",
                        "estimatedCost", "actualCost", "manualIncomeAmount", "manualCostAmount", "totalCost", "profitAmount", "costComplete", "costIncompleteCount",
                        "grossProfit", "grossMargin", "collectionRate", "paymentRate",
                        "receivableCleared", "payableCleared", "settlement", "receivables", "payables", "recentLedgers"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "cost", "property", "customer", "manager", "designer",
                        "siteAddr", "status", "progress", "startDate", "planEndDate", "actualEndDate" }),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "stageName", "type", "amount", "receivedAmount", "dueDate",
                        "status", "invoiceRequired", "invoiceNo", "remark", "property" }),
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "name", "type", "payeeEmployee", "payeeName", "period", "amount", "paidAmount",
                        "dueDate", "status", "urgent", "sourceNo", "remark", "property" }),
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "direction", "category", "source", "amount", "account", "occurredAt",
                        "counterparty", "summary", "remark"}),
        @AllowProperties(model = Settlement.class,
                properties = {"settlementId", "contractAmount", "receivableAmount", "receivedAmount", "payableAmount",
                        "paidAmount", "materialAmount", "salaryAmount", "projectAmount", "expenseAmount", "totalCost",
                        "profitAmount", "grossMargin", "status",
                        "confirmedBy", "confirmedAt", "closedAt", "remark"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/project/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.projects.report.Report {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long projectId;

    private final FinanceReportContext reportContext;

    public Report(FinanceReportContext reportContext) {
        this.reportContext = reportContext;
    }

    @Override
    public FinanceProjectReport call(Project request) {
        return reportContext.project(employee, projectId);
    }
}
