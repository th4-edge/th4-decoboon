package com.th4.edge.decoboon.material.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.material.configure.SettlementScope;
import com.th4.edge.decoboon.material.configure.SettlementStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.decoboon.material.model.MaterialSettlementItem;
import com.th4.edge.decoboon.material.repository.MaterialSettlementItemRepository;
import com.th4.edge.decoboon.material.repository.MaterialSettlementRepository;
import com.th4.edge.decoboon.project.context.MaterialPurchaseContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主材采购结算（P3）：结算价回填 → 暂估应付修正 → 实际成本收敛。
 *
 * <p><b>结算规则（DEC-073）</b>：逐单结算与月结**并存**；同一条采购明细**只允许被一种方式计入一次**（互斥，数据库唯一约束兜底）；
 * 逐单优先（能确认单张采购单最终价时先逐单结算）；月结兜底（期末只结算**尚未逐单结算**的明细）。</p>
 *
 * <p><b>计价口径（BR-045）</b>：结算价是实际成本的唯一来源；结算价**不沉淀**为供应商参考价（DEC-072 第 5 项），
 * 也不回溯改写报价与合同快照。</p>
 */
@Component
public class MaterialSettlementContext {

    private final MaterialSettlementRepository settlementRepository;
    private final MaterialSettlementItemRepository settlementItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final PayableRepository payableRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final PartnerRepository partnerRepository;
    private final ProjectAccessContext accessContext;
    private final MaterialPurchaseContext materialPurchaseContext;
    private final ModelIdentityContext modelIdentityContext;

    public MaterialSettlementContext(MaterialSettlementRepository settlementRepository,
                                     MaterialSettlementItemRepository settlementItemRepository,
                                     PurchaseOrderRepository purchaseOrderRepository,
                                     PurchaseOrderItemRepository purchaseItemRepository,
                                     PayableRepository payableRepository,
                                     QuoteLineRepository quoteLineRepository,
                                     PartnerRepository partnerRepository,
                                     ProjectAccessContext accessContext,
                                     MaterialPurchaseContext materialPurchaseContext,
                                     ModelIdentityContext modelIdentityContext) {
        this.settlementRepository = settlementRepository;
        this.settlementItemRepository = settlementItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.payableRepository = payableRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.partnerRepository = partnerRepository;
        this.accessContext = accessContext;
        this.materialPurchaseContext = materialPurchaseContext;
        this.modelIdentityContext = modelIdentityContext;
    }

    public List<MaterialSettlement> page(Employee principal, Long supplierId) {
        Employee current = accessContext.requireEmployee(principal);
        Long comId = current.getCompany().getComId();
        return supplierId == null
                ? settlementRepository.findByCompany_ComIdOrderBySettlementIdDesc(comId)
                : settlementRepository.findByCompany_ComIdAndSupplier_PartnerIdOrderBySettlementIdDesc(comId, supplierId);
    }

    public MaterialSettlement info(Employee principal, Long settlementId) {
        Employee current = accessContext.requireEmployee(principal);
        MaterialSettlement settlement = requireSettlement(current, settlementId);
        settlement.setItems(settlementItemRepository.findBySettlement_SettlementIdOrderBySettlementItemIdAsc(settlementId));
        return settlement;
    }

    /**
     * 结算预演：列出该供应商在本方式下**可结算**的采购明细（已结算明细自动剔除，体现互斥与月结兜底）。
     */
    public List<com.th4.edge.decoboon.material.model.SettlementPreviewRow> preview(
            Employee principal, Long supplierId, SettlementScope scope, String period, Long purchaseOrderId,
            Long projectId) {
        Employee current = accessContext.requireEmployee(principal);
        materialPurchaseContext.requireOperator(current);
        return candidates(current, supplierId, scope, period, purchaseOrderId, projectId).stream()
                .map(this::toCandidate)
                .toList();
    }

    /** 兼容旧签名（无项目过滤）。 */
    public List<com.th4.edge.decoboon.material.model.SettlementPreviewRow> preview(
            Employee principal, Long supplierId, SettlementScope scope, String period, Long purchaseOrderId) {
        return preview(principal, supplierId, scope, period, purchaseOrderId, null);
    }

    /**
     * 确认结算：回填结算价 → 修正暂估应付 → 把差额补记到实际成本。
     *
     * @param lines 至少给出采购明细与结算价（数量缺省取采购数量）
     */
    public MaterialSettlement confirm(Employee principal, Long supplierId, SettlementScope scope, String period,
                                      List<MaterialSettlementItem> lines, String remark) {
        Employee current = accessContext.requireEmployee(principal);
        materialPurchaseContext.requireOperator(current);
        if (lines == null || lines.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要结算的采购明细"));
        }
        Long comId = current.getCompany().getComId();
        Partner supplier = requireSupplier(current, supplierId);
        List<Long> purchaseItemIds = lines.stream()
                .map(line -> line.getPurchaseItem() == null ? null : line.getPurchaseItem().getPurchaseItemId())
                .toList();
        if (purchaseItemIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new DefinedException(new DefaultErrorCode("400", "结算明细缺少采购明细"));
        }
        // 互斥：同一采购明细只能被一种结算方式确认一次（数据库唯一约束兜底）
        List<Long> settled = settlementItemRepository.findByPurchaseItem_PurchaseItemIdIn(purchaseItemIds).stream()
                .map(item -> item.getPurchaseItem().getPurchaseItemId())
                .toList();
        if (!settled.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该采购明细已结算，不允许重复计入（结算互斥，DEC-073）：" + settled));
        }
        Map<Long, PurchaseOrderItem> itemsById = new LinkedHashMap<>();
        purchaseItemRepository.findAllById(purchaseItemIds).forEach(item -> itemsById.put(item.getPurchaseItemId(), item));
        for (Long id : purchaseItemIds) {
            if (!itemsById.containsKey(id)) {
                throw new DefinedException(new DefaultErrorCode("404", "采购明细不存在或无权访问：" + id));
            }
        }

        MaterialSettlement settlement = new MaterialSettlement();
        settlement.setCompany(current.getCompany());
        settlement.setNo("ST" + System.currentTimeMillis());
        settlement.setScope(scope == null ? SettlementScope.PerPurchaseOrder : scope);
        settlement.setPeriod(period);
        settlement.setSupplier(supplier);
        settlement.setSettledAt(LocalDate.now());
        settlement.setStatus(SettlementStatus.Confirmed);
        settlement.setRemark(remark);
        settlement.setConfirmer(current);
        settlement.setTotalAmount(BigDecimal.ZERO);
        // 经营归属：逐单结算沿用采购单归属；月结跨单时取首单归属（明细仍各自可追溯）
        PurchaseOrder firstOrder = itemsById.values().stream()
                .map(PurchaseOrderItem::getPurchaseOrder)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (firstOrder != null) {
            settlement.setStore(firstOrder.getStore());
            settlement.setTeam(firstOrder.getTeam());
            settlement.setOwner(firstOrder.getOwner() == null ? current : firstOrder.getOwner());
            settlement.setProject(firstOrder.getProject());
        } else {
            settlement.setOwner(current);
        }
        MaterialSettlement saved = settlementRepository.save(modelIdentityContext.prepare(settlement));

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, BigDecimal> settledAmountByOrder = new LinkedHashMap<>();
        List<MaterialSettlementItem> persistedItems = new ArrayList<>();
        for (MaterialSettlementItem command : lines) {
            PurchaseOrderItem purchaseItem = itemsById.get(command.getPurchaseItem().getPurchaseItemId());
            BigDecimal price = command.getSettlementPrice();
            if (price == null || price.signum() < 0) {
                throw new DefinedException(new DefaultErrorCode("400", "结算价必须大于等于 0"));
            }
            // 数量缺省（null 或 0）时取采购数量：实体默认值为 0，不能把"未填"当成"结算 0 件"。
            BigDecimal quantity = (command.getQuantity() == null || command.getQuantity().signum() <= 0)
                    ? value(purchaseItem.getQuantity()) : command.getQuantity();
            BigDecimal amount = money(quantity.multiply(price));

            MaterialSettlementItem item = new MaterialSettlementItem();
            item.setCompany(current.getCompany());
            item.setSettlement(saved);
            item.setPurchaseItem(purchaseItem);
            item.setQuantity(quantity);
            item.setSettlementPrice(price);
            item.setAmount(amount);
            item.setRemark(command.getRemark());
            persistedItems.add(settlementItemRepository.save(modelIdentityContext.prepare(item)));

            // 回填结算价（实际成本唯一计价来源）
            purchaseItem.setSettlementPrice(price);
            purchaseItem.setPriceState(SheetPriceState.Settled);
            purchaseItemRepository.save(purchaseItem);
            total = total.add(amount);
            PurchaseOrder order = purchaseItem.getPurchaseOrder();
            if (order != null && order.getPurchaseId() != null) {
                settledAmountByOrder.merge(order.getPurchaseId(), amount, BigDecimal::add);
            }
        }

        // 采购单头价格状态 + 应付修正（暂估 → 确定）
        for (Map.Entry<Long, BigDecimal> entry : settledAmountByOrder.entrySet()) {
            Long purchaseId = entry.getKey();
            PurchaseOrder order = purchaseOrderRepository.findById(purchaseId).orElse(null);
            if (order == null) {
                continue;
            }
            List<PurchaseOrderItem> orderItems =
                    purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(order);
            boolean allSettled = orderItems.stream()
                    .allMatch(item -> item.getPriceState() == SheetPriceState.Settled);
            order.setPriceState(allSettled ? SheetPriceState.Settled : SheetPriceState.Mixed);
            purchaseOrderRepository.save(order);
            BigDecimal orderAmount = orderItems.stream()
                    .map(item -> money(value(item.getSettlementPrice() == null
                            ? item.getUnitPrice() : item.getSettlementPrice()).multiply(value(item.getQuantity()))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            settlePayable(current, order, orderAmount, allSettled);
        }

        // 实际成本补记：按「结算价 − 采购价」差额累计（无价采购的采购价为 0，等于全额补记）
        applyActualCostDelta(lines, itemsById);

        saved.setTotalAmount(total);
        saved.setItems(persistedItems);
        return settlementRepository.save(saved);
    }

    /**
     * 撤销结算（本轮开放）。
     *
     * <p><b>口径</b>：撤销是纠错动作——结算头保留（置 `Cancelled` 并记录撤销人/时间/原因），
     * 结算明细删除以**释放"同一采购明细只能结算一次"的唯一约束**，允许重新结算；
     * 同时按原结算价**冲回**采购明细结算价、价格状态、应付与已补记的实际成本，
     * 使链路回到"未结算"状态（成本重新判为不完整）。</p>
     *
     * <p>不覆盖历史事实：原结算单的确认人、确认时间与金额仍保留在结算头上可追溯。</p>
     */
    public MaterialSettlement cancel(Employee principal, Long settlementId, String reason) {
        Employee current = accessContext.requireEmployee(principal);
        materialPurchaseContext.requireOperator(current);
        MaterialSettlement settlement = requireSettlement(current, settlementId);
        if (settlement.getStatus() == SettlementStatus.Cancelled) {
            return settlement;
        }
        List<MaterialSettlementItem> items =
                settlementItemRepository.findBySettlement_SettlementIdOrderBySettlementItemIdAsc(settlementId);
        Map<Long, BigDecimal> deltaByQuoteLine = new LinkedHashMap<>();
        Map<Long, PurchaseOrder> orders = new LinkedHashMap<>();
        for (MaterialSettlementItem item : items) {
            PurchaseOrderItem purchaseItem = item.getPurchaseItem();
            if (purchaseItem == null) {
                continue;
            }
            // 冲回实际成本差额（结算价 − 采购价），方向相反
            if (purchaseItem.getMaterialPlan() != null && purchaseItem.getMaterialPlan().getQuoteItem() != null) {
                Long quoteLineId = purchaseItem.getMaterialPlan().getQuoteItem().getItemId();
                BigDecimal delta = money(value(item.getSettlementPrice()).subtract(value(purchaseItem.getUnitPrice()))
                        .multiply(value(item.getQuantity())));
                deltaByQuoteLine.merge(quoteLineId, delta.negate(), BigDecimal::add);
            }
            purchaseItem.setSettlementPrice(null);
            purchaseItem.setPriceState(purchaseItem.getUnitPrice() != null && purchaseItem.getUnitPrice().signum() > 0
                    ? SheetPriceState.Priced : SheetPriceState.Unpriced);
            purchaseItemRepository.save(purchaseItem);
            PurchaseOrder order = purchaseItem.getPurchaseOrder();
            if (order != null && order.getPurchaseId() != null) {
                orders.put(order.getPurchaseId(), order);
            }
        }
        // 释放互斥约束：删除结算明细（结算头保留审计留痕）
        settlementItemRepository.deleteAll(items);
        for (PurchaseOrder order : orders.values()) {
            List<PurchaseOrderItem> orderItems =
                    purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(order);
            boolean anyUnpriced = orderItems.stream()
                    .anyMatch(item -> item.getPriceState() == SheetPriceState.Unpriced);
            boolean anyUnsettled = orderItems.stream()
                    .anyMatch(item -> item.getPriceState() != SheetPriceState.Settled);
            order.setPriceState(anyUnpriced ? SheetPriceState.Unpriced
                    : (anyUnsettled ? SheetPriceState.Mixed : SheetPriceState.Settled));
            purchaseOrderRepository.save(order);
            // 应付回到暂估：金额回退为采购价口径，estimated 恢复
            settlePayableBackToEstimated(current, order);
        }
        applyCostDeltaByQuoteLine(deltaByQuoteLine);

        settlement.setStatus(SettlementStatus.Cancelled);
        settlement.setCancelledBy(current);
        settlement.setCancelledAt(java.time.LocalDateTime.now());
        settlement.setCancelReason(reason == null || reason.isBlank() ? "撤销结算" : reason.trim());
        settlement.setItems(items);
        return settlementRepository.save(settlement);
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 结算候选集。
     *
     * <p>scope 为空＝**财务工作台用**：返回该企业（可按项目/供应商/期间过滤）全部可结算明细，
     * 财务无需先选供应商或采购单即可看到"哪些明细可以结"。</p>
     */
    private List<PurchaseOrderItem> candidates(Employee current, Long supplierId, SettlementScope scope,
                                               String period, Long purchaseOrderId, Long projectId) {
        Long comId = current.getCompany().getComId();
        List<PurchaseOrder> orders;
        if (scope == SettlementScope.Monthly) {
            if (supplierId == null) {
                throw new DefinedException(new DefaultErrorCode("400", "月结必须指定供应商"));
            }
            orders = purchaseOrderRepository.findByCompany_ComIdAndSupplier_PartnerIdOrderByPurchaseIdDesc(comId, supplierId)
                    .stream()
                    .filter(order -> inPeriod(order, period))
                    .toList();
        } else if (scope == SettlementScope.PerPurchaseOrder) {
            if (purchaseOrderId == null) {
                throw new DefinedException(new DefaultErrorCode("400", "逐单结算必须指定采购单"));
            }
            PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                    .filter(item -> item.getCompany() != null && item.getCompany().getComId().equals(comId))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "采购单不存在或无权访问")));
            orders = List.of(order);
        } else {
            orders = (projectId == null
                    ? purchaseOrderRepository.findByCompany_ComIdOrderByPurchaseIdDesc(comId)
                    : purchaseOrderRepository.findByProject_ProjectIdInOrderByPurchaseIdDesc(List.of(projectId)))
                    .stream()
                    .filter(order -> order.getCompany() != null && order.getCompany().getComId().equals(comId))
                    .filter(order -> supplierId == null || (order.getSupplier() != null
                            && supplierId.equals(order.getSupplier().getPartnerId())))
                    .filter(order -> inPeriod(order, period))
                    .toList();
        }
        if (orders.isEmpty()) {
            return List.of();
        }
        // 一次批量取明细（不逐单查库）
        List<PurchaseOrderItem> items = purchaseItemRepository
                .findByPurchaseOrderInOrderByPurchaseItemIdAsc(orders);
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> ids = items.stream().map(PurchaseOrderItem::getPurchaseItemId).toList();
        List<Long> settled = settlementItemRepository.findByPurchaseItem_PurchaseItemIdIn(ids).stream()
                .map(item -> item.getPurchaseItem().getPurchaseItemId())
                .toList();
        // 月结兜底 + 互斥：已结算明细不再进入可结算集合
        return items.stream().filter(item -> !settled.contains(item.getPurchaseItemId())).toList();
    }

    private boolean inPeriod(PurchaseOrder order, String period) {
        if (period == null || period.isBlank()) {
            return true;
        }
        LocalDate date = order.getOrderedAt() != null ? order.getOrderedAt()
                : (order.getCreateTime() == null || order.getCreateTime().getDate() == null
                        ? null : LocalDate.ofEpochDay(order.getCreateTime().getDate()));
        if (date == null) {
            return true;
        }
        try {
            YearMonth target = YearMonth.parse(period.trim());
            return YearMonth.from(date).equals(target);
        } catch (Exception ignored) {
            return true;
        }
    }

    private com.th4.edge.decoboon.material.model.SettlementPreviewRow toCandidate(PurchaseOrderItem item) {
        PurchaseOrder order = item.getPurchaseOrder();
        boolean unpriced = item.getPriceState() == SheetPriceState.Unpriced;
        com.th4.edge.decoboon.material.model.SettlementPreviewRow row =
                new com.th4.edge.decoboon.material.model.SettlementPreviewRow();
        row.setPurchaseItemId(item.getPurchaseItemId());
        row.setPurchaseOrderId(order == null ? null : order.getPurchaseId());
        row.setPurchaseNo(order == null ? null : order.getNo());
        row.setSupplierId(order == null || order.getSupplier() == null ? null : order.getSupplier().getPartnerId());
        row.setSupplierName(order == null ? null : order.getSupplierName());
        row.setMaterialName(item.getMaterialPlan() == null || item.getMaterialPlan().getCatalogItem() == null
                ? null : item.getMaterialPlan().getCatalogItem().getName());
        row.setQuantity(value(item.getQuantity()));
        row.setReceivedQuantity(value(item.getReceivedQuantity()));
        row.setPurchasePrice(value(item.getUnitPrice()));
        row.setSuggestedSettlementPrice(unpriced ? null : value(item.getUnitPrice()));
        row.setUnpriced(unpriced);
        return row;
    }

    private void settlePayable(Employee current, PurchaseOrder order, BigDecimal amount, boolean fullySettled) {
        Long comId = current.getCompany().getComId();
        Payable payable = payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(comId, order.getPurchaseId())
                .orElse(null);
        if (payable == null) {
            payable = payableRepository.findFirstByCompany_ComIdAndSourceNoAndType(comId, order.getNo(),
                    com.th4.edge.decoboon.finance.configure.PayableType.Material).orElse(null);
        }
        if (payable == null) {
            return;
        }
        // 只有整单明细全部结算，才解除"暂估"；否则保留暂估标记，成本继续判为不完整（BR-045）。
        payable.setAmount(money(amount));
        payable.setSettledAmount(money(amount));
        payable.setEstimated(!fullySettled);
        payable.setRemark(fullySettled
                ? "结算确认：金额以结算价为准（原暂估已修正）"
                : "结算确认：部分明细已结算，仍有未定价明细，应付仍为暂估");
        payableRepository.save(payable);
    }

    /** 应付回退为暂估（撤销结算用）：金额回到采购价口径，estimated 恢复为 true。 */
    private void settlePayableBackToEstimated(Employee current, PurchaseOrder order) {
        Long comId = current.getCompany().getComId();
        Payable payable = payableRepository
                .findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(comId, order.getPurchaseId())
                .orElse(null);
        if (payable == null) {
            return;
        }
        BigDecimal amount = purchaseItemRepository.findByPurchaseOrderOrderByPurchaseItemIdAsc(order).stream()
                .map(item -> money(value(item.getUnitPrice()).multiply(value(item.getQuantity()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        payable.setAmount(money(amount));
        payable.setSettledAmount(null);
        payable.setEstimated(Boolean.TRUE);
        payable.setRemark("结算已撤销：应付回退为暂估，成本重新判为不完整");
        payableRepository.save(payable);
    }

    private void applyCostDeltaByQuoteLine(Map<Long, BigDecimal> deltaByQuoteLine) {
        if (deltaByQuoteLine.isEmpty()) {
            return;
        }
        List<QuoteLine> quoteLines = quoteLineRepository.findAllById(new ArrayList<>(deltaByQuoteLine.keySet()));
        for (QuoteLine line : quoteLines) {
            BigDecimal delta = deltaByQuoteLine.get(line.getItemId());
            if (delta == null || delta.signum() == 0) {
                continue;
            }
            BigDecimal actualMaterial = value(line.getActualMaterialCost()).add(delta).max(BigDecimal.ZERO);
            line.setActualMaterialCost(actualMaterial);
            line.setActualCost(actualMaterial
                    .add(value(line.getActualLaborCost()))
                    .add(value(line.getActualOtherCost())));
            quoteLineRepository.save(line);
        }
    }

    /** 实际成本按结算价重算差额：结算价 − 采购价，差额计入报价行实际材料成本。 */
    private void applyActualCostDelta(List<MaterialSettlementItem> lines, Map<Long, PurchaseOrderItem> itemsById) {
        List<Long> quoteLineIds = new ArrayList<>();
        Map<Long, BigDecimal> deltaByQuoteLine = new LinkedHashMap<>();
        for (MaterialSettlementItem command : lines) {
            PurchaseOrderItem item = itemsById.get(command.getPurchaseItem().getPurchaseItemId());
            if (item.getMaterialPlan() == null || item.getMaterialPlan().getQuoteItem() == null) {
                continue;
            }
            Long quoteLineId = item.getMaterialPlan().getQuoteItem().getItemId();
            // 与结算数量同口径：null 或 0 视为"未填"，取采购数量（实体默认值为 0，不能当成 0 件）
            BigDecimal quantity = (command.getQuantity() == null || command.getQuantity().signum() <= 0)
                    ? value(item.getQuantity()) : command.getQuantity();
            BigDecimal delta = money(value(command.getSettlementPrice()).subtract(value(item.getUnitPrice())).multiply(quantity));
            deltaByQuoteLine.merge(quoteLineId, delta, BigDecimal::add);
            quoteLineIds.add(quoteLineId);
        }
        if (quoteLineIds.isEmpty()) {
            return;
        }
        List<QuoteLine> quoteLines = quoteLineRepository.findAllById(quoteLineIds);
        for (QuoteLine line : quoteLines) {
            BigDecimal delta = deltaByQuoteLine.get(line.getItemId());
            if (delta == null) {
                continue;
            }
            BigDecimal actualMaterial = value(line.getActualMaterialCost()).add(delta).max(BigDecimal.ZERO);
            line.setActualMaterialCost(actualMaterial);
            line.setActualCost(actualMaterial
                    .add(value(line.getActualLaborCost()))
                    .add(value(line.getActualOtherCost())));
            quoteLineRepository.save(line);
        }
    }

    private MaterialSettlement requireSettlement(Employee current, Long settlementId) {
        if (settlementId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少结算单索引"));
        }
        return settlementRepository.findFirstBySettlementIdAndCompany_ComId(settlementId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "结算单不存在或无权访问")));
    }

    private Partner requireSupplier(Employee current, Long supplierId) {
        if (supplierId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少供应商"));
        }
        return partnerRepository.findById(supplierId)
                .filter(item -> item.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "供应商不存在或无权访问")));
    }

    private BigDecimal money(BigDecimal value) {
        return value(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal input) {
        return input == null ? BigDecimal.ZERO : input;
    }
}
