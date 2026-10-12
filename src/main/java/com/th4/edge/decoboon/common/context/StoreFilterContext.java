package com.th4.edge.decoboon.common.context;

import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.stereotype.Component;

/**
 * 门店维度解析（DEC-067）。
 *
 * <p>用于 Provider 层：门店维度经 provider 实例的 {@code @AutoInject} 字段绑定（`GAP-048`：
 * 请求模型不承载 HTTP 参数），再由此解析为生效门店并完成越权校验。判定实现统一在
 * {@link StoreScope#resolve}，统计、客户、项目、财务各域共用，禁止再各写一套。</p>
 */
@Component
public class StoreFilterContext {

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final StoreRepository storeRepository;

    public StoreFilterContext(TenantContext tenantContext,
                              DataScopeContext dataScopeContext,
                              StoreRepository storeRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.storeRepository = storeRepository;
    }

    /**
     * 解析生效门店维度（不传 / {@code null} / {@code 0} 等价为全部门店；非法门店 403/404）。
     *
     * @return 生效门店 ID；{@link StoreScope#ALL_STORES} 表示不限门店
     */
    public Long resolve(Employee principal, Long requestedStoreId) {
        Employee current = tenantContext.requireEmployee(principal);
        return StoreScope.resolve(dataScopeContext.resolve(current), requestedStoreId, storeRepository);
    }
}
