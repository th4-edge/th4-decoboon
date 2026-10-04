package com.th4.edge.decoboon.finance.service.payables.report.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.FinancePayableReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
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
import java.util.List;

/** 应付筛选汇总实现。 */
@Note("应付账款汇总")
@Properties(allow = @AllowProperties(model = FinancePayableReport.class,
        properties = {"count", "totalAmount", "paidAmount", "outstandingAmount", "urgentAmount"}))
@Service("finance/payable/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.payables.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String type;
    @AutoInject
    private String status;
    @AutoInject
    private Boolean urgent;
    @AutoInject
    private Long partnerId;
    @AutoInject
    private String keyword;

    private final PayableRepository payableRepository;
    private final FinanceAccessContext accessContext;

    public Report(PayableRepository payableRepository, FinanceAccessContext accessContext) {
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public FinancePayableReport call(Payable request) {
        Employee current = accessContext.requireReader(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Payable> items = payableRepository.findAll(accessContext.visible(current, Payable.class)).stream()
                .filter(item -> projectId == null || (item.getProject() != null && projectId.equals(item.getProject().getProjectId())))
                .filter(item -> type == null || type.isBlank() || item.getType().name().equalsIgnoreCase(type))
                .filter(item -> status == null || status.isBlank() || item.getStatus().name().equalsIgnoreCase(status))
                .filter(item -> urgent == null || urgent.equals(item.getUrgent()))
                .filter(item -> partnerId == null
                        || (item.getPayeePartner() != null && partnerId.equals(item.getPayeePartner().getPartnerId())))
                .filter(item -> kw.isBlank()
                        || item.getName().toLowerCase().contains(kw)
                        || (item.getPayeeName() != null && item.getPayeeName().toLowerCase().contains(kw))
                        || (item.getProject() != null && item.getProject().getName().toLowerCase().contains(kw)))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal urgentAmount = BigDecimal.ZERO;
        for (Payable item : items) {
            BigDecimal itemOutstanding = accessContext.outstanding(item.getAmount(), item.getPaidAmount());
            total = total.add(value(item.getAmount()));
            paid = paid.add(value(item.getPaidAmount()));
            outstanding = outstanding.add(itemOutstanding);
            if (item.getStatus() == com.th4.edge.decoboon.finance.configure.PayableStatus.Overdue
                    || Boolean.TRUE.equals(item.getUrgent())) {
                urgentAmount = urgentAmount.add(itemOutstanding);
            }
        }

        FinancePayableReport report = new FinancePayableReport();
        report.setCount(items.size());
        report.setTotalAmount(total);
        report.setPaidAmount(paid);
        report.setOutstandingAmount(outstanding);
        report.setUrgentAmount(urgentAmount);
        return report;
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
