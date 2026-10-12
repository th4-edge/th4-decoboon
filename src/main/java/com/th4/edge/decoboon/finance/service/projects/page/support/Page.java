package com.th4.edge.decoboon.finance.service.projects.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.project.configure.ProjectArchivePolicy;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

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
    /** 当前结算工作台默认只看当前项目；archive 用于查看历史接管归档。 */
    @AutoInject
    private String scope;
    /** 门店维度（DEC-065）：为空表示不限门店（企业范围）或本店（门店范围）。 */
    @AutoInject
    private Long storeId;

    private final FinanceReportContext reportContext;

    public Page(FinanceReportContext reportContext) {
        this.reportContext = reportContext;
    }

    @Override
    public org.springframework.data.domain.Page<FinanceProjectReport> call(Project request) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<FinanceProjectReport> items = reportContext.projectReports(employee, storeId).stream()
                .filter(item -> matchesScope(item, scope))
                .filter(item -> status == null || status.isBlank() || matchesStatus(item, status.trim()))
                .filter(item -> kw.isBlank()
                        || item.getProject().getName().toLowerCase().contains(kw)
                        || item.getProject().getNo().toLowerCase().contains(kw)
                        || item.getProject().getSiteAddr().toLowerCase().contains(kw))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }

    private boolean matchesScope(FinanceProjectReport item, String value) {
        boolean historicalArchive = ProjectArchivePolicy.isHistoricalArchive(item.getProject());
        if (value == null || value.isBlank() || "current".equalsIgnoreCase(value.trim())) {
            return !historicalArchive;
        }
        if ("archive".equalsIgnoreCase(value.trim())) {
            return historicalArchive;
        }
        return true;
    }

    /**
     * 过滤同时支持项目状态与结算状态：结算状态优先，避免"结算状态"筛选被项目状态吞掉。
     */
    private boolean matchesStatus(FinanceProjectReport item, String value) {
        for (com.th4.edge.decoboon.finance.configure.SettlementStatus candidate
                : com.th4.edge.decoboon.finance.configure.SettlementStatus.values()) {
            if (candidate.name().equalsIgnoreCase(value)) {
                Settlement settlement = item.getSettlement();
                return settlement != null && settlement.getStatus() != null
                        && settlement.getStatus().name().equalsIgnoreCase(value);
            }
        }
        return item.getProject().getStatus().name().equalsIgnoreCase(value);
    }
}
