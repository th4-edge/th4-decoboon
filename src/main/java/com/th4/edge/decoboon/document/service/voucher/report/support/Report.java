package com.th4.edge.decoboon.document.service.voucher.report.support;

import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.VoucherMonthAmount;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.model.VoucherReport;
import com.th4.edge.decoboon.document.repository.VoucherRecordRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 凭证筛选汇总实现。 */
@Note("凭证汇总")
@Properties(allow = {
        @AllowProperties(model = VoucherReport.class,
                properties = {"count", "totalAmount", "receiptAmount", "paymentAmount", "monthlyAmounts"}),
        @AllowProperties(model = VoucherMonthAmount.class, properties = {"month", "count", "amount"})
})
@Service("document/voucher/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.document.service.voucher.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String status;
    @AutoInject
    private String keyword;
    @AutoInject
    private String type;
    @AutoInject
    private LocalDate startDate;
    @AutoInject
    private LocalDate endDate;

    private final VoucherRecordRepository recordRepository;
    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;

    public Report(VoucherRecordRepository recordRepository, TenantContext tenantContext,
                  DataScopeContext dataScopeContext) {
        this.recordRepository = recordRepository;
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherReport call(VoucherRecord request) {
        Employee viewer = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.Finance, EmployeeRole.Pm, EmployeeRole.Designer);
        Long comId = viewer.getCompany().getComId();
        Specification<VoucherRecord> visible = viewer.getRole() == EmployeeRole.Boss
                || viewer.getRole() == EmployeeRole.Finance
                ? (root, query, builder) -> builder.equal(root.get("company").get("comId"), comId)
                : DataScopeSpecification.visible(VoucherRecord.class, dataScopeContext.resolve(viewer));
        List<VoucherRecord> items = recordRepository.findAll(visible).stream()
                .filter(item -> projectId == null
                        || (item.getProject() != null && projectId.equals(item.getProject().getProjectId())))
                .filter(item -> status == null || status.isBlank() || item.getStatus().name().equalsIgnoreCase(status))
                .filter(item -> type == null || type.isBlank() || item.getType().name().equalsIgnoreCase(type))
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> startDate == null || (item.getGeneratedAt() != null
                        && !item.getGeneratedAt().toLocalDate().isBefore(startDate)))
                .filter(item -> endDate == null || (item.getGeneratedAt() != null
                        && !item.getGeneratedAt().toLocalDate().isAfter(endDate)))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal receipt = BigDecimal.ZERO;
        BigDecimal payment = BigDecimal.ZERO;
        for (VoucherRecord item : items) {
            BigDecimal amount = value(item.getTotalAmount());
            total = total.add(amount);
            if (item.getType() == DocumentType.Receipt) {
                receipt = receipt.add(amount);
            } else if (item.getType() == DocumentType.Payment) {
                payment = payment.add(amount);
            }
        }

        VoucherReport report = new VoucherReport();
        report.setCount(items.size());
        report.setTotalAmount(total);
        report.setReceiptAmount(receipt);
        report.setPaymentAmount(payment);

        Map<YearMonth, VoucherMonthAmount> byMonth = new LinkedHashMap<>();
        for (VoucherRecord item : items) {
            if (item.getGeneratedAt() == null) continue;
            YearMonth month = YearMonth.from(item.getGeneratedAt());
            VoucherMonthAmount monthly = byMonth.computeIfAbsent(month, key -> {
                VoucherMonthAmount created = new VoucherMonthAmount();
                created.setMonth(key.toString());
                return created;
            });
            monthly.setCount(monthly.getCount() + 1);
            monthly.setAmount(monthly.getAmount().add(value(item.getTotalAmount())));
        }
        List<VoucherMonthAmount> monthlyAmounts = new ArrayList<>(byMonth.values());
        monthlyAmounts.sort(Comparator.comparing(VoucherMonthAmount::getMonth).reversed());
        report.setMonthlyAmounts(monthlyAmounts);
        return report;
    }

    private boolean matchesKeyword(VoucherRecord item, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String kw = keyword.trim().toLowerCase();
        return contains(item.getVoucherNo(), kw)
                || contains(item.getPartyName(), kw)
                || contains(item.getWorker() == null ? null : item.getWorker().getName(), kw)
                || contains(item.getProject() == null ? null : item.getProject().getName(), kw);
    }

    private boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
