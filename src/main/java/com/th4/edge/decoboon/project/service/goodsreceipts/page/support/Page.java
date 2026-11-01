package com.th4.edge.decoboon.project.service.goodsreceipts.page.support;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.*;
import com.th4.edge.decoboon.project.repository.*;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.*;
import com.th4.edge.framework.exception.*;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
@Note("材料到场分页")
@Properties(allow={
 @AllowProperties(model=GoodsReceipt.class,properties={"receiptId","property","project","purchase","materialPlan","quantity","unit","arrivedAt","inspector","images","remark","createTime"}),
 @AllowProperties(model=PurchaseOrder.class,properties={"purchaseId","no","supplierName","quantity","unitPrice","amount","receivedQuantity","status","deliveryMode","warehouse"}),
 @AllowProperties(model=MaterialPlan.class,properties={"planId","catalogItem","quantity","unit","status"}),
 @AllowProperties(model=CatalogItem.class,properties={"itemId","name","unit","brand","spec","type"}),
 @AllowProperties(model=Property.class,properties={"propertyId","propertyNo","address","status"}),
 @AllowProperties(model=Project.class,properties={"projectId","no","name","status"}),
 @AllowProperties(model=Employee.class,properties={"empId","name","role"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name", "status"})
})
@Service("project/goods-receipt/page") @Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.goodsreceipts.page.Page{
 @AutoInject private Long purchaseId; @User private Employee employee;
 private final PurchaseOrderRepository purchaseRepository; private final GoodsReceiptRepository receiptRepository; private final ProjectAccessContext accessContext;
 public Page(PurchaseOrderRepository p,GoodsReceiptRepository r,ProjectAccessContext a){purchaseRepository=p;receiptRepository=r;accessContext=a;}
 @Override public org.springframework.data.domain.Page<GoodsReceipt> call(GoodsReceipt request){PurchaseOrder purchase=purchaseRepository.findOne(((Specification<PurchaseOrder>)(root,query,builder)->builder.equal(root.get("purchaseId"),purchaseId)).and(accessContext.visibleChild(employee, PurchaseOrder.class, "project"))).orElseThrow(()->new DefinedException(new DefaultErrorCode("404","采购订单不存在或无权访问")));return new PageImpl<>(receiptRepository.findByPurchaseOrderByReceiptIdDesc(purchase));}
}
