package com.th4.edge.decoboon.catalog;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.catalog.context.CatalogMutationContext;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogMutationContextTest {

    @Test
    void lockWrapsAnIndependentTransactionAndAlwaysUnlocks() {
        DistributedLock lock = mock(DistributedLock.class);
        TransactionTemplate transaction = mock(TransactionTemplate.class);
        when(lock.tryLock("catalog-item:1:20", 1000, 30000)).thenReturn(true);
        when(transaction.execute(org.mockito.ArgumentMatchers.<TransactionCallback<String>>any()))
                .thenAnswer(invocation -> invocation.<TransactionCallback<String>>getArgument(0)
                        .doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));

        String result = new CatalogMutationContext(lock, transaction).execute(1L, 20L, () -> "saved");

        assertThat(result).isEqualTo("saved");
        verify(lock).unlock("catalog-item:1:20");
    }

    @Test
    void lockTimeoutReturnsStableConflictWithoutStartingTransaction() {
        DistributedLock lock = mock(DistributedLock.class);
        TransactionTemplate transaction = mock(TransactionTemplate.class);
        when(lock.tryLock("catalog-item:1:20", 1000, 30000)).thenReturn(false);

        assertThatThrownBy(() -> new CatalogMutationContext(lock, transaction).execute(1L, 20L, () -> "saved"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("正在被处理");

        verify(transaction, never()).execute(any());
        verify(lock, never()).unlock(any());
    }

    @Test
    void revisionUniqueConflictIsMappedTo409() {
        DistributedLock lock = mock(DistributedLock.class);
        TransactionTemplate transaction = mock(TransactionTemplate.class);
        when(lock.tryLock(eq("catalog-item:1:20"), eq(1000L), eq(30000L))).thenReturn(true);
        when(transaction.execute(org.mockito.ArgumentMatchers.<TransactionCallback<String>>any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for uk_catalog_revision_item_revision"));

        assertThatThrownBy(() -> new CatalogMutationContext(lock, transaction).execute(1L, 20L, () -> "saved"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("刷新后重试");

        verify(lock).unlock("catalog-item:1:20");
    }
}
