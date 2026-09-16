package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.model.Team;

import java.util.List;

/**
 * 团队仓储。
 */
public interface TeamRepository extends JpaRepository<Team, Long> {

    Team findByTeamIdAndCompany_ComId(Long teamId, Long comId);

    List<Team> findByCompany_ComIdOrderByName(Long comId);

    List<Team> findByCompany_ComIdAndStore_StoreIdOrderByName(Long comId, Long storeId);
}
