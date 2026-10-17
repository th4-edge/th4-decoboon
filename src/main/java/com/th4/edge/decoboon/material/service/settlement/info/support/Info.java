package com.th4.edge.decoboon.material.service.settlement.info.support;

import com.th4.edge.decoboon.material.configure.SettlementScope;
import com.th4.edge.decoboon.material.context.MaterialSettlementContext;
import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.decoboon.material.model.MaterialSettlementItem;
import com.th4.edge.decoboon.material.model.SettlementPreviewRow;
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

import java.util.List;

/** 主材采购结算单详情 */
@Note("主材采购结算单详情")
@Properties(allow = {
        @AllowProperties(model = MaterialSettlement.class,
                properties = {"settlementId", "no", "scope", "period", "settledAt", "totalAmount", "status",
                        "remark", "supplier", "items", "createTime"}),
        @AllowProperties(model = MaterialSettlementItem.class,
                properties = {"settlementItemId", "quantity", "settlementPrice", "amount", "remark", "createTime"}),
        @AllowProperties(model = SettlementPreviewRow.class,
                properties = {"purchaseItemId", "purchaseOrderId", "purchaseNo", "supplierName", "materialName",
                        "quantity", "receivedQuantity", "purchasePrice", "suggestedSettlementPrice", "unpriced"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "type", "name", "contactName", "telNo", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/settlement/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.material.service.settlement.info.Info {

    @User private Employee employee;
    @AutoInject @NotNull private Long settlementId;

    private final MaterialSettlementContext context;

    public Info(MaterialSettlementContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public MaterialSettlement call(MaterialSettlement request) {
        MaterialSettlement settlement = context.info(employee, settlementId);
        if (settlement.getSupplier() != null) {
            settlement.getSupplier().getPartnerId();
            settlement.getSupplier().getName();
        }
        return settlement;
    }
}
