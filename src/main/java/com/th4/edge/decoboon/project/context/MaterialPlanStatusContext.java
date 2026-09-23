package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 材料计划行状态派生。
 *
 * <p>下单/到场进度一律由采购单与其明细实时汇总得出，禁止人工写入或前端本地推断
 * （可完成状态投影）。优先使用采购明细；无明细时回退到采购单单行口径。</p>
 */
@Note("材料计划行状态派生")
@Component
public class MaterialPlanStatusContext {

    private static final Set<PurchaseStatus> ACTIVE = Set.of(
            PurchaseStatus.Submitted, PurchaseStatus.Approved, PurchaseStatus.Ordered,
            PurchaseStatus.PartiallyReceived, PurchaseStatus.Received);

    private final PurchaseOrderRepository purchaseRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;

    public MaterialPlanStatusContext(PurchaseOrderRepository purchaseRepository,
                                     PurchaseOrderItemRepository purchaseItemRepository) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
    }

    @Transactional(readOnly = true)
    public MaterialPlanStatus resolve(MaterialPlan plan) {
        BigDecimal need = value(plan.getQuantity());
        if (need.signum() <= 0) {
            return MaterialPlanStatus.Cancelled;
        }
        List<PurchaseOrderItem> items = purchaseItemRepository
                .findByMaterialPlanOrderByPurchaseItemIdAsc(plan);
        BigDecimal ordered;
        BigDecimal received;
        if (!items.isEmpty()) {
            ordered = items.stream()
                    .filter(item -> item.getStatus() != null && ACTIVE.contains(item.getStatus()))
                    .map(item -> value(item.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            received = items.stream()
                    .map(item -> value(item.getReceivedQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            List<PurchaseOrder> orders = purchaseRepository
                    .findByMaterialPlanOrderByPurchaseIdAsc(plan);
            ordered = orders.stream()
                    .filter(order -> order.getStatus() != null && ACTIVE.contains(order.getStatus()))
                    .map(order -> value(order.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            received = orders.stream()
                    .map(order -> value(order.getReceivedQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (received.compareTo(need) >= 0) {
            return MaterialPlanStatus.Arrived;
        }
        if (received.signum() > 0) {
            return MaterialPlanStatus.PartiallyArrived;
        }
        if (ordered.signum() <= 0) {
            return MaterialPlanStatus.ReadyToOrder;
        }
        return ordered.compareTo(need) >= 0 ? MaterialPlanStatus.Ordered : MaterialPlanStatus.PartiallyOrdered;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
