package com.th4.edge.decoboon.project.service.purchases.page.support;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.*;
import com.th4.edge.decoboon.project.repository.*;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.*;
import com.th4.edge.framework.exception.*;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
@Note("采购订单分页")
@Properties(allow={
 @AllowProperties(model=PurchaseOrder.class,properties={"purchaseId","no","property","project","contract","materialPlan","supplier","supplierName","quoteLine","deliveryMode","warehouse","quantity","unit","unitPrice","amount","receivedQuantity","status","expectedAt","orderedAt","createdBy","approvedBy","approvedAt","approvalRemark","remark","createTime","items"}),
 @AllowProperties(model=PurchaseOrderItem.class,properties={"purchaseItemId","materialPlan","quantity","unit","unitPrice","amount","receivedQuantity","status","priceSource","remark"}),
 @AllowProperties(model=Property.class,properties={"propertyId","propertyNo","address","status"}),
 @AllowProperties(model=Project.class,properties={"projectId","no","name","status"}),
 @AllowProperties(model=ProjectContract.class,properties={"contractId","no","currentAmount","status"}),
 @AllowProperties(model=MaterialPlan.class,properties={"planId","catalogItem","quantity","unit","unitPrice","cost","status"}),
 @AllowProperties(model=CatalogItem.class,properties={"itemId","name","unit","brand","spec","type"}),
 @AllowProperties(model=Partner.class,properties={"partnerId","type","name","contactName","telNo","status"}),
 @AllowProperties(model=Employee.class,properties={"empId","name","role"})})
@Service("project/purchase/page") @Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.purchases.page.Page{
 @AutoInject private Long projectId; @User private Employee employee;
 private final ProjectRepository projectRepository; private final PurchaseOrderRepository purchaseRepository; private final ProjectAccessContext accessContext; private final PurchaseOrderItemRepository purchaseItemRepository;
 public Page(ProjectRepository p,PurchaseOrderRepository o,ProjectAccessContext a,PurchaseOrderItemRepository i){projectRepository=p;purchaseRepository=o;accessContext=a;purchaseItemRepository=i;}
 @Override public org.springframework.data.domain.Page<PurchaseOrder> call(PurchaseOrder request){
   List<Project> projects = projectId == null
       ? projectRepository.findAll(accessContext.visibleProject(employee), Sort.by(Sort.Direction.DESC, "projectId"))
       : List.of(accessContext.requireProject(projectId, employee));
   List<PurchaseOrder> orders = projects.stream()
       .flatMap(project -> purchaseRepository.findByProjectOrderByPurchaseIdDesc(project).stream())
       .sorted(Comparator.comparing(PurchaseOrder::getPurchaseId).reversed())
       .toList();
   java.util.List<Long> purchaseIds = orders.stream().map(PurchaseOrder::getPurchaseId).filter(java.util.Objects::nonNull).toList();
   java.util.Map<Long, java.util.List<PurchaseOrderItem>> itemsByPurchase = purchaseIds.isEmpty()
       ? java.util.Map.of()
       : purchaseItemRepository.findByPurchaseOrder_PurchaseIdIn(purchaseIds).stream()
           .filter(item -> item.getPurchaseOrder() != null && item.getPurchaseOrder().getPurchaseId() != null)
           .collect(java.util.stream.Collectors.groupingBy(item -> item.getPurchaseOrder().getPurchaseId()));
   orders.forEach(order -> order.setItems(itemsByPurchase.getOrDefault(order.getPurchaseId(), java.util.List.of())));
   return new PageImpl<>(orders);
 }
}
