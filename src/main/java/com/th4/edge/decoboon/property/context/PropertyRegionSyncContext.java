package com.th4.edge.decoboon.property.context;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.configure.SpaceValueOrigin;
import com.th4.edge.decoboon.design.context.DesignFormulaRecalcContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 房屋区域实测值联动（{@code DEC-064} 第 6 条）。
 *
 * <p>修改房屋区域实测面积/周长后：</p>
 * <ul>
 *   <li>{@code DesignStatus.Draft} 方案中**继承来源**（{@code House}）的区域自动同步并重算
 *       （实测值、分项工程量、方案面积）；</li>
 *   <li>方案内**覆盖来源**（{@code Override}）的区域**不改其值**，只计入待复核；</li>
 *   <li>非草稿方案（待谈单/已讲解/客户选中/已归档）**不自动重算**、不静默改金额，只计入待复核，
 *       由设计师显式同步（对齐报价快照口径）。</li>
 * </ul>
 *
 * <p>幂等：值未变化的区域不写库；重复保存同一实测值不产生重复重算。</p>
 */
@Note("房屋区域实测值联动")
@Component
public class PropertyRegionSyncContext {

    private final SpaceRepository spaceRepository;
    private final DesignRepository designRepository;
    private final DesignFormulaRecalcContext formulaRecalcContext;

    public PropertyRegionSyncContext(SpaceRepository spaceRepository,
                                     DesignRepository designRepository,
                                     DesignFormulaRecalcContext formulaRecalcContext) {
        this.spaceRepository = spaceRepository;
        this.designRepository = designRepository;
        this.formulaRecalcContext = formulaRecalcContext;
    }

    /** 联动结果：已自动重算的草稿方案数 / 需要人工复核（或显式同步）的方案数。 */
    public record Result(int linkedDraftDesignCount, int pendingDesignCount) {
    }

    @Transactional
    public Result syncToDesigns(PropertyRegion region) {
        List<Space> spaces = spaceRepository.findByHouseRegionId(region.getRegionId());
        if (spaces.isEmpty()) {
            return new Result(0, 0);
        }
        Map<Long, List<Space>> spacesByDesign = new HashMap<>();
        for (Space space : spaces) {
            if (space.getDesign() == null || space.getDesign().getDesignId() == null) {
                continue;
            }
            spacesByDesign.computeIfAbsent(space.getDesign().getDesignId(), key -> new java.util.ArrayList<>())
                    .add(space);
        }
        int linked = 0;
        int pending = 0;
        for (Map.Entry<Long, List<Space>> entry : spacesByDesign.entrySet()) {
            Design design = designRepository.findById(entry.getKey()).orElse(null);
            if (design == null) {
                continue;
            }
            List<Space> designSpaces = entry.getValue();
            boolean draft = design.getStatus() == null || design.getStatus() == DesignStatus.Draft;
            boolean changed = false;
            boolean needsReview = !draft;
            for (Space space : designSpaces) {
                if (space.getValueOrigin() == SpaceValueOrigin.Override) {
                    // 覆盖来源：不覆盖设计师的现场修正，只提示复核
                    needsReview = true;
                    continue;
                }
                if (!draft) {
                    continue;
                }
                if (applyHouseValue(space, region)) {
                    spaceRepository.save(space);
                    changed = true;
                }
            }
            if (changed) {
                formulaRecalcContext.recalculate(design);
                designRepository.save(design);
                linked++;
            }
            if (needsReview) {
                pending++;
            }
        }
        return new Result(linked, pending);
    }

    /** 把房屋实测值同步到方案区域；返回是否发生实际变化（幂等）。 */
    private boolean applyHouseValue(Space space, PropertyRegion region) {
        boolean changed = !Objects.equals(space.getArea(), region.getArea())
                || !Objects.equals(space.getPerimeter(), region.getPerimeter())
                || space.getAreaSource() != region.getAreaSource()
                || space.getValueOrigin() != SpaceValueOrigin.House
                || !Objects.equals(space.getHouseRegionId(), region.getRegionId());
        space.setArea(region.getArea());
        space.setPerimeter(region.getPerimeter());
        space.setAreaSource(region.getAreaSource());
        space.setValueOrigin(SpaceValueOrigin.House);
        space.setHouseRegionId(region.getRegionId());
        space.setInheritedArea(region.getArea());
        space.setInheritedPerimeter(region.getPerimeter());
        space.setOverrideBy(null);
        space.setOverrideAt(null);
        return changed;
    }

    /** 供实例化与显式同步复用：把房屋档案值写入方案区域（含来源与同步快照）。 */
    public void applyFromHouse(Space space, PropertyRegion region) {
        space.setArea(region.getArea());
        space.setPerimeter(region.getPerimeter());
        space.setAreaSource(region.getAreaSource());
        space.setValueOrigin(SpaceValueOrigin.House);
        space.setHouseRegionId(region.getRegionId());
        space.setInheritedArea(region.getArea());
        space.setInheritedPerimeter(region.getPerimeter());
        space.setOverrideBy(null);
        space.setOverrideAt(null);
    }

    /** 房屋档案值是否与方案记录的房屋快照不一致（用于「房屋实测值已变更」提示）。 */
    public boolean isStale(Space space, PropertyRegion region) {
        if (region == null) {
            return false;
        }
        BigDecimal houseArea = region.getArea();
        BigDecimal housePerimeter = region.getPerimeter();
        return !Objects.equals(space.getInheritedArea(), houseArea)
                || !Objects.equals(space.getInheritedPerimeter(), housePerimeter);
    }
}
