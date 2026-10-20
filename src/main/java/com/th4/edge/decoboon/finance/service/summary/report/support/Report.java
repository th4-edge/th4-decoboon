package com.th4.edge.decoboon.finance.service.summary.report.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceSummary;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 财务工作台汇总实现。 */
@Note("财务工作台汇总")
@Properties(allow = {
        @AllowProperties(model = FinanceSummary.class,
                properties = {"projectCount", "activeProjectCount", "contractAmount", "receivableAmount", "receivableCount", "receivedAmount",
                        "outstandingAmount", "overdueAmount", "payableAmount", "paidAmount", "unpaidAmount",
                        "materialPayableAmount", "salaryPayableAmount", "employeeSalaryPayableAmount", "employeeCommissionPayableAmount", "projectPayableAmount", "otherPayableAmount",
                        "monthIncome", "monthExpense", "monthNetCashFlowAmount", "receivableCount", "receiptCount", "paymentCount", "grossReceiptAmount", "reversalAmount", "receiptAmount", "paymentAmount",
                        "monthReceiptAmount", "monthPaymentAmount", "urgentPayableAmount", "cashBalance", "openingBalanceAmount", "openingBalanceCaptured", "estimatedCost", "actualCost",
                        "manualIncomeAmount", "manualCostAmount",
                        "costComplete", "costIncompleteProjectCount", "costIncompleteItemCount",
                        "profitAmount", "grossMargin", "collectionRate",
                        "urgentReceivables", "overdueReceivables", "dueSoonReceivables", "urgentPayables", "recentLedgers"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "project", "property", "customer", "name", "stageName", "type", "amount",
                        "receivedAmount", "dueDate", "status"}),
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "name", "type", "payeeEmployee", "payeeName", "period",
                        "amount", "paidAmount", "dueDate", "status", "urgent", "property" }),
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "project", "direction", "category", "categoryNode", "purpose", "source", "amount", "account",
                        "occurredAt", "counterparty", "summary"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.LedgerCategory.class,
                properties = {"categoryId", "name", "description", "direction", "builtIn"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/summary/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.summary.report.Report {

    @User
    private Employee employee;

    /**
     * 门店维度（DEC-065）。
     *
     * <p>注意：框架对 Provider 的请求模型（{@code Provider<Request, Response>} 的 Request）
     * 不做请求体/查询参数绑定，只对 provider 实例上的 {@code @AutoInject} 字段注入请求参数，
     * 因此门店维度必须落在字段上，不能放在请求模型里。</p>
     */
    @AutoInject
    private Long storeId;

    private final FinanceReportContext reportContext;

    public Report(FinanceReportContext reportContext) {
        this.reportContext = reportContext;
    }

    @Override
    public FinanceSummary call(Project request) {
        return reportContext.summary(employee, storeId);
    }
}
