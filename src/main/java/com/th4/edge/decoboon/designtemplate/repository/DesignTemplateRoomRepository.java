package com.th4.edge.decoboon.designtemplate.repository;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignTemplateRoomRepository extends JpaRepository<DesignTemplateRoom, Long> {

    List<DesignTemplateRoom> findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(Long versionId, Long comId);

    Optional<DesignTemplateRoom> findByVersion_VersionIdAndCompany_ComIdAndRoomKey(Long versionId, Long comId, String roomKey);
}
