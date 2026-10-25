package com.th4.edge.decoboon.finance.service.projects.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceProjectLedgerContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
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

/** 项目财务账本分页实现。 */
@Note("项目财务账本分页")
@Properties(allow = {
        @AllowProperties(model = FinanceProjectReport.class,
                properties = {"project", "contractAmount", "contractRevenue", "receivableAmount", "receivedAmount", "outstandingAmount",
                        "overdueAmount", "payableAmount", "paidAmount", "unpaidAmount", "receiptCount", "paymentCount", "receiptAmount", "paymentAmount",
                        "monthReceiptAmount", "monthPaymentAmount", "urgentPayableAmount", "materialAmount", "materialCost", "salaryAmount", "employeeSalaryAmount", "employeeCommissionAmount",
                        "laborCost", "projectPaymentAmount", "projectCost", "otherPayableAmount", "expenseAllocation",
                        "estimatedCost", "actualCost", "totalCost", "profitAmount", "costComplete", "costIncompleteCount",
                        "grossProfit", "grossMargin", "collectionRate", "paymentRate",
                        "receivableCleared", "payableCleared", "historicalArchive", "archiveLabel", "settlementStatusLabel",
                        "settlementActionable", "settlement"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "businessOrigin", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status", "progress" }),
        @AllowProperties(model = Receivable.class, properties = {"receivableId", "property" }),
        @AllowProperties(model = Payable.class, properties = {"payableId", "property" }),
        @AllowProperties(model = Ledger.class, properties = {"ledgerId"}),
        @AllowProperties(model = Settlement.class,
                properties = {"settlementId", "status", "expenseAmount", "totalCost", "estimatedCost", "actualCost", "profitAmount", "costComplete", "costIncompleteCount", "grossMargin", "confirmedAt", "closedAt"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "community", "address"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/project/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.projects.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String keyword;
    @AutoInject
    private String status;
    @AutoInject
    private String scope;
    /** all / risk / settle */
    @AutoInject
    private String filter;
    @AutoInject
    private Long storeId;

    private final FinanceProjectLedgerContext ledgerContext;

    public Page(FinanceProjectLedgerContext ledgerContext) {
        this.ledgerContext = ledgerContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<FinanceProjectReport> call(Project request) {
        int page = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 20 : size;
        return ledgerContext.page(employee, storeId, keyword, status, scope, filter, page, pageSize);
    }
}
