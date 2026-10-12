package com.th4.edge.decoboon.common.context;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;

/**
 * 门店维度统一工具（DEC-065 / DEC-067 BOSS-STORE-10）。
 *
 * <p>全产品唯一门店哨兵与判定实现：finance 域与统计/业务域共用，禁止各自再定义。</p>
 *
 * <p>约定「{@link #ALL_STORES}（0）= 全部门店」，与「不传」三者等价（不传 / {@code null} / {@code 0}）；
 * 请求层只允许不传或真实门店 ID，禁止把前端状态值 {@code '0'} 当门店 ID 传入。
 * 用 {@code 0} 而不用 {@code null} 表达「不限门店」，是因为 {@code Long} 包装类型在 mock 上下文下的
 * 默认值是 {@code 0}，若以 {@code null} 表达会被误判成「指定门店 0」而过滤掉全部数据（传 0 静默返回空）。</p>
 */
public final class StoreScope {

    /** 门店维度哨兵：不限门店（全部门店）。 */
    public static final long ALL_STORES = 0L;

    private StoreScope() {
    }

    /** 归一化请求值：不传 / {@code null} / {@code 0} 等价为「全部门店」。 */
    public static Long normalize(Long requestedStoreId) {
        return requestedStoreId == null || requestedStoreId == ALL_STORES ? ALL_STORES : requestedStoreId;
    }

    /** 是否不限门店。 */
    public static boolean isAllStores(Long storeFilter) {
        return storeFilter == null || storeFilter == ALL_STORES;
    }

    /** 门店维度判定：{@code storeFilter} 不限门店时保留全部单据（含 {@code store} 为空的记录）。 */
    public static boolean inStore(Long recordStoreId, Long storeFilter) {
        if (isAllStores(storeFilter)) {
            return true;
        }
        return storeFilter.equals(recordStoreId);
    }

    /**
     * 解析生效的门店维度（含越权校验）。
     *
     * <ul>
     *   <li>企业范围（BOSS / 总部财务）：不限门店，或本企业任一门店；他企业门店与不存在的门店一律拒绝；</li>
     *   <li>门店范围（门店财务 / 店长）：恒为本店；传入非本店一律拒绝，不得静默忽略。</li>
     * </ul>
     *
     * @return 生效门店 ID；{@link #ALL_STORES} 表示不限门店
     */
    public static Long resolve(DataScope scope, Long requestedStoreId, StoreRepository storeRepository) {
        if (scope == null || scope.type() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未配置数据范围"));
        }
        Long requested = normalize(requestedStoreId);
        if (scope.type() == DataScopeType.Company) {
            if (requested == ALL_STORES) {
                return ALL_STORES;
            }
            Store store = storeRepository == null ? null : storeRepository.findByStoreId(requested);
            if (store == null || store.getCompany() == null
                    || !scope.comId().equals(store.getCompany().getComId())) {
                throw new DefinedException(new DefaultErrorCode("403", "门店不存在或无权访问"));
            }
            return requested;
        }
        Long ownStoreId = scope.storeId();
        if (ownStoreId == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未归属门店"));
        }
        if (requested == ALL_STORES || requested.equals(ownStoreId)) {
            return ownStoreId;
        }
        throw new DefinedException(new DefaultErrorCode("403", "门店不存在或无权访问"));
    }

    /** 单据门店归属。 */
    public static Long storeIdOf(Store store) {
        return store == null ? null : store.getStoreId();
    }

    /**
     * 项目门店归属：优先项目自身门店，缺失时回退客户门店（DEC-067 BOSS-STORE-09 特例）。
     * 与既有项目范围判定（按客户门店）保持一致，避免历史项目 {@code store_id} 为空时被漏统计。
     */
    public static Long storeIdOf(Project project) {
        if (project == null) {
            return null;
        }
        Long storeId = storeIdOf(project.getStore());
        if (storeId != null) {
            return storeId;
        }
        return project.getCustomer() == null ? null : storeIdOf(project.getCustomer().getStore());
    }
}
