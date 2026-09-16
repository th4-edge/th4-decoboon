package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 报价项目自动编号格式回归测试。 */
class CatalogCodeContextTest {

    @Test
    void firstCodeMustUseHumanReadablePrefixAndSequence() {
        CatalogItemRepository repository = mock(CatalogItemRepository.class);
        when(repository.findFirstByCompany_ComIdAndTypeOrderByCodeDesc(1L, ItemType.Construction))
                .thenReturn(Optional.empty());

        String code = new CatalogCodeContext(repository).nextCode(1L, ItemType.Construction);

        assertThat(code).isEqualTo("SG-0001");
    }

    @Test
    void nextCodeMustIncrementExistingSequence() {
        CatalogItem latest = new CatalogItem();
        latest.setCode("SG-0009");
        CatalogItemRepository repository = mock(CatalogItemRepository.class);
        when(repository.findFirstByCompany_ComIdAndTypeOrderByCodeDesc(1L, ItemType.Construction))
                .thenReturn(Optional.of(latest));

        String code = new CatalogCodeContext(repository).nextCode(1L, ItemType.Construction);

        assertThat(code).isEqualTo("SG-0010");
    }
}
