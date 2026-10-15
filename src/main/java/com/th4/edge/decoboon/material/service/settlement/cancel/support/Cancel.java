package com.th4.edge.decoboon.material.service.settlement.cancel.support;

import com.th4.edge.decoboon.material.context.MaterialSettlementContext;
import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.decoboon.material.model.MaterialSettlementItem;
import com.th4.edge.decoboon.supply.model.Partner;
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
import org.springframework.transaction.annotation.Transactional;

/** material/settlement/cancel 实现：撤销结算（仅财务/店长/负责人，纠错动作）。 */
@Note("撤销结算")
@Properties(allow = {
        @AllowProperties(model = MaterialSettlement.class,
                properties = {"settlementId", "no", "scope", "period", "settledAt", "totalAmount", "status",
                        "remark", "cancelledAt", "cancelReason", "items", "createTime"}),
        @AllowProperties(model = MaterialSettlementItem.class,
                properties = {"settlementItemId", "quantity", "settlementPrice", "amount", "remark"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("material/settlement/cancel")
@Scope(ScopeTypes.PROTOTYPE)
public class Cancel implements com.th4.edge.decoboon.material.service.settlement.cancel.Cancel {

    @User private Employee employee;
    @AutoInject @NotNull private Long settlementId;
    @AutoInject private String reason;

    private final MaterialSettlementContext context;

    public Cancel(MaterialSettlementContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public MaterialSettlement call(MaterialSettlement request) {
        return context.cancel(employee, settlementId, reason);
    }
}
