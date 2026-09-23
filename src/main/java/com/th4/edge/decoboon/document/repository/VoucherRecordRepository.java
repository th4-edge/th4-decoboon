package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 凭证记录仓储。 */
public interface VoucherRecordRepository extends JpaRepository<VoucherRecord, Long>,
        JpaSpecificationExecutor<VoucherRecord> {

    Optional<VoucherRecord> findByVoucherIdAndCompany_ComId(Long voucherId, Long comId);

    List<VoucherRecord> findByCompany_ComIdOrderByVoucherIdDesc(Long comId);

    long countByCompany_ComIdAndVoucherNoStartingWith(Long comId, String prefix);

    boolean existsByCompany_ComIdAndVoucherNo(Long comId, String voucherNo);
}
