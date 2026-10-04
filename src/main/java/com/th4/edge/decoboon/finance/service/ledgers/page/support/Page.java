package com.th4.edge.decoboon.finance.service.ledgers.page.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 资金流水分页实现。 */
@Note("资金流水分页")
@Properties(allow = {
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "project", "direction", "category", "categoryNode", "source", "sourceId", "amount",
                        "account", "occurredAt", "counterparty", "summary", "remark", "originalLedger",
                        "correctionGroupId", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.LedgerCategory.class,
                properties = {"categoryId", "name", "direction", "code", "builtIn", "sort", "active"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/ledger/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.ledgers.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long ledgerId;
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

    private final LedgerRepository ledgerRepository;
    private final FinanceAccessContext accessContext;

    public Page(LedgerRepository ledgerRepository, FinanceAccessContext accessContext) {
        this.ledgerRepository = ledgerRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Ledger> call(Ledger request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Ledger> items = ledgerRepository.findAll(accessContext.visible(currentEmployee, Ledger.class))
                .stream()
                .filter(item -> ledgerId == null || ledgerId.equals(item.getLedgerId()))
                .filter(item -> projectId == null || (item.getProject() != null && projectId.equals(item.getProject().getProjectId())))
                .filter(item -> direction == null || direction.isBlank() || item.getDirection().name().equalsIgnoreCase(direction))
                .filter(item -> category == null || category.isBlank() || item.getCategory().equals(category))
                .filter(item -> startDate == null || (item.getOccurredAt() != null && !item.getOccurredAt().isBefore(startDate)))
                .filter(item -> endDate == null || (item.getOccurredAt() != null && !item.getOccurredAt().isAfter(endDate)))
                .filter(item -> kw.isBlank()
                        || item.getSummary().toLowerCase().contains(kw)
                        || (item.getCounterparty() != null && item.getCounterparty().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(Ledger::getOccurredAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Ledger::getLedgerId,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
