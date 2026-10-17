package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 材料商业字段可见性：采购价、成本价、结算价、采购金额和供应商商务信息仅对
 * Boss / StoreManager / Finance 返回。Designer / PM 只保留履约事实（数量、状态、到场）。
 *
 * <p>只在 {@code @Transactional(readOnly = true)} 的读取 Provider 中裁剪，
 * 避免把响应裁剪误写成数据库更新。</p>
 */
@Component
public class MaterialCommercialPrivacyContext {

    private static final Set<EmployeeRole> COMMERCIAL_VISIBLE = EnumSet.of(
            EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance);

    public boolean canViewCommercial(Employee employee) {
        return employee != null && employee.getRole() != null && COMMERCIAL_VISIBLE.contains(employee.getRole());
    }

    public MaterialOrderSheet mask(MaterialOrderSheet sheet, Employee employee) {
        if (sheet == null || canViewCommercial(employee)) return sheet;
        sheet.setPriceState(null);
        if (sheet.getItems() != null) {
            for (MaterialOrderSheetItem item : sheet.getItems()) {
                if (item == null) continue;
                item.setQuoteCostPrice(null);
                item.setPurchasePrice(null);
                item.setSettlementPrice(null);
                item.setPriceState(null);
            }
        }
        return sheet;
    }

    public List<MaterialOrderSheet> maskSheets(List<MaterialOrderSheet> sheets, Employee employee) {
        if (sheets == null) return null;
        sheets.forEach(sheet -> mask(sheet, employee));
        return sheets;
    }

    public PurchaseOrder mask(PurchaseOrder order, Employee employee) {
        if (order == null || canViewCommercial(employee)) return order;
        order.setSupplier(null);
        order.setSupplierName(null);
        order.setQuoteLine(null);
        order.setContract(null);
        order.setUnitPrice(null);
        order.setAmount(null);
        order.setApprovalRemark(null);
        if (order.getItems() != null) {
            for (PurchaseOrderItem item : order.getItems()) {
                if (item == null) continue;
                item.setUnitPrice(null);
                item.setAmount(null);
                item.setSettlementPrice(null);
                item.setPriceSource(null);
                item.setPriceState(null);
                MaterialPlan plan = item.getMaterialPlan();
                if (plan != null) {
                    plan.setUnitPrice(null);
                    plan.setCost(null);
                }
            }
        }
        return order;
    }
}
