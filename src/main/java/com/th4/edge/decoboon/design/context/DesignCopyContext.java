package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 完整复制设计方案及其区域、分项。 */
@Note("设计方案复制上下文")
@Component
public class DesignCopyContext {

    private static final int MAX_NAME_LENGTH = 128;

    private final DesignContext designContext;
    private final DesignRepository designRepository;
    private final SpaceRepository spaceRepository;
    private final DesignItemRepository itemRepository;

    public DesignCopyContext(DesignContext designContext,
                             DesignRepository designRepository,
                             SpaceRepository spaceRepository,
                             DesignItemRepository itemRepository) {
        this.designContext = designContext;
        this.designRepository = designRepository;
        this.spaceRepository = spaceRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public Design copy(Long sourceId, Employee principal, Employee current) {
        Design source = designContext.requireDesign(sourceId, principal);
        designContext.requireEditable(source);
        Design copy = new Design();
        copy.setNo("D" + System.currentTimeMillis());
        copy.setName(nextCopyName(source));
        copy.setVersion(1);
        copy.setStyle(source.getStyle());
        copy.setStatus(DesignStatus.Draft);
        copy.setArea(zero(source.getArea()));
        copy.setLayout(source.getLayout());
        copy.setDescription(source.getDescription());
        copy.setCoverUrl(source.getCoverUrl());
        copy.setCadUrl(source.getCadUrl());
        copy.setCadPreviewUrl(source.getCadPreviewUrl());
        copy.setCadConvertStatus(source.getCadConvertStatus());
        copy.setOriginalPlanUrl(source.getOriginalPlanUrl());
        copy.setDemolitionUrl(source.getDemolitionUrl());
        copy.setMasonryUrl(source.getMasonryUrl());
        copy.setOptimizationUrl(source.getOptimizationUrl());
        copy.setOriginalPlanImages(copyList(source.getOriginalPlanImages()));
        copy.setSitePhotos(copyList(source.getSitePhotos()));
        copy.setSurveyPhotos(copyList(source.getSurveyPhotos()));

        Employee owner = EmployeeRole.Boss.equals(current.getRole()) ? source.getOwner() : current;
        designContext.apply(copy, source.getCustomer(), owner, source.getProperty());
        Design saved = designRepository.save(copy);

        List<Space> sourceSpaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(source.getDesignId());
        List<Space> copiedSpaces = new ArrayList<>();
        Map<Long, Space> copiedSpaceBySourceId = new HashMap<>();
        for (Space sourceSpace : sourceSpaces) {
            Space copiedSpace = copySpace(sourceSpace, saved);
            copiedSpaces.add(copiedSpace);
            copiedSpaceBySourceId.put(sourceSpace.getSpaceId(), copiedSpace);
        }
        if (!copiedSpaces.isEmpty()) {
            spaceRepository.saveAll(copiedSpaces);
        }

        List<DesignItem> sourceItems = itemRepository.findByDesign_DesignIdOrderBySortAsc(source.getDesignId());
        List<DesignItem> copiedItems = new ArrayList<>();
        for (DesignItem sourceItem : sourceItems) {
            Space copiedSpace = sourceItem.getSpace() == null
                    ? null
                    : copiedSpaceBySourceId.get(sourceItem.getSpace().getSpaceId());
            copiedItems.add(copyItem(sourceItem, saved, copiedSpace));
        }
        if (!copiedItems.isEmpty()) {
            itemRepository.saveAll(copiedItems);
        }
        return saved;
    }

    private Space copySpace(Space source, Design design) {
        Space copy = new Space();
        copy.setType(source.getType());
        copy.setName(source.getName());
        copy.setLength(source.getLength());
        copy.setWidth(source.getWidth());
        copy.setHeight(source.getHeight());
        copy.setArea(zero(source.getArea()));
        copy.setPerimeter(zero(source.getPerimeter()));
        copy.setSort(source.getSort());
        copy.setRemark(source.getRemark());
        copy.setImages(copyList(source.getImages()));
        designContext.apply(copy, design);
        return copy;
    }

    private DesignItem copyItem(DesignItem source, Design design, Space space) {
        DesignItem copy = new DesignItem();
        copy.setType(source.getType());
        copy.setCategory(source.getCategory());
        copy.setCatalogItem(source.getCatalogItem());
        copy.setName(source.getName());
        copy.setBrand(source.getBrand());
        copy.setSpec(source.getSpec());
        copy.setUnit(source.getUnit());
        copy.setQuantity(source.getQuantity());
        copy.setMaterialPrice(zero(source.getMaterialPrice()));
        copy.setManualPrice(zero(source.getManualPrice()));
        copy.setCostPrice(zero(source.getCostPrice()));
        copy.setPricingMode(source.getPricingMode());
        copy.setAmount(zero(source.getAmount()));
        copy.setCost(zero(source.getCost()));
        copy.setFormula(source.getFormula());
        copy.setCriterion(source.getCriterion());
        copy.setCraft(source.getCraft());
        copy.setRemark(source.getRemark());
        copy.setSort(source.getSort());
        copy.setImages(copyList(source.getImages()));
        designContext.apply(copy, design, space);
        return copy;
    }

    private String nextCopyName(Design source) {
        String sourceName = source.getName() == null ? "设计方案" : source.getName().trim();
        String base = sourceName.replaceFirst("（副本(?:\\d+)?）$", "").trim();
        if (base.isBlank()) {
            base = "设计方案";
        }
        Set<String> existingNames = new HashSet<>(designRepository
                .findByCompany_ComIdAndCustomer_CustIdOrderByVersionDesc(
                        source.getCompany().getComId(), source.getCustomer().getCustId())
                .stream()
                .map(Design::getName)
                .toList());
        for (int index = 1; index < 10000; index++) {
            String suffix = index == 1 ? "（副本）" : "（副本" + index + "）";
            String candidate = truncate(base, MAX_NAME_LENGTH - suffix.length()) + suffix;
            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }
        String suffix = "（副本" + System.currentTimeMillis() + "）";
        return truncate(base, MAX_NAME_LENGTH - suffix.length()) + suffix;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, Math.max(1, maxLength));
    }

    private <T> List<T> copyList(List<T> source) {
        return source == null ? null : new ArrayList<>(source);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
