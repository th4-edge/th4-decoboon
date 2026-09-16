package com.th4.edge.decoboon.project.service.purchases.order.support;

import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 采购单下单。 */
@Note("采购单下单")
@Properties(allow = @AllowProperties(model = PurchaseOrder.class,
        properties = {"purchaseId", "no", "status", "orderedAt", "expectedAt", "createTime"}))
@Service("project/purchase/order")
@Scope(ScopeTypes.PROTOTYPE)
public class Order implements com.th4.edge.decoboon.project.service.purchases.order.Order {
    @User private Employee employee;
    @AutoInject @NotNull private Long purchaseId;
    private final PurchaseOrderRepository purchaseRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectAccessContext accessContext;

    public Order(PurchaseOrderRepository purchaseRepository,
                 MaterialPlanRepository materialPlanRepository,
                 ProjectAccessContext accessContext) {
        this.purchaseRepository = purchaseRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public PurchaseOrder call(PurchaseOrder request) {
        Employee current = accessContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Finance
                && current.getRole() != EmployeeRole.ProjectSupport && current.getRole() != EmployeeRole.Pm) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色不能执行采购下单"));
        }
        Specification<PurchaseOrder> byId = (root, query, builder) ->
                builder.equal(root.get("purchaseId"), purchaseId);
        PurchaseOrder purchase = purchaseRepository.findOne(
                        byId.and(accessContext.visibleChild(employee, PurchaseOrder.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "采购单不存在或无权访问")));
        if (purchase.getStatus() != PurchaseStatus.Approved) {
            throw new DefinedException(new DefaultErrorCode("400", "采购单审批通过后才能下单"));
        }
        purchase.setStatus(PurchaseStatus.Ordered);
        purchase.setOrderedAt(LocalDate.now());
        purchase.getMaterialPlan().setStatus(MaterialPlanStatus.Ordered);
        materialPlanRepository.save(purchase.getMaterialPlan());
        return purchaseRepository.save(purchase);
    }
}
