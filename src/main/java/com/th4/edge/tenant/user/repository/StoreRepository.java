package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.model.Store;

import java.util.List;

/**
 * 门店仓储。
 */
public interface StoreRepository extends JpaRepository<Store, Long> {

    Store findByStoreId(Long storeId);

    Store findByCompany_ComIdAndDefaultStoreTrue(Long comId);

    List<Store> findByCompany_ComId(Long comId);

    /** 门店列表（P0D-04）：按 `sort` 升序、同值按 `storeId` 升序。 */
    List<Store> findByCompany_ComIdOrderBySortAscStoreIdAsc(Long comId);

    /** 指定状态的门店数（停用校验：最后一家启用门店不可停用）。 */
    long countByCompany_ComIdAndStatus(Long comId, com.th4.edge.tenant.user.configure.StoreStatus status);

    long countByCompany_ComId(Long comId);
}
