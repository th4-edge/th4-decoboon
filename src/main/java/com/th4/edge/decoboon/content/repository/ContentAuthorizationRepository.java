package com.th4.edge.decoboon.content.repository;

import com.th4.edge.decoboon.content.configure.ContentAuthorizationPurpose;
import com.th4.edge.decoboon.content.configure.ContentAuthorizationStatus;
import com.th4.edge.decoboon.content.model.ContentAuthorization;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 内容素材授权仓储。 */
public interface ContentAuthorizationRepository extends JpaRepository<ContentAuthorization, Long> {

    Optional<ContentAuthorization> findByAuthIdAndCompany_ComId(Long authId, Long comId);

    List<ContentAuthorization> findByAsset_AssetIdOrderByAuthIdDesc(Long assetId);

    List<ContentAuthorization> findByAsset_AssetIdAndPurposeAndStatus(
            Long assetId, ContentAuthorizationPurpose purpose, ContentAuthorizationStatus status);

    List<ContentAuthorization> findByCompany_ComIdAndStatusAndValidToBefore(
            Long comId, ContentAuthorizationStatus status, java.time.LocalDate date);
}
