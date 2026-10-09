package com.th4.edge.decoboon.content.repository;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 内容素材仓储。 */
public interface ContentAssetRepository extends JpaRepository<ContentAsset, Long> {

    Optional<ContentAsset> findByAssetIdAndCompany_ComId(Long assetId, Long comId);

    List<ContentAsset> findByCompany_ComIdOrderByAssetIdDesc(Long comId);

    List<ContentAsset> findByCompany_ComIdAndProject_ProjectIdOrderByAssetIdDesc(Long comId, Long projectId);

    List<ContentAsset> findByCompany_ComIdAndStatusOrderByAssetIdDesc(Long comId, ContentAssetStatus status);
}
