package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.material.context.MaterialCommercialPrivacyContext;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** B6-DEF-001/002：设计/PM 读取采购与作业单时必须裁剪商业字段。 */
class MaterialCommercialPrivacyContextTest {

    private final MaterialCommercialPrivacyContext context = new MaterialCommercialPrivacyContext();

    @Test
    void designerAndPmCannotSeeSheetPrices() {
        for (EmployeeRole role : List.of(EmployeeRole.Designer, EmployeeRole.Pm)) {
            MaterialOrderSheet sheet = new MaterialOrderSheet();
            MaterialOrderSheetItem item = new MaterialOrderSheetItem();
            item.setQuoteCostPrice(new BigDecimal("120"));
            item.setPurchasePrice(new BigDecimal("100"));
            item.setSettlementPrice(new BigDecimal("110"));
            sheet.setItems(List.of(item));

            context.mask(sheet, employee(role));

            assertThat(sheet.getItems().get(0).getQuoteCostPrice()).isNull();
            assertThat(sheet.getItems().get(0).getPurchasePrice()).isNull();
            assertThat(sheet.getItems().get(0).getSettlementPrice()).isNull();
        }
    }

    @Test
    void designerAndPmCannotSeePurchaseAmountsOrSupplier() {
        for (EmployeeRole role : List.of(EmployeeRole.Designer, EmployeeRole.Pm)) {
            PurchaseOrder order = new PurchaseOrder();
            order.setSupplierName("敏感供应商");
            order.setAmount(new BigDecimal("300"));
            order.setUnitPrice(new BigDecimal("100"));
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setAmount(new BigDecimal("300"));
            item.setUnitPrice(new BigDecimal("100"));
            order.setItems(List.of(item));

            context.mask(order, employee(role));

            assertThat(order.getSupplierName()).isNull();
            assertThat(order.getAmount()).isNull();
            assertThat(order.getUnitPrice()).isNull();
            assertThat(order.getItems().get(0).getAmount()).isNull();
            assertThat(order.getItems().get(0).getUnitPrice()).isNull();
        }
    }

    @Test
    void financeBossAndStoreManagerKeepCommercialFields() {
        for (EmployeeRole role : List.of(EmployeeRole.Finance, EmployeeRole.Boss, EmployeeRole.StoreManager)) {
            PurchaseOrder order = new PurchaseOrder();
            order.setSupplierName("供应商");
            order.setAmount(new BigDecimal("300"));
            context.mask(order, employee(role));
            assertThat(order.getSupplierName()).isEqualTo("供应商");
            assertThat(order.getAmount()).isEqualByComparingTo("300");
        }
    }

    private Employee employee(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setRole(role);
        return employee;
    }
}
