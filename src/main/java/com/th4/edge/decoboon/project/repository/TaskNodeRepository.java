package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.configure.TaskNodeKey;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.model.TaskNode;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 施工任务节点仓储。 */
public interface TaskNodeRepository extends JpaRepository<TaskNode, Long>,
        JpaSpecificationExecutor<TaskNode> {

    List<TaskNode> findByTaskOrderByCreateTime_DateTimeAsc(ProjectTask task);

    Optional<TaskNode> findByTaskAndNodeKey(ProjectTask task, TaskNodeKey nodeKey);
}
