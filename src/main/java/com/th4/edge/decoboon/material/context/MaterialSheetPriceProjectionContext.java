package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetItemRepository;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetRepository;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 作业单价格投影：从采购明细事实聚合行/单价格状态。
 *
 * <p>同一作业单行允许拆给多个供应商，也可能一部分已定价、一部分未定价、一部分已结算。
 * 不能用最后一次下单覆盖行价格状态；读取时按采购明细聚合，写入时同步冗余投影。</p>
 */
@Component
public class MaterialSheetPriceProjectionContext {

    private final MaterialOrderSheetItemRepository itemRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final MaterialOrderSheetRepository sheetRepository;

    public MaterialSheetPriceProjectionContext(MaterialOrderSheetItemRepository itemRepository,
                                               PurchaseOrderItemRepository purchaseItemRepository,
                                               MaterialOrderSheetRepository sheetRepository) {
        this.itemRepository = itemRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.sheetRepository = sheetRepository;
    }

    /** 只读响应：按采购明细批量聚合，不落库。 */
    public void apply(MaterialOrderSheet sheet, List<MaterialOrderSheetItem> lines) {
        enrich(lines);
        if (sheet != null) sheet.setPriceState(stateOf(lines));
    }

    /** 写事务内同步行与单的价格投影。调用方已保存采购/结算事实。 */
    public void persist(List<MaterialOrderSheetItem> lines) {
        if (lines == null || lines.isEmpty()) return;
        purchaseItemRepository.flush();
        enrich(lines);
        itemRepository.saveAll(lines);
        persistSheetStates(lines);
    }

    /** 结算确认/撤销后按材料计划批量刷新作业单投影。 */
    public void syncByPlanIds(Collection<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) return;
        purchaseItemRepository.flush();
        List<MaterialOrderSheetItem> lines = itemRepository.findByMaterialPlan_PlanIdIn(planIds);
        if (lines == null || lines.isEmpty()) return;
        enrich(lines);
        itemRepository.saveAll(lines);
        persistSheetStates(lines);
    }

    private void enrich(List<MaterialOrderSheetItem> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<Long> planIds = lines.stream()
                .filter(Objects::nonNull)
                .map(MaterialOrderSheetItem::getMaterialPlan)
                .filter(Objects::nonNull)
                .map(plan -> plan.getPlanId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (planIds.isEmpty()) {
            lines.forEach(line -> project(line, List.of()));
            return;
        }
        Map<Long, List<PurchaseOrderItem>> itemsByPlan = new LinkedHashMap<>();
        for (PurchaseOrderItem item : purchaseItemRepository.findByMaterialPlan_PlanIdIn(planIds)) {
            if (item == null || item.getMaterialPlan() == null || item.getMaterialPlan().getPlanId() == null
                    || !active(item.getPurchaseOrder())) continue;
            itemsByPlan.computeIfAbsent(item.getMaterialPlan().getPlanId(), key -> new java.util.ArrayList<>()).add(item);
        }
        for (MaterialOrderSheetItem line : lines) {
            Long planId = line.getMaterialPlan() == null ? null : line.getMaterialPlan().getPlanId();
            project(line, itemsByPlan.getOrDefault(planId, List.of()));
        }
    }

    private void project(MaterialOrderSheetItem line, List<PurchaseOrderItem> purchaseItems) {
        line.setPriceState(stateOfItems(purchaseItems));
        line.setPurchasePrice(singlePurchasePrice(purchaseItems));
        line.setSettlementPrice(singleSettlementPrice(purchaseItems));
    }

    private SheetPriceState stateOf(List<MaterialOrderSheetItem> lines) {
        if (lines == null || lines.isEmpty()) return SheetPriceState.Unpriced;
        long priced = lines.stream().filter(line -> line.getPriceState() == SheetPriceState.Priced).count();
        long settled = lines.stream().filter(line -> line.getPriceState() == SheetPriceState.Settled).count();
        long unpriced = lines.stream().filter(line -> line.getPriceState() == SheetPriceState.Unpriced).count();
        long mixed = lines.stream().filter(line -> line.getPriceState() == SheetPriceState.Mixed).count();
        if (mixed > 0 || (priced > 0 && unpriced > 0) || (settled > 0 && (priced > 0 || unpriced > 0))) {
            return SheetPriceState.Mixed;
        }
        if (settled == lines.size()) return SheetPriceState.Settled;
        if (priced == lines.size()) return SheetPriceState.Priced;
        return SheetPriceState.Unpriced;
    }

    private SheetPriceState stateOfItems(List<PurchaseOrderItem> items) {
        if (items == null || items.isEmpty()) return SheetPriceState.Unpriced;
        long priced = items.stream().filter(item -> item.getPriceState() == SheetPriceState.Priced).count();
        long settled = items.stream().filter(item -> item.getPriceState() == SheetPriceState.Settled).count();
        long unpriced = items.stream().filter(item -> item.getPriceState() == SheetPriceState.Unpriced).count();
        if (settled == items.size()) return SheetPriceState.Settled;
        if (priced == items.size()) return SheetPriceState.Priced;
        if (unpriced == items.size()) return SheetPriceState.Unpriced;
        return SheetPriceState.Mixed;
    }

    private BigDecimal singlePurchasePrice(List<PurchaseOrderItem> items) {
        Set<BigDecimal> prices = new LinkedHashSet<>();
        for (PurchaseOrderItem item : items) {
            if (item.getPriceState() == SheetPriceState.Unpriced || item.getPriceState() == SheetPriceState.Settled) continue;
            if (item.getUnitPrice() != null) prices.add(item.getUnitPrice().stripTrailingZeros());
        }
        return prices.size() == 1 ? prices.iterator().next() : null;
    }

    private BigDecimal singleSettlementPrice(List<PurchaseOrderItem> items) {
        if (items.isEmpty() || items.stream().anyMatch(item -> item.getPriceState() != SheetPriceState.Settled)) return null;
        Set<BigDecimal> prices = new LinkedHashSet<>();
        for (PurchaseOrderItem item : items) {
            if (item.getSettlementPrice() != null) prices.add(item.getSettlementPrice().stripTrailingZeros());
        }
        return prices.size() == 1 ? prices.iterator().next() : null;
    }

    private boolean active(PurchaseOrder order) {
        return order != null && order.getStatus() != com.th4.edge.decoboon.project.configure.PurchaseStatus.Cancelled
                && order.getStatus() != com.th4.edge.decoboon.project.configure.PurchaseStatus.Rejected
                && order.getStatus() != com.th4.edge.decoboon.project.configure.PurchaseStatus.Closed;
    }

    private void persistSheetStates(List<MaterialOrderSheetItem> lines) {
        Map<Long, MaterialOrderSheet> sheets = new LinkedHashMap<>();
        Map<Long, List<MaterialOrderSheetItem>> grouped = new LinkedHashMap<>();
        for (MaterialOrderSheetItem line : lines) {
            if (line.getSheet() == null || line.getSheet().getSheetId() == null) continue;
            Long id = line.getSheet().getSheetId();
            sheets.putIfAbsent(id, line.getSheet());
            grouped.computeIfAbsent(id, key -> new java.util.ArrayList<>()).add(line);
        }
        for (Map.Entry<Long, List<MaterialOrderSheetItem>> entry : grouped.entrySet()) {
            MaterialOrderSheet sheet = sheets.get(entry.getKey());
            sheet.setPriceState(stateOf(entry.getValue()));
            sheetRepository.save(sheet);
        }
    }
}
