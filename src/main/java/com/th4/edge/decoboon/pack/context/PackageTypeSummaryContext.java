package com.th4.edge.decoboon.pack.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** 套餐详情按分区和分项类型聚合，供 App 展示稳定计数。 */
@Note("套餐分项类型摘要上下文")
@Component
public class PackageTypeSummaryContext {

    private final PackageLineRepository lineRepository;
    private final PackageSectionRepository sectionRepository;

    public PackageTypeSummaryContext(PackageLineRepository lineRepository, PackageSectionRepository sectionRepository) {
        this.lineRepository = lineRepository;
        this.sectionRepository = sectionRepository;
    }

    public void apply(PackageTemplate template) {
        Map<Long, Map<ItemType, Integer>> sectionTypeCounts = new LinkedHashMap<>();
        Map<Long, Integer> sectionLineCounts = new LinkedHashMap<>();
        Map<ItemType, Integer> typeCounts = new LinkedHashMap<>();
        Map<Long, BigDecimal> sectionReferenceAmounts = new LinkedHashMap<>();
        Map<Long, Map<ItemType, BigDecimal>> sectionTypeReferenceAmounts = new LinkedHashMap<>();
        Map<ItemType, BigDecimal> typeReferenceAmounts = new LinkedHashMap<>();
        int total = 0;
        BigDecimal totalReferenceAmount = BigDecimal.ZERO;
        BigDecimal totalReferenceCost = BigDecimal.ZERO;

        for (PackageSection section : sectionRepository.findByCompany_ComIdOrderByPriorityAscGroupIdAsc(
                template.getCompany().getComId())) {
            Long sectionId = section.getGroupId();
            sectionTypeCounts.put(sectionId, new LinkedHashMap<>());
            sectionLineCounts.put(sectionId, 0);
            sectionReferenceAmounts.put(sectionId, BigDecimal.ZERO);
            sectionTypeReferenceAmounts.put(sectionId, new LinkedHashMap<>());
        }

        for (Object[] row : lineRepository.summarizeTypeCountsByTemplateId(template.getTemplateId())) {
            if (row == null || row.length < 5 || row[0] == null || row[1] == null || row[2] == null) continue;
            Long sectionId = ((Number) row[0]).longValue();
            ItemType type = ItemType.valueOf(String.valueOf(row[1]));
            int count = ((Number) row[2]).intValue();
            BigDecimal referenceAmount = money(row[3]);
            BigDecimal referenceCost = money(row[4]);
            sectionTypeCounts.computeIfAbsent(sectionId, ignored -> new LinkedHashMap<>()).merge(type, count, Integer::sum);
            sectionLineCounts.merge(sectionId, count, Integer::sum);
            typeCounts.merge(type, count, Integer::sum);
            sectionReferenceAmounts.merge(sectionId, referenceAmount, BigDecimal::add);
            sectionTypeReferenceAmounts.computeIfAbsent(sectionId, ignored -> new LinkedHashMap<>())
                    .merge(type, referenceAmount, BigDecimal::add);
            typeReferenceAmounts.merge(type, referenceAmount, BigDecimal::add);
            totalReferenceAmount = totalReferenceAmount.add(referenceAmount);
            totalReferenceCost = totalReferenceCost.add(referenceCost);
            total += count;
        }

        template.setTypeCounts(typeCounts);
        template.setSectionTypeCounts(sectionTypeCounts);
        template.setSectionLineCounts(sectionLineCounts);
        template.setItemCount(total);
        template.setLineCount(total);
        int usedSectionCount = (int) sectionLineCounts.values().stream().filter(count -> count > 0).count();
        template.setSectionCount(usedSectionCount);
        template.setReady(usedSectionCount > 0 && total > 0);
        template.setReferenceAmount(money(totalReferenceAmount));
        template.setReferenceCost(money(totalReferenceCost));
        template.setTypeReferenceAmounts(moneyMap(typeReferenceAmounts));
        template.setSectionReferenceAmounts(moneyMapLong(sectionReferenceAmounts));
        template.setSectionTypeReferenceAmounts(moneyNestedMap(sectionTypeReferenceAmounts));
    }

    private BigDecimal money(Object value) {
        if (value == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<ItemType, BigDecimal> moneyMap(Map<ItemType, BigDecimal> source) {
        Map<ItemType, BigDecimal> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, money(value)));
        return result;
    }

    private Map<Long, BigDecimal> moneyMapLong(Map<Long, BigDecimal> source) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, money(value)));
        return result;
    }

    private Map<Long, Map<ItemType, BigDecimal>> moneyNestedMap(Map<Long, Map<ItemType, BigDecimal>> source) {
        Map<Long, Map<ItemType, BigDecimal>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, moneyMap(value)));
        return result;
    }
}
