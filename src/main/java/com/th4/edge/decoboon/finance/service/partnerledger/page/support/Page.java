package com.th4.edge.decoboon.finance.service.partnerledger.page.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.PartnerLedgerAggregateContext;
import com.th4.edge.decoboon.finance.model.PartnerLedger;
import com.th4.edge.decoboon.finance.model.Payable;
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

/** 供应商往来分页实现。 */
@Note("供应商往来分页")
@Properties(allow = {
        @AllowProperties(model = PartnerLedger.class,
                properties = {"partner", "payableAmount", "paidAmount", "outstanding", "overdueAmount",
                        "pendingCount", "nearestDueDate"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "name", "type", "contactName", "telNo", "settlementTerms",
                        "settlementMode", "settlementDays", "cutoffDay", "status"})
})
@Service("finance/partner-ledger/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.partnerledger.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Long partnerId;
    @AutoInject
    private String keyword;
    @AutoInject
    private Boolean overdueOnly;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long storeId;

    private final FinanceAccessContext accessContext;
    private final PartnerLedgerAggregateContext aggregateContext;

    public Page(FinanceAccessContext accessContext, PartnerLedgerAggregateContext aggregateContext) {
        this.accessContext = accessContext;
        this.aggregateContext = aggregateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PartnerLedger> call(Payable request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        int page = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        return aggregateContext.page(currentEmployee, partnerId, keyword, overdueOnly, storeFilter, page, pageSize);
    }
}
