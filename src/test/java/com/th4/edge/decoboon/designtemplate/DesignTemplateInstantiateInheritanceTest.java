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
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
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
 * 模版实例化的区域取值（DEC-066 / DEC-062）。
 *
 * <p>覆盖：① 最近一次**已完成**量房的原始区域实测值 → 继承为方案起点（可作工程量基数、计入方案面积）；
 * ② 量房已建但未录实测值 + 模版参考值 → 参考值打底（TemplateReference + Estimated，不进入金额）；
 * ③ 量房未完成 → 不继承（原始结构尚未量定）；④ 匹配歧义（同类型同名多条）→ 不静默择一；
 * ⑤ 无任何来源 → 保持未录入（待补）。</p>
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
    private final MeasureRepository measureRepository = mock(MeasureRepository.class);
    private final MeasureRegionRepository measureRegionRepository = mock(MeasureRegionRepository.class);

    private final com.th4.edge.decoboon.designtemplate.service.templates.instantiate.support.Instantiate instantiate =
            new com.th4.edge.decoboon.designtemplate.service.templates.instantiate.support.Instantiate(
                    templateRepository, versionRepository, roomRepository, propertyRepository, designRepository,
                    spaceRepository, designItemRepository, templateItemRepository,
                    new DesignTemplateFormulaContext(), accessContext, measureRepository, measureRegionRepository);

    @Test
    void inheritsOriginalRegionFromLatestCompletedMeasure() {
        Fixture fixture = fixture(MeasureStatus.Completed, List.of(
                region(900L, SpaceType.Bedroom, "主卧", "15.30", "16.20")));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        assertThat(space.getArea()).isEqualByComparingTo("15.30");
        assertThat(space.getPerimeter()).isEqualByComparingTo("16.20");
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Measured);
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.Measure);
        assertThat(space.getSourceMeasureRegionId()).isEqualTo(900L);
        // 方案面积 = 继承到的原始结构实测面积合计（起点）
        assertThat(design.getArea()).isEqualByComparingTo("15.30");
        assertThat(fixture.savedDesign).isSameAs(design);
    }

    @Test
    void doesNotInheritBeforeMeasureCompleted() {
        Fixture fixture = fixture(MeasureStatus.Scheduled, List.of(
                region(900L, SpaceType.Bedroom, "主卧", "15.30", "16.20")));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        // 原始结构尚未量定：不得把未完成的量房当成原始结构事实
        assertThat(space.getArea()).isNull();
        assertThat(space.getValueOrigin()).isNull();
        assertThat(design.getArea()).isEqualByComparingTo("0");
    }

    @Test
    void fallsBackToTemplateReferenceWithoutCountingIntoAmount() {
        Fixture fixture = fixture(MeasureStatus.Completed, List.of());
        fixture.room.setReferenceArea(new BigDecimal("14.60"));
        fixture.room.setReferencePerimeter(new BigDecimal("15.80"));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        assertThat(space.getArea()).isEqualByComparingTo("14.60");
        // 参考值只是打底：模版参考值 + 估算来源，不得作为工程量基数
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.TemplateReference);
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Estimated);
        assertThat(design.getArea()).isEqualByComparingTo("0");
    }

    @Test
    void leavesAreaEmptyWhenMeasureRegionHasNoMeasuredValueYet() {
        Fixture fixture = fixture(MeasureStatus.Completed, List.of(
                region(900L, SpaceType.Bedroom, "主卧", null, null)));
        fixture.room.setReferenceArea(new BigDecimal("14.60"));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        // 关联到原始区域但尚未录入实测值：仍走模版参考值打底（待实测确认），不计金额
        assertThat(space.getSourceMeasureRegionId()).isEqualTo(900L);
        assertThat(space.getValueOrigin()).isEqualTo(SpaceValueOrigin.TemplateReference);
        assertThat(space.getAreaSource()).isEqualTo(AreaSource.Estimated);
        assertThat(design.getArea()).isEqualByComparingTo("0");
    }

    @Test
    void doesNotPickOneValueWhenMeasureRegionsAreAmbiguous() {
        Fixture fixture = fixture(MeasureStatus.Completed, List.of(
                region(900L, SpaceType.Bedroom, "主卧", "15.30", "16.20"),
                region(901L, SpaceType.Bedroom, "主卧", "16.10", "17.00")));

        Design design = instantiate.call(new Design());

        Space space = design.getSpaces().get(0);
        assertThat(space.getArea()).isNull();
        assertThat(space.getSourceMeasureRegionId()).isNull();
    }

    private static final class Fixture {
        final DesignTemplateRoom room = new DesignTemplateRoom();
        Design savedDesign;
    }

    private Fixture fixture(MeasureStatus measureStatus, List<MeasureRegion> regions) {
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

        Measure measure = new Measure();
        measure.setMeasureId(800L);
        measure.setStatus(measureStatus);
        measure.setProperty(property);
        when(measureRepository.findByPropertyOrderByMeasureTimeDesc(property)).thenReturn(List.of(measure));
        when(measureRegionRepository.findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(1L, 800L))
                .thenReturn(regions);

        fixture.room.setRoomKey("R1");
        fixture.room.setName("主卧");
        fixture.room.setRoomType(SpaceType.Bedroom);
        fixture.room.setSortNo(0);
        when(roomRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of(fixture.room));
        when(templateItemRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of());
        when(designRepository.findFirstByCompany_ComIdAndProperty_PropertyIdAndDesignTemplateVersion_VersionId(
                1L, 700L, 600L)).thenReturn(Optional.empty());
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> {
            Design saved = invocation.getArgument(0);
            saved.setDesignId(900L);
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

    private MeasureRegion region(Long id, SpaceType type, String name, String area, String perimeter) {
        MeasureRegion region = new MeasureRegion();
        region.setRegionId(id);
        region.setRoomType(type);
        region.setName(name);
        if (area != null) {
            region.setArea(new BigDecimal(area));
            region.setPerimeter(new BigDecimal(perimeter));
            region.setAreaSource(AreaSource.Measured);
        }
        return region;
    }
}
