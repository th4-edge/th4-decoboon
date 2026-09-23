package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.PartnerCredit;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 供应商往来余额仓储。 */
public interface PartnerCreditRepository extends JpaRepository<PartnerCredit, Long>,
        JpaSpecificationExecutor<PartnerCredit> {

    List<PartnerCredit> findByCompany_ComIdAndPartner_PartnerIdOrderByPartnerCreditIdDesc(Long comId, Long partnerId);

    List<PartnerCredit> findByCompany_ComIdOrderByPartnerCreditIdDesc(Long comId);
}
