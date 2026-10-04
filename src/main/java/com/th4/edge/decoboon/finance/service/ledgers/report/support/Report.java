package com.th4.edge.decoboon.finance.service.ledgers.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.FinanceLedgerReport;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** 资金台账筛选汇总实现。 */
@Note("资金台账汇总")
@Properties(allow = @AllowProperties(model = FinanceLedgerReport.class,
        properties = {"count", "totalAmount", "incomeAmount", "expenseAmount", "netCashFlowAmount"}))
@Service("finance/ledger/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.ledgers.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String direction;
    @AutoInject
    private String category;
    @AutoInject
    private LocalDate startDate;
    @AutoInject
    private LocalDate endDate;
    @AutoInject
    private String keyword;
    /** 生成凭证前，按已选流水主键汇总；与列表筛选条件互不冲突。 */
    @AutoInject
    private List<Long> ledgerIds;

    private final LedgerRepository ledgerRepository;
    private final FinanceAccessContext accessContext;

    public Report(LedgerRepository ledgerRepository, FinanceAccessContext accessContext) {
        this.ledgerRepository = ledgerRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceLedgerReport call(Ledger request) {
        Employee current = accessContext.requireReader(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        Set<Long> selected = ledgerIds == null ? Set.of() : Set.copyOf(ledgerIds);
        List<Ledger> items = ledgerRepository.findAll(accessContext.visible(current, Ledger.class)).stream()
                .filter(item -> selected.isEmpty() || selected.contains(item.getLedgerId()))
                .filter(item -> projectId == null || (item.getProject() != null && projectId.equals(item.getProject().getProjectId())))
                .filter(item -> direction == null || direction.isBlank() || item.getDirection().name().equalsIgnoreCase(direction))
                .filter(item -> category == null || category.isBlank() || item.getCategory().equals(category))
                .filter(item -> startDate == null || (item.getOccurredAt() != null && !item.getOccurredAt().isBefore(startDate)))
                .filter(item -> endDate == null || (item.getOccurredAt() != null && !item.getOccurredAt().isAfter(endDate)))
                .filter(item -> kw.isBlank()
                        || item.getSummary().toLowerCase().contains(kw)
                        || (item.getCounterparty() != null && item.getCounterparty().toLowerCase().contains(kw)))
                .toList();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Ledger item : items) {
            BigDecimal amount = value(item.getAmount());
            total = total.add(amount);
            if (item.getDirection() == com.th4.edge.decoboon.finance.configure.LedgerDirection.Income) {
                income = income.add(amount);
            } else {
                expense = expense.add(amount);
            }
        }

        FinanceLedgerReport report = new FinanceLedgerReport();
        report.setCount(items.size());
        report.setTotalAmount(total);
        report.setIncomeAmount(income);
        report.setExpenseAmount(expense);
        report.setNetCashFlowAmount(income.subtract(expense));
        return report;
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
