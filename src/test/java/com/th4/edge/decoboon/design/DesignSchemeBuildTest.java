package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignSchemeBuildTest {

    @Test
    void shouldMarkReadyWithoutSpacesWhenItemsExist() {
        Employee designer = employee(10L);
        Design design = design(20L, new BigDecimal("88"));

        DesignContext designContext = mock(DesignContext.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        SpaceRepository spaceRepository = mock(SpaceRepository.class);
        DesignItemRepository itemRepository = mock(DesignItemRepository.class);
        when(designContext.requireDesign(20L, designer)).thenReturn(design);
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(20L)).thenReturn(List.of());
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(20L)).thenReturn(List.of(new DesignItem()));
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Design result = build(designContext, designRepository, spaceRepository, itemRepository, designer).call(new Design());

        assertThat(result.getStatus()).isEqualTo(DesignStatus.Ready);
        assertThat(result.getArea()).isEqualByComparingTo("88");
    }

    @Test
    void shouldStillRejectReadyWithoutItems() {
        Employee designer = employee(10L);
        Design design = design(20L, new BigDecimal("88"));

        DesignContext designContext = mock(DesignContext.class);
        DesignRepository designRepository = mock(DesignRepository.class);
        SpaceRepository spaceRepository = mock(SpaceRepository.class);
        DesignItemRepository itemRepository = mock(DesignItemRepository.class);
        when(designContext.requireDesign(20L, designer)).thenReturn(design);
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(20L)).thenReturn(List.of());
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(20L)).thenReturn(List.of());

        assertThatThrownBy(() -> build(designContext, designRepository, spaceRepository, itemRepository, designer).call(new Design()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("请先配置施工或材料分项");
    }

    private com.th4.edge.decoboon.design.service.schemes.build.support.Build build(
            DesignContext designContext,
            DesignRepository designRepository,
            SpaceRepository spaceRepository,
            DesignItemRepository itemRepository,
            Employee designer) {
        var build = new com.th4.edge.decoboon.design.service.schemes.build.support.Build(
                designContext, designRepository, spaceRepository, itemRepository);
        ReflectionTestUtils.setField(build, "designId", 20L);
        ReflectionTestUtils.setField(build, "status", DesignStatus.Ready);
        ReflectionTestUtils.setField(build, "employee", designer);
        return build;
    }

    private Design design(Long id, BigDecimal area) {
        Design design = new Design();
        design.setDesignId(id);
        design.setArea(area);
        return design;
    }

    private Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(EmployeeRole.Designer);
        return employee;
    }
}
