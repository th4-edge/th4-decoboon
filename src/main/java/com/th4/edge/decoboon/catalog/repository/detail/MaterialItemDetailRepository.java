package com.th4.edge.decoboon.catalog.repository.detail;

import com.th4.edge.decoboon.catalog.model.detail.MaterialItemDetail;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MaterialItemDetailRepository extends JpaRepository<MaterialItemDetail, Long> {
    Optional<MaterialItemDetail> findByItem_ItemId(Long itemId);
}
