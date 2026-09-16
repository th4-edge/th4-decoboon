package com.th4.edge.decoboon.catalog.repository.detail;

import com.th4.edge.decoboon.catalog.model.detail.ConstructionItemDetail;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConstructionItemDetailRepository extends JpaRepository<ConstructionItemDetail, Long> {
    Optional<ConstructionItemDetail> findByItem_ItemId(Long itemId);
}
