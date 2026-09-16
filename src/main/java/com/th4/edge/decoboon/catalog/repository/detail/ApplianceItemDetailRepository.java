package com.th4.edge.decoboon.catalog.repository.detail;

import com.th4.edge.decoboon.catalog.model.detail.ApplianceItemDetail;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplianceItemDetailRepository extends JpaRepository<ApplianceItemDetail, Long> {
    Optional<ApplianceItemDetail> findByItem_ItemId(Long itemId);
}
