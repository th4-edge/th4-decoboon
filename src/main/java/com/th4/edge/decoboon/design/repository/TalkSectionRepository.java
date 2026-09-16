package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 谈单页仓储。 */
public interface TalkSectionRepository extends JpaRepository<TalkSection, Long>,
        JpaSpecificationExecutor<TalkSection> {

    List<TalkSection> findByTalk_TalkIdOrderBySortAsc(Long talkId);

    long deleteByTalk_TalkId(Long talkId);
}
