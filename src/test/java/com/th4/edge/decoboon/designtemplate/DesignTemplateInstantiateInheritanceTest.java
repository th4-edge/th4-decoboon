package com.th4.edge.decoboon.designtemplate;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.configure.SpaceValueOrigin;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.decoboon.property.repository.PropertyRegionRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 模版实例化的区域实测值继承（DEC-064 / OBJ-007）。
 *
 * <p>覆盖三条继承顺位与金额门禁：① 房屋区域实测档案有 Measured 值 → 继承并计入方案面积；
 * ② 房屋无实测值但有模版参考值 → 落 TemplateReference + Estimated（不进金额）；
 * ③ 匹配歧义（同类型同名多条）→ 不静默择一；④ 无任何来源 → 保持未录入。</p>
 */
class DesignTemplateInstantiateInheritanceTest {

    private final DesignTemplateRepository templateRepository = mock(DesignTemplateRepository.class);
    private final DesignTemplateVersionRepository versionRepository = mock(DesignTemplateVersionRepository.class);
    private final DesignTemplateRoomRepository roomRepository = mock(DesignTemplateRoomRepository.class);
    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final DesignRepository designRepository = mock(DesignRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignItemRepository designItemRepository = mock(DesignItemRepository.class);
    private final DesignTemplateItemRepository templateItemRepository = mock(DesignTemplateItemRepository.class);
    private final DesignTemplateAccessContext accessContext = mock(DesignTemplateAccessContext.class);
    private final PropertyRegionRepository propertyRegionRepository = mock(PropertyRegionRepository.class);

    private final com.th4.edge.decoboon.designtemplate.service.templates.instantiate.support.Instantiate instantiate =
            new com.th4.edge.decoboon.designtemplate.service.templates.instantiate.support.Instantiate(
                    templateRepository, versionRepository, roomRepository, propertyRepository, designRepository,
                    spaceRepository, designItemRepository, templateItemRepository,
                    new DesignTemplateFormulaContext(), accessContext, propertyRegionRepository);

    @Test
    void inheritsMeasuredHouseRegionAndCountsDesignArea() {
        Fixture fixture = fixture(List.of(
                region(900L, SpaceType.Bedroom, "主卧", "15.30", "16.20", AreaSource.Measured)));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        assertThat(space.getArea()).isEqualByComparingTo("15.30");
        assertThat(space.getPerimeter()).isEqualByComparingTo("16.20");
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Measured);
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.House);
        assertThat(space.getHouseRegionId()).isEqualTo(900L);
        assertThat(space.getInheritedArea()).isEqualByComparingTo("15.30");
        // 方案面积 = 继承到的实测面积合计
        assertThat(design.getArea()).isEqualByComparingTo("15.30");
        assertThat(fixture.savedDesign).isSameAs(design);
    }

    @Test
    void fallsBackToTemplateReferenceWithoutCountingIntoAmount() {
        Fixture fixture = fixture(List.of());
        fixture.room.setReferenceArea(new BigDecimal("14.60"));
        fixture.room.setReferencePerimeter(new BigDecimal("15.80"));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        assertThat(space.getArea()).isEqualByComparingTo("14.60");
        // 参考值只是打底：来源标记为模版参考值且按估算处理，不得作为工程量基数
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.TemplateReference);
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Estimated);
        assertThat(design.getArea()).isEqualByComparingTo("0");
    }

    @Test
    void leavesAreaEmptyWhenNothingToInherit() {
        Fixture fixture = fixture(List.of());

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        assertThat(space.getArea()).isNull();
        assertThat(space.getPerimeter()).isNull();
        assertThat(space.getAreaSource()).isNull();
        assertThat(space.getValueOrigin()).isNull();
        assertThat(design.getArea()).isEqualByComparingTo("0");
    }

    @Test
    void doesNotPickOneValueWhenHouseRegionsAreAmbiguous() {
        Fixture fixture = fixture(List.of(
                region(900L, SpaceType.Bedroom, "主卧", "15.30", "16.20", AreaSource.Measured),
                region(901L, SpaceType.Bedroom, "主卧", "16.10", "17.00", AreaSource.Measured)));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        // 同类型同名两条视为歧义：不静默择一，留待设计师在方案内显式关联
        assertThat(space.getArea()).isNull();
        assertThat(space.getHouseRegionId()).isNull();
    }

    @Test
    void recordsHouseRegionLinkWhenHouseHasNoMeasuredValueYet() {
        Fixture fixture = fixture(List.of(region(900L, SpaceType.Bedroom, "主卧", null, null, null)));
        fixture.room.setReferenceArea(new BigDecimal("14.60"));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        assertThat(space.getHouseRegionId()).isEqualTo(900L);
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.TemplateReference);
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Estimated);
        assertThat(design.getArea()).isEqualByComparingTo("0");
    }

    /** 测试夹具：模版已发布、房屋存在、区域仅「主卧」一条，房屋区域档案由用例注入。 */
    private static final class Fixture {
        final DesignTemplateRoom room = new DesignTemplateRoom();
        Design savedDesign;
    }

    private Fixture fixture(List<PropertyRegion> houseRegions) {
        Fixture fixture = new Fixture();
        Company company = new Company();
        company.setComId(1L);
        Employee designer = new Employee();
        designer.setEmpId(10L);
        designer.setRole(EmployeeRole.Designer);
        designer.setCompany(company);

        DesignTemplate template = new DesignTemplate();
        template.setTemplateId(500L);
        template.setStatus(DesignTemplate.Status.Published);
        template.setCompany(company);
        template.setCurrentVersion(new DesignTemplateVersion());
        when(templateRepository.findByTemplateIdAndCompany_ComId(500L, 1L)).thenReturn(Optional.of(template));
        when(accessContext.requireEmployee(designer)).thenReturn(designer);
        when(accessContext.visible(designer, template)).thenReturn(true);

        DesignTemplateVersion version = mock(DesignTemplateVersion.class);
        when(version.getVersionId()).thenReturn(600L);
        when(version.getStatus()).thenReturn(DesignTemplate.Status.Published);
        when(version.getName()).thenReturn("三室两厅");
        when(versionRepository.findByVersionIdAndCompany_ComId(600L, 1L)).thenReturn(Optional.of(version));

        Property property = new Property();
        property.setPropertyId(700L);
        property.setCompany(company);
        when(propertyRepository.findByPropertyIdAndCompany_ComId(700L, 1L)).thenReturn(Optional.of(property));

        fixture.room.setRoomKey("R1");
        fixture.room.setName("主卧");
        fixture.room.setRoomType(SpaceType.Bedroom);
        fixture.room.setSortNo(0);
        when(roomRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of(fixture.room));
        when(templateItemRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of());
        when(propertyRegionRepository.findByCompany_ComIdAndProperty_PropertyIdOrderBySortAscRegionIdAsc(1L, 700L))
                .thenAnswer(invocation -> houseRegions);
        when(designRepository.findFirstByCompany_ComIdAndProperty_PropertyIdAndDesignTemplateVersion_VersionId(
                1L, 700L, 600L)).thenReturn(Optional.empty());
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> {
            Design saved = invocation.getArgument(0);
            saved.setDesignId(800L);
            fixture.savedDesign = saved;
            return saved;
        });
        when(spaceRepository.saveAll(any())).thenAnswer(invocation -> new ArrayList<>(invocation.getArgument(0)));

        ReflectionTestUtils.setField(instantiate, "templateId", 500L);
        ReflectionTestUtils.setField(instantiate, "versionId", 600L);
        ReflectionTestUtils.setField(instantiate, "propertyId", 700L);
        ReflectionTestUtils.setField(instantiate, "employee", designer);
        return fixture;
    }

    private PropertyRegion region(Long id, SpaceType type, String name, String area, String perimeter,
                                  AreaSource source) {
        PropertyRegion region = new PropertyRegion();
        region.setRegionId(id);
        region.setRoomType(type);
        region.setName(name);
        region.setArea(area == null ? null : new BigDecimal(area));
        region.setPerimeter(perimeter == null ? null : new BigDecimal(perimeter));
        region.setAreaSource(source);
        return region;
    }
}
