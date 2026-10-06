package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.model.VoucherRevision;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 凭证修订轨迹仓储。 */
public interface VoucherRevisionRepository extends JpaRepository<VoucherRevision, Long> {

    List<VoucherRevision> findByCompany_ComIdAndVoucher_VoucherIdOrderByRevisionIdDesc(Long comId, Long voucherId);
}
