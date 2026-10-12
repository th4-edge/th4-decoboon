package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.configure.SpaceValueOrigin;
import com.th4.edge.decoboon.design.context.DesignFormulaRecalcContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.property.context.PropertyRegionSyncContext;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 房屋区域实测值 → 方案的有界联动（DEC-064 第 6 条）。
 *
 * <p>草稿方案继承来源自动重算；覆盖来源不改值只提示；非草稿方案不静默改金额；
 * 值未变化时不重复写库与重算（幂等）。</p>
 */
class PropertyRegionSyncContextTest {

    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignRepository designRepository = mock(DesignRepository.class);
    private final DesignFormulaRecalcContext formulaRecalcContext = mock(DesignFormulaRecalcContext.class);

    private final PropertyRegionSyncContext syncContext =
            new PropertyRegionSyncContext(spaceRepository, designRepository, formulaRecalcContext);

    @Test
    void recalculatesDraftDesignThatInheritsTheRegion() {
        Design design = design(100L, DesignStatus.Draft);
        Space space = space(200L, design, SpaceValueOrigin.House, "10.00", "12.00");
        when(spaceRepository.findByHouseRegionId(900L)).thenReturn(List.of(space));
        when(designRepository.findById(100L)).thenReturn(Optional.of(design));

        PropertyRegionSyncContext.Result result = syncContext.syncToDesigns(region("15.30", "16.20"));

        assertThat(space.getArea()).isEqualByComparingTo("15.30");
        assertThat(space.getPerimeter()).isEqualByComparingTo("16.20");
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Measured);
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.House);
        assertThat(space.getInheritedArea()).isEqualByComparingTo("15.30");
        assertThat(result.linkedDraftDesignCount()).isEqualTo(1);
        assertThat(result.pendingDesignCount()).isZero();
        verify(formulaRecalcContext).recalculate(design);
    }

    @Test
    void keepsOverrideValueAndOnlyFlagsItForReview() {
        Design design = design(100L, DesignStatus.Draft);
        Space space = space(200L, design, SpaceValueOrigin.Override, "9.80", "11.50");
        when(spaceRepository.findByHouseRegionId(900L)).thenReturn(List.of(space));
        when(designRepository.findById(100L)).thenReturn(Optional.of(design));

        PropertyRegionSyncContext.Result result = syncContext.syncToDesigns(region("15.30", "16.20"));

        // 方案内覆盖值是设计师的现场修正：联动不得改写，只提示复核
        assertThat(space.getArea()).isEqualByComparingTo("9.80");
        assertThat(space.getPerimeter()).isEqualByComparingTo("11.50");
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.Override);
        assertThat(result.linkedDraftDesignCount()).isZero();
        assertThat(result.pendingDesignCount()).isEqualTo(1);
        verify(spaceRepository, never()).save(any(Space.class));
        verify(formulaRecalcContext, never()).recalculate(any(Design.class));
    }

    @Test
    void doesNotSilentlyChangeAmountOfNonDraftDesign() {
        Design design = design(100L, DesignStatus.Selected);
        Space space = space(200L, design, SpaceValueOrigin.House, "10.00", "12.00");
        when(spaceRepository.findByHouseRegionId(900L)).thenReturn(List.of(space));
        when(designRepository.findById(100L)).thenReturn(Optional.of(design));

        PropertyRegionSyncContext.Result result = syncContext.syncToDesigns(region("15.30", "16.20"));

        // 客户已选中的方案：金额不得静默变化，只提示设计师显式同步
        assertThat(space.getArea()).isEqualByComparingTo("10.00");
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Measured);
        assertThat(result.linkedDraftDesignCount()).isZero();
        assertThat(result.pendingDesignCount()).isEqualTo(1);
        verify(spaceRepository, never()).save(any(Space.class));
        verify(formulaRecalcContext, never()).recalculate(any(Design.class));
    }

    @Test
    void isIdempotentWhenValueUnchanged() {
        Design design = design(100L, DesignStatus.Draft);
        Space space = space(200L, design, SpaceValueOrigin.House, "15.30", "16.20");
        when(spaceRepository.findByHouseRegionId(900L)).thenReturn(List.of(space));
        when(designRepository.findById(100L)).thenReturn(Optional.of(design));

        PropertyRegionSyncContext.Result result = syncContext.syncToDesigns(region("15.30", "16.20"));

        assertThat(result.linkedDraftDesignCount()).isZero();
        verify(spaceRepository, never()).save(any(Space.class));
        verify(formulaRecalcContext, never()).recalculate(any(Design.class));
    }

    private Space space(Long id, Design design, SpaceValueOrigin origin, String area, String perimeter) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setDesign(design);
        space.setType(SpaceType.Bedroom);
        space.setName("主卧");
        space.setArea(new BigDecimal(area));
        space.setPerimeter(new BigDecimal(perimeter));
        space.setAreaSource(AreaSource.Measured);
        space.setValueOrigin(origin);
        space.setHouseRegionId(900L);
        space.setInheritedArea(new BigDecimal("10.00"));
        space.setInheritedPerimeter(new BigDecimal("12.00"));
        return space;
    }

    private Design design(Long id, DesignStatus status) {
        Design design = new Design();
        design.setDesignId(id);
        design.setStatus(status);
        return design;
    }

    private PropertyRegion region(String area, String perimeter) {
        PropertyRegion region = new PropertyRegion();
        region.setRegionId(900L);
        region.setRoomType(SpaceType.Bedroom);
        region.setName("主卧");
        region.setArea(new BigDecimal(area));
        region.setPerimeter(new BigDecimal(perimeter));
        region.setAreaSource(AreaSource.Measured);
        return region;
    }
}
