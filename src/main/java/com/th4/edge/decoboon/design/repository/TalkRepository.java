package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 谈单 PPT 仓储。 */
public interface TalkRepository extends JpaRepository<Talk, Long>,
        JpaSpecificationExecutor<Talk> {

    boolean existsByDesign_DesignId(Long designId);

    List<Talk> findByCompany_ComIdOrderByTalkIdDesc(Long comId);
}
