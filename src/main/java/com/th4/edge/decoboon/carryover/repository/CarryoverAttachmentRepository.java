package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverAttachment;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 承接项目附件仓储。 */
public interface CarryoverAttachmentRepository extends JpaRepository<CarryoverAttachment, Long>,
        JpaSpecificationExecutor<CarryoverAttachment> {

    List<CarryoverAttachment> findByItem_ItemIdOrderByAttachmentIdAsc(Long itemId);

    void deleteByItem_ItemId(Long itemId);
}
