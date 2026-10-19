package com.th4.edge.decoboon.statistic.repository;

import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/** 月度经营目标仓储（DEC-076：公司目标 + 门店目标两层）。 */
public interface BusinessTargetRepository extends JpaRepository<BusinessTarget, Long> {

    /** 公司级目标：store 为空。 */
    Optional<BusinessTarget> findByCompany_ComIdAndStoreIsNullAndTargetMonth(Long comId, LocalDate targetMonth);

    /** 门店级目标：company + store + month 唯一。 */
    Optional<BusinessTarget> findByCompany_ComIdAndStore_StoreIdAndTargetMonth(
            Long comId, Long storeId, LocalDate targetMonth);
}
