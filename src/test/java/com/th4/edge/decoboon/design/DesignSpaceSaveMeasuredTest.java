package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignFormulaRecalcContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 区域保存口径回归（BR-040 §4 / DEC-062 / EV-213）：
 * 面积与周长只接受设计师实测录入，服务端不得由长宽推算，未录入保持为空而不是 0。
 */
class DesignSpaceSaveMeasuredTest {

    private final DesignContext designContext = mock(DesignContext.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignRepository designRepository = mock(DesignRepository.class);
    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);
    private final DesignFormulaRecalcContext formulaRecalcContext = mock(DesignFormulaRecalcContext.class);

    private final com.th4.edge.decoboon.design.service.spaces.save.support.Save save =
            new com.th4.edge.decoboon.design.service.spaces.save.support.Save(
                    designContext, spaceRepository, designRepository, dataScopeContext, formulaRecalcContext);

    @Test
    void savesMeasuredValuesAndNeverDerivesFromSnapshotLengthWidth() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        givenEditable(designer, design);
        ReflectionTestUtils.setField(save, "area", new BigDecimal("12.04"));
        ReflectionTestUtils.setField(save, "perimeter", new BigDecimal("15.90"));
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of());

        Space saved = save.call(new Space());

        // 3.6 × 4.2 = 15.12；12.04 才是设计师按 CAD 录入的实测值，必须原样保存
        assertThat(saved.getLength()).isEqualByComparingTo("3.60");
        assertThat(saved.getWidth()).isEqualByComparingTo("4.20");
        assertThat(saved.getArea()).isEqualByComparingTo("12.04");
        assertThat(saved.getPerimeter()).isEqualByComparingTo("15.90");
        assertThat(saved.getAreaSource()).isEqualTo(AreaSource.Measured);
        verify(formulaRecalcContext).recalculate(design);
    }

    @Test
    void keepsUnmeasuredSpaceEmptyInsteadOfZero() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        givenEditable(designer, design);
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of());

        Space saved = save.call(new Space());

        assertThat(saved.getArea()).isNull();
        assertThat(saved.getPerimeter()).isNull();
        assertThat(saved.getAreaSource()).isNull();
        // 未录入不得计入方案面积，也不得伪装成 0 已完成
        assertThat(design.getArea()).isEqualByComparingTo("0");
    }

    @Test
    void refusesNonPositiveMeasuredValues() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        givenEditable(designer, design);
        ReflectionTestUtils.setField(save, "area", BigDecimal.ZERO);

        assertThatThrownBy(() -> save.call(new Space()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("实测面积必须大于 0");
    }

    @Test
    void dropsLegacyEstimatedValueOnFirstMeasuredEntry() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        Space existing = new Space();
        existing.setSpaceId(200L);
        existing.setArea(new BigDecimal("24.00"));
        existing.setPerimeter(new BigDecimal("20.00"));
        existing.setAreaSource(AreaSource.Estimated);
        givenEditable(designer, design);
        when(dataScopeContext.requireScope(designer))
                .thenReturn(new DataScope(DataScopeType.Self, 1L, null, null, 10L));
        when(spaceRepository.findOne(any(Specification.class))).thenReturn(Optional.of(existing));
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of(existing));
        ReflectionTestUtils.setField(save, "spaceId", 200L);
        ReflectionTestUtils.setField(save, "perimeter", new BigDecimal("18.40"));

        Space saved = save.call(new Space());

        // 历史推算周长不可沿用：只录入实测周长时面积按未录入处理
        assertThat(saved.getArea()).isNull();
        assertThat(saved.getPerimeter()).isEqualByComparingTo("18.40");
        assertThat(saved.getAreaSource()).isEqualTo(AreaSource.Measured);
    }

    @Test
    void designAreaSumsMeasuredSpacesOnly() {
        Company company = company(1L);
        Employee designer = employee(10L, company);
        Design design = design(100L, company);
        givenEditable(designer, design);
        Space measured = space(201L, "12.04", "15.90", AreaSource.Measured);
        Space legacy = space(202L, "24.00", "20.00", AreaSource.Estimated);
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of(measured, legacy));
        ReflectionTestUtils.setField(save, "area", new BigDecimal("12.04"));
        ReflectionTestUtils.setField(save, "perimeter", new BigDecimal("15.90"));

        save.call(new Space());

        assertThat(design.getArea()).isEqualByComparingTo("12.04");
    }

    private void givenEditable(Employee designer, Design design) {
        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        ReflectionTestUtils.setField(save, "designId", 100L);
        ReflectionTestUtils.setField(save, "employee", designer);
        ReflectionTestUtils.setField(save, "type", SpaceType.Bedroom);
        ReflectionTestUtils.setField(save, "name", "主卧");
        ReflectionTestUtils.setField(save, "length", new BigDecimal("3.60"));
        ReflectionTestUtils.setField(save, "width", new BigDecimal("4.20"));
        ReflectionTestUtils.setField(save, "height", new BigDecimal("2.80"));
    }

    private Space space(Long id, String area, String perimeter, AreaSource source) {
        Space space = new Space();
        space.setSpaceId(id);
        space.setArea(new BigDecimal(area));
        space.setPerimeter(new BigDecimal(perimeter));
        space.setAreaSource(source);
        return space;
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setComId(id);
        return company;
    }

    private Employee employee(Long id, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(EmployeeRole.Designer);
        employee.setCompany(company);
        return employee;
    }

    private Design design(Long id, Company company) {
        Design design = new Design();
        design.setDesignId(id);
        design.setCompany(company);
        return design;
    }
}
