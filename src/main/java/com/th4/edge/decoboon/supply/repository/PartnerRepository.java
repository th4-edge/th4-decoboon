package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 合作方仓储。 */
public interface PartnerRepository extends JpaRepository<Partner, Long>,
        JpaSpecificationExecutor<Partner> {
    @EntityGraph(attributePaths = "brands")
    List<Partner> findByCompany_ComIdOrderByPartnerIdDesc(Long comId);
    @EntityGraph(attributePaths = "brands")
    List<Partner> findByCompany_ComIdAndTypeAndStatusOrderByName(Long comId, PartnerType type, PartnerStatus status);
    Partner findByCompany_ComIdAndName(Long comId, String name);

    /** 分页后一次性初始化品牌集合，避免逐行懒加载或集合 JOIN 分页失真。 */
    @Query("select distinct p from Partner p left join fetch p.brands "
            + "where p.company.comId = :comId and p.partnerId in :partnerIds")
    List<Partner> findDetailsByIdIn(@Param("comId") Long comId,
                                    @Param("partnerIds") Collection<Long> partnerIds);
}
