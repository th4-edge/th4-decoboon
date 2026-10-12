package com.th4.edge.decoboon.finance.service.partnerledger.report.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.PartnerLedger;
import com.th4.edge.decoboon.finance.model.PartnerLedgerReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商应付台账。
 *
 * <p>按供应商聚合应付总额、已付、待付、逾期与最近到期日，仅统计当前员工可见范围内的应付数据。
 * 前端据此呈现「欠谁多少钱、什么时候到期、逾期多少」。</p>
 */
@Note("供应商应付台账")
@Properties(allow = {
        @AllowProperties(model = PartnerLedgerReport.class,
                properties = {"rows", "partnerCount", "payableAmount", "paidAmount", "outstandingAmount", "overdueAmount"}),
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

    /** 可选：只看某个供应商。 */
    @AutoInject
    private Long partnerId;

    /** 可选：只看存在逾期金额的供应商。 */
    @AutoInject
    private Boolean overdueOnly;

    /** 门店维度（`DEC-065` / `P1-a2`）：不传 / 0 = 全部门店；门店范围角色恒锁本店，传他店 403。 */
    @AutoInject
    private Long storeId;
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.common.context.StoreFilterContext storeFilterContext;

    private final PayableRepository payableRepository;
    private final FinanceAccessContext accessContext;

    public Report(PayableRepository payableRepository, FinanceAccessContext accessContext) {
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerLedgerReport call(Payable request) {
        Employee current = accessContext.requireReader(employee);
        Long storeFilter = storeFilterContext.resolve(current, storeId);
        LocalDate today = LocalDate.now();
        Map<Long, PartnerLedger> aggregated = new LinkedHashMap<>();
        for (Payable item : payableRepository.findAll(accessContext.visible(current, Payable.class))) {
            if (!StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter)) {
                continue;
            }
            Partner partner = item.getPayeePartner();
            if (partner == null || partner.getPartnerId() == null) {
                continue;
            }
            if (partnerId != null && !partnerId.equals(partner.getPartnerId())) {
                continue;
            }
            BigDecimal amount = value(item.getAmount());
            BigDecimal paid = value(item.getPaidAmount());
            BigDecimal outstanding = amount.subtract(paid).max(BigDecimal.ZERO);
            PartnerLedger ledger = aggregated.computeIfAbsent(partner.getPartnerId(), key -> {
                PartnerLedger created = new PartnerLedger();
                created.setPartner(partner);
                return created;
            });
            ledger.setPayableAmount(ledger.getPayableAmount().add(amount));
            ledger.setPaidAmount(ledger.getPaidAmount().add(paid));
            ledger.setOutstanding(ledger.getOutstanding().add(outstanding));
            if (outstanding.signum() > 0) {
                ledger.setPendingCount(ledger.getPendingCount() + 1);
                if (item.getDueDate() != null) {
                    if (item.getDueDate().isBefore(today)) {
                        ledger.setOverdueAmount(ledger.getOverdueAmount().add(outstanding));
                    }
                    if (ledger.getNearestDueDate() == null || item.getDueDate().isBefore(ledger.getNearestDueDate())) {
                        ledger.setNearestDueDate(item.getDueDate());
                    }
                }
            }
        }
        List<PartnerLedger> result = new ArrayList<>(aggregated.values());
        if (Boolean.TRUE.equals(overdueOnly)) {
            result.removeIf(ledger -> ledger.getOverdueAmount().signum() <= 0);
        }
        result.sort((left, right) -> right.getOutstanding().compareTo(left.getOutstanding()));

        PartnerLedgerReport report = new PartnerLedgerReport();
        report.setRows(result);
        report.setPartnerCount(result.size());
        report.setPayableAmount(result.stream().map(PartnerLedger::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setPaidAmount(result.stream().map(PartnerLedger::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setOutstandingAmount(result.stream().map(PartnerLedger::getOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setOverdueAmount(result.stream().map(PartnerLedger::getOverdueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return report;
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
