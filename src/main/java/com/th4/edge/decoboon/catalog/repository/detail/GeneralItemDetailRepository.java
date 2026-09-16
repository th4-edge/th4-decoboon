package com.th4.edge.decoboon.catalog.repository.detail;

import com.th4.edge.decoboon.catalog.model.detail.GeneralItemDetail;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GeneralItemDetailRepository extends JpaRepository<GeneralItemDetail, Long> {
    Optional<GeneralItemDetail> findByItem_ItemId(Long itemId);
}
