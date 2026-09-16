package com.th4.edge.decoboon.project.service.purchases.page.support;
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
import org.springframework.stereotype.Service;
@Note("采购订单分页")
@Properties(allow={
 @AllowProperties(model=PurchaseOrder.class,properties={"purchaseId","no","property","project","contract","materialPlan","supplier","supplierName","quoteLine","warehouse","quantity","unit","unitPrice","amount","receivedQuantity","status","expectedAt","orderedAt","createdBy","approvedBy","approvedAt","approvalRemark","remark","createTime"}),
 @AllowProperties(model=Property.class,properties={"propertyId","propertyNo","address","status"}),
 @AllowProperties(model=Project.class,properties={"projectId","no","name","status"}),
 @AllowProperties(model=ProjectContract.class,properties={"contractId","no","currentAmount","status"}),
 @AllowProperties(model=MaterialPlan.class,properties={"planId","name","quantity","unit","unitPrice","cost","status"}),
 @AllowProperties(model=Employee.class,properties={"empId","name","role"})})
@Service("project/purchase/page") @Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.purchases.page.Page{
 @AutoInject private Long projectId; @User private Employee employee;
 private final ProjectRepository projectRepository; private final PurchaseOrderRepository purchaseRepository; private final ProjectAccessContext accessContext;
 public Page(ProjectRepository p,PurchaseOrderRepository o,ProjectAccessContext a){projectRepository=p;purchaseRepository=o;accessContext=a;}
 @Override public org.springframework.data.domain.Page<PurchaseOrder> call(PurchaseOrder request){Project project=accessContext.requireProject(projectId,employee);return new PageImpl<>(purchaseRepository.findByProjectOrderByPurchaseIdDesc(project));}
}
