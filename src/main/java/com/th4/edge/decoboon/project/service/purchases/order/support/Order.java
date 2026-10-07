package com.th4.edge.decoboon.project.service.purchases.order.support;

import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.MaterialPurchaseContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 采购单下单兼容入口。
 *
 * <p>新采购保存已在下单事务内完成应付生成；本入口保留给历史未下单采购，
 * 并保证重复调用幂等。</p>
 */
@Note("采购单下单")
@Properties(allow = @AllowProperties(model = PurchaseOrder.class,
        properties = {"purchaseId", "no", "status", "orderedAt", "expectedAt", "createTime"}))
@Service("project/purchase/order")
@Scope(ScopeTypes.PROTOTYPE)
public class Order implements com.th4.edge.decoboon.project.service.purchases.order.Order {
    @User private Employee employee;
    @AutoInject @NotNull private Long purchaseId;
    private final PurchaseOrderRepository purchaseRepository;
    private final ProjectAccessContext accessContext;
    private final MaterialPurchaseContext materialPurchaseContext;

    public Order(PurchaseOrderRepository purchaseRepository,
                 ProjectAccessContext accessContext,
                 MaterialPurchaseContext materialPurchaseContext) {
        this.purchaseRepository = purchaseRepository;
        this.accessContext = accessContext;
        this.materialPurchaseContext = materialPurchaseContext;
    }

    @Override
    @Transactional
    public PurchaseOrder call(PurchaseOrder request) {
        Employee current = accessContext.requireEmployee(employee);
        materialPurchaseContext.requireOperator(current);
        Specification<PurchaseOrder> byId = (root, query, builder) ->
                builder.equal(root.get("purchaseId"), purchaseId);
        PurchaseOrder purchase = purchaseRepository.findOne(
                        byId.and(accessContext.visibleChild(employee, PurchaseOrder.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "采购单不存在或无权访问")));
        if (purchase.getStatus() != PurchaseStatus.Approved
                && purchase.getStatus() != PurchaseStatus.Ordered) {
            throw new DefinedException(new DefaultErrorCode("400", "采购单审批通过后才能下单"));
        }
        return materialPurchaseContext.confirmOrder(purchase, current);
    }
}
