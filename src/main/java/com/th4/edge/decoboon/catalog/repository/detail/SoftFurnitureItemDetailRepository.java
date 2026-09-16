package com.th4.edge.decoboon.catalog.repository.detail;

import com.th4.edge.decoboon.catalog.model.detail.SoftFurnitureItemDetail;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SoftFurnitureItemDetailRepository extends JpaRepository<SoftFurnitureItemDetail, Long> {
    Optional<SoftFurnitureItemDetail> findByItem_ItemId(Long itemId);
}
