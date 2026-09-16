package com.th4.edge.decoboon.catalog.repository.detail;

import com.th4.edge.decoboon.catalog.model.detail.CustomFurnitureItemDetail;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomFurnitureItemDetailRepository extends JpaRepository<CustomFurnitureItemDetail, Long> {
    Optional<CustomFurnitureItemDetail> findByItem_ItemId(Long itemId);
}
