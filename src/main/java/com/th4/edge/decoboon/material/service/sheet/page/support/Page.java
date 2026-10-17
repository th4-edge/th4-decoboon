package com.th4.edge.decoboon.material.service.sheet.page.support;

import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.model.MaterialCategory;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 主材下单作业单列表 */
@Note("主材下单作业单列表")
@Properties(allow = {
        @AllowProperties(model = MaterialOrderSheet.class,
                properties = {"sheetId", "sheetNo", "sheetType", "manufacturerName", "measureAt", "groupChatAt",
                        "deliverAt", "installAt", "templateVersion", "status", "priceState", "signatures",
                        "remark", "attachments", "items", "project", "property", "supplier", "createTime", "updateTime"}),
        @AllowProperties(model = MaterialOrderSheetItem.class,
                properties = {"itemId", "sourceKind", "standardIncluded", "spaceName", "locationName", "materialName",
                        "brand", "model", "spec", "color", "size", "craft", "glass", "quantity", "unit", "lossRate",
                        "lossQuantity", "quoteCostPrice", "purchasePrice", "settlementPrice", "priceState",
                        "orderedQuantity", "sort", "remark", "attachments", "materialPlan", "createTime"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "type", "name", "contactName", "telNo", "status"}),
        @AllowProperties(model = MaterialCategory.class, properties = {"materialCategoryId", "name", "defaultLossRate"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "community", "building", "roomNo", "address"}),
        @AllowProperties(model = MaterialPlan.class, properties = {"planId", "status", "quantity", "unit"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("material/sheet/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.material.service.sheet.page.Page {

    @User private Employee employee;
    @AutoInject private Long projectId;
    @AutoInject private String sheetType;
    @AutoInject private com.th4.edge.decoboon.material.configure.MaterialSheetStatus status;

    private final MaterialSheetContext context;

    public Page(MaterialSheetContext context) {
        this.context = context;
    }

    /** 在事务内显式触碰响应所需关联，确保关闭 OSIV 时序列化不触发懒加载异常。 */
    private void initializeProjection(MaterialOrderSheet sheet, List<MaterialOrderSheetItem> items) {
        if (sheet.getProject() != null) {
            sheet.getProject().getProjectId();
            sheet.getProject().getName();
        }
        if (sheet.getProperty() != null) {
            sheet.getProperty().getAddress();
        }
        if (sheet.getSupplier() != null) {
            sheet.getSupplier().getPartnerId();
            sheet.getSupplier().getName();
        }
        if (items != null) {
            for (MaterialOrderSheetItem item : items) {
                if (item.getMaterialPlan() != null) {
                    item.getMaterialPlan().getPlanId();
                }
            }
        }
    }

    @Override
    @Transactional
    public List<MaterialOrderSheet> call(MaterialOrderSheet request) {
        List<MaterialOrderSheet> sheets = context.page(employee, projectId, sheetType, status);
        for (MaterialOrderSheet sheet : sheets) {
            sheet.setItems(context.items(sheet.getSheetId()));
            initializeProjection(sheet, sheet.getItems());
        }
        return sheets;
    }
}
