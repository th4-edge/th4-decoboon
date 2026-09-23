package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.model.VoucherItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/** 凭证明细仓储。 */
public interface VoucherItemRepository extends JpaRepository<VoucherItem, Long> {

    List<VoucherItem> findByVoucher_VoucherIdOrderByItemId(Long voucherId);

    List<VoucherItem> findByCompany_ComIdAndSourceIdIn(Long comId, Collection<Long> sourceIds);
}
