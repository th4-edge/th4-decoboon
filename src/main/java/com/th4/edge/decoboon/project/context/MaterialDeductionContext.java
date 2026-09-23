package com.th4.edge.decoboon.project.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.PartnerCredit;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PartnerCreditRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialReturnStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.MaterialReturn;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.MaterialReturnRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.supply.context.InventoryContext;
import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.decoboon.supply.repository.InventoryStockRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 材料扣减与退货冲减。
 *
 * <p>按 DEC-MP-02 分三段处理，绝不直接修改应付而不留退货记录：</p>
 * <ol>
 *   <li>已到货 → 生成退货单，冲减应付与已确认成本；</li>
 *   <li>已下单未到货 → 调整采购明细数量与金额，按差额下调应付；</li>
 *   <li>未下单 → 直接冲减计划数量（先冲损耗量，再冲净用量）。</li>
 * </ol>
 * <p>退货造成的不可冲减余款：先冲抵同一供应商其他未结清应付，仍有余款转入供应商往来余额。</p>
 */
@Note("材料扣减与退货冲减")
@Component
public class MaterialDeductionContext {

    private final MaterialReturnRepository materialReturnRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final PayableRepository payableRepository;
    private final PartnerCreditRepository partnerCreditRepository;
    private final MaterialPlanStatusContext materialPlanStatusContext;
    private final ProjectPermissionContext permissionContext;
    private final ModelIdentityContext modelIdentityContext;
    private final InventoryContext inventoryContext;
    private final InventoryStockRepository inventoryStockRepository;

    public MaterialDeductionContext(MaterialReturnRepository materialReturnRepository,
                                   PurchaseOrderRepository purchaseRepository,
                                   PurchaseOrderItemRepository purchaseItemRepository,
                                   MaterialPlanRepository materialPlanRepository,
                                   PayableRepository payableRepository,
                                   PartnerCreditRepository partnerCreditRepository,
                                   MaterialPlanStatusContext materialPlanStatusContext,
                                   ProjectPermissionContext permissionContext,
                                   ModelIdentityContext modelIdentityContext,
                                   InventoryContext inventoryContext,
                                   InventoryStockRepository inventoryStockRepository) {
        this.materialReturnRepository = materialReturnRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.payableRepository = payableRepository;
        this.partnerCreditRepository = partnerCreditRepository;
        this.materialPlanStatusContext = materialPlanStatusContext;
        this.permissionContext = permissionContext;
        this.modelIdentityContext = modelIdentityContext;
        this.inventoryContext = inventoryContext;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    @Transactional
    public List<MaterialReturn> reduce(MaterialPlan plan, BigDecimal reduceQuantity, String reason, Employee operator) {
        List<MaterialReturn> returns = new ArrayList<>();
        BigDecimal remaining = value(reduceQuantity);
        if (plan == null || remaining.signum() <= 0) {
            return returns;
        }
        List<PurchaseOrderItem> items = purchaseItemRepository
                .findByMaterialPlanOrderByPurchaseItemIdAsc(plan);
        if (!items.isEmpty()) {
            remaining = reduceByItems(plan, items, remaining, reason, operator, returns);
        } else {
            remaining = reduceByOrders(plan, remaining, reason, operator, returns);
        }
        if (remaining.signum() > 0) {
            cutPlanQuantity(plan, remaining);
        }
        plan.setStatus(materialPlanStatusContext.resolve(plan));
        materialPlanRepository.save(plan);
        return returns;
    }

    /** 采购明细口径：支持按供应商合并下单与一条材料拆多供应商。 */
    private BigDecimal reduceByItems(MaterialPlan plan, List<PurchaseOrderItem> items, BigDecimal reduceQuantity,
                                     String reason, Employee operator, List<MaterialReturn> returns) {
        BigDecimal remaining = reduceQuantity;
        for (PurchaseOrderItem item : items) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (!active(item.getStatus())) {
                continue;
            }
            BigDecimal received = value(item.getReceivedQuantity());
            if (received.signum() <= 0) {
                continue;
            }
            BigDecimal cut = received.min(remaining);
            returns.add(createReturn(plan, item.getPurchaseOrder(), item, cut, reason, operator));
            remaining = remaining.subtract(cut);
        }
        for (PurchaseOrderItem item : items) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (!active(item.getStatus())) {
                continue;
            }
            BigDecimal pending = value(item.getQuantity()).subtract(value(item.getReceivedQuantity()));
            if (pending.signum() <= 0) {
                continue;
            }
            BigDecimal cut = pending.min(remaining);
            BigDecimal unitPrice = value(item.getUnitPrice());
            BigDecimal newQuantity = value(item.getQuantity()).subtract(cut);
            item.setQuantity(newQuantity);
            item.setAmount(newQuantity.multiply(unitPrice));
            if (newQuantity.signum() <= 0) {
                item.setStatus(PurchaseStatus.Cancelled);
            }
            purchaseItemRepository.save(item);
            reducePayable(item.getPurchaseOrder(), cut.multiply(unitPrice), reason);
            recomputeHeader(item.getPurchaseOrder());
            remaining = remaining.subtract(cut);
        }
        return remaining;
    }

    /** 兼容口径：无采购明细时按采购单单行处理。 */
    private BigDecimal reduceByOrders(MaterialPlan plan, BigDecimal reduceQuantity,
                                      String reason, Employee operator, List<MaterialReturn> returns) {
        BigDecimal remaining = reduceQuantity;
        List<PurchaseOrder> purchases = purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan).stream()
                .filter(order -> active(order.getStatus()))
                .toList();
        for (PurchaseOrder purchase : purchases) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal received = value(purchase.getReceivedQuantity());
            if (received.signum() <= 0) {
                continue;
            }
            BigDecimal cut = received.min(remaining);
            returns.add(createReturn(plan, purchase, null, cut, reason, operator));
            remaining = remaining.subtract(cut);
        }
        for (PurchaseOrder purchase : purchases) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal pending = value(purchase.getQuantity()).subtract(value(purchase.getReceivedQuantity()));
            if (pending.signum() <= 0) {
                continue;
            }
            BigDecimal cut = pending.min(remaining);
            BigDecimal unitPrice = value(purchase.getUnitPrice());
            BigDecimal newQuantity = value(purchase.getQuantity()).subtract(cut);
            purchase.setQuantity(newQuantity);
            purchase.setAmount(newQuantity.multiply(unitPrice));
            if (newQuantity.signum() <= 0) {
                purchase.setStatus(PurchaseStatus.Cancelled);
            }
            purchaseRepository.save(purchase);
            reducePayable(purchase, cut.multiply(unitPrice), reason);
            remaining = remaining.subtract(cut);
        }
        return remaining;
    }

    private MaterialReturn createReturn(MaterialPlan plan, PurchaseOrder purchase, PurchaseOrderItem item,
                                        BigDecimal quantity, String reason, Employee operator) {
        BigDecimal unitPrice = item != null ? value(item.getUnitPrice()) : value(purchase.getUnitPrice());
        BigDecimal amount = quantity.multiply(unitPrice);
        MaterialReturn entity = new MaterialReturn();
        entity.setCompany(plan.getCompany());
        entity.setProperty(plan.getProperty());
        entity.setProject(plan.getProject());
        permissionContext.apply(entity, plan.getProject());
        entity.setMaterialPlan(plan);
        entity.setPurchaseOrder(purchase);
        entity.setSupplier(purchase.getSupplier());
        entity.setNo("MR" + System.currentTimeMillis());
        entity.setQuantity(quantity);
        entity.setUnit(plan.getUnit());
        entity.setUnitPrice(unitPrice);
        entity.setAmount(amount);
        entity.setReturnedAt(LocalDate.now());
        entity.setReason(reason);
        entity.setCreatedBy(operator);
        entity.setStatus(MaterialReturnStatus.Confirmed);
        entity = materialReturnRepository.save(modelIdentityContext.prepare(entity));

        // 到场数量回退（明细优先，回退后重算采购单头）
        if (item != null) {
            item.setReceivedQuantity(value(item.getReceivedQuantity()).subtract(quantity).max(BigDecimal.ZERO));
            item.setStatus(item.getReceivedQuantity().signum() <= 0
                    ? PurchaseStatus.Ordered
                    : (item.getReceivedQuantity().compareTo(value(item.getQuantity())) >= 0
                            ? PurchaseStatus.Received : PurchaseStatus.PartiallyReceived));
            purchaseItemRepository.save(item);
            recomputeHeader(purchase);
        } else {
            BigDecimal newReceived = value(purchase.getReceivedQuantity()).subtract(quantity).max(BigDecimal.ZERO);
            purchase.setReceivedQuantity(newReceived);
            purchase.setStatus(newReceived.signum() <= 0
                    ? PurchaseStatus.Ordered
                    : (newReceived.compareTo(value(purchase.getQuantity())) >= 0
                            ? PurchaseStatus.Received : PurchaseStatus.PartiallyReceived));
            purchaseRepository.save(purchase);
        }

        // 计划实际成本回冲
        plan.setCost(value(plan.getCost()).subtract(amount).max(BigDecimal.ZERO));

        // 入仓材料退货统一入口：同步库存退回（直送工地的材料不涉及库存）
        postInventoryReturn(purchase, plan, quantity, operator, entity.getNo(), reason);

        // 应付冲减：先冲减本采购单应付，再把不可冲减部分用于冲抵同供应商其他应付
        OffsetResult result = reducePayable(purchase, amount, reason);
        if (!result.payableFound()) {
            entity.setStatus(MaterialReturnStatus.Pending);
            entity.setRemark("未找到对应应付，退货已登记待财务核对");
            return materialReturnRepository.save(entity);
        }
        BigDecimal unoffset = result.residual();
        if (unoffset.signum() > 0) {
            unoffset = offsetOtherPayables(purchase, unoffset, reason);
        }
        if (unoffset.signum() > 0) {
            createPartnerCredit(purchase, unoffset, entity.getNo(), operator, reason);
            entity.setRemark("本单应付无可冲减余额，已转供应商往来余额 ¥" + unoffset.toPlainString());
        }
        entity.setStatus(MaterialReturnStatus.Confirmed);
        return materialReturnRepository.save(entity);
    }

    /**
     * 入仓材料退货：按库存批次 FIFO 退回，保持退货单与库存流水一致；
     * 直送工地的材料未进库存，不产生库存流水。
     */
    private void postInventoryReturn(PurchaseOrder purchase, MaterialPlan plan, BigDecimal quantity,
                                     Employee operator, String sourceNo, String reason) {
        if (purchase.getDeliveryMode() == null || !purchase.getDeliveryMode().maintainsInventory()
                || plan.getCatalogItem() == null || plan.getCatalogItem().getItemId() == null
                || plan.getCompany() == null) {
            return;
        }
        List<InventoryStock> stocks = inventoryStockRepository
                .findByCompany_ComIdAndItemOrderByStockIdAsc(
                        plan.getCompany().getComId(), plan.getCatalogItem());
        BigDecimal remaining = quantity;
        for (InventoryStock stock : stocks) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal available = inventoryContext.available(stock);
            if (available.signum() <= 0) {
                continue;
            }
            BigDecimal cut = available.min(remaining);
            inventoryContext.supplierReturn(stock, plan.getProject(), plan.getQuoteItem(), plan, cut,
                    operator, sourceNo, null, reason);
            remaining = remaining.subtract(cut);
        }
    }

    /** 按差额下调应付；返回是否找到应付以及无法冲减的金额（已付款部分）。 */
    private OffsetResult reducePayable(PurchaseOrder purchase, BigDecimal reduceAmount, String reason) {
        if (reduceAmount.signum() <= 0 || purchase == null || purchase.getCompany() == null) {
            return new OffsetResult(false, BigDecimal.ZERO);
        }
        Long comId = purchase.getCompany().getComId();
        Optional<Payable> found = payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(comId, purchase.getPurchaseId());
        if (found.isEmpty()) {
            found = payableRepository.findFirstByCompany_ComIdAndSourceNoAndType(
                    comId, purchase.getNo(), PayableType.Material);
        }
        if (found.isEmpty()) {
            return new OffsetResult(false, BigDecimal.ZERO);
        }
        Payable payable = found.get();
        BigDecimal reducible = value(payable.getAmount()).subtract(value(payable.getPaidAmount()));
        if (reducible.signum() <= 0) {
            return new OffsetResult(true, reduceAmount);
        }
        BigDecimal cut = reducible.min(reduceAmount);
        payable.setAmount(value(payable.getAmount()).subtract(cut));
        if (value(payable.getAmount()).compareTo(value(payable.getPaidAmount())) <= 0) {
            payable.setStatus(PayableStatus.Paid);
        }
        payable.setRemark(append(payable.getRemark(), "扣减冲减 ¥" + cut.toPlainString()
                + (reason == null || reason.isBlank() ? "" : "（" + reason + "）")));
        payableRepository.save(payable);
        return new OffsetResult(true, reduceAmount.subtract(cut));
    }

    /** 把退货余款冲抵同一供应商其他未结清应付（按到期日先后）。 */
    private BigDecimal offsetOtherPayables(PurchaseOrder purchase, BigDecimal residual, String reason) {
        if (purchase.getSupplier() == null || purchase.getSupplier().getPartnerId() == null) {
            return residual;
        }
        Long comId = purchase.getCompany().getComId();
        List<Payable> others = payableRepository
                .findByCompany_ComIdAndPayeePartner_PartnerIdOrderByDueDateAsc(
                        comId, purchase.getSupplier().getPartnerId());
        BigDecimal remaining = residual;
        for (Payable other : others) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (isSamePurchase(other, purchase) || other.getStatus() == PayableStatus.Closed) {
                continue;
            }
            BigDecimal reducible = value(other.getAmount()).subtract(value(other.getPaidAmount()));
            if (reducible.signum() <= 0) {
                continue;
            }
            BigDecimal cut = reducible.min(remaining);
            other.setAmount(value(other.getAmount()).subtract(cut));
            if (value(other.getAmount()).compareTo(value(other.getPaidAmount())) <= 0) {
                other.setStatus(PayableStatus.Paid);
            }
            other.setRemark(append(other.getRemark(), "退货冲抵 ¥" + cut.toPlainString()
                    + (reason == null || reason.isBlank() ? "" : "（" + reason + "）")));
            payableRepository.save(other);
            remaining = remaining.subtract(cut);
        }
        return remaining;
    }

    private boolean isSamePurchase(Payable payable, PurchaseOrder purchase) {
        if (payable.getPurchaseOrder() != null && payable.getPurchaseOrder().getPurchaseId() != null
                && payable.getPurchaseOrder().getPurchaseId().equals(purchase.getPurchaseId())) {
            return true;
        }
        return payable.getSourceNo() != null && payable.getSourceNo().equals(purchase.getNo());
    }

    /** 形成供应商往来余额，供后续采购或付款抵扣。 */
    private void createPartnerCredit(PurchaseOrder purchase, BigDecimal amount, String sourceNo,
                                     Employee operator, String reason) {
        PartnerCredit credit = new PartnerCredit();
        credit.setCompany(purchase.getCompany());
        credit.setPartner(purchase.getSupplier());
        credit.setAmount(amount);
        credit.setSourceNo(sourceNo);
        credit.setEffectiveDate(LocalDate.now());
        credit.setRemark(append("退货冲减形成供应商往来余额", reason));
        credit.setCreatedBy(operator);
        partnerCreditRepository.save(modelIdentityContext.prepare(credit));
    }

    /** 采购单头按明细汇总重算（数量、金额、到场、状态）。 */
    private void recomputeHeader(PurchaseOrder purchase) {
        if (purchase == null) {
            return;
        }
        List<PurchaseOrderItem> rows = purchaseItemRepository
                .findByPurchaseOrderOrderByPurchaseItemIdAsc(purchase);
        if (rows.isEmpty()) {
            return;
        }
        BigDecimal quantity = rows.stream().map(row -> value(row.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = rows.stream().map(row -> value(row.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = rows.stream().map(row -> value(row.getReceivedQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        purchase.setQuantity(quantity);
        purchase.setAmount(amount);
        purchase.setReceivedQuantity(received);
        if (quantity.signum() <= 0) {
            purchase.setStatus(PurchaseStatus.Cancelled);
        } else if (received.compareTo(quantity) >= 0) {
            purchase.setStatus(PurchaseStatus.Received);
        } else if (received.signum() > 0) {
            purchase.setStatus(PurchaseStatus.PartiallyReceived);
        } else {
            purchase.setStatus(PurchaseStatus.Ordered);
        }
        purchaseRepository.save(purchase);
    }

    private void cutPlanQuantity(MaterialPlan plan, BigDecimal amount) {
        BigDecimal remaining = amount;
        BigDecimal loss = value(plan.getLossQuantity());
        BigDecimal lossCut = loss.min(remaining);
        loss = loss.subtract(lossCut);
        remaining = remaining.subtract(lossCut);
        BigDecimal net = value(plan.getNetQuantity());
        BigDecimal netCut = net.min(remaining);
        net = net.subtract(netCut);
        plan.setLossQuantity(loss);
        plan.setNetQuantity(net);
        plan.setQuantity(net.add(loss));
    }

    private boolean active(PurchaseStatus status) {
        return status != null && status != PurchaseStatus.Cancelled && status != PurchaseStatus.Rejected;
    }

    private String append(String origin, String extra) {
        if (origin == null || origin.isBlank()) {
            return extra;
        }
        String merged = origin + "；" + extra;
        return merged.length() > 500 ? merged.substring(0, 500) : merged;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 内部分段结果：是否命中应付 + 未冲减余额。 */
    private record OffsetResult(boolean payableFound, BigDecimal residual) {
    }
}
