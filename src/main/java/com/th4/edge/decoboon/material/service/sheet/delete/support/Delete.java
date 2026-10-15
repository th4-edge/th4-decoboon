package com.th4.edge.decoboon.material.service.sheet.delete.support;

import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.model.MaterialCategory;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.project.configure.DeliveryMode;
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

/** 删除主材下单作业单（仅草稿/已取消） */
@Note("删除主材下单作业单（仅草稿/已取消）")
@Properties(allow = {
        @AllowProperties(model = MaterialOrderSheet.class,
                properties = {"sheetId", "sheetNo", "sheetType", "manufacturerName", "measureAt", "groupChatAt",
                        "deliverAt", "installAt", "templateVersion", "status", "priceState", "signatures",
                        "remark", "attachments", "items", "createTime", "updateTime"}),
        @AllowProperties(model = MaterialOrderSheetItem.class,
                properties = {"itemId", "sourceKind", "standardIncluded", "spaceName", "locationName", "materialName",
                        "brand", "model", "spec", "color", "size", "craft", "glass", "quantity", "unit", "lossRate",
                        "lossQuantity", "quoteCostPrice", "purchasePrice", "settlementPrice", "priceState",
                        "orderedQuantity", "sort", "remark", "attachments", "createTime"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "type", "name", "contactName", "telNo", "status"}),
        @AllowProperties(model = MaterialCategory.class, properties = {"materialCategoryId", "name", "defaultLossRate"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("material/sheet/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.material.service.sheet.delete.Delete {

    @User private Employee employee;
    @AutoInject @NotNull private Long sheetId;

    private final MaterialSheetContext context;

    public Delete(MaterialSheetContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public MaterialOrderSheet call(MaterialOrderSheet request) {
        MaterialOrderSheet sheet = context.info(employee, sheetId);
        context.delete(employee, sheetId);
        return sheet;
    }
}
