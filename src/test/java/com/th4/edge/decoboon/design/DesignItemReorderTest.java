package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.service.items.reorder.support.Reorder;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 设计分项纯排序回归：只更新 sort，且整单 itemId 必须与方案分项完全一致。 */
class DesignItemReorderTest {

    private final DesignContext designContext = mock(DesignContext.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);

    private final Reorder reorder = new Reorder(designContext, itemRepository, spaceRepository);

    @Test
    void shouldWriteContiguousSortInRequestedOrder() {
        Design design = new Design();
        design.setDesignId(100L);
        Employee designer = new Employee();
        DesignItem first = item(301L, 0);
        DesignItem second = item(302L, 1);
        DesignItem third = item(303L, 2);

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of(first, second, third));
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(100L)).thenReturn(List.of());

        ReflectionTestUtils.setField(reorder, "designId", 100L);
        ReflectionTestUtils.setField(reorder, "employee", designer);
        ReflectionTestUtils.setField(reorder, "itemIds", List.of(303L, 301L, 302L));

        Design result = reorder.call(new Design());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DesignItem>> saved = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(DesignItem::getItemId).containsExactly(303L, 301L, 302L);
        assertThat(saved.getValue()).extracting(DesignItem::getSort).containsExactly(0, 1, 2);
        assertThat(result.getItems()).extracting(DesignItem::getItemId).containsExactly(303L, 301L, 302L);
    }

    @Test
    void shouldRejectIncompleteOrderWithoutWriting() {
        Design design = new Design();
        design.setDesignId(100L);
        Employee designer = new Employee();

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(100L))
                .thenReturn(List.of(item(301L, 0), item(302L, 1)));

        ReflectionTestUtils.setField(reorder, "designId", 100L);
        ReflectionTestUtils.setField(reorder, "employee", designer);
        ReflectionTestUtils.setField(reorder, "itemIds", List.of(302L));

        assertThatThrownBy(() -> reorder.call(new Design()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("分项排序数据不完整");
        verify(itemRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectDuplicateOrForeignItemIdWithoutWriting() {
        Design design = new Design();
        design.setDesignId(100L);
        Employee designer = new Employee();

        when(designContext.requireDesign(100L, designer)).thenReturn(design);
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(100L))
                .thenReturn(List.of(item(301L, 0), item(302L, 1)));

        ReflectionTestUtils.setField(reorder, "designId", 100L);
        ReflectionTestUtils.setField(reorder, "employee", designer);
        ReflectionTestUtils.setField(reorder, "itemIds", List.of(301L, 301L));

        assertThatThrownBy(() -> reorder.call(new Design()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("分项排序数据不完整");
        verify(itemRepository, never()).saveAll(any());
    }

    private DesignItem item(Long itemId, int sort) {
        DesignItem item = new DesignItem();
        item.setItemId(itemId);
        item.setSort(sort);
        return item;
    }
}
