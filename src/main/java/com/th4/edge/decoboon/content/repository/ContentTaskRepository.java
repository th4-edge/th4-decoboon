package com.th4.edge.decoboon.content.repository;

import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** 内容任务仓储。 */
public interface ContentTaskRepository extends JpaRepository<ContentTask, Long> {
    Optional<ContentTask> findByTaskIdAndCompany_ComId(Long taskId, Long comId);
    List<ContentTask> findByCompany_ComIdOrderByTaskIdDesc(Long comId);
    List<ContentTask> findByCompany_ComIdAndStatusNotOrderByTaskIdDesc(Long comId, com.th4.edge.decoboon.content.configure.ContentTaskStatus status);
}
