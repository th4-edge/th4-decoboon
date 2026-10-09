package com.th4.edge.decoboon.designtemplate.repository;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DesignTemplateRoomRepository extends JpaRepository<DesignTemplateRoom, Long> {

    List<DesignTemplateRoom> findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(Long versionId, Long comId);

    Optional<DesignTemplateRoom> findByVersion_VersionIdAndCompany_ComIdAndRoomKey(Long versionId, Long comId, String roomKey);

    @Query("select r.version.versionId, count(r) from DesignTemplateRoom r "
            + "where r.company.comId = :comId and r.version.versionId in :versionIds group by r.version.versionId")
    List<Object[]> countByVersionIds(@Param("comId") Long comId, @Param("versionIds") Collection<Long> versionIds);
}
