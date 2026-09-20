package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignCopyContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesignCopyContextTest {

    private final DesignContext designContext = mock(DesignContext.class);
    private final DesignRepository designRepository = mock(DesignRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final DesignCopyContext context = new DesignCopyContext(
            designContext, designRepository, spaceRepository, itemRepository);

    @Test
    void shouldCopyCompleteDesignAndRestartVersion() {
        Employee actor = employee(7L, EmployeeRole.Designer);
        Design source = design(actor);
        Space sourceSpace = space("客厅");
        DesignItem sourceItem = item(sourceSpace);

        when(designContext.requireDesign(3L, actor)).thenReturn(source);
        when(designRepository.findByCompany_ComIdAndCustomer_CustIdOrderByVersionDesc(1L, 2L))
                .thenReturn(List.of(source, existingCopy()));
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> {
            Design copied = invocation.getArgument(0);
            copied.setDesignId(99L);
            return copied;
        });
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(3L)).thenReturn(List.of(sourceSpace));
        when(spaceRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Space> spaces = invocation.getArgument(0);
            spaces.get(0).setSpaceId(101L);
            return spaces;
        });
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(3L)).thenReturn(List.of(sourceItem));
        when(itemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<DesignItem> items = invocation.getArgument(0);
            items.get(0).setItemId(201L);
            return items;
        });
        doAnswer(invocation -> {
            Design copied = invocation.getArgument(0);
            copied.setCustomer(invocation.getArgument(1));
            copied.setOwner(invocation.getArgument(2));
            copied.setProperty(invocation.getArgument(3));
            return null;
        }).when(designContext).apply(any(Design.class), any(Customer.class), any(Employee.class), any(Property.class));
        doAnswer(invocation -> {
            Space copied = invocation.getArgument(0);
            copied.setDesign(invocation.getArgument(1));
            return null;
        }).when(designContext).apply(any(Space.class), any(Design.class));
        doAnswer(invocation -> {
            DesignItem copied = invocation.getArgument(0);
            copied.setDesign(invocation.getArgument(1));
            copied.setSpace(invocation.getArgument(2));
            return null;
        }).when(designContext).apply(any(DesignItem.class), any(Design.class), any(Space.class));

        Design copied = context.copy(3L, actor, actor);

        assertThat(copied.getDesignId()).isEqualTo(99L);
        assertThat(copied.getNo()).isNotBlank().isNotEqualTo(source.getNo());
        assertThat(copied.getName()).isEqualTo("现代简约方案（副本2）");
        assertThat(copied.getVersion()).isEqualTo(1);
        assertThat(copied.getStatus()).isEqualTo(DesignStatus.Draft);
        assertThat(copied.getStyle()).isEqualTo("现代");
        assertThat(copied.getCoverUrl()).isEqualTo("cover-url");
        assertThat(copied.getOriginalPlanImages()).containsExactly("plan-url");
        assertThat(copied.getSitePhotos()).containsExactly("site-url");
        assertThat(copied.getSurveyPhotos()).containsExactly("survey-url");

        ArgumentCaptor<List<Space>> spaceCaptor = ArgumentCaptor.forClass(List.class);
        verify(spaceRepository).saveAll(spaceCaptor.capture());
        Space copiedSpace = spaceCaptor.getValue().get(0);
        assertThat(copiedSpace.getSpaceId()).isEqualTo(101L);
        assertThat(copiedSpace.getName()).isEqualTo("客厅");
        assertThat(copiedSpace.getImages()).containsExactly("space-image");
        assertThat(copiedSpace.getDesign()).isSameAs(copied);

        ArgumentCaptor<List<DesignItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(itemCaptor.capture());
        DesignItem copiedItem = itemCaptor.getValue().get(0);
        assertThat(copiedItem.getItemId()).isEqualTo(201L);
        assertThat(copiedItem.getName()).isEqualTo("墙砖铺贴");
        assertThat(copiedItem.getAmount()).isEqualByComparingTo("1200.00");
        assertThat(copiedItem.getCost()).isEqualByComparingTo("800.00");
        assertThat(copiedItem.getImages()).containsExactly("item-image");
        assertThat(copiedItem.getCatalogItem()).isSameAs(sourceItem.getCatalogItem());
        assertThat(copiedItem.getDesign()).isSameAs(copied);
        assertThat(copiedItem.getSpace()).isSameAs(copiedSpace);
    }

    private Design design(Employee owner) {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        customer.setCompany(company);
        Property property = new Property();
        property.setPropertyId(4L);
        property.setCustomer(customer);
        Design design = new Design();
        design.setDesignId(3L);
        design.setNo("D-OLD");
        design.setName("现代简约方案");
        design.setVersion(6);
        design.setStatus(DesignStatus.Presented);
        design.setStyle("现代");
        design.setArea(new BigDecimal("118.00"));
        design.setCoverUrl("cover-url");
        design.setOriginalPlanImages(List.of("plan-url"));
        design.setSitePhotos(List.of("site-url"));
        design.setSurveyPhotos(List.of("survey-url"));
        design.setCompany(company);
        design.setOwner(owner);
        design.setCustomer(customer);
        design.setProperty(property);
        return design;
    }

    private Design existingCopy() {
        Design design = new Design();
        design.setName("现代简约方案（副本）");
        return design;
    }

    private Space space(String name) {
        Space space = new Space();
        space.setSpaceId(11L);
        space.setName(name);
        space.setArea(new BigDecimal("28.50"));
        space.setPerimeter(new BigDecimal("22.00"));
        space.setImages(List.of("space-image"));
        return space;
    }

    private DesignItem item(Space space) {
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setItemId(31L);
        DesignItem item = new DesignItem();
        item.setItemId(21L);
        item.setSpace(space);
        item.setCatalogItem(catalogItem);
        item.setName("墙砖铺贴");
        item.setAmount(new BigDecimal("1200.00"));
        item.setCost(new BigDecimal("800.00"));
        item.setImages(List.of("item-image"));
        return item;
    }

    private Employee employee(Long id, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setRole(role);
        return employee;
    }
}
