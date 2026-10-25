package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** 报价项目写操作协调：先取 item 级锁，再在锁内开启独立事务。 */
@Component
public class CatalogMutationContext {

    private final DistributedLock distributedLock;
    private final TransactionTemplate transactionTemplate;

    public CatalogMutationContext(DistributedLock distributedLock, TransactionTemplate transactionTemplate) {
        this.distributedLock = distributedLock;
        this.transactionTemplate = transactionTemplate;
    }

    public <T> T execute(Long comId, Long itemId, Supplier<T> action) {
        String lockKey = "catalog-item:" + comId + ":" + itemId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "报价项目正在被处理，请稍后重试"));
        }
        try {
            return transactionTemplate.execute(status -> action.get());
        } catch (DataIntegrityViolationException ex) {
            if (isRevisionConflict(ex)) {
                throw new DefinedException(new DefaultErrorCode("409", "报价项目已被其他人更新，请刷新后重试"));
            }
            throw ex;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private boolean isRevisionConflict(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        return message != null && message.contains("uk_catalog_revision_item_revision");
    }
}
