package com.th4.edge.decoboon.material.service.sheet.measure.support;

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

/** 测量与配送/安装时间确认（项目经理或设计师） */
@Note("测量与配送/安装时间确认（项目经理或设计师）")
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
@Service("material/sheet/measure")
@Scope(ScopeTypes.PROTOTYPE)
public class Measure implements com.th4.edge.decoboon.material.service.sheet.measure.Measure {

    @User private Employee employee;
    @AutoInject @NotNull private Long sheetId;
    @AutoInject private LocalDate measureAt;
    @AutoInject private LocalDate deliverAt;
    @AutoInject private LocalDate installAt;
    @AutoInject private String signatures;

    private final MaterialSheetContext context;

    public Measure(MaterialSheetContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public MaterialOrderSheet call(MaterialOrderSheet request) {
        return context.measure(employee, sheetId, measureAt, deliverAt, installAt, signatures);
    }
}
