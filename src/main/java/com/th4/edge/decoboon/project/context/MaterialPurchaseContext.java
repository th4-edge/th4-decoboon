package com.th4.edge.decoboon.project.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.PartnerCreditContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 材料采购确认、数量门禁与应付联动。
 *
 * <p>2026-09-23 业务裁决：确认下单即形成应付，到货只确认实际成本和回填账期。
 * 采购确认与应付生成必须处于同一事务，避免出现“已审批但无应付”的中间态。</p>
 */
@Note("材料采购确认与应付联动")
@Component
public class MaterialPurchaseContext {
    /** 收付/单据归属统一写入（`DEC-070`：应付三路径同源）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.finance.context.FinanceAccessContext financeAccessContext;


    /** 门店归属唯一写入解析器（`DEC-070`）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.common.context.StoreAttributionContext storeAttributionContext;


    public static final String PRICE_SOURCE_MANUAL = "手工录入";

    private final PurchaseOrderRepository purchaseRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final MaterialPlanStatusContext materialPlanStatusContext;
    private final PayableRepository payableRepository;
    private final ModelIdentityContext modelIdentityContext;
    private final PartnerCreditContext partnerCreditContext;

    public MaterialPurchaseContext(PurchaseOrderRepository purchaseRepository,
                                   PurchaseOrderItemRepository purchaseItemRepository,
                                   MaterialPlanRepository materialPlanRepository,
                                   MaterialPlanStatusContext materialPlanStatusContext,
                                   PayableRepository payableRepository,
                                   ModelIdentityContext modelIdentityContext,
                                   PartnerCreditContext partnerCreditContext) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.materialPlanStatusContext = materialPlanStatusContext;
        this.payableRepository = payableRepository;
        this.modelIdentityContext = modelIdentityContext;
        this.partnerCreditContext = partnerCreditContext;
    }

    /** 供应商决定权归财务、店长和企业负责人，项目经理只参与履约，不负责锁供应商。 */
    public void requireOperator(Employee current) {
        if (current == null || current.getRole() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "请先登录"));
        }
        EmployeeRole role = current.getRole();
        if (role != EmployeeRole.Boss
                && role != EmployeeRole.StoreManager
                && role != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人、店长或财务可以确认采购下单"));
        }
    }

    /** 下单量不得超过净用量 + 损耗量。 */
    public void requireOrderQuantity(MaterialPlan plan, BigDecimal quantity) {
        if (plan == null) {
            throw new DefinedException(new DefaultErrorCode("404", "材料计划不存在或无权访问"));
        }
        BigDecimal value = value(quantity);
        if (value.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "采购数量必须大于 0"));
        }
        BigDecimal remaining = remainingQuantity(plan);
        if (value.compareTo(remaining) > 0) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "本次下单量超过可下单数量 " + remaining.stripTrailingZeros().toPlainString()
                            + "；如需多备料，请先调整主材分类损耗率或损耗量"));
        }
    }

    public BigDecimal remainingQuantity(MaterialPlan plan) {
        BigDecimal demand = value(plan == null ? null : plan.getQuantity());
        BigDecimal ordered = materialPlanStatusContext.orderedQuantity(plan);
        return demand.subtract(ordered).max(BigDecimal.ZERO);
    }

    /**
     * 采购执行价只作为应付事实的输入；供应商旧价格模型已退场，不再推断供应关系价或目录合作价。
     */
    public String resolvePriceSource(MaterialPlan plan, Partner supplier, BigDecimal executionPrice) {
        return PRICE_SOURCE_MANUAL;
    }

    /** 确认采购下单：采购单、计划状态、材料应付和供应商往来抵扣在同一事务完成。 */
    @Transactional
    public PurchaseOrder confirmOrder(PurchaseOrder purchase, Employee operator) {
        requireOperator(operator);
        if (purchase == null || purchase.getPurchaseId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "采购单不存在"));
        }
        if (purchase.getSupplier() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "采购单缺少供应商，不能形成应付"));
        }
        purchase.setStatus(PurchaseStatus.Ordered);
        if (purchase.getOrderedAt() == null) {
            purchase.setOrderedAt(LocalDate.now());
        }
        List<PurchaseOrderItem> items = purchaseItemRepository
                .findByPurchaseOrderOrderByPurchaseItemIdAsc(purchase);
        if (items.isEmpty()) {
            MaterialPlan plan = purchase.getMaterialPlan();
            if (plan != null) {
                plan.setStatus(materialPlanStatusContext.resolve(plan));
                materialPlanRepository.save(plan);
            }
        } else {
            for (PurchaseOrderItem item : items) {
                item.setStatus(PurchaseStatus.Ordered);
                purchaseItemRepository.save(item);
                MaterialPlan plan = item.getMaterialPlan();
                if (plan != null) {
                    plan.setStatus(materialPlanStatusContext.resolve(plan));
                    materialPlanRepository.save(plan);
                }
            }
        }
        PurchaseOrder saved = purchaseRepository.save(purchase);
        ensurePayable(saved, operator);
        applyCredits(saved);
        return saved;
    }

    private void ensurePayable(PurchaseOrder purchase, Employee operator) {
        Long comId = purchase.getCompany().getComId();
        if (payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(comId, purchase.getPurchaseId())
                .isPresent()) {
            return;
        }
        if (payableRepository
                .findFirstByCompany_ComIdAndSourceNoAndType(comId, purchase.getNo(), PayableType.Material)
                .isPresent()) {
            return;
        }
        Payable payable = new Payable();
        payable.setCompany(purchase.getCompany());
        payable.setProperty(purchase.getProperty());
        payable.setProject(purchase.getProject());
        payable.setMaterialPlan(purchase.getMaterialPlan());
        payable.setPurchaseOrder(purchase);
        payable.setOwner(resolveOwner(purchase, operator));
        // DEC-070：应付门店统一走财务域同一方法（客户门店快照；采购/到货/手工三路径同源）
        financeAccessContext.applyPayableStore(payable,
                purchase.getProject() == null ? null : purchase.getProject().getCustomer(),
                purchase.getOwner() == null ? operator : purchase.getOwner(),
                purchase.getStore());
        payable.setTeam(purchase.getTeam());
        payable.setName("材料采购 · " + purchase.getSupplierName());
        payable.setType(PayableType.Material);
        payable.setPayeePartner(purchase.getSupplier());
        payable.setPayeeName(purchase.getSupplierName());
        // DEC-072 第 1 项：无价/混合价下单形成**暂估应付**（金额待定，结算时修正留痕）。
        // 既有 amount 为 NOT NULL，暂估期 amount=0 且 estimated=true；禁止把 0 当作真实价参与成本与利润。
        boolean estimated = purchase.getPriceState() == SheetPriceState.Unpriced
                || purchase.getPriceState() == SheetPriceState.Mixed;
        payable.setAmount(value(purchase.getAmount()));
        payable.setEstimated(estimated);
        payable.setSettledAmount(null);
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setReceiptDate(null);
        payable.setSettlementMode(purchase.getSupplier().getSettlementMode());
        payable.setDueDate(null);
        payable.setStatus(PayableStatus.Pending);
        payable.setSourceNo(purchase.getNo());
        payable.setRemark(estimated ? "确认下单自动形成暂估应付（含未定价行，结算后修正）" : "确认下单自动形成应付");
        payable.setCreatedBy(operator);
        payableRepository.save(modelIdentityContext.prepare(payable));
    }

    private void applyCredits(PurchaseOrder purchase) {
        payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(
                        purchase.getCompany().getComId(), purchase.getPurchaseId())
                .ifPresent(partnerCreditContext::applyTo);
    }

    private Employee resolveOwner(PurchaseOrder purchase, Employee fallback) {
        if (purchase.getOwner() != null) {
            return purchase.getOwner();
        }
        if (purchase.getProperty() != null && purchase.getProperty().getOwner() != null) {
            return purchase.getProperty().getOwner();
        }
        return fallback;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
