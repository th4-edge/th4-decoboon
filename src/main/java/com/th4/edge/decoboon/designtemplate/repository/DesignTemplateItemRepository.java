package com.th4.edge.decoboon.designtemplate.repository;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DesignTemplateItemRepository extends JpaRepository<DesignTemplateItem, Long> {

    List<DesignTemplateItem> findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(Long versionId, Long comId);

    long countByVersion_VersionIdAndCompany_ComIdAndRoomKey(Long versionId, Long comId, String roomKey);

    @Query("select i.version.versionId, count(i) from DesignTemplateItem i "
            + "where i.company.comId = :comId and i.version.versionId in :versionIds group by i.version.versionId")
    List<Object[]> countByVersionIds(@Param("comId") Long comId, @Param("versionIds") Collection<Long> versionIds);
}
