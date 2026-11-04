package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 门店范围下的工资单只读投影：只汇总本店工资项，不修改持久化工资单头金额。 */
@Component
public class PayrollSheetScopeContext {

    private final PayrollItemRepository itemRepository;

    public PayrollSheetScopeContext(PayrollItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Page<PayrollSheet> scopePage(Long comId, Page<PayrollSheet> page, Long storeFilter) {
        if (storeFilter == null || storeFilter == 0L || page.isEmpty()) return page;
        List<Long> sheetIds = page.getContent().stream().map(PayrollSheet::getSheetId).toList();
        Map<Long, List<PayrollItem>> itemsBySheet = itemRepository
                .findByCompany_ComIdAndSheet_SheetIdInOrderByEmployee_NameAsc(comId, sheetIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getSheet().getSheetId()));
        List<PayrollSheet> projected = page.getContent().stream()
                .map(sheet -> scope(sheet, itemsBySheet.getOrDefault(sheet.getSheetId(), List.of()), storeFilter))
                .toList();
        return new PageImpl<>(projected, page.getPageable(), page.getTotalElements());
    }

    public PayrollSheet scope(Long comId, PayrollSheet sheet, Long storeFilter) {
        if (storeFilter == null || storeFilter == 0L) return sheet;
        List<PayrollItem> items = itemRepository
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(comId, sheet.getSheetId());
        return scope(sheet, items, storeFilter);
    }

    private PayrollSheet scope(PayrollSheet source, List<PayrollItem> items, Long storeFilter) {
        PayrollSheet view = new PayrollSheet();
        view.setSheetId(source.getSheetId());
        view.setCompany(source.getCompany());
        view.setStore(source.getStore());
        view.setTeam(source.getTeam());
        view.setOwner(source.getOwner());
        view.setPeriod(source.getPeriod());
        view.setStatus(source.getStatus());
        view.setAttendanceComplete(source.getAttendanceComplete());
        view.setCommissionComplete(source.getCommissionComplete());
        view.setConfirmedBy(source.getConfirmedBy());
        view.setConfirmedAt(source.getConfirmedAt());
        view.setRemark(source.getRemark());
        view.setCreatedBy(source.getCreatedBy());
        view.setCreateTime(source.getCreateTime());

        List<PayrollItem> ownItems = items.stream()
                .filter(item -> item.getStore() != null && storeFilter.equals(item.getStore().getStoreId()))
                .toList();
        view.setTotalGrossAmount(sum(ownItems, PayrollItem::getGrossAmount));
        view.setTotalDeductionAmount(sum(ownItems, PayrollItem::getDeductionAmount));
        view.setTotalNetAmount(sum(ownItems, PayrollItem::getNetAmount));
        view.setMixedStore(items.stream().anyMatch(item ->
                item.getStore() == null || !Objects.equals(storeFilter, item.getStore().getStoreId())));
        return view;
    }

    private BigDecimal sum(List<PayrollItem> items,
                           java.util.function.Function<PayrollItem, BigDecimal> amount) {
        return items.stream()
                .map(amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
