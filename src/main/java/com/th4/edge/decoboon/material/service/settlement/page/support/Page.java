package com.th4.edge.decoboon.material.service.settlement.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
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

/** 主材采购结算单列表 */
@Note("主材采购结算单列表")
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
@Service("material/settlement/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.material.service.settlement.page.Page {

    @User private Employee employee;
    @AutoInject private Long supplierId;
    @AutoInject private Long storeId;

    private final MaterialSettlementContext context;
    private final StoreFilterContext storeFilterContext;

    public Page(MaterialSettlementContext context, StoreFilterContext storeFilterContext) {
        this.context = context;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional
    public List<MaterialSettlement> call(MaterialSettlement request) {
        Long storeFilter = storeFilterContext.resolve(employee, storeId);
        List<MaterialSettlement> rows = context.page(employee, supplierId).stream()
                .filter(row -> StoreScope.inStore(StoreScope.storeIdOf(row.getStore()), storeFilter))
                .toList();
        // 关闭 OSIV 时也要能序列化供应商摘要（只触碰 response 实际使用字段）。
        rows.forEach(row -> {
            if (row.getSupplier() != null) {
                row.getSupplier().getPartnerId();
                row.getSupplier().getName();
            }
        });
        return rows;
    }
}
