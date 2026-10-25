package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 凭证记录仓储。 */
public interface VoucherRecordRepository extends JpaRepository<VoucherRecord, Long>,
        JpaSpecificationExecutor<VoucherRecord> {

    Optional<VoucherRecord> findByVoucherIdAndCompany_ComId(Long voucherId, Long comId);

    @EntityGraph(attributePaths = {"project", "worker", "generatedBy", "items"})
    Optional<VoucherRecord> findDetailByCompany_ComIdAndVoucherId(Long comId, Long voucherId);

    @EntityGraph(attributePaths = {"project", "worker", "generatedBy", "items"})
    @Query("select distinct v from VoucherRecord v where v.voucherId in :ids")
    List<VoucherRecord> findDetailsByIdIn(@Param("ids") Collection<Long> ids);

    List<VoucherRecord> findByCompany_ComIdOrderByVoucherIdDesc(Long comId);

    List<VoucherRecord> findByCompany_ComIdAndSourceTypeAndSourceIdOrderByVoucherIdDesc(
            Long comId, String sourceType, Long sourceId);

    long countByCompany_ComIdAndVoucherNoStartingWith(Long comId, String prefix);

    boolean existsByCompany_ComIdAndVoucherNo(Long comId, String voucherNo);
}
