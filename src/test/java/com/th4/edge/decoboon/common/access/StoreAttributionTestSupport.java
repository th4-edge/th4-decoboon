package com.th4.edge.decoboon.common.access;

import com.th4.edge.decoboon.common.context.StoreAttributionContext;
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;

/**
 * 单元测试用门店归属解析器注入（`DEC-070`）。
 *
 * <p>生产代码里 {@code storeAttributionContext} 由 Spring 注入；单元测试直接构造上下文（无容器），
 * 需要用本工具把解析器塞进对应字段。解析器对「挂在持久化聚合上的门店」做宽松校验，
 * 因此 mock 仓储即可满足单元测试的既有夹具。</p>
 */
public final class StoreAttributionTestSupport {

    private StoreAttributionTestSupport() {
    }

    public static StoreAttributionContext resolver() {
        return new StoreAttributionContext(mock(StoreRepository.class), mock(StoreFilterContext.class));
    }

    /** 把解析器（以及财务域统一写入方法所在上下文）注入目标的对应字段（字段不存在时忽略）。 */
    public static <T> T inject(T target) {
        try {
            ReflectionTestUtils.setField(target, "storeAttributionContext", resolver());
        } catch (IllegalArgumentException ignored) {
            // 目标类没有该字段（无需注入）
        }
        try {
            ReflectionTestUtils.setField(target, "financeAccessContext", financeAccess());
        } catch (IllegalArgumentException ignored) {
            // 目标类没有该字段（无需注入）
        }
        return target;
    }

    /** 财务域归属写入上下文（`applyPayableStore` / `applyOwnership` 的同一入口）。 */
    public static com.th4.edge.decoboon.finance.context.FinanceAccessContext financeAccess() {
        com.th4.edge.decoboon.finance.context.FinanceAccessContext context =
                new com.th4.edge.decoboon.finance.context.FinanceAccessContext(
                        mock(com.th4.edge.tenant.user.context.TenantContext.class),
                        mock(com.th4.edge.tenant.user.access.context.DataScopeContext.class),
                        mock(com.th4.edge.decoboon.project.repository.ProjectRepository.class),
                        mock(com.th4.edge.decoboon.finance.repository.LedgerRepository.class),
                        mock(StoreRepository.class));
        ReflectionTestUtils.setField(context, "storeAttributionContext", resolver());
        return context;
    }
}
