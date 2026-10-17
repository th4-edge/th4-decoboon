package com.th4.edge.decoboon.material.service.settlement.confirm.support;

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

/** 确认结算（回填结算价 + 修正暂估应付 + 补记实际成本） */
@Note("确认结算（回填结算价 + 修正暂估应付 + 补记实际成本）")
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
@Service("material/settlement/confirm")
@Scope(ScopeTypes.PROTOTYPE)
public class Confirm implements com.th4.edge.decoboon.material.service.settlement.confirm.Confirm {

    @User private Employee employee;
    @AutoInject @NotNull private Long supplierId;
    @AutoInject private SettlementScope scope;
    @AutoInject private String period;
    @AutoInject private String remark;
    @AutoInject private List<MaterialSettlementItem> items;

    private final MaterialSettlementContext context;

    public Confirm(MaterialSettlementContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public MaterialSettlement call(Employee request) {
        MaterialSettlement settlement = context.confirm(employee, supplierId, scope, period, items, remark);
        if (settlement.getSupplier() != null) {
            settlement.getSupplier().getPartnerId();
            settlement.getSupplier().getName();
        }
        return settlement;
    }
}
