package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 量房原始区域口径回归（DEC-066 / DEC-062）：
 * 原始结构在量房期可录，**量房完成后冻结**；留空=未录入而不是 0；填值必须 > 0 且标 Measured。
 */
class MeasureRegionSaveTest {

    private final PropertyContext propertyContext = mock(PropertyContext.class);
    private final MeasureRepository measureRepository = mock(MeasureRepository.class);
    private final MeasureRegionRepository regionRepository = mock(MeasureRegionRepository.class);

    private final com.th4.edge.decoboon.property.service.measureregions.save.support.Save save =
            new com.th4.edge.decoboon.property.service.measureregions.save.support.Save(
                    propertyContext, measureRepository, regionRepository);

    @Test
    void savesOriginalRegionMeasuredValuesWhileMeasuring() {
        Employee measurer = fixture(MeasureStatus.Scheduled);
        ReflectionTestUtils.setField(save, "area", new BigDecimal("15.30"));
        ReflectionTestUtils.setField(save, "perimeter", new BigDecimal("16.20"));
        ReflectionTestUtils.setField(save, "name", "主卧");
        ReflectionTestUtils.setField(save, "roomType", SpaceType.Bedroom);
        when(regionRepository.save(any(MeasureRegion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(regionRepository.findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(1L, 500L))
                .thenReturn(List.of());

        MeasureRegion saved = save.call(new MeasureRegion());

        assertThat(saved.getArea()).isEqualByComparingTo("15.30");
        assertThat(saved.getPerimeter()).isEqualByComparingTo("16.20");
        assertThat(saved.getAreaSource()).isEqualTo(AreaSource.Measured);
        assertThat(saved.getMeasuredBy()).isSameAs(measurer);
        assertThat(saved.getProperty()).isNotNull();
        assertThat(saved.getMeasure()).isNotNull();
    }

    @Test
    void keepsUnmeasuredRegionEmptyInsteadOfZero() {
        fixture(MeasureStatus.Scheduled);
        ReflectionTestUtils.setField(save, "name", "主卧");
        when(regionRepository.save(any(MeasureRegion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(regionRepository.findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(1L, 500L))
                .thenReturn(List.of());

        MeasureRegion saved = save.call(new MeasureRegion());

        assertThat(saved.getArea()).isNull();
        assertThat(saved.getPerimeter()).isNull();
        assertThat(saved.getAreaSource()).isNull();
    }

    @Test
    void freezesOriginalRegionsAfterMeasureCompleted() {
        fixture(MeasureStatus.Completed);
        ReflectionTestUtils.setField(save, "name", "主卧");
        ReflectionTestUtils.setField(save, "area", new BigDecimal("15.30"));

        assertThatThrownBy(() -> save.call(new MeasureRegion()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("该量房已完成，原始结构不可再修改");
    }

    @Test
    void refusesNonPositiveMeasuredValues() {
        fixture(MeasureStatus.Scheduled);
        ReflectionTestUtils.setField(save, "name", "主卧");
        ReflectionTestUtils.setField(save, "area", BigDecimal.ZERO);

        assertThatThrownBy(() -> save.call(new MeasureRegion()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("实测面积必须大于 0");
    }

    /** 量房单 + 房屋 + 操作人；返回完整员工实体。 */
    private Employee fixture(MeasureStatus status) {
        Company company = new Company();
        company.setComId(1L);
        Employee designer = new Employee();
        designer.setEmpId(10L);
        designer.setRole(EmployeeRole.Designer);
        designer.setCompany(company);

        Property property = new Property();
        property.setPropertyId(700L);
        property.setCompany(company);
        property.setOwner(designer);
        property.setStore(new Store());
        property.setTeam(new Team());

        Measure measure = new Measure();
        measure.setMeasureId(500L);
        measure.setCompany(company);
        measure.setProperty(property);
        measure.setOwner(designer);
        measure.setStatus(status);

        when(propertyContext.requireEmployee(designer)).thenReturn(designer);
        when(propertyContext.canView(any(Property.class), any(Employee.class))).thenReturn(true);
        when(measureRepository.findById(500L)).thenReturn(Optional.of(measure));
        ReflectionTestUtils.setField(save, "measureId", 500L);
        ReflectionTestUtils.setField(save, "employee", designer);
        return designer;
    }
}
