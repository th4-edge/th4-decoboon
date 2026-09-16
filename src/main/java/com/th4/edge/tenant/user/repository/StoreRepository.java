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

    long countByCompany_ComId(Long comId);
}
