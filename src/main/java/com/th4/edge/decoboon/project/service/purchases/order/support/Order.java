package com.th4.edge.decoboon.project.service.purchases.order.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.context.PartnerCreditContext;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.context.MaterialPlanStatusContext;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购单下单。
 *
 * <p>DEC-MP-01：确认下单即形成应付（`receiptDate/dueDate` 暂空，到场确认时回填账期）；
 * 应付 ≠ 成本，实际材料成本仍在到场/入库时确认。</p>
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
    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectAccessContext accessContext;
    private final PayableRepository payableRepository;
    private final ModelIdentityContext modelIdentityContext;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final MaterialPlanStatusContext materialPlanStatusContext;
    private final PartnerCreditContext partnerCreditContext;

    public Order(PurchaseOrderRepository purchaseRepository,
                 MaterialPlanRepository materialPlanRepository,
                 ProjectAccessContext accessContext,
                 PayableRepository payableRepository,
                 ModelIdentityContext modelIdentityContext,
                 PurchaseOrderItemRepository purchaseItemRepository,
                 MaterialPlanStatusContext materialPlanStatusContext,
                 PartnerCreditContext partnerCreditContext) {
        this.purchaseRepository = purchaseRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.accessContext = accessContext;
        this.payableRepository = payableRepository;
        this.modelIdentityContext = modelIdentityContext;
        this.purchaseItemRepository = purchaseItemRepository;
        this.materialPlanStatusContext = materialPlanStatusContext;
        this.partnerCreditContext = partnerCreditContext;
    }

    @Override
    @Transactional
    public PurchaseOrder call(PurchaseOrder request) {
        Employee current = accessContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Finance
                && current.getRole() != EmployeeRole.Pm) {
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
        // 明细行同步下单；计划行状态按明细/采购实时派生
        java.util.List<PurchaseOrderItem> items =
                purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(purchase);
        if (items.isEmpty()) {
            purchase.getMaterialPlan().setStatus(materialPlanStatusContext.resolve(purchase.getMaterialPlan()));
            materialPlanRepository.save(purchase.getMaterialPlan());
        } else {
            for (PurchaseOrderItem item : items) {
                item.setStatus(PurchaseStatus.Ordered);
                purchaseItemRepository.save(item);
                item.getMaterialPlan().setStatus(materialPlanStatusContext.resolve(item.getMaterialPlan()));
                materialPlanRepository.save(item.getMaterialPlan());
            }
        }
        PurchaseOrder saved = purchaseRepository.save(purchase);
        ensurePayable(saved, current);
        // 供应商往来余额自动抵扣新形成的应付
        applyCredits(saved);
        return saved;
    }

    /** 确认下单形成应付；同一采购单重复下单幂等，不重复生成。 */
    private void applyCredits(PurchaseOrder purchase) {
        payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(
                        purchase.getCompany().getComId(), purchase.getPurchaseId())
                .ifPresent(partnerCreditContext::applyTo);
    }

    private void ensurePayable(PurchaseOrder purchase, Employee current) {
        if (purchase.getSupplier() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "采购单缺少供应商，不能形成应付"));
        }
        Long comId = purchase.getCompany().getComId();
        if (payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(comId, purchase.getPurchaseId())
                .isPresent()) {
            return;
        }
        // 兼容历史数据：早期材料应付只写来源单号，避免重复生成
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
        payable.setOwner(resolveOwner(purchase, current));
        payable.setStore(purchase.getStore());
        payable.setTeam(purchase.getTeam());
        payable.setName("材料采购 · " + purchase.getSupplierName());
        payable.setType(PayableType.Material);
        payable.setPayeePartner(purchase.getSupplier());
        payable.setPayeeName(purchase.getSupplierName());
        payable.setAmount(value(purchase.getAmount()));
        payable.setPaidAmount(BigDecimal.ZERO);
        // 下单即形成应付；账期起算日与实际到期日到场确认时回填
        payable.setReceiptDate(null);
        payable.setSettlementMode(purchase.getSupplier().getSettlementMode());
        payable.setDueDate(null);
        payable.setStatus(PayableStatus.Pending);
        payable.setSourceNo(purchase.getNo());
        payable.setRemark("确认下单自动形成应付");
        payable.setCreatedBy(current);
        payableRepository.save(modelIdentityContext.prepare(payable));
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
