package com.th4.edge.decoboon.designtemplate;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 模版新版本与复制必须完整继承区域参考面积/周长。 */
class DesignTemplateVersionCopyTest {

    private final DesignTemplateRepository templateRepository = mock(DesignTemplateRepository.class);
    private final DesignTemplateVersionRepository versionRepository = mock(DesignTemplateVersionRepository.class);
    private final DesignTemplateRoomRepository roomRepository = mock(DesignTemplateRoomRepository.class);
    private final DesignTemplateItemRepository itemRepository = mock(DesignTemplateItemRepository.class);
    private final DesignTemplateAccessContext accessContext = mock(DesignTemplateAccessContext.class);
    private final Employee employee = employee();
    private final DesignTemplate template = template(employee);
    private final DesignTemplateVersion source = version(template, 600L, 1, DesignTemplate.Status.Published);
    private final DesignTemplateRoom sourceRoom = room(source);

    @Test
    void newDraftVersionKeepsReferenceAreaAndPerimeter() {
        when(templateRepository.findByTemplateIdAndCompany_ComId(500L, 1L)).thenReturn(Optional.of(template));
        when(accessContext.requireEmployee(employee)).thenReturn(employee);
        when(versionRepository.findByTemplate_TemplateIdAndCompany_ComIdAndStatus(
                500L, 1L, DesignTemplate.Status.Draft)).thenReturn(Optional.empty());
        when(versionRepository.findByTemplate_TemplateIdAndCompany_ComIdOrderByVersionDesc(500L, 1L))
                .thenReturn(List.of(source));
        when(versionRepository.save(any(DesignTemplateVersion.class))).thenAnswer(invocation -> {
            DesignTemplateVersion saved = invocation.getArgument(0);
            saved.setVersionId(601L);
            return saved;
        });
        when(roomRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of(sourceRoom));
        when(roomRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(601L, 1L))
                .thenReturn(List.of());
        when(itemRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of());
        when(itemRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(601L, 1L))
                .thenReturn(List.of());

        var build = new com.th4.edge.decoboon.designtemplate.service.templates.version.build.support.Build(
                templateRepository, versionRepository, roomRepository, itemRepository, accessContext);
        ReflectionTestUtils.setField(build, "templateId", 500L);
        ReflectionTestUtils.setField(build, "employee", employee);

        build.call(new DesignTemplate());

        ArgumentCaptor<DesignTemplateRoom> captor = ArgumentCaptor.forClass(DesignTemplateRoom.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceArea()).isEqualByComparingTo("20.000");
        assertThat(captor.getValue().getReferencePerimeter()).isEqualByComparingTo("18.000");
    }

    @Test
    void copiedPersonalTemplateKeepsReferenceAreaAndPerimeter() {
        when(templateRepository.findByTemplateIdAndCompany_ComId(500L, 1L)).thenReturn(Optional.of(template));
        when(accessContext.requireEmployee(employee)).thenReturn(employee);
        when(accessContext.visible(employee, template)).thenReturn(true);
        when(versionRepository.save(any(DesignTemplateVersion.class))).thenAnswer(invocation -> {
            DesignTemplateVersion saved = invocation.getArgument(0);
            saved.setVersionId(601L);
            return saved;
        });
        when(roomRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of(sourceRoom));
        when(roomRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(601L, 1L))
                .thenReturn(List.of());
        when(itemRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(600L, 1L))
                .thenReturn(List.of());
        when(itemRepository.findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(601L, 1L))
                .thenReturn(List.of());

        var copy = new com.th4.edge.decoboon.designtemplate.service.templates.copy.support.Copy(
                templateRepository, versionRepository, roomRepository, itemRepository, accessContext);
        ReflectionTestUtils.setField(copy, "templateId", 500L);
        ReflectionTestUtils.setField(copy, "employee", employee);

        copy.call(new DesignTemplate());

        ArgumentCaptor<DesignTemplateRoom> captor = ArgumentCaptor.forClass(DesignTemplateRoom.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceArea()).isEqualByComparingTo("20.000");
        assertThat(captor.getValue().getReferencePerimeter()).isEqualByComparingTo("18.000");
    }

    private static Employee employee() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(10L);
        employee.setCompany(company);
        return employee;
    }

    private static DesignTemplate template(Employee employee) {
        DesignTemplate template = new DesignTemplate();
        template.setTemplateId(500L);
        template.setCompany(employee.getCompany());
        template.setOwner(employee);
        template.setStatus(DesignTemplate.Status.Published);
        template.setCurrentVersion(version(template, 600L, 1, DesignTemplate.Status.Published));
        return template;
    }

    private static DesignTemplateVersion version(
            DesignTemplate template, Long id, int number, DesignTemplate.Status status) {
        DesignTemplateVersion version = new DesignTemplateVersion();
        version.setVersionId(id);
        version.setTemplate(template);
        version.setVersion(number);
        version.setStatus(status);
        version.setName("三室两厅");
        version.setHouseType("三室两厅");
        version.setAreaMin(new BigDecimal("100"));
        version.setAreaMax(new BigDecimal("120"));
        return version;
    }

    private static DesignTemplateRoom room(DesignTemplateVersion version) {
        DesignTemplateRoom room = new DesignTemplateRoom();
        room.setCompany(version.getTemplate().getCompany());
        room.setVersion(version);
        room.setRoomKey("R1");
        room.setName("主卧");
        room.setRoomType(SpaceType.Bedroom);
        room.setSortNo(0);
        room.setReferenceArea(new BigDecimal("20.000"));
        room.setReferencePerimeter(new BigDecimal("18.000"));
        room.setCount(1);
        return room;
    }
}
